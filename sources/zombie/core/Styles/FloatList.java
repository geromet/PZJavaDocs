/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.Styles;

import java.io.Serializable;

public class FloatList
implements Serializable {
    private static final long serialVersionUID = 1L;
    private float[] value;
    private int count;
    private final ExpandStyle expandStyle;

    public FloatList() {
        this(0);
    }

    public FloatList(int size) {
        this(ExpandStyle.Fast, size);
    }

    public FloatList(ExpandStyle style, int size) {
        this.expandStyle = style;
        this.value = new float[size];
    }

    public float add(float f) {
        if (this.count == this.value.length) {
            float[] oldValue = this.value;
            this.value = this.expandStyle == ExpandStyle.Fast ? new float[(oldValue.length << 1) + 1] : (this.expandStyle == ExpandStyle.Normal ? new float[oldValue.length + oldValue.length / 10 + 10] : new float[oldValue.length + 1]);
            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }
        this.value[this.count] = f;
        return this.count++;
    }

    public float remove(int idx) {
        if (idx >= this.count || idx < 0) {
            throw new IndexOutOfBoundsException("Referenced " + idx + ", size=" + this.count);
        }
        float ret = this.value[idx];
        if (idx < this.count - 1) {
            System.arraycopy(this.value, idx + 1, this.value, idx, this.count - idx - 1);
        }
        --this.count;
        return ret;
    }

    public void addAll(float[] f) {
        this.ensureCapacity(this.count + f.length);
        System.arraycopy(f, 0, this.value, this.count, f.length);
        this.count += f.length;
    }

    public void addAll(FloatList f) {
        this.ensureCapacity(this.count + f.count);
        System.arraycopy(f.value, 0, this.value, this.count, f.count);
        this.count += f.count;
    }

    public float[] array() {
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
        float[] oldValue = this.value;
        this.value = new float[size];
        System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
    }

    public float get(int index) {
        return this.value[index];
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public int size() {
        return this.count;
    }

    public void toArray(Object[] dest) {
        System.arraycopy(this.value, 0, dest, 0, this.count);
    }

    public void trimToSize() {
        if (this.count == this.value.length) {
            return;
        }
        float[] oldValue = this.value;
        this.value = new float[this.count];
        System.arraycopy(oldValue, 0, this.value, 0, this.count);
    }

    public static enum ExpandStyle {
        Slow,
        Normal,
        Fast;

    }
}

