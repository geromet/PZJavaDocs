/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import zombie.util.Pool;
import zombie.worldMap.symbols.DoublePoint;

public class DoublePointPool {
    private static final Pool<DoublePoint> s_pool = new Pool<DoublePoint>(DoublePoint::new);

    public static DoublePoint alloc() {
        return s_pool.alloc();
    }

    public static void release(DoublePoint obj) {
        s_pool.release(obj);
    }
}

