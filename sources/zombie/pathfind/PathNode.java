/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

public final class PathNode {
    public float x;
    public float y;
    public float z;
    int flags;

    PathNode init(float x, float y, float z, int flags) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.flags = flags;
        return this;
    }

    PathNode init(PathNode other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.flags = other.flags;
        return this;
    }

    boolean hasFlag(int flag) {
        return (this.flags & flag) != 0;
    }

    boolean isApproximatelyEqual(float x, float y, float z) {
        return Math.abs(this.x - x) < 0.01f && Math.abs(this.y - y) < 0.01f && Math.abs(this.z - z) < 0.01f;
    }

    boolean isApproximatelyEqual(PathNode other) {
        return this.isApproximatelyEqual(other.x, other.y, other.z);
    }
}

