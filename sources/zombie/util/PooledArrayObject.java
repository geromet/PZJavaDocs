/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.util.function.Function;
import zombie.util.PooledObject;

public class PooledArrayObject<T>
extends PooledObject {
    private T[] array;

    public T[] array() {
        return this.array;
    }

    public int length() {
        return this.array.length;
    }

    public T get(int idx) {
        return this.array[idx];
    }

    public void set(int idx, T val) {
        this.array[idx] = val;
    }

    protected void initCapacity(int count, Function<Integer, T[]> allocator) {
        if (this.array == null || this.array.length != count) {
            this.array = allocator.apply(count);
        }
    }

    public boolean isEmpty() {
        return this.array == null || this.array.length == 0;
    }
}

