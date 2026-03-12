/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.list.PZArrayUtil;

public class IntMap<E>
extends PooledObject {
    private int count;
    private int[] keys;
    private Object[] elements;
    private static final Pool<IntMap<?>> s_pool = new Pool<IntMap>(IntMap::new);

    public static <ET> IntMap<ET> alloc() {
        return s_pool.alloc();
    }

    @Override
    public void onReleased() {
        this.count = 0;
        this.keys = PZArrayUtil.arraySet(this.keys, 0);
        this.elements = PZArrayUtil.arraySet(this.elements, null);
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public E get(int key) {
        int indexOf = this.indexOf(key);
        if (indexOf > -1) {
            return (E)this.elements[indexOf];
        }
        return null;
    }

    public E set(int key, E element) {
        int indexOf = this.indexOf(key);
        if (indexOf > -1) {
            Object oldElement = this.elements[indexOf];
            Pool.tryRelease(oldElement);
            this.elements[indexOf] = element;
        } else if ((indexOf = this.count++) == PZArrayUtil.lengthOf(this.keys)) {
            if (indexOf == 0) {
                this.keys = new int[0];
                this.elements = new Object[0];
            }
            this.keys = PZArrayUtil.add(this.keys, key);
            this.elements = PZArrayUtil.add(this.elements, element);
        } else {
            this.keys[indexOf] = key;
            this.elements[indexOf] = element;
        }
        return (E)this.elements[indexOf];
    }

    private int indexOf(int key) {
        for (int i = 0; i < this.count; ++i) {
            if (this.keys[i] != key) continue;
            return i;
        }
        return -1;
    }
}

