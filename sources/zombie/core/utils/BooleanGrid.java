/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BooleanGrid
implements Serializable,
Cloneable {
    private static final long serialVersionUID = 1L;
    private final int width;
    private final int height;
    private final int bitWidth;
    private final int[] value;

    public BooleanGrid(int width, int height) {
        this.bitWidth = width;
        this.width = width / 32 + (width % 32 != 0 ? 1 : 0);
        this.height = height;
        this.value = new int[this.width * this.height];
    }

    public BooleanGrid clone() throws CloneNotSupportedException {
        BooleanGrid ret = new BooleanGrid(this.bitWidth, this.height);
        System.arraycopy(this.value, 0, ret.value, 0, this.value.length);
        return ret;
    }

    public void copy(BooleanGrid src) {
        if (src.bitWidth != this.bitWidth || src.height != this.height) {
            throw new IllegalArgumentException("src must be same size as this: " + String.valueOf(src) + " cannot be copied into " + String.valueOf(this));
        }
        System.arraycopy(src.value, 0, this.value, 0, src.value.length);
    }

    public void clear() {
        Arrays.fill(this.value, 0);
    }

    public void fill() {
        Arrays.fill(this.value, -1);
    }

    private int getIndex(int x, int y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return -1;
        }
        return x + y * this.width;
    }

    public boolean getValue(int x, int y) {
        if (x >= this.bitWidth || x < 0 || y >= this.height || y < 0) {
            return false;
        }
        int xx = x / 32;
        int xxx = 1 << (x & 0x1F);
        int idx = this.getIndex(xx, y);
        if (idx == -1) {
            return false;
        }
        int v = this.value[idx];
        return (v & xxx) != 0;
    }

    public void setValue(int x, int y, boolean newValue) {
        if (x >= this.bitWidth || x < 0 || y >= this.height || y < 0) {
            return;
        }
        int xx = x / 32;
        int xxx = 1 << (x & 0x1F);
        int idx = this.getIndex(xx, y);
        if (idx == -1) {
            return;
        }
        if (newValue) {
            int n = idx;
            this.value[n] = this.value[n] | xxx;
        } else {
            int n = idx;
            this.value[n] = this.value[n] & ~xxx;
        }
    }

    public final int getWidth() {
        return this.width;
    }

    public final int getHeight() {
        return this.height;
    }

    public String toString() {
        return "BooleanGrid [width=" + this.width + ", height=" + this.height + ", bitWidth=" + this.bitWidth + "]";
    }

    public void LoadFromByteBuffer(ByteBuffer cache) {
        int w = this.width * this.height;
        for (int x = 0; x < w; ++x) {
            this.value[x] = cache.getInt();
        }
    }

    public void PutToByteBuffer(ByteBuffer cache) {
        int w = this.width * this.height;
        for (int x = 0; x < w; ++x) {
            cache.putInt(this.value[x]);
        }
    }
}

