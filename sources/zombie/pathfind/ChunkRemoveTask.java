/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.iso.IsoChunk;
import zombie.pathfind.Cell;
import zombie.pathfind.IChunkTask;
import zombie.pathfind.PolygonalMap2;

final class ChunkRemoveTask
implements IChunkTask {
    PolygonalMap2 map;
    int wx;
    int wy;
    static final ArrayDeque<ChunkRemoveTask> pool = new ArrayDeque();

    ChunkRemoveTask() {
    }

    ChunkRemoveTask init(PolygonalMap2 map, IsoChunk chunk) {
        this.map = map;
        this.wx = chunk.wx;
        this.wy = chunk.wy;
        return this;
    }

    @Override
    public void execute() {
        Cell cell = this.map.getCellFromChunkPos(this.wx, this.wy);
        cell.removeChunk(this.wx, this.wy);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static ChunkRemoveTask alloc() {
        ArrayDeque<ChunkRemoveTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            return pool.isEmpty() ? new ChunkRemoveTask() : pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void release() {
        ArrayDeque<ChunkRemoveTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }
}

