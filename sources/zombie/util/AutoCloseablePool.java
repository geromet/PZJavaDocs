/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.util.ArrayList;
import java.util.function.Supplier;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.lambda.Invokers;

public class AutoCloseablePool
extends PooledObject
implements AutoCloseable {
    private static final Pool<AutoCloseablePool> s_pool = new Pool<AutoCloseablePool>(AutoCloseablePool::new);
    private final ArrayList<AutoCloseableEntry<?>> entries = new ArrayList();

    public static AutoCloseablePool alloc() {
        return s_pool.alloc();
    }

    private AutoCloseablePool() {
    }

    @Override
    public void onReleased() {
        this.releaseAll();
    }

    @Override
    public void close() {
        this.release();
    }

    private void releaseAll() {
        for (AutoCloseableEntry<?> entry : this.entries) {
            entry.release();
        }
        this.entries.clear();
    }

    public <T> T alloc(Supplier<T> alloc, Invokers.Params1.ICallback<T> release) {
        T newVal = alloc.get();
        AutoCloseableEntry<T> autoCloseable = AutoCloseableEntry.alloc(newVal, release);
        this.entries.add(autoCloseable);
        return newVal;
    }

    public Vector2 allocVector2() {
        return this.alloc(() -> (Vector2)Vector2ObjectPool.get().alloc(), val -> Vector2ObjectPool.get().release(val));
    }

    private static class AutoCloseableEntry<T>
    extends PooledObject {
        private T entry;
        private Invokers.Params1.ICallback<T> onRelease;
        private static final Pool<AutoCloseableEntry<?>> s_pool = new Pool<AutoCloseableEntry>(AutoCloseableEntry::new);

        @Override
        public void onReleased() {
            this.onRelease.accept(this.entry);
        }

        private AutoCloseableEntry() {
        }

        public static <T> AutoCloseableEntry<T> alloc(T val, Invokers.Params1.ICallback<T> onRelease) {
            AutoCloseableEntry<?> newInstance = s_pool.alloc();
            newInstance.entry = val;
            newInstance.onRelease = onRelease;
            return newInstance;
        }
    }
}

