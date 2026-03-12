/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.CRC32;
import zombie.GameTime;
import zombie.MainThread;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugType;
import zombie.entity.GameEntityManager;
import zombie.inventory.types.MapItem;
import zombie.iso.IsoChunk;
import zombie.iso.IsoWorld;
import zombie.iso.SaveBufferMap;
import zombie.network.ChunkChecksum;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.savefile.PlayerDB;
import zombie.util.ByteBufferPooledObject;
import zombie.vehicles.VehiclesDB2;
import zombie.worldMap.WorldMapVisited;

public class ChunkSaveWorker {
    public static final ChunkSaveWorker instance = new ChunkSaveWorker();
    private final ArrayList<QueuedSave> tempList = new ArrayList();
    public final ConcurrentLinkedQueue<QueuedSave> toSaveQueue = new ConcurrentLinkedQueue();
    private final HashMap<IsoChunk, QueuedSave> toSaveMap = new HashMap();
    private final ConcurrentLinkedQueue<ByteBuffer> byteBufferPool = new ConcurrentLinkedQueue();
    public boolean saving;
    private static final SaveBufferMap saveBufferMap = new SaveBufferMap();

    public void Update(IsoChunk aboutToLoad) {
        if (GameServer.server) {
            return;
        }
        QueuedSave qs = null;
        boolean bl = this.saving = !this.toSaveQueue.isEmpty();
        if (!this.saving) {
            return;
        }
        if (aboutToLoad != null) {
            for (QueuedSave qs2 : this.toSaveQueue) {
                if (qs2.chunk.wx != aboutToLoad.wx || qs2.chunk.wy != aboutToLoad.wy) continue;
                if (!this.toSaveQueue.remove(qs2)) break;
                qs = qs2;
                break;
            }
        }
        if (qs == null) {
            qs = this.toSaveQueue.poll();
        }
        if (qs == null) {
            return;
        }
        this.WriteQueuedSave(qs);
        if (this.toSaveQueue.isEmpty() && !GameClient.client && !GameServer.server) {
            this.HotsaveAncilliarySystems();
        }
    }

    private void HotsaveAncilliarySystems() {
        saveBufferMap.clear();
        MainThread.invokeOnMainThread(() -> {
            IsoWorld.instance.metaGrid.saveToBufferMap(saveBufferMap);
            AnimalPopulationManager.getInstance().saveToBufferMap(saveBufferMap);
            GameTime.instance.saveToBufferMap(saveBufferMap);
            MapItem.SaveWorldMapToBufferMap(saveBufferMap);
            WorldMapVisited.getInstance().saveToBufferMap(saveBufferMap);
            GameEntityManager.saveToBufferMap(saveBufferMap);
        });
        if (PlayerDB.isAllow()) {
            PlayerDB.getInstance().savePlayers();
        }
        try {
            saveBufferMap.save(ChunkSaveWorker::writeBufferToDisk);
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        saveBufferMap.clear();
    }

    private static void writeBufferToDisk(String outFilePath, ByteBufferPooledObject buffer) throws IOException {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        File outFile = new File(outFilePath);
        try (FileOutputStream fos = new FileOutputStream(outFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);){
            for (int i = 0; i < buffer.capacity(); ++i) {
                bos.write(buffer.get(i));
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void WriteQueuedSave(QueuedSave qs) {
        try {
            if (qs.isHotSave) {
                DebugType.Saving.debugln("ChunkSaveWorker.WriteQueuedSave - Saving (ch=%d, %d) isHotSave=%b", qs.chunk.wx, qs.chunk.wy, qs.isHotSave);
            }
            if (qs.byteBuffer != null) {
                long crc = ChunkChecksum.getChecksumIfExists(qs.chunk.wx, qs.chunk.wy);
                if (crc == qs.crc.getValue()) {
                    DebugType.Saving.debugln("ChunkSaveWorker.WriteQueuedSave - Aborted Saving Unchanged Chunk (ch=%d, %d) crc=%d", qs.chunk.wx, qs.chunk.wy, crc);
                } else {
                    ChunkChecksum.setChecksum(qs.chunk.wx, qs.chunk.wy, qs.crc.getValue());
                    IsoChunk.SafeWrite(qs.chunk.wx, qs.chunk.wy, qs.byteBuffer);
                }
            } else {
                qs.chunk.Save(qs.isHotSave);
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        finally {
            qs.chunk = null;
            qs.releaseBuffer();
        }
    }

    public void SaveNow(ArrayList<IsoChunk> aboutToLoad) {
        this.tempList.clear();
        QueuedSave qs2 = this.toSaveQueue.poll();
        while (qs2 != null) {
            boolean saved = false;
            for (int i = 0; i < aboutToLoad.size(); ++i) {
                IsoChunk ch = aboutToLoad.get(i);
                if (qs2.chunk.wx != ch.wx || qs2.chunk.wy != ch.wy) continue;
                this.WriteQueuedSave(qs2);
                saved = true;
                break;
            }
            if (!saved) {
                this.tempList.add(qs2);
            }
            qs2 = this.toSaveQueue.poll();
        }
        for (int i = 0; i < this.tempList.size(); ++i) {
            this.toSaveQueue.add(this.tempList.get(i));
        }
        this.tempList.clear();
    }

    public void SaveNow() {
        DebugType.ExitDebug.debugln("ChunkSaveWorker.SaveNow 1");
        QueuedSave qs = this.toSaveQueue.poll();
        while (qs != null) {
            DebugType.ExitDebug.debugln("ChunkSaveWorker.SaveNow 2 (ch=" + qs.chunk.wx + ", " + qs.chunk.wy + ")");
            this.WriteQueuedSave(qs);
            qs = this.toSaveQueue.poll();
        }
        this.removeCompletedJobs();
        this.saving = false;
        DebugType.ExitDebug.debugln("ChunkSaveWorker.SaveNow 3");
    }

    public void AddHotSave(IsoChunk ch) {
        this.removeCompletedJobs();
        QueuedSave qs = this.findQueuedSaveForChunk(ch);
        if (qs == null) {
            qs = new QueuedSave(ch, true);
            try {
                qs.allocBuffer();
                qs.byteBuffer = qs.chunk.Save(qs.byteBuffer, qs.crc, true);
            }
            catch (Exception e) {
                qs.releaseBuffer();
                DebugType.Saving.error("ChunkSaveWorker.AddHotSave FAILED - (ch=%d, %d)", ch.wx, ch.wy);
                return;
            }
            DebugType.Saving.debugln("ChunkSaveWorker.AddHotSave - (ch=%d, %d)", ch.wx, ch.wy);
            this.toSaveMap.put(ch, qs);
            this.toSaveQueue.add(qs);
            return;
        }
        if (qs.isHotSave) {
            try {
                qs.byteBuffer = qs.chunk.Save(qs.byteBuffer, qs.crc, true);
            }
            catch (Exception e) {
                qs.releaseBuffer();
                DebugType.Saving.error("ChunkSaveWorker.AddHotSave UPDATE FAILED - (ch=%d, %d)", ch.wx, ch.wy);
                return;
            }
            this.toSaveMap.put(ch, qs);
            this.toSaveQueue.add(qs);
            DebugType.Saving.debugln("ChunkSaveWorker.AddHotSave UPDATED - (ch=%d, %d)", ch.wx, ch.wy);
        }
    }

    public void Add(IsoChunk ch) {
        if (Core.getInstance().isNoSave()) {
            for (int i = 0; i < ch.vehicles.size(); ++i) {
                VehiclesDB2.instance.updateVehicle(ch.vehicles.get(i));
            }
        }
        this.removeCompletedJobs();
        QueuedSave qs = this.findQueuedSaveForChunk(ch);
        if (qs == null) {
            qs = new QueuedSave(ch, false);
        } else {
            qs.isHotSave = false;
            qs.releaseBuffer();
            qs.crc = null;
        }
        this.toSaveMap.put(ch, qs);
        this.toSaveQueue.add(qs);
    }

    private void removeCompletedJobs() {
        this.toSaveMap.entrySet().removeIf(entry -> ((QueuedSave)entry.getValue()).chunk == null);
    }

    private QueuedSave findQueuedSaveForChunk(IsoChunk ch) {
        QueuedSave qs = this.toSaveMap.remove(ch);
        if (qs == null) {
            return null;
        }
        boolean removed = this.toSaveQueue.remove(qs);
        if (removed) {
            return qs;
        }
        return null;
    }

    private static final class QueuedSave {
        public IsoChunk chunk;
        public boolean isHotSave;
        public ByteBuffer byteBuffer;
        public CRC32 crc;

        QueuedSave(IsoChunk chunk, boolean isHotSave) {
            this.chunk = chunk;
            this.isHotSave = isHotSave;
            this.byteBuffer = null;
            this.crc = isHotSave ? new CRC32() : null;
        }

        void allocBuffer() {
            if (this.byteBuffer != null) {
                return;
            }
            this.byteBuffer = ChunkSaveWorker.instance.byteBufferPool.poll();
            if (this.byteBuffer == null) {
                this.byteBuffer = ByteBuffer.allocate(65536);
            }
        }

        void releaseBuffer() {
            if (this.byteBuffer != null) {
                if (ChunkSaveWorker.instance.byteBufferPool.size() < 30) {
                    ChunkSaveWorker.instance.byteBufferPool.add(this.byteBuffer);
                }
                this.byteBuffer = null;
            }
        }
    }
}

