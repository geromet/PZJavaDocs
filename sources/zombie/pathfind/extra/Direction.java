/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.extra;

import zombie.iso.worldgen.utils.SquareCoord;

public enum Direction {
    NORTH(0, -1, 0),
    SOUTH(0, 1, 0),
    WEST(-1, 0, 0),
    EAST(1, 0, 0),
    NORTH_WEST(-1, -1, 0),
    NORTH_EAST(1, -1, 0),
    SOUTH_WEST(-1, 1, 0),
    SOUTH_EAST(1, 1, 0);

    private final int x;
    private final int y;
    private final int z;

    private Direction(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static SquareCoord move(SquareCoord coords, Direction direction) {
        return direction.move(coords);
    }

    public SquareCoord move(SquareCoord coords) {
        return new SquareCoord(coords.x() + this.x, coords.y() + this.y, coords.z() + this.z);
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }
}

