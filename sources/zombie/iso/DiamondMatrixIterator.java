/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import org.joml.Vector2i;

public class DiamondMatrixIterator {
    private int size;
    private int lineSize;
    private int line;
    private int column;

    public DiamondMatrixIterator(int size) {
        this.size = size;
        this.lineSize = 1;
        this.line = 0;
        this.column = 0;
    }

    public DiamondMatrixIterator reset(int size) {
        this.size = size;
        this.lineSize = 1;
        this.line = 0;
        this.column = 0;
        return this;
    }

    public void reset() {
        this.lineSize = 1;
        this.line = 0;
        this.column = 0;
    }

    public boolean next(Vector2i vec) {
        if (this.lineSize == 0) {
            vec.x = 0;
            vec.y = 0;
            return false;
        }
        if (this.line == 0 && this.column == 0) {
            vec.set(0, 0);
            ++this.column;
            return true;
        }
        if (this.column < this.lineSize) {
            ++vec.x;
            --vec.y;
            ++this.column;
        } else {
            this.column = 1;
            ++this.line;
            if (this.line < this.size) {
                ++this.lineSize;
                vec.x = 0;
                vec.y = this.line;
            } else {
                --this.lineSize;
                vec.x = this.line - this.size + 1;
                vec.y = this.size - 1;
            }
        }
        if (this.lineSize == 0) {
            vec.x = 0;
            vec.y = 0;
            return false;
        }
        return true;
    }

    public Vector2i i2line(int a) {
        int l;
        int v = 0;
        for (l = 1; l < this.size + 1; ++l) {
            if (a + 1 > (v += l)) continue;
            return new Vector2i(a - v + l, l - 1);
        }
        for (l = this.size + 1; l < this.size * 2; ++l) {
            if (a + 1 > (v += this.size * 2 - l)) continue;
            return new Vector2i(a - v + this.size * 2 - l, l - 1);
        }
        return null;
    }

    public Vector2i line2coord(Vector2i a) {
        if (a == null) {
            return null;
        }
        if (a.y < this.size) {
            Vector2i ret = new Vector2i(0, a.y);
            for (int i = 0; i < a.x; ++i) {
                ++ret.x;
                --ret.y;
            }
            return ret;
        }
        Vector2i ret = new Vector2i(a.y - this.size + 1, this.size - 1);
        for (int i = 0; i < a.x; ++i) {
            ++ret.x;
            --ret.y;
        }
        return ret;
    }
}

