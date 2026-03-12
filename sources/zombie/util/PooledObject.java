/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import zombie.util.IPooledObject;
import zombie.util.Pool;

public abstract class PooledObject
implements IPooledObject {
    private boolean isFree = true;
    private Pool.PoolReference pool;

    @Override
    public final Pool.PoolReference getPoolReference() {
        return this.pool;
    }

    @Override
    public final synchronized void setPool(Pool.PoolReference pool) {
        this.pool = pool;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public final synchronized void release() {
        if (this.pool != null) {
            Pool<IPooledObject> pool = this.pool.pool;
            synchronized (pool) {
                this.pool.release(this);
            }
        } else {
            this.onReleased();
        }
    }

    @Override
    public final synchronized boolean isFree() {
        return this.isFree;
    }

    @Override
    public final synchronized void setFree(boolean isFree) {
        this.isFree = isFree;
    }
}

