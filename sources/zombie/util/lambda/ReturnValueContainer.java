/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ReturnValueContainer<T>
extends PooledObject {
    public T returnVal;
    private static final Pool<ReturnValueContainer<Object>> s_pool = new Pool<ReturnValueContainer>(ReturnValueContainer::new);

    @Override
    public void onReleased() {
        this.returnVal = null;
    }

    public static <E> ReturnValueContainer<E> alloc() {
        return s_pool.alloc();
    }
}

