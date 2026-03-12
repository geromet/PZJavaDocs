/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayList;
import java.util.List;
import zombie.pathfind.SlopedSurface;
import zombie.popman.ObjectPool;

final class ChunkUpdateTaskLevel {
    final int[][] data = new int[8][8];
    final short[][] cost = new short[8][8];
    final ArrayList<SlopedSurface> slopedSurfaces = new ArrayList();
    static final ObjectPool<ChunkUpdateTaskLevel> pool = new ObjectPool<ChunkUpdateTaskLevel>(ChunkUpdateTaskLevel::new);

    ChunkUpdateTaskLevel() {
    }

    static ChunkUpdateTaskLevel alloc() {
        return pool.alloc();
    }

    void release() {
        SlopedSurface.pool.releaseAll((List<SlopedSurface>)this.slopedSurfaces);
        this.slopedSurfaces.clear();
        pool.release(this);
    }
}

