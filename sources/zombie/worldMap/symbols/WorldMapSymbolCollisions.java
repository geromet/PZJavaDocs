/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TFloatArrayList;

public final class WorldMapSymbolCollisions {
    final TFloatArrayList boxes = new TFloatArrayList();
    final TByteArrayList collide = new TByteArrayList();

    boolean addBox(float x, float y, float w, float h, boolean collide) {
        int b1 = this.boxes.size() / 4 - 1;
        int b2 = b1 + 1;
        this.boxes.add(x);
        this.boxes.add(y);
        this.boxes.add(x + w);
        this.boxes.add(y + h);
        this.collide.add(collide ? (byte)1 : 0);
        if (!collide) {
            return false;
        }
        for (int b = 0; b <= b1; ++b) {
            if (!this.isCollision(b, b2)) continue;
            this.boxes.set(b2 * 4, (x += w / 2.0f) - 3.0f - 1.0f);
            this.boxes.set(b2 * 4 + 1, (y += h / 2.0f) - 3.0f - 1.0f);
            this.boxes.set(b2 * 4 + 2, x + 3.0f + 1.0f);
            this.boxes.set(b2 * 4 + 3, y - 3.0f + 1.0f);
            return true;
        }
        return false;
    }

    boolean isCollision(int b1, int b2) {
        if (this.collide.getQuick(b1) == 0 || this.collide.getQuick(b2) == 0) {
            return false;
        }
        float x1 = this.boxes.get(b1 *= 4);
        float y1 = this.boxes.get(b1 + 1);
        float x2 = this.boxes.get(b1 + 2);
        float y2 = this.boxes.get(b1 + 3);
        float x3 = this.boxes.get(b2 *= 4);
        float y3 = this.boxes.get(b2 + 1);
        float x4 = this.boxes.get(b2 + 2);
        float y4 = this.boxes.get(b2 + 3);
        return x1 < x4 && x2 > x3 && y1 < y4 && y2 > y3;
    }

    boolean isCollision(int b1) {
        for (int i = 0; i < this.boxes.size() / 4; ++i) {
            if (i == b1 || !this.isCollision(b1, i)) continue;
            return true;
        }
        return false;
    }
}

