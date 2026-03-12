/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.Styles;

import java.io.Serializable;

public class ShortList
implements Serializable {
    private static final long serialVersionUID = 1L;
    private short[] value;
    private short count;
    private final boolean fastExpand;

    public ShortList() {
        this(0);
    }

    public ShortList(int size) {
        this(true, size);
    }

    public ShortList(boolean fastExpand, int size) {
        this.fastExpand = fastExpand;
        this.value = new short[size];
    }

    public short add(short f) {
        if (this.count == this.value.length) {
            short[] oldValue = this.value;
            this.value = this.fastExpand ? new short[(oldValue.length << 1) + 1] : new short[oldValue.length + 1];
            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }
        this.value[this.count] = f;
        short s = this.count;
        this.count = (short)(s + 1);
        return s;
    }

    public short remove(int idx) {
        if (idx >= this.count || idx < 0) {
            throw new IndexOutOfBoundsException("Referenced " + idx + ", size=" + this.count);
        }
        short ret = this.value[idx];
        if (idx < this.count - 1) {
            System.arraycopy(this.value, idx + 1, this.value, idx, this.count - idx - 1);
        }
        this.count = (short)(this.count - 1);
        return ret;
    }

    public void addAll(short[] f) {
        this.ensureCapacity(this.count + f.length);
        System.arraycopy(f, 0, this.value, this.count, f.length);
        this.count = (short)(this.count + f.length);
    }

    public void addAll(ShortList f) {
        this.ensureCapacity(this.count + f.count);
        System.arraycopy(f.value, 0, this.value, this.count, f.count);
        this.count = (short)(this.count + f.count);
    }

    public short[] array() {
        return this.value;
    }

    public int capacity() {
        return this.value.length;
    }

    public void clear() {
        this.count = 0;
    }

    public void ensureCapacity(int size) {
        if (this.value.length >= size) {
            return;
        }
        short[] oldValue = this.value;
        this.value = new short[size];
        System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
    }

    public short get(int index) {
        return this.value[index];
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public int size() {
        return this.count;
    }

    public short[] toArray(short[] dest) {
        if (dest == null) {
            dest = new short[this.count];
        }
        System.arraycopy(this.value, 0, dest, 0, this.count);
        return dest;
    }

    public void trimToSize() {
        if (this.count == this.value.length) {
            return;
        }
        short[] oldValue = this.value;
        this.value = new short[this.count];
        System.arraycopy(oldValue, 0, this.value, 0, this.count);
    }
}

