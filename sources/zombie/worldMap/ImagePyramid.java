/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.ImageData;
import zombie.core.textures.MipMapLevel;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.utils.DirectBufferAllocator;
import zombie.debug.DebugType;
import zombie.worldMap.FileTask_LoadImagePyramidTexture;

public final class ImagePyramid {
    static final int TILE_SIZE = 256;
    String directory;
    String zipFile;
    FileSystem zipFs;
    final HashMap<String, PyramidTexture> textures = new HashMap();
    final HashSet<String> missing = new HashSet();
    HashMap<String, FileTask_LoadImagePyramidTexture> fileTasks = new HashMap();
    int requestNumber;
    int minX;
    int minY;
    int maxX;
    int maxY;
    int imageWidth = -1;
    int imageHeight = -1;
    float resolution = 1.0f;
    int clampS = 33071;
    int clampT = 33071;
    int minFilter = 9729;
    int magFilter = 9728;
    int minZ;
    int maxZ;
    int maxTextures = 100;
    static int maxRequestNumber = Core.debug ? 10000 : Integer.MAX_VALUE;
    int texturesLoadedThisFrame;
    boolean destroyed;
    static final ThreadLocal<TIntObjectHashMap<ImageKeyXYZ>> TL_imageKeys = ThreadLocal.withInitial(TIntObjectHashMap::new);
    static final HashSet<String> s_required = new HashSet();
    static final int[] s_tilesCoveringCell = new int[4];
    final ConcurrentLinkedQueue<FileTaskRequest> queueLoading = new ConcurrentLinkedQueue();
    final ConcurrentLinkedQueue<FileTaskResult> queueRender = new ConcurrentLinkedQueue();
    final ConcurrentLinkedQueue<FileTask_LoadImagePyramidTexture> queueCalled = new ConcurrentLinkedQueue();
    final ConcurrentLinkedQueue<String> queueCancel = new ConcurrentLinkedQueue();

    static String getKey(int x, int y, int z) {
        TIntObjectHashMap<ImageKeyXYZ> map = TL_imageKeys.get();
        ImageKeyXYZ xyz = map.get(x);
        if (xyz == null) {
            xyz = new ImageKeyXYZ();
            map.put(x, xyz);
        }
        return xyz.get(x, y, z);
    }

    public void setDirectory(String directory) {
        if (this.zipFile != null) {
            this.zipFile = null;
            if (this.zipFs != null) {
                try {
                    this.zipFs.close();
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                this.zipFs = null;
            }
        }
        this.directory = directory;
    }

    public void setZipFile(String zipFile) {
        this.directory = null;
        this.zipFile = zipFile;
        this.zipFs = this.openZipFile();
        this.readInfoFile();
        if (this.imageWidth == -1) {
            this.imageWidth = this.maxX - this.minX;
            this.imageHeight = this.maxY - this.minY;
        }
        this.resolution = (float)(this.maxX - this.minX) / (float)this.imageWidth;
        this.minZ = Integer.MAX_VALUE;
        this.maxZ = Integer.MIN_VALUE;
        if (this.zipFs != null) {
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(this.zipFs.getPath("/", new String[0]));){
                for (Path path : dstrm) {
                    if (!Files.isDirectory(path, new LinkOption[0])) continue;
                    int z = PZMath.tryParseInt(path.getFileName().toString(), -1);
                    this.minZ = PZMath.min(this.minZ, z);
                    this.maxZ = PZMath.max(this.maxZ, z);
                }
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
            }
        }
    }

    public boolean isValidTile(int x, int y, int z) {
        if (z < this.minZ || z > this.maxZ) {
            return false;
        }
        if (x < 0 || y < 0) {
            return false;
        }
        int scaledDownImageWidth = this.imageWidth / (1 << z);
        int scaledDownImageHeight = this.imageHeight / (1 << z);
        return x <= scaledDownImageWidth / 256 && y <= scaledDownImageHeight / 256;
    }

    public Texture getImage(int x, int y, int z) {
        String key = ImagePyramid.getKey(x, y, z);
        if (this.missing.contains(key)) {
            return null;
        }
        File file = new File(this.directory, String.format(Locale.ENGLISH, "%s%d%stile%dx%d.png", File.separator, z, File.separator, x, y));
        if (!file.exists()) {
            this.missing.add(key);
            return null;
        }
        return Texture.getSharedTexture(file.getAbsolutePath());
    }

    public PyramidTexture getTexture(int x, int y, int z) {
        this.checkQueue();
        if (!this.isValidTile(x, y, z)) {
            return null;
        }
        String key = ImagePyramid.getKey(x, y, z);
        if (this.textures.containsKey(key)) {
            PyramidTexture pyramidTexture = this.textures.get(key);
            pyramidTexture.requestNumber = this.requestNumber++;
            pyramidTexture.requiredThisFrame = true;
            if (this.requestNumber >= maxRequestNumber) {
                this.resetRequestNumbers();
            }
            if (!(pyramidTexture.isReady() || pyramidTexture.state != TextureState.Init && pyramidTexture.state != TextureState.Cancelled)) {
                Path path = this.zipFs.getPath(String.valueOf(z), String.format(Locale.ENGLISH, "tile%dx%d.png", x, y));
                this.startLoadingFromZip(pyramidTexture, path);
            }
            return pyramidTexture;
        }
        if (this.missing.contains(key)) {
            return null;
        }
        if (this.zipFile != null) {
            if (this.zipFs == null || !this.zipFs.isOpen()) {
                return null;
            }
            try {
                Path path = this.zipFs.getPath(String.valueOf(z), String.format(Locale.ENGLISH, "tile%dx%d.png", x, y));
                if (!Files.exists(path, new LinkOption[0])) {
                    this.missing.add(key);
                    return null;
                }
                PyramidTexture pyramidTexture = this.checkTextureCache(x, y, z, key);
                if (pyramidTexture == null) {
                    return null;
                }
                this.startLoadingFromZip(pyramidTexture, path);
                return pyramidTexture;
            }
            catch (Exception ex) {
                this.missing.add(key);
                ExceptionLogger.logException(ex);
                return null;
            }
        }
        return null;
    }

    private void startLoadingFromZip(PyramidTexture pyramidTexture, Path path) {
        if (pyramidTexture.state == TextureState.Loading) {
            return;
        }
        pyramidTexture.state = TextureState.Loading;
        FileTaskRequest fileTaskRequest = new FileTaskRequest(this, pyramidTexture.key, path);
        this.queueLoading.add(fileTaskRequest);
    }

    private void cancelFileTask(FileTask_LoadImagePyramidTexture fileTask) {
        fileTask.cancelled = true;
        if (fileTask.asyncOp == -1) {
            this.fileTasks.remove(fileTask.key);
            return;
        }
        GameWindow.fileSystem.cancelAsync(fileTask.asyncOp);
        fileTask.asyncOp = -1;
    }

    public void checkCalledQueue() {
        FileTask_LoadImagePyramidTexture fileTask = this.queueCalled.poll();
        while (fileTask != null) {
            if (fileTask.cancelled) {
                if (fileTask == this.fileTasks.get(fileTask.key)) {
                    this.fileTasks.remove(fileTask.key);
                }
                this.disposeImageData(fileTask.imageData);
                fileTask.imageData = null;
            }
            fileTask = this.queueCalled.poll();
        }
    }

    public void checkCancelQueue() {
        String key = this.queueCancel.poll();
        while (key != null) {
            FileTask_LoadImagePyramidTexture fileTask = this.fileTasks.get(key);
            if (fileTask != null) {
                this.cancelFileTask(fileTask);
            }
            key = this.queueCancel.poll();
        }
    }

    public void checkLoadingQueue() {
        FileTaskRequest fileTaskRequest = this.queueLoading.poll();
        while (fileTaskRequest != null) {
            FileTask_LoadImagePyramidTexture old = this.fileTasks.get(fileTaskRequest.key);
            if (old != null) {
                this.cancelFileTask(old);
            }
            FileTask_LoadImagePyramidTexture fileTask = new FileTask_LoadImagePyramidTexture(this, fileTaskRequest.path, fileTaskRequest.key, GameWindow.fileSystem, null);
            this.fileTasks.put(fileTask.key, fileTask);
            fileTask.asyncOp = GameWindow.fileSystem.runAsync(fileTask);
            fileTaskRequest = this.queueLoading.poll();
        }
    }

    public PyramidTexture getReadyTexture(int x, int y, int z) {
        this.checkQueue();
        String key = ImagePyramid.getKey(x, y, z);
        if (this.textures.containsKey(key)) {
            PyramidTexture pyramidTexture = this.textures.get(key);
            if (!pyramidTexture.isReady()) {
                return null;
            }
            pyramidTexture.requestNumber = this.requestNumber++;
            pyramidTexture.requiredThisFrame = true;
            if (this.requestNumber >= maxRequestNumber) {
                this.resetRequestNumbers();
            }
            return pyramidTexture;
        }
        return null;
    }

    public PyramidTexture getLowerResTexture(int x, int y, int z) {
        PyramidTexture pyramidTexture;
        int y1;
        int div = 2;
        for (int z1 = z + 1; z1 <= this.maxZ; ++z1) {
            int y12;
            int x1 = PZMath.fastfloor((float)x / (float)div);
            PyramidTexture pyramidTexture2 = this.getReadyTexture(x1, y12 = PZMath.fastfloor((float)y / (float)div), z1);
            if (pyramidTexture2 != null && pyramidTexture2.isReady()) {
                return pyramidTexture2;
            }
            div *= 2;
        }
        int x1 = PZMath.fastfloor((float)x / (float)(div /= 2));
        if (this.isValidTile(x1, y1 = PZMath.fastfloor((float)y / (float)div), this.maxZ) && (pyramidTexture = this.getTexture(x1, y1, this.maxZ)) != null && pyramidTexture.isReady()) {
            return pyramidTexture;
        }
        return null;
    }

    public int getMinFilter() {
        return this.minFilter;
    }

    public int getMagFilter() {
        return this.magFilter;
    }

    public int getClampS() {
        return this.clampS;
    }

    public int getClampT() {
        return this.clampT;
    }

    private void replaceTextureData(PyramidTexture pyramidTexture, ImageData imageData) {
        if (GL.getCapabilities().GL_ARB_texture_compression) {
            // empty if block
        }
        int internalFormat = 6408;
        Texture.lastTextureID = pyramidTexture.textureId.getID();
        GL11.glBindTexture(3553, Texture.lastTextureID);
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        GL11.glTexImage2D(3553, 0, internalFormat, imageData.getWidthHW(), imageData.getHeightHW(), 0, 6408, 5121, imageData.getData().getBuffer());
        imageData.dispose();
    }

    public void generateFiles(String imageFile, String outputDirectory) throws Exception {
        ImageData imageData = new ImageData(imageFile);
        if (imageData == null) {
            return;
        }
        int tileSize = 256;
        int levels = 5;
        for (int level = 0; level < 5; ++level) {
            MipMapLevel mipMapLevel = imageData.getMipMapData(level);
            float width = (float)imageData.getWidth() / (float)(1 << level);
            float height = (float)imageData.getHeight() / (float)(1 << level);
            int columns = (int)Math.ceil(width / 256.0f);
            int rows = (int)Math.ceil(height / 256.0f);
            for (int row = 0; row < rows; ++row) {
                for (int col = 0; col < columns; ++col) {
                    BufferedImage bufferedImage = this.getBufferedImage(mipMapLevel, col, row, 256);
                    this.writeImageToFile(bufferedImage, outputDirectory, col, row, level);
                }
            }
        }
    }

    public FileSystem openZipFile() {
        try {
            return FileSystems.newFileSystem(Paths.get(this.zipFile, new String[0]));
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
            return null;
        }
    }

    public void generateZip(String imageFile, String zipFile) throws Exception {
        ImageData imageData = new ImageData(imageFile);
        if (imageData == null) {
            return;
        }
        int tileSize = 256;
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos);){
            int levels = 5;
            for (int level = 0; level < 5; ++level) {
                MipMapLevel mipMapLevel = imageData.getMipMapData(level);
                float width = (float)imageData.getWidth() / (float)(1 << level);
                float height = (float)imageData.getHeight() / (float)(1 << level);
                int columns = (int)Math.ceil(width / 256.0f);
                int rows = (int)Math.ceil(height / 256.0f);
                for (int row = 0; row < rows; ++row) {
                    for (int col = 0; col < columns; ++col) {
                        BufferedImage bufferedImage = this.getBufferedImage(mipMapLevel, col, row, 256);
                        this.writeImageToZip(bufferedImage, zos, col, row, level);
                    }
                }
                if (!(width <= 256.0f) || !(height <= 256.0f)) continue;
                break;
            }
        }
    }

    BufferedImage getBufferedImage(MipMapLevel mipMapLevel, int col, int row, int tileSize) {
        BufferedImage bufferedImage = new BufferedImage(tileSize, tileSize, 2);
        int[] rowABGR = new int[tileSize];
        IntBuffer intBuffer = mipMapLevel.getBuffer().asIntBuffer();
        for (int y = 0; y < tileSize; ++y) {
            intBuffer.get(col * tileSize + (row * tileSize + y) * mipMapLevel.width, rowABGR);
            for (int x = 0; x < tileSize; ++x) {
                int abgr = rowABGR[x];
                int r = abgr & 0xFF;
                int g = abgr >> 8 & 0xFF;
                int b = abgr >> 16 & 0xFF;
                int a = abgr >> 24 & 0xFF;
                rowABGR[x] = a << 24 | r << 16 | g << 8 | b;
            }
            bufferedImage.setRGB(0, y, tileSize, 1, rowABGR, 0, tileSize);
        }
        return bufferedImage;
    }

    void writeImageToFile(BufferedImage bufferedImage, String outputDirectory, int col, int row, int z) throws Exception {
        File file = new File(outputDirectory + File.separator + z);
        if (!file.exists() && !file.mkdirs()) {
            return;
        }
        file = new File(file, String.format(Locale.ENGLISH, "tile%dx%d.png", col, row));
        ImageIO.write((RenderedImage)bufferedImage, "png", file);
    }

    void writeImageToZip(BufferedImage bufferedImage, ZipOutputStream zos, int col, int row, int z) throws Exception {
        zos.putNextEntry(new ZipEntry(String.format(Locale.ENGLISH, "%d/tile%dx%d.png", z, col, row)));
        ImageIO.write((RenderedImage)bufferedImage, "PNG", zos);
        zos.closeEntry();
    }

    void startFrame() {
        this.texturesLoadedThisFrame = 0;
        for (PyramidTexture pyramidTexture : this.textures.values()) {
            pyramidTexture.requiredThisFrame = false;
        }
    }

    double calculateMetersPerTile(int z) {
        double originalFullResImageWidth = Math.ceil((double)this.imageWidth / 16.0) * 16.0;
        double fudge = originalFullResImageWidth / (double)this.imageWidth;
        double scaledDownImageWidth = originalFullResImageWidth / (double)(1 << z);
        scaledDownImageWidth = (int)scaledDownImageWidth;
        double resolution = fudge * (double)(this.maxX - this.minX) / scaledDownImageWidth;
        return 256.0 * resolution;
    }

    void calculateRequiredTiles(TIntArrayList rasterizeXY, int ptz) {
        s_required.clear();
        if (ptz < this.maxZ) {
            for (int i = 0; i < rasterizeXY.size() - 1; i += 2) {
                int pty;
                int ptx = rasterizeXY.getQuick(i);
                if (!this.isValidTile(ptx, pty = rasterizeXY.getQuick(i + 1), ptz)) continue;
                String key = ImagePyramid.getKey(ptx, pty, ptz);
                s_required.add(key);
                PyramidTexture pyramidTexture = this.textures.get(key);
                if (pyramidTexture == null) continue;
                pyramidTexture.requiredThisFrame = true;
            }
        }
        int required = s_required.size();
        int scaledDownImageWidth = this.imageWidth / (1 << this.maxZ);
        int scaledDownImageHeight = this.imageHeight / (1 << this.maxZ);
        int tileSpanX = (int)PZMath.ceil((float)scaledDownImageWidth / 256.0f);
        int tileSpanY = (int)PZMath.ceil((float)scaledDownImageHeight / 256.0f);
        this.maxTextures = PZMath.max(this.maxTextures, required += tileSpanX * tileSpanY);
    }

    boolean calculateTilesCoveringCellF(int cellX, int cellY, float metersPerTile, int[] out) {
        int clipX1 = PZMath.clamp(this.minX, cellX * 256, (cellX + 1) * 256);
        int clipY1 = PZMath.clamp(this.minY, cellY * 256, (cellY + 1) * 256);
        int clipX2 = PZMath.clamp(this.maxX, cellX * 256, (cellX + 1) * 256);
        int clipY2 = PZMath.clamp(this.maxY, cellY * 256, (cellY + 1) * 256);
        if (clipX1 == clipX2 || clipY1 == clipY2) {
            return false;
        }
        out[0] = PZMath.fastfloor((float)(cellX * 256 - this.minX) / metersPerTile);
        out[1] = PZMath.fastfloor((float)(cellY * 256 - this.minY) / metersPerTile);
        out[2] = PZMath.fastfloor((float)((cellX + 1) * 256 - this.minX) / metersPerTile);
        out[3] = PZMath.fastfloor((float)((cellY + 1) * 256 - this.minY) / metersPerTile);
        return true;
    }

    boolean calculateTilesCoveringCell(int cellX, int cellY, int ptz, int[] out) {
        float metersPerTile = (float)this.calculateMetersPerTile(ptz);
        return this.calculateTilesCoveringCellF(cellX, cellY, metersPerTile, out);
    }

    void calculateRequiredTilesForCells(TIntArrayList rasterizeXY, int ptz) {
        s_required.clear();
        if (ptz < this.maxZ) {
            float metersPerTile = (float)this.calculateMetersPerTile(ptz);
            for (int i = 0; i < rasterizeXY.size() - 1; i += 2) {
                int cellY;
                int cellX = rasterizeXY.get(i);
                if (!this.calculateTilesCoveringCellF(cellX, cellY = rasterizeXY.get(i + 1), metersPerTile, s_tilesCoveringCell)) continue;
                int tileMinX = s_tilesCoveringCell[0];
                int tileMinY = s_tilesCoveringCell[1];
                int tileMaxX = s_tilesCoveringCell[2];
                int tileMaxY = s_tilesCoveringCell[3];
                for (int pty = tileMinY; pty <= tileMaxY; ++pty) {
                    for (int ptx = tileMinX; ptx <= tileMaxX; ++ptx) {
                        if (!this.isValidTile(ptx, pty, ptz)) continue;
                        String key = ImagePyramid.getKey(ptx, pty, ptz);
                        s_required.add(key);
                        PyramidTexture pyramidTexture = this.textures.get(key);
                        if (pyramidTexture == null) continue;
                        pyramidTexture.requiredThisFrame = true;
                    }
                }
            }
        }
        int required = s_required.size();
        int scaledDownImageWidth = this.imageWidth / (1 << this.maxZ);
        int scaledDownImageHeight = this.imageHeight / (1 << this.maxZ);
        int tileSpanX = (int)PZMath.ceil((float)scaledDownImageWidth / 256.0f);
        int tileSpanY = (int)PZMath.ceil((float)scaledDownImageHeight / 256.0f);
        this.maxTextures = PZMath.max(this.maxTextures, required += tileSpanX * tileSpanY);
    }

    void endFrame() {
        for (PyramidTexture pyramidTexture : this.textures.values()) {
            if (pyramidTexture.z == this.maxZ || pyramidTexture.requiredThisFrame || pyramidTexture.state != TextureState.Loading) continue;
            pyramidTexture.state = TextureState.Cancelled;
            this.queueCancel.add(pyramidTexture.key);
        }
    }

    PyramidTexture checkTextureCache(int x, int y, int z, String key) {
        if (this.textures.size() < this.maxTextures) {
            PyramidTexture pyramidTexture = new PyramidTexture();
            pyramidTexture.x = x;
            pyramidTexture.y = y;
            pyramidTexture.z = z;
            pyramidTexture.key = key;
            pyramidTexture.requestNumber = this.requestNumber++;
            pyramidTexture.requiredThisFrame = true;
            this.textures.put(key, pyramidTexture);
            if (this.requestNumber >= maxRequestNumber) {
                this.resetRequestNumbers();
            }
            return pyramidTexture;
        }
        PyramidTexture oldest = null;
        int required = 0;
        for (PyramidTexture pyramidTexture : this.textures.values()) {
            if (pyramidTexture.z == this.maxZ) {
                ++required;
                continue;
            }
            if (pyramidTexture.requiredThisFrame) {
                if (!s_required.contains(pyramidTexture.key)) {
                    boolean bl = true;
                }
                ++required;
                continue;
            }
            if (oldest != null && oldest.requestNumber <= pyramidTexture.requestNumber) continue;
            oldest = pyramidTexture;
        }
        if (oldest == null) {
            if (z == this.maxZ) {
                ++this.maxTextures;
                PyramidTexture pyramidTexture = new PyramidTexture();
                pyramidTexture.x = x;
                pyramidTexture.y = y;
                pyramidTexture.z = z;
                pyramidTexture.key = key;
                pyramidTexture.requestNumber = this.requestNumber++;
                pyramidTexture.requiredThisFrame = true;
                this.textures.put(key, pyramidTexture);
                if (this.requestNumber >= maxRequestNumber) {
                    this.resetRequestNumbers();
                }
                return pyramidTexture;
            }
            return null;
        }
        this.textures.remove(oldest.key);
        oldest.x = x;
        oldest.y = y;
        oldest.z = z;
        oldest.key = key;
        oldest.requestNumber = this.requestNumber++;
        oldest.requiredThisFrame = true;
        oldest.state = TextureState.Init;
        this.textures.put(oldest.key, oldest);
        if (!s_required.contains(oldest.key)) {
            boolean bl = true;
        }
        if (this.requestNumber >= maxRequestNumber) {
            this.resetRequestNumbers();
        }
        return oldest;
    }

    void resetRequestNumbers() {
        ArrayList<PyramidTexture> sorted2 = new ArrayList<PyramidTexture>(this.textures.values());
        sorted2.sort(Comparator.comparingInt(o -> o.requestNumber));
        this.requestNumber = 1;
        for (PyramidTexture pyramidTexture : sorted2) {
            ++this.requestNumber;
            pyramidTexture.requestNumber = pyramidTexture.requestNumber;
        }
        sorted2.clear();
    }

    private void readInfoFile() {
        if (this.zipFs == null || !this.zipFs.isOpen()) {
            return;
        }
        Path path = this.zipFs.getPath("pyramid.txt", new String[0]);
        try (InputStream is = Files.newInputStream(path, new OpenOption[0]);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr);){
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("VERSION=")) {
                    line = line.substring("VERSION=".length());
                    int n = PZMath.tryParseInt(line, -1);
                    continue;
                }
                if (line.startsWith("bounds=")) {
                    String[] ss = (line = line.substring("bounds=".length())).split(" ");
                    if (ss.length != 4) continue;
                    this.minX = PZMath.tryParseInt(ss[0], -1);
                    this.minY = PZMath.tryParseInt(ss[1], -1);
                    this.maxX = PZMath.tryParseInt(ss[2], -1);
                    this.maxY = PZMath.tryParseInt(ss[3], -1);
                    continue;
                }
                if (line.startsWith("clampS=")) {
                    String value = line.substring("clampS=".length()).trim();
                    if ("clamp_to_edge".equalsIgnoreCase(value)) {
                        this.clampS = 33071;
                        continue;
                    }
                    if (!"repeat".equalsIgnoreCase(value)) continue;
                    this.clampS = 10497;
                    continue;
                }
                if (line.startsWith("clampT=")) {
                    String value = line.substring("clampT=".length()).trim();
                    if ("clamp_to_edge".equalsIgnoreCase(value)) {
                        this.clampT = 33071;
                        continue;
                    }
                    if (!"repeat".equalsIgnoreCase(value)) continue;
                    this.clampT = 10497;
                    continue;
                }
                if (line.startsWith("minFilter=")) {
                    String value = line.substring("minFilter=".length()).trim();
                    if ("linear".equalsIgnoreCase(value)) {
                        this.minFilter = 9729;
                        continue;
                    }
                    if (!"nearest".equalsIgnoreCase(value)) continue;
                    this.minFilter = 9728;
                    continue;
                }
                if (line.startsWith("magFilter=")) {
                    String value = line.substring("magFilter=".length()).trim();
                    if ("linear".equalsIgnoreCase(value)) {
                        this.magFilter = 9729;
                        continue;
                    }
                    if (!"nearest".equalsIgnoreCase(value)) continue;
                    this.magFilter = 9728;
                    continue;
                }
                if (line.startsWith("imageSize=")) {
                    String[] ss = (line = line.substring("imageSize=".length())).split(" ");
                    if (ss.length != 2) continue;
                    this.imageWidth = PZMath.tryParseInt(ss[0], -1);
                    this.imageHeight = PZMath.tryParseInt(ss[1], -1);
                    continue;
                }
                if (!line.startsWith("resolution=")) continue;
                line = line.substring("resolution=".length());
                this.resolution = PZMath.tryParseFloat(line, 1.0f);
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public boolean isDestroyed() {
        return this.destroyed;
    }

    public void destroy() {
        this.destroyed = true;
        for (FileTask_LoadImagePyramidTexture fileTask : this.fileTasks.values()) {
            fileTask.cancelled = true;
            GameWindow.fileSystem.cancelAsync(fileTask.asyncOp);
            fileTask.asyncOp = -1;
        }
        DebugType.ExitDebug.debugln("ImagePyramid.destroy() 1");
        while (!this.fileTasks.isEmpty()) {
            GameWindow.fileSystem.updateAsyncTransactions();
            this.checkCalledQueue();
            this.checkQueue();
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            Thread.onSpinWait();
        }
        DebugType.ExitDebug.debugln("ImagePyramid.destroy() 2");
        FileTaskResult fileTask = this.queueRender.poll();
        while (fileTask != null) {
            this.disposeImageData(fileTask.imageData);
            fileTask = this.queueRender.poll();
        }
        if (this.zipFs != null) {
            try {
                this.zipFs.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
            this.zipFs = null;
        }
        RenderThread.invokeOnRenderContext(() -> {
            for (PyramidTexture pyramidTexture : this.textures.values()) {
                if (pyramidTexture.textureId == null) continue;
                pyramidTexture.textureId.destroy();
                pyramidTexture.textureId = null;
            }
        });
        this.missing.clear();
        this.textures.clear();
    }

    void checkQueue() {
        FileTaskResult fileTaskResult = this.queueRender.poll();
        while (fileTaskResult != null) {
            ImageData imageData = fileTaskResult.imageData;
            PyramidTexture pyramidTexture = this.textures.get(fileTaskResult.key);
            if (pyramidTexture == null) {
                this.disposeImageData(imageData);
            } else if (pyramidTexture.state == TextureState.Cancelled) {
                this.disposeImageData(imageData);
            } else if (imageData == null) {
                pyramidTexture.requestNumber = 0;
                this.missing.add(pyramidTexture.key);
            } else {
                if (pyramidTexture.textureId == null) {
                    pyramidTexture.textureId = new TextureID(imageData);
                } else {
                    this.replaceTextureData(pyramidTexture, imageData);
                }
                pyramidTexture.state = TextureState.Ready;
                if (++this.texturesLoadedThisFrame >= 5) break;
            }
            fileTaskResult = this.queueRender.poll();
        }
    }

    void disposeImageData(ImageData imageData) {
        if (imageData != null) {
            imageData.dispose();
            DirectBufferAllocator.destroyDisposed();
        }
    }

    void onFileTaskFinished(Object result) {
        FileTask_LoadImagePyramidTexture fileTask = (FileTask_LoadImagePyramidTexture)result;
        if (fileTask == this.fileTasks.get(fileTask.key)) {
            this.fileTasks.remove(fileTask.key);
            FileTaskResult fileTaskResult = new FileTaskResult(fileTask.key, fileTask.imageData);
            fileTask.imageData = null;
            this.queueRender.add(fileTaskResult);
            return;
        }
        this.disposeImageData(fileTask.imageData);
        fileTask.imageData = null;
    }

    void onFileTaskCancelled(FileTask_LoadImagePyramidTexture fileTask) {
        if (fileTask == this.fileTasks.get(fileTask.key)) {
            this.fileTasks.remove(fileTask.key);
        }
        this.disposeImageData(fileTask.imageData);
        fileTask.imageData = null;
    }

    void onFileTaskCalled(FileTask_LoadImagePyramidTexture fileTask) {
        this.queueCalled.add(fileTask);
    }

    static final class ImageKeyXYZ {
        final TIntObjectHashMap<ImageKeyYZ> yz = new TIntObjectHashMap();

        ImageKeyXYZ() {
        }

        String get(int x, int y, int z) {
            ImageKeyYZ yz = this.yz.get(y);
            if (yz == null) {
                yz = new ImageKeyYZ();
                this.yz.put(y, yz);
            }
            return yz.get(x, y, z);
        }
    }

    public static final class PyramidTexture {
        int x;
        int y;
        int z;
        String key;
        int requestNumber;
        boolean requiredThisFrame;
        TextureID textureId;
        TextureState state = TextureState.Init;

        public boolean isReady() {
            return this.state == TextureState.Ready;
        }

        public TextureID getTextureID() {
            return this.textureId;
        }
    }

    static enum TextureState {
        Init,
        Loading,
        Cancelled,
        Ready;

    }

    static final class FileTaskRequest {
        ImagePyramid pyramid;
        String key;
        Path path;

        FileTaskRequest(ImagePyramid pyramid, String key, Path path) {
            this.pyramid = pyramid;
            this.key = key;
            this.path = path;
        }
    }

    static final class FileTaskResult {
        String key;
        ImageData imageData;

        FileTaskResult(String key, ImageData imageData) {
            this.key = key;
            this.imageData = imageData;
        }
    }

    static final class ImageKeyYZ {
        TIntObjectHashMap<String> z = new TIntObjectHashMap();

        ImageKeyYZ() {
        }

        String get(int x, int y, int z) {
            String s = this.z.get(z);
            if (s == null) {
                s = String.format(Locale.ENGLISH, "%dx%dx%d", x, y, z);
                this.z.put(z, s);
            }
            return s;
        }
    }
}

