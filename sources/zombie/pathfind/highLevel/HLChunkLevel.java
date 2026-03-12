/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.highLevel;

import java.util.ArrayList;
import java.util.List;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.pathfind.Chunk;
import zombie.pathfind.ChunkLevel;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.highLevel.HLAStar;
import zombie.pathfind.highLevel.HLChunkRegion;
import zombie.pathfind.highLevel.HLGlobals;
import zombie.pathfind.highLevel.HLLevelTransition;
import zombie.pathfind.highLevel.HLSlopedSurface;
import zombie.pathfind.highLevel.HLStaircase;

public final class HLChunkLevel {
    static final int CPW = 8;
    public int modificationCount = -1;
    int modificationCountStairs = -1;
    final ChunkLevel chunkLevel;
    final ArrayList<HLChunkRegion> regionList = new ArrayList();
    final ArrayList<HLStaircase> stairs = new ArrayList();
    final ArrayList<HLSlopedSurface> slopedSurfaces = new ArrayList();

    public HLChunkLevel(ChunkLevel chunkLevel) {
        this.chunkLevel = chunkLevel;
    }

    Chunk getChunk() {
        return this.chunkLevel.getChunk();
    }

    int getLevel() {
        return this.chunkLevel.getLevel();
    }

    public void initRegions() {
        this.releaseRegions();
        Square[][] squares = this.getChunk().getSquaresForLevel(this.getLevel());
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                Square square = squares[x][y];
                if (!this.canWalkOnSquare(square) || this.isSquareInRegion(square.getX(), square.getY()) || PolygonalMap2.instance.getVisGraphAt((float)square.getX() + 0.5f, (float)square.getY() + 0.5f, square.getZ(), 1) != null && !square.has(504) && !square.hasSlopedSurface()) continue;
                HLChunkRegion region = HLGlobals.chunkRegionPool.alloc();
                region.levelData = this;
                region.squaresMask.clear();
                this.floodFill(region, squares, square);
                region.initEdges();
                this.regionList.add(region);
            }
        }
        if (!this.regionList.isEmpty()) {
            // empty if block
        }
    }

    void releaseRegions() {
        HLGlobals.chunkRegionPool.releaseAll((List<HLChunkRegion>)this.regionList);
        this.regionList.clear();
    }

    void initStairsIfNeeded() {
        if (HLAStar.modificationCount == this.modificationCountStairs) {
            return;
        }
        this.modificationCountStairs = HLAStar.modificationCount;
        this.initStairs();
    }

    void initStairs() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = HLAStar.PerfInitStairs.profile();){
            this.initStairsInternal();
        }
    }

    void initStairsInternal() {
        this.releaseStairs();
        Square[][] squares = this.getChunk().getSquaresForLevel(this.getLevel());
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                HLStaircase stair;
                Square square = squares[x][y];
                if (square == null) continue;
                if (square.has(64)) {
                    stair = HLGlobals.staircasePool.alloc();
                    stair.set(IsoDirections.N, square.getX(), square.getY(), square.getZ());
                    this.stairs.add(stair);
                }
                if (!square.has(8)) continue;
                stair = HLGlobals.staircasePool.alloc();
                stair.set(IsoDirections.W, square.getX(), square.getY(), square.getZ());
                this.stairs.add(stair);
            }
        }
        this.initSlopedSurfaces();
    }

    void releaseStairs() {
        HLGlobals.staircasePool.releaseAll((List<HLStaircase>)this.stairs);
        this.stairs.clear();
    }

    HLStaircase getStaircaseAt(int x, int y) {
        for (int i = 0; i < this.stairs.size(); ++i) {
            HLStaircase staircase = this.stairs.get(i);
            if (!staircase.isBottomFloorAt(x, y)) continue;
            return staircase;
        }
        return null;
    }

    void initSlopedSurfaces() {
        this.releaseSlopedSurfaces();
        Square[][] squares = this.getChunk().getSquaresForLevel(this.getLevel());
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                IsoDirections slopeDir;
                Square square = squares[x][y];
                if (square == null || (slopeDir = square.getSlopedSurfaceDirection()) == null || square.getSlopedSurfaceHeightMax() < 1.0f) continue;
                HLSlopedSurface slopedSurface = HLGlobals.slopedSurfacePool.alloc();
                slopedSurface.set(slopeDir, square.getX(), square.getY(), square.getZ());
                this.slopedSurfaces.add(slopedSurface);
            }
        }
    }

    void releaseSlopedSurfaces() {
        HLGlobals.slopedSurfacePool.releaseAll((List<HLSlopedSurface>)this.slopedSurfaces);
        this.slopedSurfaces.clear();
    }

    HLSlopedSurface getSlopedSurfaceAt(int x, int y) {
        for (int i = 0; i < this.slopedSurfaces.size(); ++i) {
            HLSlopedSurface slopedSurface = this.slopedSurfaces.get(i);
            if (!slopedSurface.isBottomFloorAt(x, y)) continue;
            return slopedSurface;
        }
        return null;
    }

    HLLevelTransition getLevelTransitionAt(int x, int y) {
        HLLevelTransition levelTransition = this.getStaircaseAt(x, y);
        if (levelTransition == null) {
            levelTransition = this.getSlopedSurfaceAt(x, y);
        }
        return levelTransition;
    }

    public void removeFromWorld() {
        this.releaseRegions();
        this.releaseStairs();
        this.releaseSlopedSurfaces();
        this.modificationCount = -1;
        this.modificationCountStairs = -1;
    }

    HLChunkRegion findRegionContainingSquare(int worldSquareX, int worldSquareY) {
        for (int i = 0; i < this.regionList.size(); ++i) {
            HLChunkRegion region = this.regionList.get(i);
            if (!region.containsSquare(worldSquareX, worldSquareY)) continue;
            return region;
        }
        return null;
    }

    boolean isSquareInRegion(int worldSquareX, int worldSquareY) {
        return this.findRegionContainingSquare(worldSquareX, worldSquareY) != null;
    }

    boolean canWalkOnSquare(Square square) {
        if (square == null) {
            return false;
        }
        if (!square.TreatAsSolidFloor()) {
            return false;
        }
        return !square.isReallySolid();
    }

    boolean isCanPathTransition(Square square1, Square square2) {
        if (square1 == null || square2 == null || square1 == square2 || square1.getZ() != square2.getZ()) {
            return false;
        }
        int dx = square2.getX() - square1.getX();
        int dy = square2.getY() - square1.getY();
        if (dx < 0 ? square1.has(8192) : dx > 0 && square2.has(8192)) {
            return true;
        }
        return dy < 0 ? square1.has(16384) : dy > 0 && square2.has(16384);
    }

    void floodFill(HLChunkRegion region, Square[][] squares, Square square) {
        HLGlobals.floodFill.reset();
        HLGlobals.floodFill.calculate(region, squares, square);
    }

    void renderDebug() {
        int i;
        for (i = 0; i < this.regionList.size(); ++i) {
            HLChunkRegion region = this.regionList.get(i);
            region.renderDebug();
        }
        this.initStairsIfNeeded();
        for (i = 0; i < this.stairs.size(); ++i) {
            HLStaircase stair = this.stairs.get(i);
            LineDrawer.addLine((float)stair.getBottomFloorX() + 0.5f, (float)stair.getBottomFloorY() + 0.5f, (float)(stair.getBottomFloorZ() - 32), (float)stair.getTopFloorX() + 0.5f, (float)stair.getTopFloorY() + 0.5f, (float)(stair.getTopFloorZ() - 32), 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}

