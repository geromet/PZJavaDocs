/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import java.io.Serializable;
import java.util.Arrays;

public class IntGrid
implements Serializable,
Cloneable {
    private static final long serialVersionUID = 1L;
    private final int width;
    private final int height;
    private final int[] value;

    public IntGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.value = new int[width * height];
    }

    public IntGrid clone() throws CloneNotSupportedException {
        IntGrid ret = new IntGrid(this.width, this.height);
        System.arraycopy(this.value, 0, ret.value, 0, this.value.length);
        return ret;
    }

    public void clear() {
        Arrays.fill(this.value, 0);
    }

    public void fill(int newValue) {
        Arrays.fill(this.value, newValue);
    }

    private int getIndex(int x, int y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return -1;
        }
        return x + y * this.width;
    }

    public int getValue(int x, int y) {
        int idx = this.getIndex(x, y);
        if (idx == -1) {
            return 0;
        }
        return this.value[idx];
    }

    public void setValue(int x, int y, int newValue) {
        int idx = this.getIndex(x, y);
        if (idx == -1) {
            return;
        }
        this.value[idx] = newValue;
    }

    public final int getWidth() {
        return this.width;
    }

    public final int getHeight() {
        return this.height;
    }
}

