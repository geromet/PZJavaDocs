/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.utils;

import java.nio.ByteBuffer;

public record SquareCoord(int x, int y, int z) {
    public static SquareCoord load(ByteBuffer bb) {
        return new SquareCoord(bb.getInt(), bb.getInt(), bb.getInt());
    }

    public void save(ByteBuffer bb) {
        bb.putInt(this.x);
        bb.putInt(this.y);
        bb.putInt(this.z);
    }
}

