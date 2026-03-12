/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.list.PZArrayUtil;

public final class PooledFloatArrayObject
extends PooledObject {
    private static final Pool<PooledFloatArrayObject> s_pool = new Pool<PooledFloatArrayObject>(PooledFloatArrayObject::new);
    private float[] array = PZArrayUtil.emptyFloatArray;

    public static PooledFloatArrayObject alloc(int count) {
        PooledFloatArrayObject newObject = s_pool.alloc();
        newObject.initCapacity(count);
        return newObject;
    }

    public static PooledFloatArrayObject toArray(PooledFloatArrayObject source2) {
        if (source2 == null) {
            return null;
        }
        int sourceCount = source2.length();
        PooledFloatArrayObject newObject = PooledFloatArrayObject.alloc(sourceCount);
        if (sourceCount > 0) {
            System.arraycopy(source2.array(), 0, newObject.array(), 0, sourceCount);
        }
        return newObject;
    }

    private void initCapacity(int count) {
        if (this.array.length != count) {
            this.array = new float[count];
        }
    }

    public float[] array() {
        return this.array;
    }

    public float get(int idx) {
        return this.array[idx];
    }

    public void set(int idx, float val) {
        this.array[idx] = val;
    }

    public int length() {
        return this.array.length;
    }
}

