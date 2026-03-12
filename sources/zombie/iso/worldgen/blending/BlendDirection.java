/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.blending;

public enum BlendDirection {
    NORTH(0, 1, 0, -1, 0, 1, 7),
    SOUTH(1, 0, 0, 1, 0, 1, 0),
    WEST(2, 3, -1, 0, 1, 0, 7),
    EAST(3, 2, 1, 0, 1, 0, 0);

    public final int x;
    public final int y;
    public final int planeX;
    public final int planeY;
    public final int index;
    private final int opposite;
    public final byte defaultDepth;

    private BlendDirection(int index, int opposite, int x, int y, int planeX, int planeY, byte defaultDepth) {
        this.x = x;
        this.y = y;
        this.planeX = planeX;
        this.planeY = planeY;
        this.index = index;
        this.opposite = opposite;
        this.defaultDepth = defaultDepth;
    }

    public BlendDirection opposite() {
        return BlendDirection.values()[this.opposite];
    }
}

