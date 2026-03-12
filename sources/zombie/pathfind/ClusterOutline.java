/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;

final class ClusterOutline {
    int x;
    int y;
    int z;
    boolean w;
    boolean n;
    boolean e;
    boolean s;
    boolean tw;
    boolean tn;
    boolean te;
    boolean ts;
    boolean inner;
    boolean innerCorner;
    boolean start;
    static final ArrayDeque<ClusterOutline> pool = new ArrayDeque();

    ClusterOutline() {
    }

    ClusterOutline init(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.s = false;
        this.e = false;
        this.n = false;
        this.w = false;
        this.ts = false;
        this.te = false;
        this.tn = false;
        this.tw = false;
        this.start = false;
        this.innerCorner = false;
        this.inner = false;
        return this;
    }

    static ClusterOutline alloc() {
        return pool.isEmpty() ? new ClusterOutline() : pool.pop();
    }

    void release() {
        assert (!pool.contains(this));
        pool.push(this);
    }
}

