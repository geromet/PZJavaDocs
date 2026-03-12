/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.util.List;
import zombie.util.Pool;

public interface IPooledObject {
    public Pool.PoolReference getPoolReference();

    public void setPool(Pool.PoolReference var1);

    public void release();

    public boolean isFree();

    public void setFree(boolean var1);

    default public void onReleased() {
    }

    public static <E extends IPooledObject> E[] release(E[] list) {
        int count = list.length;
        for (int i = 0; i < count; ++i) {
            Pool.tryRelease(list[i]);
        }
        return null;
    }

    public static <E extends IPooledObject> E[] tryReleaseAndBlank(E[] list) {
        if (list != null) {
            return IPooledObject.releaseAndBlank(list);
        }
        return null;
    }

    public static <E extends IPooledObject> E[] releaseAndBlank(E[] list) {
        int count = list.length;
        for (int i = 0; i < count; ++i) {
            list[i] = Pool.tryRelease(list[i]);
        }
        return null;
    }

    public static void release(List<? extends IPooledObject> list) {
        int count = list.size();
        for (int i = 0; i < count; ++i) {
            Pool.tryRelease(list.get(i));
        }
        list.clear();
    }
}

