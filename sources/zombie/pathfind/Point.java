/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

public final class Point {
    public int x;
    public int y;

    Point init(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object other) {
        if (!(other instanceof Point)) return false;
        Point point = (Point)other;
        if (point.x != this.x) return false;
        if (point.y != this.y) return false;
        return true;
    }
}

