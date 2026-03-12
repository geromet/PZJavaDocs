/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.pathfind.Point;

final class PointPool {
    final ArrayDeque<Point> pool = new ArrayDeque();

    PointPool() {
    }

    Point alloc() {
        return this.pool.isEmpty() ? new Point() : this.pool.pop();
    }

    void release(Point pt) {
        this.pool.push(pt);
    }
}

