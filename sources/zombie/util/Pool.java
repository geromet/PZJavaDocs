/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import zombie.util.IPooledObject;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class Pool<PO extends IPooledObject> {
    private final Supplier<PO> allocator;
    private final ThreadLocal<PoolStacks> stacks = ThreadLocal.withInitial(PoolStacks::new);

    public ThreadLocal<PoolStacks> getPoolStacks() {
        return this.stacks;
    }

    public Pool(Supplier<PO> allocator) {
        this.allocator = allocator;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PO alloc() {
        Supplier<PO> allocator = this.allocator;
        PoolStacks poolStacks = this.stacks.get();
        Object object = poolStacks.lock;
        synchronized (object) {
            return this.allocInternal(poolStacks, allocator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void release(IPooledObject item) {
        PoolReference itemPoolRef = item.getPoolReference();
        Pool<IPooledObject> itemPool = itemPoolRef.getPool();
        PoolStacks itemStacks = itemPoolRef.getPoolStacks();
        Object object = itemStacks.lock;
        synchronized (object) {
            this.releaseItemInternal(item, itemStacks, itemPool);
        }
    }

    private PO allocInternal(PoolStacks poolStacks, Supplier<PO> allocator) {
        IPooledObject newObj;
        THashSet<IPooledObject> usedStack = poolStacks.inUse;
        List<IPooledObject> releasedStack = poolStacks.released;
        if (!releasedStack.isEmpty()) {
            newObj = releasedStack.remove(releasedStack.size() - 1);
        } else {
            newObj = (IPooledObject)allocator.get();
            if (newObj == null) {
                throw new NullPointerException("Allocator returned a nullPtr. This is not allowed.");
            }
            newObj.setPool(new PoolReference(this, poolStacks));
        }
        newObj.setFree(false);
        usedStack.add(newObj);
        return (PO)newObj;
    }

    private void releaseItemInternal(IPooledObject item, PoolStacks poolStacks, Pool<IPooledObject> itemPool) {
        THashSet<IPooledObject> usedStack = poolStacks.inUse;
        List<IPooledObject> releasedStack = poolStacks.released;
        if (itemPool != this) {
            throw new UnsupportedOperationException("Cannot release item. Not owned by this pool.");
        }
        if (item.isFree()) {
            throw new UnsupportedOperationException("Cannot release item. Already released.");
        }
        if (!usedStack.remove(item)) {
            throw new UnsupportedOperationException("Attempting to release PooledObject not in Pool, possibly releasing on different thread than alloc. " + String.valueOf(item));
        }
        item.setFree(true);
        releasedStack.add(item);
        item.onReleased();
    }

    public static <E> E tryRelease(E obj) {
        IPooledObject pooledObject = Type.tryCastTo(obj, IPooledObject.class);
        if (pooledObject != null && !pooledObject.isFree()) {
            pooledObject.release();
            return null;
        }
        if (obj instanceof List) {
            List list = (List)obj;
            PZArrayUtil.forEach(list, Pool::tryRelease);
            list.clear();
            return obj;
        }
        if (obj instanceof Collection) {
            Collection collection = (Collection)obj;
            PZArrayUtil.forEach(collection, Pool::tryRelease);
            collection.clear();
            return obj;
        }
        if (obj instanceof Iterable) {
            Iterable iterable = (Iterable)obj;
            PZArrayUtil.forEach(iterable, Pool::tryRelease);
            return obj;
        }
        return null;
    }

    public static <E extends IPooledObject> E tryRelease(E pooledObject) {
        if (pooledObject != null && !pooledObject.isFree()) {
            pooledObject.release();
        }
        return null;
    }

    public static <E extends IPooledObject> E[] tryRelease(E[] objArray) {
        PZArrayUtil.forEach(objArray, Pool::tryRelease);
        return null;
    }

    public static final class PoolStacks {
        final THashSet<IPooledObject> inUse = new THashSet();
        final List<IPooledObject> released = new ArrayList<IPooledObject>();
        final Object lock = new Object();

        PoolStacks() {
            this.inUse.setAutoCompactionFactor(0.0f);
        }

        public THashSet<IPooledObject> getInUse() {
            return this.inUse;
        }

        public List<IPooledObject> getReleased() {
            return this.released;
        }
    }

    public static final class PoolReference {
        final Pool<IPooledObject> pool;
        final PoolStacks poolStacks;

        private PoolReference(Pool<IPooledObject> pool, PoolStacks poolStacks) {
            this.pool = pool;
            this.poolStacks = poolStacks;
        }

        public Pool<IPooledObject> getPool() {
            return this.pool;
        }

        private PoolStacks getPoolStacks() {
            return this.poolStacks;
        }

        public void release(IPooledObject item) {
            this.pool.release(item);
        }
    }
}

