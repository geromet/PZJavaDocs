/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import gnu.trove.set.hash.THashSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import zombie.debug.DebugOptions;

public class ObjectPool<T> {
    private final Allocator<T> allocator;
    private final ArrayDeque<T> pool = new ArrayDeque();
    private final THashSet<T> containsSet = new THashSet<T>(this){
        final /* synthetic */ ObjectPool this$0;
        {
            ObjectPool objectPool = this$0;
            Objects.requireNonNull(objectPool);
            this.this$0 = objectPool;
        }

        @Override
        protected boolean equals(Object notnull, Object two) {
            return two != null && two != REMOVED && notnull == two;
        }
    };

    public ObjectPool() {
        this(null);
    }

    public ObjectPool(Allocator<T> alloc) {
        this.allocator = alloc;
        this.containsSet.setAutoCompactionFactor(0.0f);
    }

    public synchronized T alloc() {
        if (this.pool.isEmpty()) {
            return this.makeObject();
        }
        T obj = this.pool.removeFirst();
        if (DebugOptions.instance.checks.objectPoolContains.getValue()) {
            this.containsSet.remove(obj);
        }
        return obj;
    }

    public synchronized void release(T obj) {
        if (obj != null) {
            if (DebugOptions.instance.checks.objectPoolContains.getValue()) {
                if (this.containsSet.contains(obj)) {
                    return;
                }
                this.containsSet.add(obj);
            }
            this.pool.add(obj);
        }
    }

    public synchronized void release(List<T> objs) {
        for (int i = 0; i < objs.size(); ++i) {
            if (objs.get(i) == null) continue;
            this.release(objs.get(i));
        }
    }

    public synchronized void release(Iterable<T> objs) {
        for (T val : objs) {
            if (val == null) continue;
            this.release(val);
        }
    }

    public synchronized void release(T[] objs) {
        if (objs == null) {
            return;
        }
        for (int i = 0; i < objs.length; ++i) {
            if (objs[i] == null) continue;
            this.release(objs[i]);
        }
    }

    public synchronized void releaseAll(List<T> objs) {
        for (int i = 0; i < objs.size(); ++i) {
            if (objs.get(i) == null) continue;
            this.release(objs.get(i));
        }
    }

    public synchronized void releaseAll(Collection<T> objs) {
        for (T obj : objs) {
            this.release(obj);
        }
    }

    public synchronized void clear() {
        this.pool.clear();
        if (DebugOptions.instance.checks.objectPoolContains.getValue()) {
            this.containsSet.clear();
        }
    }

    protected T makeObject() {
        if (this.allocator != null) {
            return this.allocator.allocate();
        }
        throw new UnsupportedOperationException("Allocator is null. The ObjectPool is intended to be used with an allocator, or with the function makeObject overridden in a subclass.");
    }

    public synchronized void forEach(Consumer<T> consumer) {
        this.pool.forEach(consumer);
    }

    public static interface Allocator<T> {
        public T allocate();
    }
}

