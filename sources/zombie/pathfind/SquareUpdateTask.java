/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.pathfind.Chunk;
import zombie.pathfind.ChunkDataZ;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.SlopedSurface;
import zombie.util.Type;

final class SquareUpdateTask {
    private PolygonalMap2 map;
    public int x;
    public int y;
    public int z;
    public int bits;
    public short cost;
    public SlopedSurface slopedSurface;
    private static final ArrayDeque<SquareUpdateTask> pool = new ArrayDeque();

    SquareUpdateTask() {
    }

    public SquareUpdateTask init(PolygonalMap2 map, IsoGridSquare square) {
        this.map = map;
        this.x = square.x;
        this.y = square.y;
        this.z = square.z + 32;
        this.bits = SquareUpdateTask.getBits(square);
        this.cost = SquareUpdateTask.getCost(square);
        if (this.slopedSurface != null) {
            this.slopedSurface.release();
            this.slopedSurface = null;
        }
        this.slopedSurface = SquareUpdateTask.initSlopedSurface(square, this.x - square.chunk.wx * 8, this.y - square.chunk.wy * 8);
        return this;
    }

    public void execute() {
        Chunk chunk = this.map.getChunkFromChunkPos(PZMath.coorddivision(this.x, 8), PZMath.coorddivision(this.y, 8));
        if (chunk != null && chunk.setData(this)) {
            ChunkDataZ.epochCount = (short)(ChunkDataZ.epochCount + 1);
            this.map.rebuild = true;
        }
    }

    public static int getBits(IsoGridSquare sq) {
        int bits = 0;
        if (sq.has(IsoFlagType.solidfloor)) {
            bits |= 0x200;
        }
        if (sq.isSolid()) {
            bits |= 1;
        }
        if (sq.isSolidTrans()) {
            bits |= 0x400;
        }
        if (sq.has(IsoFlagType.collideW)) {
            bits |= 2;
        }
        if (sq.has(IsoFlagType.collideN)) {
            bits |= 4;
        }
        if (sq.has(IsoObjectType.stairsTW)) {
            bits |= 8;
        }
        if (sq.has(IsoObjectType.stairsMW)) {
            bits |= 0x10;
        }
        if (sq.has(IsoObjectType.stairsBW)) {
            bits |= 0x20;
        }
        if (sq.has(IsoObjectType.stairsTN)) {
            bits |= 0x40;
        }
        if (sq.has(IsoObjectType.stairsMN)) {
            bits |= 0x80;
        }
        if (sq.has(IsoObjectType.stairsBN)) {
            bits |= 0x100;
        }
        if (sq.has(IsoFlagType.windowW) || sq.has(IsoFlagType.WindowW)) {
            bits |= 0x802;
            if (SquareUpdateTask.isWindowUnblocked(sq, false)) {
                bits |= 0x100000;
            }
        }
        if (sq.has(IsoFlagType.windowN) || sq.has(IsoFlagType.WindowN)) {
            bits |= 0x1004;
            if (SquareUpdateTask.isWindowUnblocked(sq, true)) {
                bits |= 0x200000;
            }
        }
        if (sq.has(IsoFlagType.canPathW)) {
            bits |= 0x2000;
        }
        if (sq.has(IsoFlagType.canPathN)) {
            bits |= 0x4000;
        }
        boolean bHasDoorW = false;
        boolean bHasDoorN = false;
        for (int i = 0; i < sq.getSpecialObjects().size(); ++i) {
            IsoThumpable isoThumpable;
            IsoObject obj = sq.getSpecialObjects().get(i);
            IsoDirections dir = null;
            if (obj instanceof IsoDoor) {
                IsoDoor isoDoor = (IsoDoor)obj;
                dir = isoDoor.getSpriteEdge(false);
                if (isoDoor.IsOpen()) {
                    dir = isoDoor.getSpriteEdge(true);
                    if (dir == IsoDirections.N) {
                        bits |= 0x800000;
                    } else if (dir == IsoDirections.W) {
                        bits |= 0x400000;
                    }
                    dir = null;
                }
            } else if (obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).isDoor()) {
                dir = isoThumpable.getSpriteEdge(false);
                if (isoThumpable.IsOpen()) {
                    dir = isoThumpable.getSpriteEdge(true);
                    if (dir == IsoDirections.N) {
                        bits |= 0x800000;
                    } else if (dir == IsoDirections.W) {
                        bits |= 0x400000;
                    }
                    dir = null;
                }
            }
            if (dir == IsoDirections.W) {
                bits |= 0x2000;
                bits |= 2;
                bHasDoorW = true;
                continue;
            }
            if (dir == IsoDirections.N) {
                bits |= 0x4000;
                bits |= 4;
                bHasDoorN = true;
                continue;
            }
            if (dir == IsoDirections.S) {
                bits |= 0x80000;
                continue;
            }
            if (dir != IsoDirections.E) continue;
            bits |= 0x40000;
        }
        if (sq.has(IsoFlagType.DoorWallW)) {
            bits |= 0x2000;
            bits |= 2;
            if (!bHasDoorW) {
                bits |= 0x400000;
            }
        }
        if (sq.has(IsoFlagType.DoorWallN)) {
            bits |= 0x4000;
            bits |= 4;
            if (!bHasDoorN) {
                bits |= 0x800000;
            }
        }
        if (SquareUpdateTask.hasSquareThumpable(sq)) {
            bits |= 0x2000;
            bits |= 0x4000;
            bits |= 0x20000;
        }
        if (SquareUpdateTask.hasWallThumpableN(sq)) {
            bits |= 0x14000;
        }
        if (SquareUpdateTask.hasWallThumpableW(sq)) {
            bits |= 0xA000;
        }
        return bits;
    }

    private static boolean isWindowUnblocked(IsoGridSquare sq, boolean north) {
        for (int i = 0; i < sq.getSpecialObjects().size(); ++i) {
            IsoWindow window;
            IsoThumpable thump;
            IsoObject special = sq.getSpecialObjects().get(i);
            if (special instanceof IsoThumpable && (thump = (IsoThumpable)special).isWindow() && north == thump.north) {
                return !thump.isBarricaded();
            }
            if (!(special instanceof IsoWindow) || north != (window = (IsoWindow)special).isNorth()) continue;
            if (window.isBarricaded()) {
                return false;
            }
            if (window.isInvincible()) {
                return false;
            }
            if (window.IsOpen()) {
                return true;
            }
            return window.isDestroyed() && window.isGlassRemoved();
        }
        IsoWindowFrame frame = sq.getWindowFrame(north);
        return frame != null && frame.canClimbThrough(null);
    }

    private static boolean hasSquareThumpable(IsoGridSquare sq) {
        int i;
        if (sq.HasStairs()) {
            return false;
        }
        for (i = 0; i < sq.getSpecialObjects().size(); ++i) {
            IsoThumpable thump = Type.tryCastTo(sq.getSpecialObjects().get(i), IsoThumpable.class);
            if (thump == null || !thump.isThumpable() || !thump.isBlockAllTheSquare()) continue;
            return true;
        }
        for (i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isMovedThumpable()) continue;
            return true;
        }
        return false;
    }

    private static boolean hasWallThumpableN(IsoGridSquare sq) {
        IsoGridSquare n = sq.getAdjacentSquare(IsoDirections.N);
        if (n == null) {
            return false;
        }
        for (int i = 0; i < sq.getSpecialObjects().size(); ++i) {
            IsoThumpable thump;
            IsoObject isoObject = sq.getSpecialObjects().get(i);
            if (!(isoObject instanceof IsoThumpable) || (thump = (IsoThumpable)isoObject).canClimbThrough(null) || thump.canClimbOver(null) || !thump.isThumpable() || thump.isBlockAllTheSquare() || thump.isDoor() || !thump.TestCollide(null, sq, n)) continue;
            return true;
        }
        return false;
    }

    private static boolean hasWallThumpableW(IsoGridSquare sq) {
        IsoGridSquare w = sq.getAdjacentSquare(IsoDirections.W);
        if (w == null) {
            return false;
        }
        for (int i = 0; i < sq.getSpecialObjects().size(); ++i) {
            IsoThumpable thump;
            IsoObject isoObject = sq.getSpecialObjects().get(i);
            if (!(isoObject instanceof IsoThumpable) || (thump = (IsoThumpable)isoObject).canClimbThrough(null) || thump.canClimbOver(null) || !thump.isThumpable() || thump.isBlockAllTheSquare() || thump.isDoor() || !thump.TestCollide(null, sq, w)) continue;
            return true;
        }
        return false;
    }

    public static short getCost(IsoGridSquare sq) {
        short cost = 0;
        if (sq.HasTree() || sq.hasBush()) {
            cost = (short)(cost + 5);
        }
        return cost;
    }

    private static SlopedSurface initSlopedSurface(IsoGridSquare square, int x, int y) {
        if (!square.hasSlopedSurface()) {
            return null;
        }
        SlopedSurface slopedSurface1 = SlopedSurface.alloc();
        slopedSurface1.x = (byte)x;
        slopedSurface1.y = (byte)y;
        slopedSurface1.direction = square.getSlopedSurfaceDirection();
        slopedSurface1.heightMin = square.getSlopedSurfaceHeightMin();
        slopedSurface1.heightMax = square.getSlopedSurfaceHeightMax();
        return slopedSurface1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static SquareUpdateTask alloc() {
        ArrayDeque<SquareUpdateTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            return pool.isEmpty() ? new SquareUpdateTask() : pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void release() {
        ArrayDeque<SquareUpdateTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }
}

