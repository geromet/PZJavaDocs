/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.iso.Vector2;
import zombie.popman.ObjectPool;

public final class Vector2ObjectPool
extends ObjectPool<Vector2> {
    private int allocated;
    private static final ThreadLocal<Vector2ObjectPool> Pool = ThreadLocal.withInitial(Vector2ObjectPool::new);

    private Vector2ObjectPool() {
        super(Vector2::new);
    }

    @Override
    protected Vector2 makeObject() {
        ++this.allocated;
        return (Vector2)super.makeObject();
    }

    public static Vector2ObjectPool get() {
        return Pool.get();
    }
}

