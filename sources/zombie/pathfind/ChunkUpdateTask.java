/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.List;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.pathfind.Chunk;
import zombie.pathfind.ChunkDataZ;
import zombie.pathfind.ChunkUpdateTaskLevel;
import zombie.pathfind.IChunkTask;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.SlopedSurface;
import zombie.pathfind.SquareUpdateTask;

final class ChunkUpdateTask
implements IChunkTask {
    PolygonalMap2 map;
    int wx;
    int wy;
    ChunkUpdateTaskLevel[] levels = new ChunkUpdateTaskLevel[1];
    int minLevel = 32;
    int maxLevel = 32;
    static final ArrayDeque<ChunkUpdateTask> pool = new ArrayDeque();

    ChunkUpdateTask() {
        this.levels[0] = new ChunkUpdateTaskLevel();
    }

    void setMinMaxLevel(int minLevel, int maxLevel) {
        if (minLevel == this.minLevel && maxLevel == this.maxLevel) {
            return;
        }
        for (int z = this.minLevel; z <= this.maxLevel; ++z) {
            ChunkUpdateTaskLevel chunkLevel;
            if (z >= minLevel && z <= maxLevel || (chunkLevel = this.levels[z - this.minLevel]) == null) continue;
            chunkLevel.release();
            this.levels[z - this.minLevel] = null;
        }
        ChunkUpdateTaskLevel[] newLevels = new ChunkUpdateTaskLevel[maxLevel - minLevel + 1];
        for (int z = minLevel; z <= maxLevel; ++z) {
            newLevels[z - minLevel] = z >= this.minLevel && z <= this.maxLevel ? this.levels[z - this.minLevel] : ChunkUpdateTaskLevel.alloc();
        }
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.levels = newLevels;
    }

    ChunkUpdateTask init(PolygonalMap2 map, IsoChunk chunk) {
        this.map = map;
        this.wx = chunk.wx;
        this.wy = chunk.wy;
        this.setMinMaxLevel(chunk.minLevel + 32, chunk.maxLevel + 32);
        for (int z = this.minLevel; z <= this.maxLevel; ++z) {
            ChunkUpdateTaskLevel chunkLevel = this.levels[z - this.minLevel];
            SlopedSurface.pool.releaseAll((List<SlopedSurface>)chunkLevel.slopedSurfaces);
            chunkLevel.slopedSurfaces.clear();
            for (int y = 0; y < 8; ++y) {
                for (int x = 0; x < 8; ++x) {
                    IsoGridSquare sq = chunk.getGridSquare(x, y, z - 32);
                    if (sq == null) {
                        chunkLevel.data[x][y] = 0;
                        chunkLevel.cost[x][y] = 0;
                        continue;
                    }
                    chunkLevel.data[x][y] = SquareUpdateTask.getBits(sq);
                    chunkLevel.cost[x][y] = SquareUpdateTask.getCost(sq);
                    if (!sq.hasSlopedSurface()) continue;
                    SlopedSurface slopedSurface = SlopedSurface.alloc();
                    slopedSurface.x = (byte)x;
                    slopedSurface.y = (byte)y;
                    slopedSurface.direction = sq.getSlopedSurfaceDirection();
                    slopedSurface.heightMin = sq.getSlopedSurfaceHeightMin();
                    slopedSurface.heightMax = sq.getSlopedSurfaceHeightMax();
                    chunkLevel.slopedSurfaces.add(slopedSurface);
                }
            }
        }
        return this;
    }

    @Override
    public void execute() {
        Chunk chunk = this.map.allocChunkIfNeeded(this.wx, this.wy);
        chunk.setData(this);
        ChunkDataZ.epochCount = (short)(ChunkDataZ.epochCount + 1);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static ChunkUpdateTask alloc() {
        ArrayDeque<ChunkUpdateTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            return pool.isEmpty() ? new ChunkUpdateTask() : pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void release() {
        ArrayDeque<ChunkUpdateTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }
}

