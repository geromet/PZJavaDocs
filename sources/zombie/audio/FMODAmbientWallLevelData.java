/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import java.util.ArrayList;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class FMODAmbientWallLevelData
extends PooledObject {
    private static final Pool<FMODAmbientWallLevelData> s_levelDataPool = new Pool<FMODAmbientWallLevelData>(FMODAmbientWallLevelData::new);
    private static final Pool<FMODAmbientWall> s_wallPool = new Pool<FMODAmbientWall>(FMODAmbientWall::new);
    public IsoChunkLevel chunkLevel;
    public final ArrayList<FMODAmbientWall> walls = new ArrayList();
    public boolean dirty = true;

    public FMODAmbientWallLevelData init(IsoChunkLevel chunkLevel) {
        this.chunkLevel = chunkLevel;
        return this;
    }

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.recreate();
        }
    }

    void recreate() {
        IsoGridSquare square;
        FMODAmbientWall wall;
        IPooledObject.release(this.walls);
        IsoChunk chunk = this.chunkLevel.getChunk();
        int level = this.chunkLevel.getLevel();
        IsoGridSquare[] squares = chunk.getSquaresForLevel(level);
        int chunksPerWidth = 8;
        for (int y = 0; y < 8; ++y) {
            wall = null;
            for (int x = 0; x < 8; ++x) {
                square = squares[x + y * 8];
                if (this.shouldAddNorth(square)) {
                    if (wall != null) continue;
                    wall = s_wallPool.alloc();
                    wall.owner = this;
                    wall.x1 = square.x;
                    wall.y1 = square.y;
                    continue;
                }
                if (wall == null) continue;
                wall.x2 = chunk.wx * 8 + x;
                wall.y2 = chunk.wy * 8 + y;
                this.walls.add(wall);
                wall = null;
            }
            if (wall == null) continue;
            wall.x2 = chunk.wx * 8 + 8;
            wall.y2 = chunk.wy * 8 + y;
            this.walls.add(wall);
        }
        for (int x = 0; x < 8; ++x) {
            wall = null;
            for (int y = 0; y < 8; ++y) {
                square = squares[x + y * 8];
                if (this.shouldAddWest(square)) {
                    if (wall != null) continue;
                    wall = s_wallPool.alloc();
                    wall.owner = this;
                    wall.x1 = square.x;
                    wall.y1 = square.y;
                    continue;
                }
                if (wall == null) continue;
                wall.x2 = chunk.wx * 8 + x;
                wall.y2 = chunk.wy * 8 + y;
                this.walls.add(wall);
                wall = null;
            }
            if (wall == null) continue;
            wall.x2 = chunk.wx * 8 + x;
            wall.y2 = chunk.wy * 8 + 8;
            this.walls.add(wall);
        }
    }

    boolean shouldAddNorth(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
        if (squareN == null || FMODAmbientWallLevelData.isOutside(square) == FMODAmbientWallLevelData.isOutside(squareN)) {
            return false;
        }
        return FMODAmbientWallLevelData.passesSoundNorth(square, true);
    }

    public static boolean passesSoundNorth(IsoGridSquare square, boolean bDoorAndWindowRattlesWhenClosed) {
        IsoObject wall;
        if (square == null) {
            return false;
        }
        if (square.getProperties().has(IsoFlagType.WallN) && (wall = square.getWall(true)) != null) {
            return wall.getProperties().has(IsoFlagType.HoppableN) || wall.getProperties().has(IsoFlagType.SpearOnlyAttackThrough);
        }
        if (square.getProperties().has(IsoFlagType.WallNW)) {
            return false;
        }
        if (!bDoorAndWindowRattlesWhenClosed) {
            IsoObject door;
            if (square.has(IsoFlagType.doorN) && FMODAmbientWallLevelData.isDoorBlocked(door = square.getDoor(true))) {
                return false;
            }
            if (square.has(IsoFlagType.WindowN) && FMODAmbientWallLevelData.isWindowBlocked(square.getWindow(true))) {
                return false;
            }
        }
        return true;
    }

    boolean shouldAddWest(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
        if (squareW == null || FMODAmbientWallLevelData.isOutside(square) == FMODAmbientWallLevelData.isOutside(squareW)) {
            return false;
        }
        return FMODAmbientWallLevelData.passesSoundWest(square, true);
    }

    public static boolean passesSoundWest(IsoGridSquare square, boolean bDoorAndWindowRattlesWhenClosed) {
        IsoObject wall;
        if (square == null) {
            return false;
        }
        if (square.getProperties().has(IsoFlagType.WallW) && (wall = square.getWall(false)) != null) {
            return wall.getProperties().has(IsoFlagType.HoppableW) || wall.getProperties().has(IsoFlagType.SpearOnlyAttackThrough);
        }
        if (square.getProperties().has(IsoFlagType.WallNW)) {
            return false;
        }
        if (!bDoorAndWindowRattlesWhenClosed) {
            IsoObject door;
            if (square.has(IsoFlagType.doorW) && FMODAmbientWallLevelData.isDoorBlocked(door = square.getDoor(false))) {
                return false;
            }
            if (square.has(IsoFlagType.WindowW) && FMODAmbientWallLevelData.isWindowBlocked(square.getWindow(false))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOutside(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        if (square.getRoom() != null) {
            return false;
        }
        if (square.haveRoof && square.associatedBuilding == null) {
            return false;
        }
        if (square.haveRoof) {
            for (int z = square.getZ() - 1; z >= 0; --z) {
                IsoGridSquare square1 = IsoWorld.instance.currentCell.getGridSquare(square.getX(), square.getY(), z);
                if (square1 == null || square1.getRoom() == null) continue;
                return false;
            }
        }
        return true;
    }

    static boolean isDoorBlocked(IsoObject object) {
        if (object instanceof IsoDoor) {
            IsoDoor door = (IsoDoor)object;
            return !door.IsOpen();
        }
        if (object instanceof IsoThumpable) {
            IsoThumpable door = (IsoThumpable)object;
            return !door.IsOpen();
        }
        return false;
    }

    static boolean isWindowBlocked(IsoWindow window) {
        if (window == null) {
            return false;
        }
        if (!window.IsOpen() && !window.isDestroyed()) {
            return true;
        }
        IsoBarricade barricade1 = window.getBarricadeOnSameSquare();
        if (barricade1 != null && barricade1.isMetal()) {
            return true;
        }
        IsoBarricade barricade2 = window.getBarricadeOnOppositeSquare();
        if (barricade2 != null && barricade2.isMetal()) {
            return true;
        }
        int numPlanks1 = barricade1 == null ? 0 : barricade1.getNumPlanks();
        int numPlanks2 = barricade2 == null ? 0 : barricade2.getNumPlanks();
        return numPlanks1 == 4 || numPlanks2 == 4;
    }

    public static FMODAmbientWallLevelData alloc() {
        return s_levelDataPool.alloc();
    }

    @Override
    public void onReleased() {
        IPooledObject.release(this.walls);
        this.dirty = true;
    }

    public static final class FMODAmbientWall
    extends PooledObject {
        FMODAmbientWallLevelData owner;
        public int x1;
        public int y1;
        public int x2;
        public int y2;

        public boolean isHorizontal() {
            return this.y1 == this.y2;
        }
    }
}

