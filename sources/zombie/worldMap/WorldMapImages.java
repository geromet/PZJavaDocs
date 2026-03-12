/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.worldMap.ImagePyramid;

public final class WorldMapImages {
    private static final HashMap<String, WorldMapImages> s_filenameToImages = new HashMap();
    private String absolutePath;
    private ImagePyramid pyramid;

    public static WorldMapImages getOrCreate(String directory) {
        String zipFile = ZomboidFileSystem.instance.getString(directory + "/pyramid.zip");
        return WorldMapImages.getOrCreateWithFileName(zipFile);
    }

    public static WorldMapImages getOrCreateWithFileName(String absolutePath) {
        if (!Files.exists(Paths.get(absolutePath, new String[0]), new LinkOption[0])) {
            return null;
        }
        WorldMapImages images = s_filenameToImages.get(absolutePath);
        if (images == null) {
            images = new WorldMapImages();
            images.absolutePath = absolutePath;
            images.pyramid = new ImagePyramid();
            images.pyramid.setZipFile(absolutePath);
            s_filenameToImages.put(absolutePath, images);
        }
        return images;
    }

    public static void checkLoadingQueue() {
        for (WorldMapImages images : s_filenameToImages.values()) {
            images.pyramid.checkCalledQueue();
            images.pyramid.checkCancelQueue();
            images.pyramid.checkLoadingQueue();
        }
    }

    public static void startFrame() {
        for (WorldMapImages images : s_filenameToImages.values()) {
            images.pyramid.startFrame();
        }
    }

    public static void endFrame() {
        for (WorldMapImages images : s_filenameToImages.values()) {
            images.pyramid.endFrame();
        }
    }

    public String getAbsolutePath() {
        return this.absolutePath;
    }

    public ImagePyramid getPyramid() {
        return this.pyramid;
    }

    public int getMinX() {
        return this.pyramid.minX;
    }

    public int getMinY() {
        return this.pyramid.minY;
    }

    public int getMaxX() {
        return this.pyramid.maxX;
    }

    public int getMaxY() {
        return this.pyramid.maxY;
    }

    public int getWidthInSquares() {
        return this.getMaxX() - this.getMinX() + 1;
    }

    public int getHeightInSquares() {
        return this.getMaxY() - this.getMinY() + 1;
    }

    public int getZoom(float zoomF) {
        int ptz = 4;
        if ((double)zoomF >= 16.0) {
            ptz = 0;
        } else if (zoomF >= 15.0f) {
            ptz = 1;
        } else if (zoomF >= 14.0f) {
            ptz = 2;
        } else if (zoomF >= 13.0f) {
            ptz = 3;
        }
        ptz = PZMath.clamp(ptz, this.pyramid.minZ, this.pyramid.maxZ);
        return ptz;
    }

    public float getResolution() {
        return this.pyramid.resolution;
    }

    private void destroy() {
        this.pyramid.destroy();
    }

    public static void Reset() {
        for (WorldMapImages images : s_filenameToImages.values()) {
            images.destroy();
        }
        s_filenameToImages.clear();
    }
}

