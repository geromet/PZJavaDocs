/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import zombie.iso.IsoDirections;
import zombie.popman.ObjectPool;

final class SlopedSurface {
    byte x;
    byte y;
    IsoDirections direction;
    float heightMin;
    float heightMax;
    static final ObjectPool<SlopedSurface> pool = new ObjectPool<SlopedSurface>(SlopedSurface::new);

    SlopedSurface() {
    }

    static SlopedSurface alloc() {
        return pool.alloc();
    }

    void release() {
        pool.release(this);
    }
}

