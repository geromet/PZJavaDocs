/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.IsoPropertyType;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.Mouse;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoDoor;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;

public final class FBORenderCutaways {
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();
    private static FBORenderCutaways instance;
    public static final byte CLDSF_NONE = 0;
    public static final byte CLDSF_SHOULD_RENDER = 1;
    public IsoCell cell;
    private final PerPlayerData[] perPlayerData = new PerPlayerData[4];
    private final HashSet<IsoChunk> invalidatedChunks = new HashSet();
    private final ArrayList<PointOfInterest> pointOfInterest = new ArrayList();
    private final ObjectPool<PointOfInterest> pointOfInterestStore = new ObjectPool<PointOfInterest>(PointOfInterest::new);
    private final Rectangle buildingRectTemp = new Rectangle();
    public static final ObjectPool<CutawayWall> s_cutawayWallPool;
    public static final ObjectPool<SlopedSurface> s_slopedSurfacePool;

    public static FBORenderCutaways getInstance() {
        if (instance == null) {
            instance = new FBORenderCutaways();
        }
        return instance;
    }

    private FBORenderCutaways() {
        for (int i = 0; i < this.perPlayerData.length; ++i) {
            this.perPlayerData[i] = new PerPlayerData();
        }
    }

    public boolean checkPlayerRoom(int playerIndex) {
        boolean bForceCutawayUpdate = false;
        IsoGridSquare playerSquare = IsoCamera.frameState.camCharacterSquare;
        if (playerSquare != null) {
            long roomID = playerSquare.getRoomID();
            if (roomID == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(playerSquare)) {
                roomID = playerSquare.associatedBuilding.getRoofRoomID(playerSquare.z);
            }
            PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
            if (perPlayerData1.lastPlayerRoomId != -1L && roomID == -1L) {
                bForceCutawayUpdate = true;
                perPlayerData1.lastPlayerRoomId = -1L;
            } else if (roomID != -1L && perPlayerData1.lastPlayerRoomId != roomID) {
                bForceCutawayUpdate = true;
                perPlayerData1.lastPlayerRoomId = roomID;
            }
        }
        return bForceCutawayUpdate;
    }

    public boolean checkExteriorWalls(ArrayList<IsoChunk> onScreenChunks) {
        IsoGridSquare camCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        if (camCharacterSquare == null) {
            return false;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        int z = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        float zoom = Core.getInstance().getZoom(playerIndex);
        boolean bForceCutawayUpdate = false;
        for (int i = 0; i < onScreenChunks.size(); ++i) {
            IsoChunk chunk = onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            if (!renderLevels.isOnScreen(z)) continue;
            ChunkLevelData chunkLevelData = chunk.getCutawayData().getDataForLevel(z);
            if (chunkLevelData.adjacentChunkLoadedCounter != chunk.adjacentChunkLoadedCounter) {
                chunkLevelData.adjacentChunkLoadedCounter = chunk.adjacentChunkLoadedCounter;
                chunkLevelData.orphanStructures.adjacentChunkLoadedCounter = chunk.adjacentChunkLoadedCounter;
                for (z1 = renderLevels.getMinLevel(z); z1 <= renderLevels.getMaxLevel(z); ++z1) {
                    chunk.getCutawayData().recreateLevel(z1);
                }
                bForceCutawayUpdate = true;
            } else {
                for (z1 = chunk.getMinLevel(); z1 <= chunk.getMaxLevel(); ++z1) {
                    if (!renderLevels.isDirty(z1, 192L, zoom)) continue;
                    chunk.getCutawayData().recreateLevel(z1);
                    bForceCutawayUpdate = true;
                }
            }
            bForceCutawayUpdate |= this.checkOrphanStructures(playerIndex, chunk);
            if (chunkLevelData.exteriorWalls.isEmpty()) continue;
            boolean bInvalidate = false;
            for (int j = 0; j < chunkLevelData.exteriorWalls.size(); ++j) {
                CutawayWall wall = chunkLevelData.exteriorWalls.get(j);
                if (wall.shouldCutawayFence()) {
                    if (wall.isPlayerInRange(playerIndex, PlayerInRange.True)) continue;
                    wall.setPlayerInRange(playerIndex, PlayerInRange.True);
                    wall.setPlayerCutawayFlag(playerIndex, true);
                    bInvalidate = true;
                    continue;
                }
                if (wall.isPlayerInRange(playerIndex, PlayerInRange.False)) continue;
                wall.setPlayerInRange(playerIndex, PlayerInRange.False);
                wall.setPlayerCutawayFlag(playerIndex, false);
                bInvalidate = true;
            }
            if (!bInvalidate) continue;
            renderLevels.invalidateLevel(z, 2048L);
        }
        return bForceCutawayUpdate;
    }

    public boolean checkSlopedSurfaces(ArrayList<IsoChunk> onScreenChunks) {
        IsoGridSquare camCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        if (camCharacterSquare == null) {
            return false;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        int z = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        boolean bForceCutawayUpdate = false;
        for (int i = 0; i < onScreenChunks.size(); ++i) {
            IsoChunk chunk = onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            if (!renderLevels.isOnScreen(z)) continue;
            ChunkLevelData chunkLevelData = chunk.getCutawayData().getDataForLevel(z);
            if (chunkLevelData.slopedSurfaces.isEmpty()) continue;
            boolean bInvalidate = false;
            for (int j = 0; j < chunkLevelData.slopedSurfaces.size(); ++j) {
                SlopedSurface slopedSurface = chunkLevelData.slopedSurfaces.get(j);
                if (slopedSurface.shouldCutaway()) {
                    if (slopedSurface.isPlayerInRange(playerIndex, PlayerInRange.True)) continue;
                    slopedSurface.setPlayerInRange(playerIndex, PlayerInRange.True);
                    slopedSurface.setPlayerCutawayFlag(playerIndex, true);
                    bInvalidate = true;
                    continue;
                }
                if (slopedSurface.isPlayerInRange(playerIndex, PlayerInRange.False)) continue;
                slopedSurface.setPlayerInRange(playerIndex, PlayerInRange.False);
                slopedSurface.setPlayerCutawayFlag(playerIndex, false);
                bInvalidate = true;
            }
            if (!bInvalidate) continue;
            renderLevels.invalidateLevel(z, 2048L);
        }
        return false;
    }

    public void squareChanged(IsoGridSquare square) {
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            this.perPlayerData[i].checkSquare = null;
        }
    }

    public boolean checkOccludedRooms(int playerIndex, ArrayList<IsoChunk> onScreenChunks) {
        IsoGridSquare camCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        if (camCharacterSquare == null) {
            return false;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        if (perPlayerData1.checkSquare == camCharacterSquare) {
            return false;
        }
        perPlayerData1.checkSquare = camCharacterSquare;
        int z = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        boolean bForceCutawayUpdate = false;
        for (int i = 0; i < onScreenChunks.size(); ++i) {
            IsoChunk chunk = onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            if (!renderLevels.isOnScreen(z)) continue;
            ChunkLevelData chunkLevelData = chunk.getCutawayData().getDataForLevel(z);
            if (chunkLevelData.allWalls.isEmpty()) continue;
            boolean bInvalidate = false;
            for (int j = 0; j < chunkLevelData.allWalls.size(); ++j) {
                CutawayWall wall = chunkLevelData.allWalls.get(j);
                if (wall.shouldCutawayBuilding(playerIndex)) {
                    if (wall.isPlayerInRange(playerIndex, PlayerInRange.True)) {
                        int mask = wall.calculateOccludedSquaresMaskForSeenRooms(playerIndex);
                        if (mask == wall.occludedSquaresMaskForSeenRooms[playerIndex]) continue;
                        wall.occludedSquaresMaskForSeenRooms[playerIndex] = mask;
                        bForceCutawayUpdate = true;
                        bInvalidate = true;
                        continue;
                    }
                    wall.setPlayerInRange(playerIndex, PlayerInRange.True);
                    wall.occludedSquaresMaskForSeenRooms[playerIndex] = wall.calculateOccludedSquaresMaskForSeenRooms(playerIndex);
                    bForceCutawayUpdate = true;
                    bInvalidate = true;
                    continue;
                }
                if (wall.isPlayerInRange(playerIndex, PlayerInRange.False)) continue;
                wall.setPlayerInRange(playerIndex, PlayerInRange.False);
                bForceCutawayUpdate = true;
                bInvalidate = true;
            }
            if (!bInvalidate) continue;
            renderLevels.invalidateLevel(z, 2048L);
        }
        return bForceCutawayUpdate;
    }

    boolean checkOrphanStructures(int playerIndex, IsoChunk chunk) {
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        boolean bForceCutawayUpdate = false;
        for (int z2 = PZMath.max(1, chunk.minLevel); z2 <= chunk.maxLevel; ++z2) {
            ChunkLevelData chunkLevelData = chunk.getCutawayData().getDataForLevel(z2);
            OrphanStructures orphanStructures = chunkLevelData.orphanStructures;
            if (!orphanStructures.hasOrphanStructures) continue;
            if (orphanStructures.shouldCutaway()) {
                if (orphanStructures.isPlayerInRange(playerIndex, PlayerInRange.True)) continue;
                orphanStructures.setPlayerInRange(playerIndex, PlayerInRange.True);
                bForceCutawayUpdate = true;
                renderLevels.invalidateLevel(z2, 2048L);
                if (z2 >= chunk.getMaxLevel()) continue;
                renderLevels.invalidateLevel(z2 + 1, 2048L);
                continue;
            }
            if (orphanStructures.isPlayerInRange(playerIndex, PlayerInRange.False)) continue;
            orphanStructures.setPlayerInRange(playerIndex, PlayerInRange.False);
            bForceCutawayUpdate = true;
            renderLevels.invalidateLevel(z2, 2048L);
            if (z2 >= chunk.getMaxLevel()) continue;
            renderLevels.invalidateLevel(z2 + 1, 2048L);
        }
        return bForceCutawayUpdate;
    }

    public void doCutawayVisitSquares(int playerIndex, ArrayList<IsoChunk> onScreenChunks) {
        FBORenderLevels renderLevels;
        IsoChunk chunk;
        int i;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        perPlayerData1.lastCutawayVisitorResults.clear();
        perPlayerData1.lastCutawayVisitorResults.addAll(perPlayerData1.cutawayVisitorResultsNorth);
        perPlayerData1.lastCutawayVisitorResults.addAll(perPlayerData1.cutawayVisitorResultsWest);
        perPlayerData1.cutawayVisitorResultsNorth.clear();
        perPlayerData1.cutawayVisitorResultsWest.clear();
        perPlayerData1.cutawayVisitorVisitedNorth.clear();
        perPlayerData1.cutawayVisitorVisitedWest.clear();
        int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        perPlayerData1.cutawayWalls.clear();
        for (int i2 = 0; i2 < onScreenChunks.size(); ++i2) {
            FBORenderLevels renderLevels2;
            IsoChunk chunk2 = onScreenChunks.get(i2);
            if (playerZ < chunk2.minLevel || playerZ > chunk2.maxLevel || !(renderLevels2 = chunk2.getRenderLevels(playerIndex)).isOnScreen(playerZ)) continue;
            ChunkLevelData levelData = chunk2.getCutawayDataForLevel(playerZ);
            perPlayerData1.cutawayWalls.addAll(levelData.allWalls);
        }
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        long currentTimeMillis = System.currentTimeMillis();
        for (i = 0; i < this.pointOfInterest.size(); ++i) {
            IsoGridSquare sq;
            PointOfInterest poi = this.pointOfInterest.get(i);
            if (poi.z != playerZ || (sq = chunkMap.getGridSquare(poi.x, poi.y, poi.z)) == null || perPlayerData1.cutawayVisitorVisitedNorth.contains(sq) || perPlayerData1.cutawayVisitorVisitedWest.contains(sq)) continue;
            this.doCutawayVisitSquares(sq, currentTimeMillis, onScreenChunks);
        }
        this.invalidatedChunks.clear();
        for (i = 0; i < perPlayerData1.lastCutawayVisitorResults.size(); ++i) {
            IsoGridSquare square = perPlayerData1.lastCutawayVisitorResults.get(i);
            square.setPlayerCutawayFlag(playerIndex, 0, currentTimeMillis);
            chunk = square.getChunk();
            if (chunk == null || this.invalidatedChunks.contains(chunk)) continue;
            this.invalidatedChunks.add(chunk);
            renderLevels = chunk.getRenderLevels(playerIndex);
            renderLevels.invalidateLevel(square.z, 2048L);
            if (chunk.IsOnScreen(false)) continue;
            chunk.getCutawayData().invalidateOccludedSquaresMaskForSeenRooms(playerIndex, square.z);
        }
        for (i = 0; i < onScreenChunks.size(); ++i) {
            FBORenderLevels renderLevels3;
            IsoChunk chunk3 = onScreenChunks.get(i);
            if (playerZ < chunk3.minLevel || playerZ > chunk3.maxLevel || !(renderLevels3 = chunk3.getRenderLevels(playerIndex)).isOnScreen(playerZ)) continue;
            ChunkLevelData levelData = chunk3.getCutawayDataForLevel(playerZ);
            for (int j = 0; j < levelData.exteriorWalls.size(); ++j) {
                CutawayWall wall = levelData.exteriorWalls.get(j);
                if (wall.isPlayerInRange(playerIndex, PlayerInRange.False)) continue;
                wall.setVisitedSquares(perPlayerData1);
            }
        }
        for (IsoGridSquare square : perPlayerData1.cutawayVisitorResultsNorth) {
            square.addPlayerCutawayFlag(playerIndex, 1, currentTimeMillis);
            chunk = square.getChunk();
            if (chunk == null || this.invalidatedChunks.contains(chunk)) continue;
            this.invalidatedChunks.add(chunk);
            renderLevels = chunk.getRenderLevels(playerIndex);
            renderLevels.invalidateLevel(square.z, 2048L);
        }
        for (IsoGridSquare square : perPlayerData1.cutawayVisitorResultsWest) {
            square.addPlayerCutawayFlag(playerIndex, 2, currentTimeMillis);
            chunk = square.getChunk();
            if (chunk == null || this.invalidatedChunks.contains(chunk)) continue;
            this.invalidatedChunks.add(chunk);
            renderLevels = chunk.getRenderLevels(playerIndex);
            renderLevels.invalidateLevel(square.z, 2048L);
        }
        for (IsoChunk chunk3 : this.invalidatedChunks) {
            boolean bHasCutawayWestWallsOnSouthEdge;
            boolean bHasCutawayWestWallsOnNorthEdge;
            boolean bHasCutawayNorthWallsOnEastEdge;
            ChunkLevelData levelData = chunk3.getCutawayData().getDataForLevel(playerZ);
            if (levelData == null) continue;
            boolean bHasCutawayNorthWallsOnWestEdge = this.hasAnyCutawayWalls(playerIndex, chunk3, playerZ, (byte)1, 0, 0, 0, 7);
            if (bHasCutawayNorthWallsOnWestEdge != levelData.hasCutawayNorthWallsOnWestEdge) {
                levelData.hasCutawayNorthWallsOnWestEdge = bHasCutawayNorthWallsOnWestEdge;
                this.invalidateChunk(playerIndex, chunkMap, chunk3.wx - 1, chunk3.wy, playerZ);
            }
            if ((bHasCutawayNorthWallsOnEastEdge = this.hasAnyCutawayWalls(playerIndex, chunk3, playerZ, (byte)1, 7, 0, 7, 7)) != levelData.hasCutawayNorthWallsOnEastEdge) {
                levelData.hasCutawayNorthWallsOnEastEdge = bHasCutawayNorthWallsOnEastEdge;
                this.invalidateChunk(playerIndex, chunkMap, chunk3.wx + 1, chunk3.wy, playerZ);
            }
            if ((bHasCutawayWestWallsOnNorthEdge = this.hasAnyCutawayWalls(playerIndex, chunk3, playerZ, (byte)2, 0, 0, 7, 0)) != levelData.hasCutawayWestWallsOnNorthEdge) {
                levelData.hasCutawayWestWallsOnNorthEdge = bHasCutawayWestWallsOnNorthEdge;
                this.invalidateChunk(playerIndex, chunkMap, chunk3.wx, chunk3.wy - 1, playerZ);
            }
            if ((bHasCutawayWestWallsOnSouthEdge = this.hasAnyCutawayWalls(playerIndex, chunk3, playerZ, (byte)2, 0, 7, 7, 7)) == levelData.hasCutawayWestWallsOnSouthEdge) continue;
            levelData.hasCutawayWestWallsOnSouthEdge = bHasCutawayWestWallsOnNorthEdge;
            this.invalidateChunk(playerIndex, chunkMap, chunk3.wx, chunk3.wy + 1, playerZ);
        }
    }

    private void invalidateChunk(int playerIndex, IsoChunkMap chunkMap, int wx, int wy, int z) {
        IsoChunk chunk = chunkMap.getChunk(wx - chunkMap.getWorldXMin(), wy - chunkMap.getWorldYMin());
        if (chunk == null) {
            return;
        }
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        renderLevels.invalidateLevel(z, 2048L);
    }

    private boolean hasAnyCutawayWalls(int playerIndex, IsoChunk chunk, int z, byte pcf, int x1, int y1, int x2, int y2) {
        long currentTimeMs = 0L;
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                IsoGridSquare square = chunk.getGridSquare(x, y, z);
                if (square == null || (square.getPlayerCutawayFlag(playerIndex, 0L) & pcf) == 0) continue;
                return true;
            }
        }
        return false;
    }

    private void doCutawayVisitSquares(IsoGridSquare sq, long currentTimeMillis, ArrayList<IsoChunk> onScreenChunks) {
        this.cutawayVisit(sq, currentTimeMillis, onScreenChunks);
    }

    private boolean IsCutawaySquare(CutawayWall wall, IsoGridSquare poiSquare, IsoGridSquare square, long currentTimeMillis) {
        ArrayList<Long> tempPlayerCutawayRoomIDs;
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (square == null) {
            return false;
        }
        if (poiSquare.getZ() != square.getZ()) {
            return false;
        }
        if (poiSquare.getRoom() == null || square.getRoom() == null || poiSquare.getBuilding() != square.getRoom().building) {
            // empty if block
        }
        if ((tempPlayerCutawayRoomIDs = this.cell.tempPlayerCutawayRoomIds.get(playerIndex)).isEmpty()) {
            return this.IsCollapsibleBuildingSquare(wall, square);
        }
        if (this.isCutawayDueToPeeking(wall, square)) {
            return true;
        }
        for (int i = 0; i < tempPlayerCutawayRoomIDs.size(); ++i) {
            long roomID = tempPlayerCutawayRoomIDs.get(i);
            if (!wall.occludedRoomIds.contains(roomID)) continue;
            int bit = wall.isHorizontal() ? square.x - wall.x1 : square.y - wall.y1;
            return (wall.occludedSquaresMaskForSeenRooms[playerIndex] & 1 << bit) != 0;
        }
        return false;
    }

    private boolean isCutawayDueToPeeking(CutawayWall wall, IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        long peekRoomID = this.cell.playerWindowPeekingRoomId[playerIndex];
        if (peekRoomID == -1L) {
            return false;
        }
        int playerX = PZMath.fastfloor(IsoCamera.frameState.camCharacterX);
        int playerY = PZMath.fastfloor(IsoCamera.frameState.camCharacterY);
        if (wall.isHorizontal()) {
            IsoObject garageDoor;
            if ((playerY == wall.y1 - 1 || playerY == wall.y1) && square.has(IsoPropertyType.GARAGE_DOOR) && (garageDoor = square.getGarageDoor(true)) != null) {
                IsoObject next;
                IsoObject prev;
                IsoObject first = garageDoor;
                IsoObject last = garageDoor;
                while (first != null && (prev = IsoDoor.getGarageDoorPrev(first)) != null) {
                    first = prev;
                }
                while (last != null && (next = IsoDoor.getGarageDoorNext(last)) != null) {
                    last = next;
                }
                return (float)square.x >= first.getX() && (float)square.x <= last.getX();
            }
            if ((playerY == wall.y1 - 1 || playerY == wall.y1) && square.x >= playerX && square.x <= playerX + 1) {
                return true;
            }
        } else {
            IsoObject garageDoor;
            if ((playerX == wall.x1 - 1 || playerX == wall.x1) && square.has(IsoPropertyType.GARAGE_DOOR) && (garageDoor = square.getGarageDoor(true)) != null) {
                IsoObject next;
                IsoObject prev;
                IsoObject first = garageDoor;
                IsoObject last = garageDoor;
                while (first != null && (prev = IsoDoor.getGarageDoorPrev(first)) != null) {
                    first = prev;
                }
                while (last != null && (next = IsoDoor.getGarageDoorNext(last)) != null) {
                    last = next;
                }
                return (float)square.y >= first.getY() && (float)square.y <= last.getY();
            }
            if ((playerX == wall.x1 - 1 || playerX == wall.x1) && square.y >= playerY && square.y <= playerY + 1) {
                return true;
            }
        }
        return false;
    }

    private void cutawayVisit(IsoGridSquare poiSquare, long currentTimeMillis, ArrayList<IsoChunk> onScreenChunks) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        if (chunkMap == null || chunkMap.ignore) {
            return;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int j = 0; j < perPlayerData1.cutawayWalls.size(); ++j) {
            ChunkLevelData levelData;
            boolean bVisited;
            IsoGridSquare test;
            CutawayWall wall = perPlayerData1.cutawayWalls.get(j);
            int level = wall.chunkLevelData.level;
            if (wall.isHorizontal()) {
                for (int x1 = wall.x1; x1 < wall.x2; ++x1) {
                    test = chunkMap.getGridSquare(x1, wall.y1, poiSquare.z);
                    if (test == null) continue;
                    bVisited = perPlayerData1.cutawayVisitorVisitedNorth.contains(test);
                    if (!bVisited) {
                        perPlayerData1.cutawayVisitorVisitedNorth.add(test);
                    }
                    if (!(levelData = test.chunk.getCutawayDataForLevel(level)).shouldRenderSquare(playerIndex, test) || test.getObjects().isEmpty() || bVisited || !this.IsCutawaySquare(wall, poiSquare, test, currentTimeMillis)) continue;
                    perPlayerData1.cutawayVisitorResultsNorth.add(test);
                    if (!test.has(IsoFlagType.WallSE)) continue;
                    perPlayerData1.cutawayVisitorResultsWest.add(test);
                }
                continue;
            }
            for (int y1 = wall.y1; y1 < wall.y2; ++y1) {
                test = this.cell.getGridSquare(wall.x1, y1, poiSquare.z);
                if (test == null) continue;
                bVisited = perPlayerData1.cutawayVisitorVisitedWest.contains(test);
                if (!bVisited) {
                    perPlayerData1.cutawayVisitorVisitedWest.add(test);
                }
                if (!(levelData = test.chunk.getCutawayDataForLevel(level)).shouldRenderSquare(playerIndex, test) || test.getObjects().isEmpty() || bVisited || !this.IsCutawaySquare(wall, poiSquare, test, currentTimeMillis)) continue;
                perPlayerData1.cutawayVisitorResultsWest.add(test);
                if (!test.has(IsoFlagType.WallSE)) continue;
                perPlayerData1.cutawayVisitorResultsNorth.add(test);
            }
        }
    }

    public boolean CalculateBuildingsToCollapse() {
        boolean changed;
        int playerIndex = IsoCamera.frameState.playerIndex;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
        btc.buildingsToCollapse.clear();
        ArrayList<IsoBuilding> buildings = new ArrayList<IsoBuilding>();
        boolean bOccludedByOrphanStructureFlag = false;
        for (int j = 0; j < this.pointOfInterest.size(); ++j) {
            PointOfInterest poi = this.pointOfInterest.get(j);
            if (poi.mousePointer) continue;
            IsoGridSquare square = this.cell.getGridSquare(poi.x, poi.y, poi.z);
            this.cell.GetBuildingsInFrontOfCharacter(buildings, square, false);
            bOccludedByOrphanStructureFlag |= this.cell.occludedByOrphanStructureFlag;
            if (buildings.isEmpty()) {
                this.cell.GetBuildingsInFrontOfCharacter(buildings, square, true);
                bOccludedByOrphanStructureFlag |= this.cell.occludedByOrphanStructureFlag;
            }
            for (int k = 0; k < buildings.size(); ++k) {
                if (btc.buildingsToCollapse.contains(buildings.get((int)k).def)) continue;
                btc.buildingsToCollapse.add(buildings.get((int)k).def);
            }
        }
        this.cell.occludedByOrphanStructureFlag = bOccludedByOrphanStructureFlag;
        long peekedRoomID = this.cell.playerWindowPeekingRoomId[playerIndex];
        if (peekedRoomID != -1L) {
            IsoRoom room = IsoWorld.instance.metaGrid.getRoomByID(peekedRoomID);
            BuildingDef buildingDef = room.building.getDef();
            if (!btc.buildingsToCollapse.contains(buildingDef)) {
                btc.buildingsToCollapse.add(buildingDef);
            }
        }
        boolean bl = changed = btc.tempLastBuildingsToCollapse.size() != btc.buildingsToCollapse.size();
        if (!changed) {
            for (int i = 0; i < btc.tempLastBuildingsToCollapse.size(); ++i) {
                BuildingDef buildingDef = btc.tempLastBuildingsToCollapse.get(i);
                if (btc.buildingsToCollapse.get(i) == buildingDef) continue;
                changed = true;
                break;
            }
        }
        if (changed) {
            BuildingDef buildingDef;
            int i;
            int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
            for (i = 0; i < btc.tempLastBuildingsToCollapse.size(); ++i) {
                buildingDef = btc.tempLastBuildingsToCollapse.get(i);
                if (btc.buildingsToCollapse.contains(buildingDef)) continue;
                buildingDef.invalidateOverlappedChunkLevelsAbove(playerIndex, playerZ, 18432L);
            }
            for (i = 0; i < btc.buildingsToCollapse.size(); ++i) {
                buildingDef = btc.buildingsToCollapse.get(i);
                if (btc.tempLastBuildingsToCollapse.contains(buildingDef)) continue;
                buildingDef.invalidateOverlappedChunkLevelsAbove(playerIndex, playerZ, 18432L);
            }
        }
        btc.tempLastBuildingsToCollapse.clear();
        PZArrayUtil.addAll(btc.tempLastBuildingsToCollapse, btc.buildingsToCollapse);
        return changed;
    }

    public ArrayList<BuildingDef> getCollapsedBuildings() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
        return btc.buildingsToCollapse;
    }

    public boolean isAnyBuildingCollapsed() {
        return !this.getCollapsedBuildings().isEmpty();
    }

    public boolean isBuildingCollapsed(BuildingDef buildingDef) {
        return this.getCollapsedBuildings().contains(buildingDef);
    }

    public boolean checkHiddenBuildingLevels() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
        boolean bForceCutawayUpdate = false;
        for (int i = 0; i < btc.buildingsToCollapse.size(); ++i) {
            BuildingDef buildingDef = btc.buildingsToCollapse.get(i);
            if (btc.maxVisibleLevel.containsKey(buildingDef)) {
                if (btc.maxVisibleLevel.get(buildingDef) == playerZ) continue;
                int minLevel = PZMath.min(btc.maxVisibleLevel.get(buildingDef), playerZ);
                btc.maxVisibleLevel.put(buildingDef, playerZ);
                buildingDef.invalidateOverlappedChunkLevelsAbove(playerIndex, minLevel, 2048L);
                bForceCutawayUpdate = true;
                continue;
            }
            btc.maxVisibleLevel.put(buildingDef, playerZ);
            buildingDef.invalidateOverlappedChunkLevelsAbove(playerIndex, playerZ, 2048L);
        }
        return bForceCutawayUpdate;
    }

    public boolean CanBuildingSquareOccludePlayer(IsoGridSquare square, int playerIndex) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
        for (int i = 0; i < btc.buildingsToCollapse.size(); ++i) {
            BuildingDef buildingDef = btc.buildingsToCollapse.get(i);
            int boundsX = buildingDef.getX();
            int boundsY = buildingDef.getY();
            int boundsWidth = buildingDef.getX2() - boundsX;
            int boundsHeight = buildingDef.getY2() - boundsY;
            this.buildingRectTemp.setBounds(boundsX - 1, boundsY - 1, boundsWidth + 2, boundsHeight + 2);
            if (!this.buildingRectTemp.contains(square.getX(), square.getY())) continue;
            return true;
        }
        return false;
    }

    public IsoObject getFirstMultiLevelObject(IsoGridSquare square) {
        if (square == null) {
            return null;
        }
        if (!square.has("SpriteGridPos")) {
            return null;
        }
        IsoObject[] objects = square.getObjects().getElements();
        int n = square.getObjects().size();
        for (int i = 0; i < n; ++i) {
            IsoObject object = objects[i];
            IsoSpriteGrid spriteGrid = object.getSpriteGrid();
            if (spriteGrid == null || spriteGrid.getLevels() <= 1) continue;
            return object;
        }
        return null;
    }

    public boolean isForceRenderSquare(int playerIndex, IsoGridSquare square) {
        IsoObject object;
        int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
        if (square.associatedBuilding != null && btc.buildingsToCollapse.contains(square.associatedBuilding) && square.z > playerZ && (object = this.getFirstMultiLevelObject(square)) != null) {
            IsoSprite sprite = object.getSprite();
            if (square.z - object.getSpriteGrid().getSpriteGridPosZ(sprite) <= playerZ) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldHideElevatedFloor(int playerIndex, IsoObject object) {
        if (object == null || object.getProperties() == null) {
            return false;
        }
        if (!object.getProperties().has(IsoFlagType.FloorHeightOneThird) && !object.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
            return false;
        }
        IsoGridSquare square = object.getSquare();
        if (square == null) {
            return false;
        }
        int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        if (square.z != playerZ) {
            return false;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
        return square.associatedBuilding != null && btc.buildingsToCollapse.contains(square.associatedBuilding);
    }

    public boolean shouldRenderBuildingSquare(int playerIndex, IsoGridSquare square) {
        int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
        if (square.associatedBuilding != null && btc.buildingsToCollapse.contains(square.associatedBuilding) && square.z > playerZ) {
            IsoObject object = this.getFirstMultiLevelObject(square);
            if (object != null) {
                IsoSprite sprite = object.getSprite();
                if (square.z - object.getSpriteGrid().getSpriteGridPosZ(sprite) <= playerZ) {
                    return true;
                }
            }
            return false;
        }
        if (square.z > playerZ && playerZ < 0) {
            return false;
        }
        if (square.z == playerZ + 1 && square.hasFloorAtTopOfStairs()) {
            return true;
        }
        if (square.z > playerZ) {
            ChunkLevelData chunkLevelData = square.chunk.getCutawayDataForLevel(square.z);
            if (chunkLevelData.orphanStructures.adjacentChunkLoadedCounter != square.chunk.adjacentChunkLoadedCounter) {
                chunkLevelData.orphanStructures.adjacentChunkLoadedCounter = square.chunk.adjacentChunkLoadedCounter;
                chunkLevelData.orphanStructures.calculate(square.chunk);
            }
            OrphanStructures orphanStructures = chunkLevelData.orphanStructures;
            if (orphanStructures.hasOrphanStructures && orphanStructures.isPlayerInRange(playerIndex, PlayerInRange.True) && (orphanStructures.isOrphanStructureSquare(square) || orphanStructures.isAdjacentToOrphanStructure(square))) {
                return false;
            }
        }
        return true;
    }

    private boolean IsCollapsibleBuildingSquare(CutawayWall wall, IsoGridSquare square) {
        if (square.getProperties().has(IsoFlagType.forceRender)) {
            return false;
        }
        if (IsoCamera.frameState.camCharacterSquare != null) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
            BuildingsToCollapse btc = perPlayerData1.buildingsToCollapse;
            int size = btc.buildingsToCollapse.size();
            for (int i = 0; i < size; ++i) {
                BuildingDef buildingDef = btc.buildingsToCollapse.get(i);
                if (!wall.isPartOfBuilding(buildingDef) || !this.cell.collapsibleBuildingSquareAlgorithm(buildingDef, square, IsoCamera.frameState.camCharacterSquare)) continue;
                return true;
            }
        }
        return false;
    }

    public void CalculatePointsOfInterest() {
        this.pointOfInterestStore.releaseAll((List<PointOfInterest>)this.pointOfInterest);
        this.pointOfInterest.clear();
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        this.AddPointOfInterest(player.getX(), player.getY(), player.getZ());
        if (playerIndex == 0 && player.isAiming() && player.getJoypadBind() == -1) {
            int mx = Mouse.getX();
            int my = Mouse.getY() + 52 * Core.tileScale;
            float ix = IsoUtils.XToIso(mx, my, player.getZi());
            float iy = IsoUtils.YToIso(mx, my, player.getZi());
            this.AddPointOfInterest(ix, iy, player.getZ(), true);
        }
        if (player.getCurrentSquare() != null) {
            int i;
            this.cell.gridSquaresTempLeft.clear();
            this.cell.gridSquaresTempRight.clear();
            this.cell.GetSquaresAroundPlayerSquare(player, player.getCurrentSquare(), this.cell.gridSquaresTempLeft, this.cell.gridSquaresTempRight);
            for (i = 0; i < this.cell.gridSquaresTempLeft.size(); ++i) {
                IsoGridSquare square = this.cell.gridSquaresTempLeft.get(i);
                if (!square.isCouldSee(playerIndex) || square.getBuilding() != null && square.getBuilding() != player.getBuilding()) continue;
                this.AddPointOfInterest(square.x, square.y, square.z);
                if (!DebugOptions.instance.fboRenderChunk.renderMustSeeSquares.getValue()) continue;
                LineDrawer.addRect(square.x, square.y, square.z, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            }
            for (i = 0; i < this.cell.gridSquaresTempRight.size(); ++i) {
                IsoGridSquare square = this.cell.gridSquaresTempRight.get(i);
                if (!square.isCouldSee(playerIndex) || square.getBuilding() != null && square.getBuilding() != player.getBuilding()) continue;
                this.AddPointOfInterest(square.x, square.y, square.z);
                if (!DebugOptions.instance.fboRenderChunk.renderMustSeeSquares.getValue()) continue;
                LineDrawer.addRect(square.x, square.y, square.z, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private void AddPointOfInterest(float x, float y, float z) {
        this.AddPointOfInterest(x, y, z, false);
    }

    private void AddPointOfInterest(float x, float y, float z, boolean mousePointer) {
        PointOfInterest p = this.pointOfInterestStore.alloc();
        p.x = PZMath.fastfloor(x);
        p.y = PZMath.fastfloor(y);
        p.z = PZMath.fastfloor(z);
        p.mousePointer = mousePointer;
        this.pointOfInterest.add(p);
    }

    public boolean isRoofRoomSquare(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        if (square.getZ() == 0) {
            return false;
        }
        if (square.getRoomID() != -1L) {
            return false;
        }
        if (square.associatedBuilding == null) {
            return false;
        }
        return square.TreatAsSolidFloor();
    }

    static {
        s_cutawayWallPool = new ObjectPool<CutawayWall>(CutawayWall::new);
        s_slopedSurfacePool = new ObjectPool<SlopedSurface>(SlopedSurface::new);
    }

    private static final class PerPlayerData {
        long lastPlayerRoomId = -1L;
        final HashSet<IsoGridSquare> cutawayVisitorResultsNorth = new HashSet();
        final HashSet<IsoGridSquare> cutawayVisitorResultsWest = new HashSet();
        final ArrayList<IsoGridSquare> lastCutawayVisitorResults = new ArrayList();
        final HashSet<IsoGridSquare> cutawayVisitorVisitedNorth = new HashSet();
        final HashSet<IsoGridSquare> cutawayVisitorVisitedWest = new HashSet();
        IsoGridSquare checkSquare;
        final ArrayList<CutawayWall> cutawayWalls = new ArrayList();
        private final BuildingsToCollapse buildingsToCollapse = new BuildingsToCollapse();

        private PerPlayerData() {
        }
    }

    public static final class ChunkLevelsData {
        public final IsoChunk chunk;
        public final TIntObjectHashMap<ChunkLevelData> levelData = new TIntObjectHashMap();

        public ChunkLevelsData(IsoChunk chunk) {
            this.chunk = chunk;
        }

        public ChunkLevelData getDataForLevel(int level) {
            if (level < -32 || level > 31) {
                return null;
            }
            int index = level + 32;
            ChunkLevelData levelData = this.levelData.get(index);
            if (levelData == null) {
                levelData = new ChunkLevelData(level);
                levelData.levelsData = this;
                this.levelData.put(index, levelData);
            }
            return levelData;
        }

        public void recreateLevel(int level) {
            this.recreateLevel_ExteriorWalls(level);
            this.recreateLevel_AllWalls(level);
            this.recreateLevel_SlopedSurfaces(level);
            if (level > 0) {
                ChunkLevelData levelData = this.getDataForLevel(level);
                levelData.orphanStructures.calculate(this.chunk);
            }
        }

        public void recreateLevel_ExteriorWalls(int level) {
            IsoGridSquare square;
            CutawayWall wall;
            ChunkLevelData levelData = this.getDataForLevel(level);
            this.clearPlayerCutawayFlags(level, levelData.exteriorWalls);
            s_cutawayWallPool.releaseAll((List<CutawayWall>)levelData.exteriorWalls);
            levelData.exteriorWalls.clear();
            if (level < this.chunk.minLevel || level > this.chunk.maxLevel) {
                return;
            }
            IsoGridSquare[] squares = this.chunk.squares[this.chunk.squaresIndexOfLevel(level)];
            int chunksPerWidth = 8;
            for (int y = 0; y < 8; ++y) {
                wall = null;
                for (int x = 0; x < 8; ++x) {
                    square = squares[x + y * 8];
                    if (square != null && square.getWall(true) != null && (square.has(IsoFlagType.WallN) || square.has(IsoFlagType.WallNW) || square.has(IsoFlagType.DoorWallN) || square.has(IsoFlagType.WindowN)) && !this.isAdjacentToRoom(square, IsoDirections.N)) {
                        if (wall != null) continue;
                        wall = s_cutawayWallPool.alloc();
                        wall.chunkLevelData = levelData;
                        wall.x1 = square.x;
                        wall.y1 = square.y;
                        Arrays.fill((Object[])wall.playerInRange, (Object)PlayerInRange.Unset);
                        wall.occludedRoomIds.resetQuick();
                        Arrays.fill(wall.occludedSquaresMaskForSeenRooms, 0);
                        continue;
                    }
                    if (wall == null) continue;
                    wall.x2 = this.chunk.wx * 8 + x;
                    wall.y2 = this.chunk.wy * 8 + y;
                    levelData.exteriorWalls.add(wall);
                    wall = null;
                }
                if (wall == null) continue;
                wall.x2 = this.chunk.wx * 8 + 8;
                wall.y2 = this.chunk.wy * 8 + y;
                levelData.exteriorWalls.add(wall);
            }
            for (int x = 0; x < 8; ++x) {
                wall = null;
                for (int y = 0; y < 8; ++y) {
                    square = squares[x + y * 8];
                    if (square != null && square.getWall(false) != null && (square.has(IsoFlagType.WallW) || square.has(IsoFlagType.WallNW) || square.has(IsoFlagType.DoorWallW) || square.has(IsoFlagType.WindowW)) && !this.isAdjacentToRoom(square, IsoDirections.W)) {
                        if (wall != null) continue;
                        wall = s_cutawayWallPool.alloc();
                        wall.chunkLevelData = levelData;
                        wall.x1 = square.x;
                        wall.y1 = square.y;
                        Arrays.fill((Object[])wall.playerInRange, (Object)PlayerInRange.Unset);
                        wall.occludedRoomIds.resetQuick();
                        Arrays.fill(wall.occludedSquaresMaskForSeenRooms, 0);
                        continue;
                    }
                    if (wall == null) continue;
                    wall.x2 = this.chunk.wx * 8 + x;
                    wall.y2 = this.chunk.wy * 8 + y;
                    levelData.exteriorWalls.add(wall);
                    wall = null;
                }
                if (wall == null) continue;
                wall.x2 = this.chunk.wx * 8 + x;
                wall.y2 = this.chunk.wy * 8 + 8;
                levelData.exteriorWalls.add(wall);
            }
        }

        public void recreateLevel_AllWalls(int level) {
            boolean bWallLike;
            IsoGridSquare square;
            CutawayWall wall;
            ChunkLevelData levelData = this.getDataForLevel(level);
            this.clearPlayerCutawayFlags(level, levelData.allWalls);
            s_cutawayWallPool.releaseAll((List<CutawayWall>)levelData.allWalls);
            levelData.allWalls.clear();
            if (level < this.chunk.minLevel || level > this.chunk.maxLevel) {
                return;
            }
            IsoGridSquare[] squares = this.chunk.squares[this.chunk.squaresIndexOfLevel(level)];
            int chunksPerWidth = 8;
            for (int y = 0; y < 8; ++y) {
                wall = null;
                for (int x = 0; x < 8; ++x) {
                    square = squares[x + y * 8];
                    boolean bl = bWallLike = !(square == null || square.getWall(true) == null && !square.has(IsoFlagType.WindowN) || !square.has(IsoFlagType.WallN) && !square.has(IsoFlagType.WallNW) && !square.has(IsoFlagType.DoorWallN) && !square.has(IsoFlagType.WindowN));
                    if (!bWallLike && square != null) {
                        bWallLike |= square.getGarageDoor(true) != null;
                    }
                    if (bWallLike) {
                        if (wall != null) continue;
                        wall = s_cutawayWallPool.alloc();
                        wall.chunkLevelData = levelData;
                        wall.x1 = square.x;
                        wall.y1 = square.y;
                        Arrays.fill((Object[])wall.playerInRange, (Object)PlayerInRange.Unset);
                        wall.occludedRoomIds.resetQuick();
                        Arrays.fill(wall.occludedSquaresMaskForSeenRooms, 0);
                        continue;
                    }
                    if (wall == null) continue;
                    wall.x2 = this.chunk.wx * 8 + x;
                    wall.y2 = this.chunk.wy * 8 + y;
                    wall.calculateOccludedRooms();
                    levelData.allWalls.add(wall);
                    wall = null;
                }
                if (wall == null) continue;
                wall.x2 = this.chunk.wx * 8 + 8;
                wall.y2 = this.chunk.wy * 8 + y;
                wall.calculateOccludedRooms();
                levelData.allWalls.add(wall);
            }
            for (int x = 0; x < 8; ++x) {
                wall = null;
                for (int y = 0; y < 8; ++y) {
                    square = squares[x + y * 8];
                    boolean bl = bWallLike = !(square == null || square.getWall(false) == null && !square.has(IsoFlagType.WindowW) || !square.has(IsoFlagType.WallW) && !square.has(IsoFlagType.WallNW) && !square.has(IsoFlagType.DoorWallW) && !square.has(IsoFlagType.WindowW));
                    if (!bWallLike && square != null) {
                        bWallLike |= square.getGarageDoor(false) != null;
                    }
                    if (bWallLike) {
                        if (wall != null) continue;
                        wall = s_cutawayWallPool.alloc();
                        wall.chunkLevelData = levelData;
                        wall.x1 = square.x;
                        wall.y1 = square.y;
                        Arrays.fill((Object[])wall.playerInRange, (Object)PlayerInRange.Unset);
                        wall.occludedRoomIds.resetQuick();
                        Arrays.fill(wall.occludedSquaresMaskForSeenRooms, 0);
                        continue;
                    }
                    if (wall == null) continue;
                    wall.x2 = this.chunk.wx * 8 + x;
                    wall.y2 = this.chunk.wy * 8 + y;
                    wall.calculateOccludedRooms();
                    levelData.allWalls.add(wall);
                    wall = null;
                }
                if (wall == null) continue;
                wall.x2 = this.chunk.wx * 8 + x;
                wall.y2 = this.chunk.wy * 8 + 8;
                wall.calculateOccludedRooms();
                levelData.allWalls.add(wall);
            }
        }

        public void recreateLevel_SlopedSurfaces(int level) {
            IsoGridSquare square;
            SlopedSurface slopedSurface;
            ChunkLevelData levelData = this.getDataForLevel(level);
            this.clearPlayerCutawayFlags2(level, levelData.slopedSurfaces);
            s_slopedSurfacePool.releaseAll((List<SlopedSurface>)levelData.slopedSurfaces);
            levelData.slopedSurfaces.clear();
            if (level < this.chunk.minLevel || level > this.chunk.maxLevel) {
                return;
            }
            IsoGridSquare[] squares = this.chunk.squares[this.chunk.squaresIndexOfLevel(level)];
            int chunksPerWidth = 8;
            for (int y = 0; y < 8; ++y) {
                slopedSurface = null;
                for (int x = 0; x < 8; ++x) {
                    square = squares[x + y * 8];
                    if (square != null && (square.getSlopedSurfaceDirection() == IsoDirections.W || square.getSlopedSurfaceDirection() == IsoDirections.E)) {
                        if (slopedSurface != null) continue;
                        slopedSurface = s_slopedSurfacePool.alloc();
                        slopedSurface.chunkLevelData = levelData;
                        slopedSurface.x1 = square.x;
                        slopedSurface.y1 = square.y;
                        Arrays.fill((Object[])slopedSurface.playerInRange, (Object)PlayerInRange.Unset);
                        continue;
                    }
                    if (slopedSurface == null) continue;
                    slopedSurface.x2 = this.chunk.wx * 8 + x;
                    slopedSurface.y2 = this.chunk.wy * 8 + y;
                    levelData.slopedSurfaces.add(slopedSurface);
                    slopedSurface = null;
                }
                if (slopedSurface == null) continue;
                slopedSurface.x2 = this.chunk.wx * 8 + 8;
                slopedSurface.y2 = this.chunk.wy * 8 + y;
                levelData.slopedSurfaces.add(slopedSurface);
            }
            for (int x = 0; x < 8; ++x) {
                slopedSurface = null;
                for (int y = 0; y < 8; ++y) {
                    square = squares[x + y * 8];
                    if (square != null && (square.getSlopedSurfaceDirection() == IsoDirections.N || square.getSlopedSurfaceDirection() == IsoDirections.S)) {
                        if (slopedSurface != null) continue;
                        slopedSurface = s_slopedSurfacePool.alloc();
                        slopedSurface.chunkLevelData = levelData;
                        slopedSurface.x1 = square.x;
                        slopedSurface.y1 = square.y;
                        Arrays.fill((Object[])slopedSurface.playerInRange, (Object)PlayerInRange.Unset);
                        continue;
                    }
                    if (slopedSurface == null) continue;
                    slopedSurface.x2 = this.chunk.wx * 8 + x;
                    slopedSurface.y2 = this.chunk.wy * 8 + y;
                    levelData.slopedSurfaces.add(slopedSurface);
                    slopedSurface = null;
                }
                if (slopedSurface == null) continue;
                slopedSurface.x2 = this.chunk.wx * 8 + x;
                slopedSurface.y2 = this.chunk.wy * 8 + 8;
                levelData.slopedSurfaces.add(slopedSurface);
            }
        }

        void clearPlayerCutawayFlags(int level, ArrayList<CutawayWall> walls) {
            int bInvalidate = 0;
            for (int i = 0; i < walls.size(); ++i) {
                CutawayWall wall = walls.get(i);
                for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                    if (!wall.isPlayerInRange(playerIndex, PlayerInRange.True)) continue;
                    wall.setPlayerCutawayFlag(playerIndex, false);
                    bInvalidate |= 1 << playerIndex;
                }
            }
            if (bInvalidate != 0) {
                for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                    if ((bInvalidate & 1 << playerIndex) == 0) continue;
                    this.chunk.getRenderLevels(playerIndex).invalidateLevel(level, 2048L);
                }
            }
        }

        void clearPlayerCutawayFlags2(int level, ArrayList<SlopedSurface> slopedSurfaces) {
            int bInvalidate = 0;
            for (int i = 0; i < slopedSurfaces.size(); ++i) {
                SlopedSurface slopedSurface = slopedSurfaces.get(i);
                for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                    if (!slopedSurface.isPlayerInRange(playerIndex, PlayerInRange.True)) continue;
                    slopedSurface.setPlayerCutawayFlag(playerIndex, false);
                    bInvalidate |= 1 << playerIndex;
                }
            }
            if (bInvalidate != 0) {
                for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                    if ((bInvalidate & 1 << playerIndex) == 0) continue;
                    this.chunk.getRenderLevels(playerIndex).invalidateLevel(level, 2048L);
                }
            }
        }

        boolean isAdjacentToRoom(IsoGridSquare square, IsoDirections dir) {
            IsoGridSquare adj = square.getAdjacentSquare(dir);
            if (adj != null && (adj.getBuilding() != null || adj.roofHideBuilding != null || this.hasRoomBelow(adj))) {
                return true;
            }
            return square.getRoom() != null || square.roofHideBuilding != null || this.hasRoomBelow(square);
        }

        boolean hasRoomBelow(IsoGridSquare square) {
            if (square == null || square.chunk == null) {
                return false;
            }
            if (square.getZ() == 0) {
                return false;
            }
            for (int z = square.z - 1; z >= square.chunk.minLevel; --z) {
                IsoGridSquare square1 = square.getCell().getGridSquare(square.x, square.y, z);
                if (square1 == null || square1.getBuilding() == null && square1.roofHideBuilding == null) continue;
                return true;
            }
            return false;
        }

        public void invalidateOccludedSquaresMaskForSeenRooms(int playerIndex, int level) {
            ChunkLevelData levelData = this.getDataForLevel(level);
            for (int i = 0; i < levelData.allWalls.size(); ++i) {
                CutawayWall wall = levelData.allWalls.get(i);
                wall.occludedSquaresMaskForSeenRooms[playerIndex] = 0;
            }
        }

        public void invalidateAll() {
            for (int z = this.chunk.getMinLevel(); z <= this.chunk.getMaxLevel(); ++z) {
                int index = z + 32;
                ChunkLevelData levelData = this.levelData.get(index);
                if (levelData == null) continue;
                levelData.adjacentChunkLoadedCounter = 0;
            }
        }

        public void removeFromWorld() {
            for (ChunkLevelData levelData : this.levelData.valueCollection()) {
                levelData.removeFromWorld();
            }
        }

        public void debugRender(int level) {
            ChunkLevelData levelData = this.getDataForLevel(level);
            levelData.debugRender();
        }
    }

    public static final class ChunkLevelData {
        public ChunkLevelsData levelsData;
        public final int level;
        public int adjacentChunkLoadedCounter;
        public final ArrayList<CutawayWall> exteriorWalls = new ArrayList();
        public final ArrayList<CutawayWall> allWalls = new ArrayList();
        public final OrphanStructures orphanStructures = new OrphanStructures();
        public final ArrayList<SlopedSurface> slopedSurfaces = new ArrayList();
        public final byte[][] squareFlags = new byte[4][64];
        public boolean hasCutawayNorthWallsOnWestEdge;
        public boolean hasCutawayNorthWallsOnEastEdge;
        public boolean hasCutawayWestWallsOnNorthEdge;
        public boolean hasCutawayWestWallsOnSouthEdge;
        public final long[] occludingSquares = new long[4];

        ChunkLevelData(int level) {
            this.level = level;
            this.orphanStructures.chunkLevelData = this;
        }

        public boolean shouldRenderSquare(int playerIndex, IsoGridSquare square) {
            if (square == null || square.chunk == null || square.z != this.level) {
                return false;
            }
            int lx = square.x - square.chunk.wx * 8;
            int ly = square.y - square.chunk.wy * 8;
            return (this.squareFlags[playerIndex][lx + ly * 8] & 1) != 0;
        }

        public boolean calculateOccludingSquares(int playerIndex, int occludedX1, int occludedY1, int occludedX2, int occludedY2, int[] occludedGrid) {
            int occludedWidth = occludedX2 - occludedX1 + 1;
            long occludingOld = this.occludingSquares[playerIndex];
            this.occludingSquares[playerIndex] = 0L;
            IsoChunk chunk = this.levelsData.chunk;
            IsoGridSquare[] squares = chunk.getSquaresForLevel(this.level);
            int chunkSizeInSquares = 8;
            for (int i = 0; i < 64; ++i) {
                IsoGridSquare square = squares[i];
                if (square == null) continue;
                int zeroX = square.x - square.z * 3 - occludedX1;
                int zeroY = square.y - square.z * 3 - occludedY1;
                if (zeroX < 0 || zeroY < 0 || zeroX > occludedX2 - occludedX1 || zeroY > occludedY2 - occludedY1 || !this.shouldRenderSquare(playerIndex, square) || !square.getVisionMatrix(0, 0, -1) && chunk.getGridSquare(i % 8, i / 8, square.z - 1) != null) continue;
                int zeroIndex = zeroX + zeroY * occludedWidth;
                occludedGrid[zeroIndex] = PZMath.max(occludedGrid[zeroIndex], square.z);
                int x = i % 8;
                int y = i / 8;
                int bit = 1 << x + y * 8;
                int n = playerIndex;
                this.occludingSquares[n] = this.occludingSquares[n] | (long)bit;
            }
            return this.occludingSquares[playerIndex] != occludingOld;
        }

        void removeFromWorld() {
            this.adjacentChunkLoadedCounter = 0;
            s_cutawayWallPool.releaseAll((List<CutawayWall>)this.exteriorWalls);
            this.exteriorWalls.clear();
            s_cutawayWallPool.releaseAll((List<CutawayWall>)this.allWalls);
            this.allWalls.clear();
            for (int i = 0; i < 4; ++i) {
                Arrays.fill(this.squareFlags[i], (byte)0);
                this.occludingSquares[i] = 0L;
            }
            this.orphanStructures.resetForStore();
            this.hasCutawayNorthWallsOnWestEdge = false;
            this.hasCutawayNorthWallsOnEastEdge = false;
            this.hasCutawayWestWallsOnNorthEdge = false;
            this.hasCutawayWestWallsOnSouthEdge = false;
        }

        void debugRender() {
            int y;
            float b;
            float g;
            float r;
            IsoGridSquare square;
            int x;
            int i;
            ArrayList<CutawayWall> walls = this.allWalls;
            for (i = 0; i < walls.size(); ++i) {
                CutawayWall wall = walls.get(i);
                if (wall.isHorizontal()) {
                    for (x = wall.x1; x < wall.x2; ++x) {
                        square = IsoWorld.instance.currentCell.getGridSquare(x, wall.y1, this.level);
                        r = 0.0f;
                        g = 1.0f;
                        b = 0.0f;
                        if (square != null && (square.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 1) != 0) {
                            r = 1.0f;
                        }
                        if (wall.isPlayerInRange(IsoCamera.frameState.playerIndex, PlayerInRange.True)) {
                            b = 1.0f;
                        }
                        LineDrawer.addLine((float)x + (x == wall.x1 ? 0.05f : 0.0f), (float)wall.y1, (float)this.level, (float)(x + 1) - (x == wall.x2 - 1 ? 0.05f : 0.0f), (float)wall.y2, (float)this.level, r, 1.0f, b, 1.0f);
                    }
                    continue;
                }
                for (y = wall.y1; y < wall.y2; ++y) {
                    square = IsoWorld.instance.currentCell.getGridSquare(wall.x1, y, this.level);
                    r = 0.0f;
                    g = 1.0f;
                    b = 0.0f;
                    if (square != null && (square.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 2) != 0) {
                        r = 1.0f;
                    }
                    if (wall.isPlayerInRange(IsoCamera.frameState.playerIndex, PlayerInRange.True)) {
                        b = 1.0f;
                    }
                    LineDrawer.addLine((float)wall.x1, (float)y + (y == wall.y1 ? 0.05f : 0.0f), (float)this.level, (float)wall.x1, (float)(y + 1) - (y == wall.y2 - 1 ? 0.05f : 0.0f), (float)this.level, r, 1.0f, b, 1.0f);
                }
            }
            for (i = 0; i < this.slopedSurfaces.size(); ++i) {
                SlopedSurface slopedSurface = this.slopedSurfaces.get(i);
                if (slopedSurface.isHorizontal()) {
                    for (x = slopedSurface.x1; x < slopedSurface.x2; ++x) {
                        square = IsoWorld.instance.currentCell.getGridSquare(x, slopedSurface.y1, this.level);
                        r = 0.0f;
                        g = 1.0f;
                        b = 0.0f;
                        if (square != null && (square.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 1) != 0) {
                            r = 1.0f;
                        }
                        LineDrawer.addLine((float)x + (x == slopedSurface.x1 ? 0.05f : 0.0f), (float)slopedSurface.y1, (float)this.level, (float)(x + 1) - (x == slopedSurface.x2 - 1 ? 0.05f : 0.0f), (float)slopedSurface.y2, (float)this.level, r, 1.0f, 0.0f, 1.0f);
                    }
                    continue;
                }
                for (y = slopedSurface.y1; y < slopedSurface.y2; ++y) {
                    square = IsoWorld.instance.currentCell.getGridSquare(slopedSurface.x1, y, this.level);
                    r = 0.0f;
                    g = 1.0f;
                    b = 0.0f;
                    if (square != null && (square.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 2) != 0) {
                        r = 1.0f;
                    }
                    if (slopedSurface.isPlayerInRange(IsoCamera.frameState.playerIndex, PlayerInRange.True)) {
                        b = 1.0f;
                    }
                    LineDrawer.addLine((float)slopedSurface.x1, (float)y + (y == slopedSurface.y1 ? 0.05f : 0.0f), (float)this.level, (float)slopedSurface.x1, (float)(y + 1) - (y == slopedSurface.y2 - 1 ? 0.05f : 0.0f), (float)this.level, r, 1.0f, b, 1.0f);
                }
            }
        }
    }

    public static final class OrphanStructures {
        ChunkLevelData chunkLevelData;
        final PlayerInRange[] playerInRange = new PlayerInRange[4];
        boolean hasOrphanStructures;
        long isOrphanStructureSquare;
        int adjacentChunkLoadedCounter;

        void calculate(IsoChunk chunk) {
            Arrays.fill((Object[])this.playerInRange, (Object)PlayerInRange.Unset);
            this.hasOrphanStructures = false;
            this.isOrphanStructureSquare = 0L;
            if (this.chunkLevelData.level < chunk.minLevel || this.chunkLevelData.level > chunk.maxLevel) {
                return;
            }
            int index = chunk.squaresIndexOfLevel(this.chunkLevelData.level);
            IsoGridSquare[] squares = chunk.squares[index];
            for (int i = 0; i < squares.length; ++i) {
                IsoGridSquare square = squares[i];
                if (!this.calculateOrphanStructureSquare(square)) continue;
                this.hasOrphanStructures = true;
                this.isOrphanStructureSquare |= 1L << i;
            }
        }

        boolean calculateOrphanStructureSquare(IsoGridSquare square) {
            IsoGridSquare squareToNorthWest;
            IsoGridSquare squareToWest;
            if (square == null) {
                return false;
            }
            IsoBuilding squareBuilding = square.getBuilding();
            if (squareBuilding == null && (squareBuilding = square.roofHideBuilding) != null && squareBuilding.isEntirelyEmptyOutside()) {
                return true;
            }
            for (int dropZ = square.getZ() - 1; dropZ >= 0 && squareBuilding == null; --dropZ) {
                IsoGridSquare testDropSquare = square.getCell().getGridSquare(square.x, square.y, dropZ);
                if (testDropSquare == null || (squareBuilding = testDropSquare.getBuilding()) != null) continue;
                squareBuilding = testDropSquare.roofHideBuilding;
            }
            if (squareBuilding != null) {
                return false;
            }
            if (square.associatedBuilding != null) {
                return false;
            }
            if (this.isPlayerBuiltSquare(square)) {
                return true;
            }
            IsoGridSquare squareToNorth = square.getAdjacentSquare(IsoDirections.N);
            if (squareToNorth != null && squareToNorth.getBuilding() == null) {
                if (this.isPlayerBuiltSquare(squareToNorth)) {
                    return true;
                }
                if (squareToNorth.HasStairsBelow()) {
                    return true;
                }
            }
            if ((squareToWest = square.getAdjacentSquare(IsoDirections.W)) != null && squareToWest.getBuilding() == null) {
                if (this.isPlayerBuiltSquare(squareToWest)) {
                    return true;
                }
                if (squareToWest.HasStairsBelow()) {
                    return true;
                }
            }
            if (square.has(IsoFlagType.WallSE) && (squareToNorthWest = square.getAdjacentSquare(IsoDirections.NW)) != null && squareToNorthWest.getBuilding() == null) {
                if (this.isPlayerBuiltSquare(squareToNorthWest)) {
                    return true;
                }
                if (squareToNorthWest.HasStairsBelow()) {
                    return true;
                }
            }
            return false;
        }

        boolean isPlayerBuiltSquare(IsoGridSquare square) {
            if (square.getPlayerBuiltFloor() != null) {
                return true;
            }
            return square.HasStairs() || square.getStairPillar() != null;
        }

        boolean isOrphanStructureSquare(IsoGridSquare square) {
            if (square == null) {
                return false;
            }
            IsoChunk chunk = square.getChunk();
            if (chunk == null) {
                return false;
            }
            if (chunk != this.chunkLevelData.levelsData.chunk) {
                int level = square.getZ();
                if (chunk.isValidLevel(level)) {
                    ChunkLevelData data = chunk.getCutawayDataForLevel(level);
                    if (data.orphanStructures.adjacentChunkLoadedCounter != chunk.adjacentChunkLoadedCounter) {
                        data.orphanStructures.adjacentChunkLoadedCounter = chunk.adjacentChunkLoadedCounter;
                        data.orphanStructures.calculate(chunk);
                    }
                    return data.orphanStructures.isOrphanStructureSquare(square);
                }
                return false;
            }
            int lx = square.x - chunk.wx * 8;
            int ly = square.y - chunk.wy * 8;
            int squareIndex = lx + ly * 8;
            return (this.isOrphanStructureSquare & 1L << squareIndex) != 0L;
        }

        boolean isAdjacentToOrphanStructure(IsoGridSquare square) {
            if (square == null) {
                return false;
            }
            for (int i = 0; i < DIRECTIONS.length; ++i) {
                IsoDirections dir = DIRECTIONS[i];
                if (!this.isOrphanStructureSquare(square.getAdjacentSquare(dir))) continue;
                return true;
            }
            return false;
        }

        boolean shouldCutaway() {
            if (IsoWorld.instance.currentCell.occludedByOrphanStructureFlag) {
                return this.chunkLevelData.level > PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
            }
            return false;
        }

        void setPlayerInRange(int playerIndex, PlayerInRange bInRange) {
            this.playerInRange[playerIndex] = bInRange;
        }

        boolean isPlayerInRange(int playerIndex, PlayerInRange bInRange) {
            return this.playerInRange[playerIndex] == bInRange;
        }

        void resetForStore() {
            Arrays.fill((Object[])this.playerInRange, (Object)PlayerInRange.Unset);
            this.hasOrphanStructures = false;
            this.isOrphanStructureSquare = 0L;
            this.adjacentChunkLoadedCounter = 0;
        }
    }

    public static final class CutawayWall {
        ChunkLevelData chunkLevelData;
        public int x1;
        public int y1;
        public int x2;
        public int y2;
        final PlayerInRange[] playerInRange = new PlayerInRange[4];
        final TLongArrayList occludedRoomIds = new TLongArrayList();
        final int[] occludedSquaresMaskForSeenRooms = new int[4];
        static final int[] NORTH_WALL_DXY = new int[]{-1, -1, -2, -2, -3, -3, 0, -1, -1, -2, -2, -3};
        static final int[] WEST_WALL_DXY = new int[]{-1, -1, -2, -2, -3, -3, -1, 0, -2, -1, -3, -2};

        boolean isHorizontal() {
            return this.y1 == this.y2;
        }

        void calculateOccludedRooms() {
            IsoCell cell = IsoWorld.instance.currentCell;
            if (this.isHorizontal()) {
                for (int x = this.x1; x < this.x2; ++x) {
                    for (int i = 0; i < NORTH_WALL_DXY.length - 1; i += 2) {
                        IsoGridSquare square = cell.getGridSquare(x + NORTH_WALL_DXY[i], this.y1 + NORTH_WALL_DXY[i + 1], this.chunkLevelData.level);
                        if (square == null) continue;
                        long roomID = square.getRoomID();
                        if (roomID == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(square)) {
                            roomID = square.associatedBuilding.getRoofRoomID(square.z);
                        }
                        if (roomID == -1L || this.occludedRoomIds.contains(roomID)) continue;
                        this.occludedRoomIds.add(roomID);
                    }
                }
            } else {
                for (int y = this.y1; y < this.y2; ++y) {
                    for (int i = 0; i < WEST_WALL_DXY.length - 1; i += 2) {
                        IsoGridSquare square = cell.getGridSquare(this.x1 + WEST_WALL_DXY[i], y + WEST_WALL_DXY[i + 1], this.chunkLevelData.level);
                        if (square == null) continue;
                        long roomID = square.getRoomID();
                        if (roomID == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(square)) {
                            roomID = square.associatedBuilding.getRoofRoomID(square.z);
                        }
                        if (roomID == -1L || this.occludedRoomIds.contains(roomID)) continue;
                        this.occludedRoomIds.add(roomID);
                    }
                }
            }
        }

        @Deprecated
        boolean isSquareOccludingRoom(IsoGridSquare square, long roomID) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoCell cell = IsoWorld.instance.currentCell;
            for (int i = 1; i <= 3; ++i) {
                IsoGridSquare square2 = cell.getGridSquare(square.x - i, square.y - i, square.z);
                if (square2 == null || square2.getRoomID() != roomID || !square2.isCouldSee(playerIndex)) continue;
                return true;
            }
            return false;
        }

        boolean shouldCutawayFence() {
            int max = 1;
            if (IsoCamera.frameState.playerIndex == 0 && IsoPlayer.players[0].isAiming()) {
                max = 2;
            }
            max = Math.min(max, FBORenderCutaways.instance.pointOfInterest.size());
            for (int i = 0; i < FBORenderCutaways.instance.pointOfInterest.size(); ++i) {
                PointOfInterest poi = FBORenderCutaways.instance.pointOfInterest.get(i);
                if (!(i >= max ? this.shouldCutawayFence(poi.getSquare(), 3) : this.shouldCutawayFence(poi.getSquare(), 6))) continue;
                return true;
            }
            return false;
        }

        boolean shouldCutawayFence(IsoGridSquare square, int range) {
            if (square == null) {
                return false;
            }
            if (!square.isCanSee(IsoCamera.frameState.playerIndex)) {
                return false;
            }
            assert (square.z == this.chunkLevelData.level);
            if (this.isHorizontal()) {
                return square.y < this.y1 && square.y >= this.y1 - range;
            }
            return square.x < this.x1 && square.x >= this.x1 - range;
        }

        boolean shouldCutawayBuilding(int playerIndex) {
            ArrayList<Long> roomIDs = FBORenderCutaways.instance.cell.tempPlayerCutawayRoomIds.get(playerIndex);
            for (int i = 0; i < roomIDs.size(); ++i) {
                long roomID = roomIDs.get(i);
                if (!this.occludedRoomIds.contains(roomID)) continue;
                return true;
            }
            return false;
        }

        int calculateOccludedSquaresMask(int playerIndex, long roomID) {
            if (!this.occludedRoomIds.contains(roomID)) {
                return 0;
            }
            int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            int mask = 0;
            if (this.isHorizontal()) {
                block0: for (int x = this.x1; x < this.x2; ++x) {
                    for (int j = 0; j < NORTH_WALL_DXY.length - 1; j += 2) {
                        IsoGridSquare square = chunkMap.getGridSquare(x + NORTH_WALL_DXY[j], this.y1 + NORTH_WALL_DXY[j + 1], playerZ);
                        if (square == null || square.getObjects().isEmpty() || !square.isCouldSee(playerIndex)) continue;
                        long squareRoomID = square.getRoomID();
                        if (squareRoomID == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(square)) {
                            squareRoomID = square.associatedBuilding.getRoofRoomID(playerZ);
                        }
                        if (squareRoomID != roomID) continue;
                        mask |= 1 << x - this.x1;
                        continue block0;
                    }
                }
            } else {
                block2: for (int y = this.y1; y < this.y2; ++y) {
                    for (int j = 0; j < WEST_WALL_DXY.length - 1; j += 2) {
                        IsoGridSquare square = chunkMap.getGridSquare(this.x1 + WEST_WALL_DXY[j], y + WEST_WALL_DXY[j + 1], playerZ);
                        if (square == null || square.getObjects().isEmpty() || !square.isCouldSee(playerIndex)) continue;
                        long squareRoomID = square.getRoomID();
                        if (squareRoomID == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(square)) {
                            squareRoomID = square.associatedBuilding.getRoofRoomID(playerZ);
                        }
                        if (squareRoomID != roomID) continue;
                        mask |= 1 << y - this.y1;
                        continue block2;
                    }
                }
            }
            return mask;
        }

        int calculateOccludedSquaresMaskForSeenRooms(int playerIndex) {
            int mask = 0;
            ArrayList<Long> roomIDs = FBORenderCutaways.instance.cell.tempPlayerCutawayRoomIds.get(playerIndex);
            for (int i = 0; i < roomIDs.size(); ++i) {
                long roomID = roomIDs.get(i);
                mask |= this.calculateOccludedSquaresMask(playerIndex, roomID);
            }
            return mask;
        }

        void setPlayerInRange(int playerIndex, PlayerInRange bInRange) {
            this.playerInRange[playerIndex] = bInRange;
        }

        boolean isPlayerInRange(int playerIndex, PlayerInRange bInRange) {
            return this.playerInRange[playerIndex] == bInRange;
        }

        void setPlayerCutawayFlag(int playerIndex, boolean bCutaway) {
            if (this.isHorizontal()) {
                for (int x = this.x1; x < this.x2; ++x) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, this.y1, this.chunkLevelData.level);
                    if (square == null) continue;
                    if (bCutaway) {
                        square.addPlayerCutawayFlag(playerIndex, 1, 0L);
                        continue;
                    }
                    square.clearPlayerCutawayFlag(playerIndex, 1, 0L);
                }
            } else {
                for (int y = this.y1; y < this.y2; ++y) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x1, y, this.chunkLevelData.level);
                    if (square == null) continue;
                    if (bCutaway) {
                        square.addPlayerCutawayFlag(playerIndex, 2, 0L);
                        continue;
                    }
                    square.clearPlayerCutawayFlag(playerIndex, 2, 0L);
                }
            }
        }

        void setVisitedSquares(PerPlayerData perPlayerData1) {
            if (this.isHorizontal()) {
                for (int x = this.x1; x < this.x2; ++x) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, this.y1, this.chunkLevelData.level);
                    if (square == null) continue;
                    perPlayerData1.cutawayVisitorResultsNorth.add(square);
                }
            } else {
                for (int y = this.y1; y < this.y2; ++y) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x1, y, this.chunkLevelData.level);
                    if (square == null) continue;
                    perPlayerData1.cutawayVisitorResultsWest.add(square);
                }
            }
        }

        boolean isPartOfBuilding(BuildingDef buildingDef) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (this.isHorizontal()) {
                if (this.y1 < buildingDef.getY() || this.y1 > buildingDef.getY2()) {
                    return false;
                }
                for (int x = this.x1; x < this.x2; ++x) {
                    IsoGridSquare sq = chunkMap.getGridSquare(x, this.y1, this.chunkLevelData.level);
                    if (sq != null && sq.getBuildingDef() == buildingDef) {
                        return true;
                    }
                    sq = chunkMap.getGridSquare(x, this.y1 - 1, this.chunkLevelData.level);
                    if (sq == null || sq.getBuildingDef() != buildingDef) continue;
                    return true;
                }
            } else {
                if (this.x1 < buildingDef.getX() || this.x1 > buildingDef.getX2()) {
                    return false;
                }
                for (int y = this.y1; y < this.y2; ++y) {
                    IsoGridSquare sq = chunkMap.getGridSquare(this.x1, y, this.chunkLevelData.level);
                    if (sq != null && sq.getBuildingDef() == buildingDef) {
                        return true;
                    }
                    sq = chunkMap.getGridSquare(this.x1 - 1, y, this.chunkLevelData.level);
                    if (sq == null || sq.getBuildingDef() != buildingDef) continue;
                    return true;
                }
            }
            return false;
        }
    }

    static enum PlayerInRange {
        Unset,
        True,
        False;

    }

    public static final class SlopedSurface {
        ChunkLevelData chunkLevelData;
        public int x1;
        public int y1;
        public int x2;
        public int y2;
        final PlayerInRange[] playerInRange = new PlayerInRange[4];

        boolean isHorizontal() {
            return this.x2 > this.x1;
        }

        void setPlayerInRange(int playerIndex, PlayerInRange bInRange) {
            this.playerInRange[playerIndex] = bInRange;
        }

        boolean isPlayerInRange(int playerIndex, PlayerInRange bInRange) {
            return this.playerInRange[playerIndex] == bInRange;
        }

        boolean shouldCutaway() {
            int max = 1;
            if (IsoCamera.frameState.playerIndex == 0 && IsoPlayer.players[0].isAiming()) {
                max = 2;
            }
            max = Math.min(max, FBORenderCutaways.instance.pointOfInterest.size());
            for (int i = 0; i < FBORenderCutaways.instance.pointOfInterest.size(); ++i) {
                PointOfInterest poi = FBORenderCutaways.instance.pointOfInterest.get(i);
                if (!(i >= max ? this.shouldCutaway(poi.getSquare(), 3) : this.shouldCutaway(poi.getSquare(), 6))) continue;
                return true;
            }
            return false;
        }

        boolean shouldCutaway(IsoGridSquare square, int range) {
            if (square == null) {
                return false;
            }
            if (IsoCamera.frameState.camCharacterSquare != null && IsoCamera.frameState.camCharacterSquare.hasSlopedSurface()) {
                return false;
            }
            if (!square.isCanSee(IsoCamera.frameState.playerIndex)) {
                return false;
            }
            assert (square.z == this.chunkLevelData.level);
            if (this.isHorizontal()) {
                return square.y < this.y1 && square.y >= this.y1 - range;
            }
            return square.x < this.x1 && square.x >= this.x1 - range && square.y >= this.y1 && square.y < this.y2;
        }

        void setPlayerCutawayFlag(int playerIndex, boolean bCutaway) {
            if (this.isHorizontal()) {
                for (int x = this.x1; x <= this.x2; ++x) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, this.y1, this.chunkLevelData.level);
                    if (square == null) continue;
                    if (bCutaway) {
                        square.addPlayerCutawayFlag(playerIndex, 1, 0L);
                        continue;
                    }
                    square.clearPlayerCutawayFlag(playerIndex, 1, 0L);
                }
            } else {
                for (int y = this.y1; y <= this.y2; ++y) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x1, y, this.chunkLevelData.level);
                    if (square == null) continue;
                    if (bCutaway) {
                        square.addPlayerCutawayFlag(playerIndex, 2, 0L);
                        continue;
                    }
                    square.clearPlayerCutawayFlag(playerIndex, 2, 0L);
                }
            }
        }
    }

    public static final class PointOfInterest {
        public int x;
        public int y;
        public int z;
        public boolean mousePointer;

        public IsoGridSquare getSquare() {
            return IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
        }
    }

    private static final class BuildingsToCollapse {
        final ArrayList<BuildingDef> buildingsToCollapse = new ArrayList();
        final ArrayList<BuildingDef> tempLastBuildingsToCollapse = new ArrayList();
        final TObjectIntHashMap<BuildingDef> maxVisibleLevel = new TObjectIntHashMap();

        private BuildingsToCollapse() {
        }
    }
}

