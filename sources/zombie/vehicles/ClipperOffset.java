/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.nio.ByteBuffer;

public final class ClipperOffset {
    private final long address = this.newInstance();

    private native long newInstance();

    public native void clear();

    public native void addPath(int var1, ByteBuffer var2, int var3, int var4);

    public native void execute(double var1);

    public native int getPolygonCount();

    public native int getPolygon(int var1, ByteBuffer var2);

    public static enum EndType {
        Polygon,
        Joined,
        Butt,
        Square,
        Round;

    }

    public static enum JoinType {
        Square,
        Bevel,
        Round,
        Miter;

    }
}

