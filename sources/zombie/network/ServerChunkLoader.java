/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.CRC32;
import zombie.GameTime;
import zombie.ZomboidFileSystem;
import zombie.core.logger.LoggerManager;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.WorldReuserThread;
import zombie.iso.areas.IsoRoom;
import zombie.network.ChunkChecksum;
import zombie.network.ClientChunkRequest;
import zombie.network.GameServer;
import zombie.network.ServerMap;

public class ServerChunkLoader {
    private final long debugSlowMapLoadingDelay = 0L;
    private boolean mapLoading;
    private final LoaderThread threadLoad;
    private final SaveChunkThread threadSave;
    private final CRC32 crcSave = new CRC32();
    private final RecalcAllThread threadRecalc;

    public ServerChunkLoader() {
        this.threadLoad = new LoaderThread(this);
        this.threadLoad.setName("LoadChunk");
        this.threadLoad.setDaemon(true);
        this.threadLoad.start();
        this.threadRecalc = new RecalcAllThread(this);
        this.threadRecalc.setName("RecalcAll");
        this.threadRecalc.setDaemon(true);
        this.threadRecalc.setPriority(10);
        this.threadRecalc.start();
        this.threadSave = new SaveChunkThread(this);
        this.threadSave.setName("SaveChunk");
        this.threadSave.setDaemon(true);
        this.threadSave.start();
    }

    public void addJob(ServerMap.ServerCell cell) {
        this.mapLoading = DebugType.MapLoading.isEnabled();
        this.threadLoad.toThread.add(cell);
    }

    public void getLoaded(ArrayList<ServerMap.ServerCell> loaded) {
        this.threadLoad.fromThread.drainTo(loaded);
    }

    public void quit() {
        this.threadLoad.quit();
        while (this.threadLoad.isAlive()) {
            try {
                Thread.sleep(500L);
            }
            catch (InterruptedException interruptedException) {}
        }
        this.threadSave.quit();
        while (this.threadSave.isAlive()) {
            try {
                Thread.sleep(500L);
            }
            catch (InterruptedException interruptedException) {}
        }
    }

    public void addSaveUnloadedJob(IsoChunk chunk) {
        this.threadSave.addUnloadedJob(chunk);
    }

    public void addSaveLoadedJob(IsoChunk chunk) {
        this.threadSave.addLoadedJob(chunk);
    }

    public void saveLater(GameTime gameTime) {
        this.threadSave.saveLater(gameTime);
    }

    public void updateSaved() {
        this.threadSave.update();
    }

    public void addRecalcJob(ServerMap.ServerCell cell) {
        this.threadRecalc.toThread.add(cell);
    }

    public void getRecalc(ArrayList<ServerMap.ServerCell> loaded) {
        this.threadRecalc.fromThread.drainTo(loaded);
    }

    private class LoaderThread
    extends Thread {
        private final LinkedBlockingQueue<ServerMap.ServerCell> toThread;
        private final LinkedBlockingQueue<ServerMap.ServerCell> fromThread;
        ArrayDeque<IsoGridSquare> isoGridSquareCache;
        final /* synthetic */ ServerChunkLoader this$0;

        private LoaderThread(ServerChunkLoader serverChunkLoader) {
            ServerChunkLoader serverChunkLoader2 = serverChunkLoader;
            Objects.requireNonNull(serverChunkLoader2);
            this.this$0 = serverChunkLoader2;
            this.toThread = new LinkedBlockingQueue();
            this.fromThread = new LinkedBlockingQueue();
            this.isoGridSquareCache = new ArrayDeque();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    while (true) {
                        ServerMap.ServerCell cell = this.toThread.take();
                        if (this.isoGridSquareCache.size() < 10000) {
                            IsoGridSquare.getSquaresForThread(this.isoGridSquareCache, 10000);
                            IsoGridSquare.loadGridSquareCache = this.isoGridSquareCache;
                        }
                        if (cell.wx == -1 && cell.wy == -1) {
                            return;
                        }
                        if (cell.cancelLoading) {
                            if (this.this$0.mapLoading) {
                                DebugLog.MapLoading.debugln("LoaderThread: cancelled " + cell.wx + "," + cell.wy);
                            }
                            cell.loadingWasCancelled = true;
                            continue;
                        }
                        long start = System.nanoTime();
                        for (int x = 0; x < 8; ++x) {
                            for (int y = 0; y < 8; ++y) {
                                int wx = cell.wx * 8 + x;
                                int wy = cell.wy * 8 + y;
                                if (!IsoWorld.instance.metaGrid.isValidChunk(wx, wy)) continue;
                                IsoChunk chunk = IsoChunkMap.chunkStore.poll();
                                if (chunk == null) {
                                    chunk = new IsoChunk((IsoCell)null);
                                }
                                chunk.assignLoadID();
                                this.this$0.threadSave.saveNow(wx, wy);
                                try {
                                    if (chunk.LoadOrCreate(wx, wy, null)) {
                                        chunk.loaded = true;
                                    } else {
                                        ChunkChecksum.setChecksum(wx, wy, 0L);
                                        chunk.Blam(wx, wy);
                                        if (chunk.LoadBrandNew(wx, wy)) {
                                            chunk.loaded = true;
                                        }
                                    }
                                }
                                catch (Exception ex) {
                                    ex.printStackTrace();
                                    LoggerManager.getLogger("map").write(ex);
                                }
                                if (!chunk.loaded) continue;
                                cell.chunks[x][y] = chunk;
                            }
                        }
                        if (GameServer.debug) {
                            // empty if block
                        }
                        float time = (float)(System.nanoTime() - start) / 1000000.0f;
                        this.fromThread.add(cell);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    LoggerManager.getLogger("map").write(e);
                    continue;
                }
                break;
            }
        }

        public void quit() {
            ServerMap.ServerCell quitCell = new ServerMap.ServerCell();
            quitCell.wx = -1;
            quitCell.wy = -1;
            this.toThread.add(quitCell);
        }
    }

    private class RecalcAllThread
    extends Thread {
        private final LinkedBlockingQueue<ServerMap.ServerCell> toThread;
        private final LinkedBlockingQueue<ServerMap.ServerCell> fromThread;
        private final GetSquare serverCellGetSquare;
        final /* synthetic */ ServerChunkLoader this$0;

        private RecalcAllThread(ServerChunkLoader serverChunkLoader) {
            ServerChunkLoader serverChunkLoader2 = serverChunkLoader;
            Objects.requireNonNull(serverChunkLoader2);
            this.this$0 = serverChunkLoader2;
            this.toThread = new LinkedBlockingQueue();
            this.fromThread = new LinkedBlockingQueue();
            this.serverCellGetSquare = new GetSquare(this.this$0);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    while (true) {
                        this.runInner();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                break;
            }
        }

        private void runInner() throws InterruptedException {
            int squaresIndexOfLevel;
            IsoGridSquare g;
            int z;
            int i;
            IsoChunk chunk;
            int cy;
            int cx;
            ServerMap.ServerCell cell = this.toThread.take();
            if (cell.cancelLoading && !this.hasAnyBrandNewChunks(cell)) {
                for (int y = 0; y < 8; ++y) {
                    for (int x = 0; x < 8; ++x) {
                        IsoChunk chunk2 = cell.chunks[x][y];
                        if (chunk2 == null) continue;
                        cell.chunks[x][y] = null;
                        WorldReuserThread.instance.addReuseChunk(chunk2);
                    }
                }
                if (this.this$0.mapLoading) {
                    DebugLog.MapLoading.debugln("RecalcAllThread: cancelled " + cell.wx + "," + cell.wy);
                }
                cell.loadingWasCancelled = true;
                return;
            }
            long start = System.nanoTime();
            this.serverCellGetSquare.cell = cell;
            int sx = cell.wx * 64;
            int sy = cell.wy * 64;
            int ex = sx + 64;
            int ey = sy + 64;
            int maxZ = 0;
            int nSquares = 64;
            for (cx = 0; cx < 8; ++cx) {
                for (cy = 0; cy < 8; ++cy) {
                    chunk = cell.chunks[cx][cy];
                    if (chunk == null) continue;
                    chunk.loaded = false;
                    for (i = 0; i < 64; ++i) {
                        for (z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                            int zz = chunk.squaresIndexOfLevel(z);
                            g = chunk.squares[zz][i];
                            if (z == 0 && g == null) {
                                int x = chunk.wx * 8 + i % 8;
                                int y = chunk.wy * 8 + i / 8;
                                g = IsoGridSquare.getNew(IsoWorld.instance.currentCell, null, x, y, z);
                                chunk.setSquare(x % 8, y % 8, z, g);
                            }
                            if (g == null) continue;
                            g.RecalcProperties();
                            IsoRoom room = g.getRoom();
                            if (room == null) continue;
                            room.addSquare(g);
                        }
                    }
                    if (chunk.maxLevel <= maxZ) continue;
                    maxZ = chunk.maxLevel;
                }
            }
            for (cx = 0; cx < 8; ++cx) {
                for (cy = 0; cy < 8; ++cy) {
                    chunk = cell.chunks[cx][cy];
                    if (chunk == null) continue;
                    for (i = 0; i < 64; ++i) {
                        for (z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                            squaresIndexOfLevel = chunk.squaresIndexOfLevel(z);
                            g = chunk.squares[squaresIndexOfLevel][i];
                            if (g == null) continue;
                            if (z != 0 && !g.getObjects().isEmpty()) {
                                this.serverCellGetSquare.EnsureSurroundNotNull(g.x - sx, g.y - sy, z);
                            }
                            g.RecalcAllWithNeighbours(false, this.serverCellGetSquare);
                        }
                    }
                }
            }
            for (cx = 0; cx < 8; ++cx) {
                for (cy = 0; cy < 8; ++cy) {
                    chunk = cell.chunks[cx][cy];
                    if (chunk == null) continue;
                    block12: for (i = 0; i < 64; ++i) {
                        for (z = chunk.maxLevel; z > chunk.minLevel; --z) {
                            squaresIndexOfLevel = chunk.squaresIndexOfLevel(z);
                            IsoGridSquare sq = chunk.squares[squaresIndexOfLevel][i];
                            if (sq == null || !sq.hasRainBlockingTile()) continue;
                            --z;
                            while (z >= chunk.minLevel) {
                                squaresIndexOfLevel = chunk.squaresIndexOfLevel(z);
                                sq = chunk.squares[squaresIndexOfLevel][i];
                                if (sq != null) {
                                    sq.haveRoof = true;
                                    sq.getProperties().unset(IsoFlagType.exterior);
                                }
                                --z;
                            }
                            continue block12;
                        }
                    }
                }
            }
            if (GameServer.debug) {
                // empty if block
            }
            float time = (float)(System.nanoTime() - start) / 1000000.0f;
            if (this.this$0.mapLoading) {
                DebugLog.MapLoading.debugln("RecalcAll for cell " + cell.wx + "," + cell.wy + " ms=" + time);
            }
            this.fromThread.add(cell);
        }

        private boolean hasAnyBrandNewChunks(ServerMap.ServerCell cell) {
            for (int y = 0; y < 8; ++y) {
                for (int x = 0; x < 8; ++x) {
                    IsoChunk chunk = cell.chunks[x][y];
                    if (chunk == null || chunk.getErosionData().init) continue;
                    return true;
                }
            }
            return false;
        }
    }

    private class SaveChunkThread
    extends Thread {
        private final LinkedBlockingQueue<SaveTask> toThread;
        private final LinkedBlockingQueue<SaveTask> fromThread;
        private boolean quit;
        private final CRC32 crc32;
        private final ClientChunkRequest ccr;
        private final ArrayList<SaveTask> toSaveChunk;
        private final ArrayList<SaveTask> savedChunks;
        final /* synthetic */ ServerChunkLoader this$0;

        private SaveChunkThread(ServerChunkLoader serverChunkLoader) {
            ServerChunkLoader serverChunkLoader2 = serverChunkLoader;
            Objects.requireNonNull(serverChunkLoader2);
            this.this$0 = serverChunkLoader2;
            this.toThread = new LinkedBlockingQueue();
            this.fromThread = new LinkedBlockingQueue();
            this.crc32 = new CRC32();
            this.ccr = new ClientChunkRequest();
            this.toSaveChunk = new ArrayList();
            this.savedChunks = new ArrayList();
        }

        @Override
        public void run() {
            do {
                SaveTask task = null;
                try {
                    task = this.toThread.take();
                    task.save();
                    this.fromThread.add(task);
                }
                catch (InterruptedException interruptedException) {
                }
                catch (Exception e) {
                    e.printStackTrace();
                    if (task != null) {
                        LoggerManager.getLogger("map").write("Error saving chunk " + task.wx() + "," + task.wy());
                    }
                    LoggerManager.getLogger("map").write(e);
                }
            } while (!this.quit || !this.toThread.isEmpty());
        }

        public void addUnloadedJob(IsoChunk chunk) {
            this.toThread.add(new SaveUnloadedTask(this.this$0, chunk));
        }

        public void addLoadedJob(IsoChunk chunk) {
            ClientChunkRequest.Chunk reqChunk = this.ccr.getChunk();
            reqChunk.wx = chunk.wx;
            reqChunk.wy = chunk.wy;
            this.ccr.getByteBuffer(reqChunk);
            try {
                chunk.SaveLoadedChunk(reqChunk, this.crc32);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                LoggerManager.getLogger("map").write(ex);
                this.ccr.releaseChunk(reqChunk);
                return;
            }
            this.toThread.add(new SaveLoadedTask(this.this$0, this.ccr, reqChunk));
        }

        public void saveLater(GameTime gameTime) {
            this.toThread.add(new SaveGameTimeTask(this.this$0, gameTime));
        }

        public void saveNow(int chunkX, int chunkY) {
            this.toSaveChunk.clear();
            this.toThread.drainTo(this.toSaveChunk);
            for (int i = 0; i < this.toSaveChunk.size(); ++i) {
                SaveTask task = this.toSaveChunk.get(i);
                if (task.wx() != chunkX || task.wy() != chunkY) continue;
                try {
                    this.toSaveChunk.remove(i--);
                    task.save();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    LoggerManager.getLogger("map").write("Error saving chunk " + chunkX + "," + chunkY);
                    LoggerManager.getLogger("map").write(e);
                }
                this.fromThread.add(task);
            }
            this.toThread.addAll(this.toSaveChunk);
        }

        public void quit() {
            this.toThread.add(new QuitThreadTask(this.this$0));
        }

        public void update() {
            this.savedChunks.clear();
            this.fromThread.drainTo(this.savedChunks);
            for (int i = 0; i < this.savedChunks.size(); ++i) {
                this.savedChunks.get(i).release();
            }
            this.savedChunks.clear();
        }
    }

    private class GetSquare
    implements IsoGridSquare.GetSquare {
        ServerMap.ServerCell cell;

        private GetSquare(ServerChunkLoader serverChunkLoader) {
            Objects.requireNonNull(serverChunkLoader);
        }

        @Override
        public IsoGridSquare getGridSquare(int x, int y, int z) {
            y -= this.cell.wy * 64;
            if ((x -= this.cell.wx * 64) < 0 || x >= 64) {
                return null;
            }
            if (y < 0 || y >= 64) {
                return null;
            }
            IsoChunk chunk = this.cell.chunks[x / 8][y / 8];
            if (chunk == null) {
                return null;
            }
            return chunk.getGridSquare(x % 8, y % 8, z);
        }

        public boolean contains(int x, int y, int z) {
            if (x < 0 || x >= 64) {
                return false;
            }
            return y >= 0 && y < 64;
        }

        public IsoChunk getChunkForSquare(int x, int y) {
            y -= this.cell.wy * 64;
            if ((x -= this.cell.wx * 64) < 0 || x >= 64) {
                return null;
            }
            if (y < 0 || y >= 64) {
                return null;
            }
            return this.cell.chunks[x / 8][y / 8];
        }

        public void EnsureSurroundNotNull(int x, int y, int z) {
            int minX = this.cell.wx * 64;
            int minY = this.cell.wy * 64;
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dy = -1; dy <= 1; ++dy) {
                    IsoGridSquare sq2;
                    if (dx == 0 && dy == 0 || !this.contains(x + dx, y + dy, z) || (sq2 = this.getGridSquare(minX + x + dx, minY + y + dy, z)) != null) continue;
                    sq2 = IsoGridSquare.getNew(IsoWorld.instance.currentCell, null, minX + x + dx, minY + y + dy, z);
                    int chx = (x + dx) / 8;
                    int chy = (y + dy) / 8;
                    int sqx = (x + dx) % 8;
                    int sqy = (y + dy) % 8;
                    if (this.cell.chunks[chx][chy] == null) continue;
                    this.cell.chunks[chx][chy].setSquare(sqx, sqy, z, sq2);
                }
            }
        }
    }

    private class QuitThreadTask
    implements SaveTask {
        final /* synthetic */ ServerChunkLoader this$0;

        private QuitThreadTask(ServerChunkLoader serverChunkLoader) {
            ServerChunkLoader serverChunkLoader2 = serverChunkLoader;
            Objects.requireNonNull(serverChunkLoader2);
            this.this$0 = serverChunkLoader2;
        }

        @Override
        public void save() throws Exception {
            this.this$0.threadSave.quit = true;
        }

        @Override
        public void release() {
        }

        @Override
        public int wx() {
            return 0;
        }

        @Override
        public int wy() {
            return 0;
        }
    }

    private class SaveGameTimeTask
    implements SaveTask {
        private byte[] bytes;

        public SaveGameTimeTask(ServerChunkLoader serverChunkLoader, GameTime gameTime) {
            Objects.requireNonNull(serverChunkLoader);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
                 DataOutputStream dos = new DataOutputStream(baos);){
                gameTime.save(dos);
                dos.close();
                this.bytes = baos.toByteArray();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }

        @Override
        public void save() throws Exception {
            if (this.bytes != null) {
                File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");
                try (FileOutputStream fos = new FileOutputStream(outFile);){
                    fos.write(this.bytes);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        @Override
        public void release() {
        }

        @Override
        public int wx() {
            return 0;
        }

        @Override
        public int wy() {
            return 0;
        }
    }

    private class SaveLoadedTask
    implements SaveTask {
        private final ClientChunkRequest ccr;
        private final ClientChunkRequest.Chunk chunk;
        final /* synthetic */ ServerChunkLoader this$0;

        public SaveLoadedTask(ServerChunkLoader serverChunkLoader, ClientChunkRequest ccr, ClientChunkRequest.Chunk chunk) {
            ServerChunkLoader serverChunkLoader2 = serverChunkLoader;
            Objects.requireNonNull(serverChunkLoader2);
            this.this$0 = serverChunkLoader2;
            this.ccr = ccr;
            this.chunk = chunk;
        }

        @Override
        public void save() throws Exception {
            long crc = ChunkChecksum.getChecksumIfExists(this.chunk.wx, this.chunk.wy);
            this.this$0.crcSave.reset();
            this.this$0.crcSave.update(this.chunk.bb.array(), 0, this.chunk.bb.position());
            if (crc != this.this$0.crcSave.getValue()) {
                ChunkChecksum.setChecksum(this.chunk.wx, this.chunk.wy, this.this$0.crcSave.getValue());
                IsoChunk.SafeWrite(this.chunk.wx, this.chunk.wy, this.chunk.bb);
            }
        }

        @Override
        public void release() {
            this.ccr.releaseChunk(this.chunk);
        }

        @Override
        public int wx() {
            return this.chunk.wx;
        }

        @Override
        public int wy() {
            return this.chunk.wy;
        }
    }

    private class SaveUnloadedTask
    implements SaveTask {
        private final IsoChunk chunk;

        public SaveUnloadedTask(ServerChunkLoader serverChunkLoader, IsoChunk chunk) {
            Objects.requireNonNull(serverChunkLoader);
            this.chunk = chunk;
        }

        @Override
        public void save() throws Exception {
            this.chunk.Save(false);
        }

        @Override
        public void release() {
            WorldReuserThread.instance.addReuseChunk(this.chunk);
        }

        @Override
        public int wx() {
            return this.chunk.wx;
        }

        @Override
        public int wy() {
            return this.chunk.wy;
        }
    }

    private static interface SaveTask {
        public void save() throws Exception;

        public void release();

        public int wx();

        public int wy();
    }
}

