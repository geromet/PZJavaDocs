/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import java.io.Serializable;
import java.util.Arrays;

public class ExpandableBooleanList
implements Serializable,
Cloneable {
    private static final long serialVersionUID = 1L;
    private int width;
    private int bitWidth;
    private int[] value;

    public ExpandableBooleanList(int width) {
        this.bitWidth = width;
        this.width = width / 32 + (width % 32 != 0 ? 1 : 0);
        this.value = new int[width];
    }

    public ExpandableBooleanList clone() throws CloneNotSupportedException {
        ExpandableBooleanList ret = new ExpandableBooleanList(this.bitWidth);
        System.arraycopy(this.value, 0, ret.value, 0, this.value.length);
        return ret;
    }

    public void clear() {
        Arrays.fill(this.value, 0);
    }

    public void fill() {
        Arrays.fill(this.value, -1);
    }

    public boolean getValue(int x) {
        if (x < 0 || x >= this.bitWidth) {
            return false;
        }
        int xx = x >> 5;
        int v = this.value[xx];
        int xxx = 1 << (x & 0x1F);
        return (v & xxx) != 0;
    }

    public void setValue(int x, boolean newValue) {
        if (x < 0) {
            return;
        }
        if (x >= this.bitWidth) {
            int[] oldValue = this.value;
            this.bitWidth = Math.max(this.bitWidth * 2, x + 1);
            this.width = this.bitWidth / 32 + (this.width % 32 != 0 ? 1 : 0);
            this.value = new int[this.width];
            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }
        int xx = x >> 5;
        int xxx = 1 << (x & 0x1F);
        if (newValue) {
            int n = xx;
            this.value[n] = this.value[n] | xxx;
        } else {
            int n = xx;
            this.value[n] = this.value[n] & ~xxx;
        }
    }

    public final int getWidth() {
        return this.width;
    }
}

