/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.iso.CellLoader;
import zombie.iso.ChunkSaveWorker;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTree;

public final class WorldReuserThread {
    public static final WorldReuserThread instance = new WorldReuserThread();
    private final ArrayList<IsoObject> objectsToReuse = new ArrayList();
    private final ArrayList<IsoTree> treesToReuse = new ArrayList();
    public boolean finished;
    private Thread worldReuser;
    private final LinkedBlockingQueue<IsoChunk> reuseGridSquares = new LinkedBlockingQueue();
    private final IsoChunk finishLoop = new IsoChunk((WorldReuserThread)null);

    public void run() {
        this.worldReuser = new Thread(ThreadGroups.Workers, () -> {
            while (!this.finished) {
                this.testReuseChunk();
                this.reconcileReuseObjects();
            }
        });
        this.worldReuser.setName("WorldReuser");
        this.worldReuser.setDaemon(true);
        this.worldReuser.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        this.worldReuser.start();
    }

    public void stop() {
        this.reuseGridSquares.add(this.finishLoop);
        while (!this.finished) {
            Thread.onSpinWait();
            try {
                Thread.sleep(20L);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void reconcileReuseObjects() {
        if (!this.objectsToReuse.isEmpty()) {
            if (CellLoader.isoObjectCache.size() < 320000) {
                CellLoader.isoObjectCache.push(this.objectsToReuse);
            }
            this.objectsToReuse.clear();
        }
        if (!this.treesToReuse.isEmpty()) {
            if (CellLoader.isoTreeCache.size() < 40000) {
                CellLoader.isoTreeCache.push(this.treesToReuse);
            }
            this.treesToReuse.clear();
        }
    }

    private void testReuseChunk() {
        try {
            IsoChunk chunk = this.reuseGridSquares.take();
            while (chunk != null) {
                if (chunk == this.finishLoop) {
                    this.finished = true;
                    return;
                }
                if (Core.debug) {
                    if (ChunkSaveWorker.instance.toSaveQueue.contains(chunk)) {
                        DebugLog.log("ERROR: reusing chunk that needs to be saved");
                    }
                    if (IsoChunkMap.chunkStore.contains(chunk)) {
                        DebugLog.log("ERROR: reusing chunk in chunkStore");
                    }
                    if (!chunk.refs.isEmpty()) {
                        DebugLog.log("ERROR: reusing chunk with refs");
                    }
                }
                if (Core.debug) {
                    // empty if block
                }
                this.reuseGridSquares(chunk);
                if (this.treesToReuse.size() > 1000 || this.objectsToReuse.size() > 5000) {
                    this.reconcileReuseObjects();
                }
                chunk = this.reuseGridSquares.take();
            }
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    public void addReuseChunk(IsoChunk chunk) {
        this.reuseGridSquares.add(chunk);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void reuseGridSquares(IsoChunk chunk) {
        int to = 64;
        for (int n = chunk.minLevel; n <= chunk.maxLevel; ++n) {
            for (int m = 0; m < 64; ++m) {
                int squaresIndexOfLevel = chunk.squaresIndexOfLevel(n);
                IsoGridSquare sq = chunk.squares[squaresIndexOfLevel][m];
                if (sq == null) continue;
                for (int a = 0; a < sq.getObjects().size(); ++a) {
                    ArrayList<IsoObject> arrayList;
                    IsoObject o = sq.getObjects().get(a);
                    if (o instanceof IsoTree) {
                        IsoTree isoTree = (IsoTree)o;
                        o.reset();
                        arrayList = this.treesToReuse;
                        synchronized (arrayList) {
                            this.treesToReuse.add(isoTree);
                            continue;
                        }
                    }
                    if (o.getClass() == IsoObject.class) {
                        o.reset();
                        arrayList = this.objectsToReuse;
                        synchronized (arrayList) {
                            this.objectsToReuse.add(o);
                            continue;
                        }
                    }
                    o.reuseGridSquare();
                }
                sq.discard();
                chunk.squares[squaresIndexOfLevel][m] = null;
            }
        }
        chunk.resetForStore();
        IsoChunkMap.chunkStore.add(chunk);
    }
}

