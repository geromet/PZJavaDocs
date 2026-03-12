/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;
import zombie.pathfind.PMMover;
import zombie.pathfind.PolygonalMap2;

public final class Square {
    static int nextID = 1;
    Integer id = nextID++;
    int x;
    int y;
    int z;
    int bits;
    short cost;
    IsoDirections slopedSurfaceDirection;
    float slopedSurfaceHeightMin;
    float slopedSurfaceHeightMax;
    static final ArrayDeque<Square> pool = new ArrayDeque();

    Square() {
    }

    Square init(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.bits = 0;
        this.cost = 0;
        this.slopedSurfaceDirection = null;
        this.slopedSurfaceHeightMin = 0.0f;
        this.slopedSurfaceHeightMax = 0.0f;
        return this;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public boolean has(int bit) {
        return (this.bits & bit) != 0;
    }

    public boolean TreatAsSolidFloor() {
        return this.has(512) || this.has(504);
    }

    public boolean isReallySolid() {
        return this.has(1) || this.has(1024) && !this.isAdjacentToWindow() && !this.isAdjacentToHoppable();
    }

    boolean isNonThumpableSolid() {
        return this.isReallySolid() && !this.has(131072);
    }

    boolean isCanPathW() {
        if (this.has(8192)) {
            return true;
        }
        Square w = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
        return w != null && (w.has(131072) || w.has(262144));
    }

    boolean isCanPathN() {
        if (this.has(16384)) {
            return true;
        }
        Square n = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
        return n != null && (n.has(131072) || n.has(524288));
    }

    boolean isCollideW() {
        if (this.has(2)) {
            return true;
        }
        Square w = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
        return w != null && (w.has(262144) || w.has(448) || w.isReallySolid());
    }

    boolean isCollideN() {
        if (this.has(4)) {
            return true;
        }
        Square n = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
        return n != null && (n.has(524288) || n.has(56) || n.isReallySolid());
    }

    boolean isThumpW() {
        if (this.has(32768)) {
            return true;
        }
        Square w = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
        return w != null && w.has(131072);
    }

    boolean isThumpN() {
        if (this.has(65536)) {
            return true;
        }
        Square n = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
        return n != null && n.has(131072);
    }

    boolean isAdjacentToWindow() {
        if (this.has(2048) || this.has(4096)) {
            return true;
        }
        Square s = PolygonalMap2.instance.getSquareRawZ(this.x, this.y + 1, this.z);
        if (s != null && s.has(4096)) {
            return true;
        }
        Square e = PolygonalMap2.instance.getSquareRawZ(this.x + 1, this.y, this.z);
        return e != null && e.has(2048);
    }

    boolean isAdjacentToHoppable() {
        if (this.has(0x1000000) || this.has(0x2000000)) {
            return true;
        }
        Square s = PolygonalMap2.instance.getSquareRawZ(this.x, this.y + 1, this.z);
        if (s != null && s.has(0x1000000)) {
            return true;
        }
        Square e = PolygonalMap2.instance.getSquareRawZ(this.x + 1, this.y, this.z);
        return e != null && e.has(0x2000000);
    }

    public boolean isUnblockedWindowN() {
        if (!this.has(0x200000)) {
            return false;
        }
        if (this.isReallySolid()) {
            return false;
        }
        Square n = PolygonalMap2.instance.getSquare(this.x, this.y - 1, this.z);
        return n != null && !n.isReallySolid();
    }

    public boolean isUnblockedWindowW() {
        if (!this.has(0x100000)) {
            return false;
        }
        if (this.isReallySolid()) {
            return false;
        }
        Square w = PolygonalMap2.instance.getSquare(this.x - 1, this.y, this.z);
        return w != null && !w.isReallySolid();
    }

    boolean isUnblockedDoorN() {
        if (!this.has(0x800000)) {
            return false;
        }
        if (this.has(1025)) {
            return false;
        }
        Square n = PolygonalMap2.instance.getSquare(this.x, this.y - 1, this.z);
        return n != null && !n.has(1025);
    }

    boolean isUnblockedDoorW() {
        if (!this.has(0x400000)) {
            return false;
        }
        if (this.has(1025)) {
            return false;
        }
        Square w = PolygonalMap2.instance.getSquare(this.x - 1, this.y, this.z);
        return w != null && !w.has(1025);
    }

    public Square getAdjacentSquare(IsoDirections dir) {
        return PolygonalMap2.instance.getSquare(this.getX() + dir.dx(), this.getY() + dir.dy(), this.getZ());
    }

    public boolean isInside(int x1, int y1, int x2, int y2) {
        return this.getX() >= x1 && this.getX() < x2 && this.getY() >= y1 && this.getY() < y2;
    }

    public boolean testPathFindAdjacent(PMMover mover, int dx, int dy, int dz) {
        if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || dz < -1 || dz > 1) {
            return true;
        }
        if (dx == 0 && dy == 0 && dz == 0) {
            return false;
        }
        return PolygonalMap2.instance.canNotMoveBetween(mover, this.getX(), this.getY(), this.getZ(), this.getX() + dx, this.getY() + dy, this.getZ() + dz);
    }

    public boolean hasTransitionToLevelAbove(IsoDirections edge) {
        if (edge == IsoDirections.N && this.has(64)) {
            return true;
        }
        if (edge == IsoDirections.W && this.has(8)) {
            return true;
        }
        return this.hasSlopedSurfaceToLevelAbove(edge);
    }

    public boolean hasSlopedSurface() {
        return this.slopedSurfaceDirection != null;
    }

    public IsoDirections getSlopedSurfaceDirection() {
        return this.slopedSurfaceDirection;
    }

    public float getSlopedSurfaceHeightMin() {
        return this.slopedSurfaceHeightMin;
    }

    public float getSlopedSurfaceHeightMax() {
        return this.slopedSurfaceHeightMax;
    }

    public boolean hasIdenticalSlopedSurface(Square other) {
        return this.getSlopedSurfaceDirection() == other.getSlopedSurfaceDirection() && this.getSlopedSurfaceHeightMin() == other.getSlopedSurfaceHeightMin() && this.getSlopedSurfaceHeightMax() == other.getSlopedSurfaceHeightMax();
    }

    public boolean isSlopedSurfaceDirectionVertical() {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        return dir == IsoDirections.N || dir == IsoDirections.S;
    }

    public boolean isSlopedSurfaceDirectionHorizontal() {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        return dir == IsoDirections.W || dir == IsoDirections.E;
    }

    public float getSlopedSurfaceHeight(float dx, float dy) {
        float z;
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null) {
            return 0.0f;
        }
        dx = PZMath.clamp(dx, 0.0f, 1.0f);
        dy = PZMath.clamp(dy, 0.0f, 1.0f);
        float slopeHeightMin = this.getSlopedSurfaceHeightMin();
        float slopeHeightMax = this.getSlopedSurfaceHeightMax();
        switch (dir) {
            case N: {
                float f = PZMath.lerp(slopeHeightMin, slopeHeightMax, 1.0f - dy);
                break;
            }
            case S: {
                float f = PZMath.lerp(slopeHeightMin, slopeHeightMax, dy);
                break;
            }
            case W: {
                float f = PZMath.lerp(slopeHeightMin, slopeHeightMax, 1.0f - dx);
                break;
            }
            case E: {
                float f = PZMath.lerp(slopeHeightMin, slopeHeightMax, dx);
                break;
            }
            default: {
                float f = z = -1.0f;
            }
        }
        if (z < 0.0f) {
            return 0.0f;
        }
        return z;
    }

    public float getSlopedSurfaceHeight(IsoDirections edge) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        if (slopeDir == null) {
            return 0.0f;
        }
        if (slopeDir == edge) {
            return this.getSlopedSurfaceHeightMax();
        }
        if (slopeDir.Rot180() == edge) {
            return this.getSlopedSurfaceHeightMin();
        }
        return -1.0f;
    }

    public boolean isSlopedSurfaceEdgeBlocked(IsoDirections edge) {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null || edge == null) {
            return false;
        }
        Square square2 = this.getAdjacentSquare(edge);
        if (square2 == null) {
            return true;
        }
        return this.getSlopedSurfaceHeight(edge) != square2.getSlopedSurfaceHeight(edge.Rot180());
    }

    public boolean hasSlopedSurfaceToLevelAbove(IsoDirections dir) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        if (slopeDir == null) {
            return false;
        }
        return this.getSlopedSurfaceHeight(dir) == 1.0f;
    }

    public boolean hasSlopedSurfaceBottom(IsoDirections slopeDir) {
        if (!this.hasSlopedSurface()) {
            return false;
        }
        return this.getSlopedSurfaceHeight(slopeDir.Rot180()) == 0.0f;
    }

    static Square alloc() {
        return pool.isEmpty() ? new Square() : pool.pop();
    }

    void release() {
        assert (!pool.contains(this));
        pool.push(this);
    }
}

