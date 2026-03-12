/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public class NearestWalls {
    private static final int CPW = 8;
    private static final int CPWx4 = 32;
    private static final int LEVELS = 64;
    private static int changeCount;
    private static int renderX;
    private static int renderY;
    private static int renderZ;

    public static void chunkLoaded(IsoChunk chunk) {
        if (++changeCount < 0) {
            changeCount = 0;
        }
        for (int z = chunk.getMinLevel(); z <= chunk.getMaxLevel(); ++z) {
            IsoChunkLevel chunkLevel = chunk.getLevelData(z);
            if (chunkLevel.nearestWalls != null) {
                chunkLevel.nearestWalls.changeCount = -1;
            }
            if (chunkLevel.nearestExteriorWalls == null) continue;
            chunkLevel.nearestExteriorWalls.changeCount = -1;
        }
    }

    private static ChunkLevelData getOrCreateLevelData(IsoChunk chunk, int z, boolean exterior) {
        IsoChunkLevel chunkLevel = chunk.getLevelData(z);
        if (chunkLevel == null) {
            return null;
        }
        if (exterior) {
            if (chunkLevel.nearestExteriorWalls == null) {
                chunkLevel.nearestExteriorWalls = new ChunkLevelData();
            }
            return chunkLevel.nearestExteriorWalls;
        }
        if (chunkLevel.nearestWalls == null) {
            chunkLevel.nearestWalls = new ChunkLevelData();
        }
        return chunkLevel.nearestWalls;
    }

    private static void calcDistanceOnThisChunkOnly(IsoChunk chunk, int z, boolean exterior) {
        IsoGridSquare square;
        int index;
        int chunksPerWidth4 = 32;
        ChunkLevelData levelData = NearestWalls.getOrCreateLevelData(chunk, z, exterior);
        byte[] distance = levelData.distanceSelf;
        for (int y = 0; y < 8; ++y) {
            int wallX = -1;
            for (int x = 0; x < 8; ++x) {
                IsoGridSquare w;
                levelData.closest[x + y * 8] = -1;
                index = x * 4 + y * 32;
                distance[index + 0] = (byte)(wallX == -1 ? -1 : (byte)(x - wallX));
                distance[index + 1] = -1;
                square = chunk.getGridSquare(x, y, z);
                if (square == null || !square.has(IsoFlagType.WallW) && !square.has(IsoFlagType.DoorWallW) && !square.has(IsoFlagType.WallNW) && !square.has(IsoFlagType.WindowW) || exterior && ((w = square.getAdjacentSquare(IsoDirections.W)) == null || square.isInARoom() == w.isInARoom())) continue;
                wallX = (byte)x;
                distance[index + 0] = 0;
                for (int x1 = x - 1; x1 >= 0 && distance[(index = x1 * 4 + y * 32) + 1] == -1; --x1) {
                    distance[index + 1] = (byte)(wallX - x1);
                }
            }
        }
        for (int x = 0; x < 8; ++x) {
            int wallY = -1;
            for (int y = 0; y < 8; ++y) {
                IsoGridSquare n;
                index = x * 4 + y * 32;
                distance[index + 2] = (byte)(wallY == -1 ? -1 : (byte)(y - wallY));
                distance[index + 3] = -1;
                square = chunk.getGridSquare(x, y, z);
                if (square == null || !square.has(IsoFlagType.WallN) && !square.has(IsoFlagType.DoorWallN) && !square.has(IsoFlagType.WallNW) && !square.has(IsoFlagType.WindowN) || exterior && ((n = square.getAdjacentSquare(IsoDirections.N)) == null || square.isInARoom() == n.isInARoom())) continue;
                wallY = (byte)y;
                distance[index + 2] = 0;
                for (int y1 = y - 1; y1 >= 0 && distance[(index = x * 4 + y1 * 32) + 3] == -1; --y1) {
                    distance[index + 3] = (byte)(wallY - y1);
                }
            }
        }
    }

    private static int getIndex(IsoChunk chunk, int x, int y) {
        return (x - chunk.wx * 8) * 4 + (y - chunk.wy * 8) * 32;
    }

    private static int getNearestWallOnSameChunk(IsoChunk chunk, int x, int y, int z, int wall, boolean exterior) {
        ChunkLevelData levelData = NearestWalls.getOrCreateLevelData(chunk, z, exterior);
        if (levelData == null) {
            return -1;
        }
        if (levelData.changeCount != changeCount) {
            NearestWalls.calcDistanceOnThisChunkOnly(chunk, z, exterior);
            levelData.changeCount = changeCount;
        }
        int index = NearestWalls.getIndex(chunk, x, y);
        return levelData.distanceSelf[index + wall];
    }

    private static boolean hasWall(IsoChunk chunk, int x, int y, int z, int wall, boolean exterior) {
        return NearestWalls.getNearestWallOnSameChunk(chunk, x, y, z, wall, exterior) == 0;
    }

    private static int getNearestWallWest(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        IsoChunk chunk2;
        boolean wall = false;
        int dx = -1;
        boolean dy = false;
        int dist = NearestWalls.getNearestWallOnSameChunk(chunk, x, y, z, 0, exterior);
        if (dist != -1) {
            return x - dist;
        }
        for (int d = 1; d <= 3 && (chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * -1, chunk.wy + d * 0)) != null; ++d) {
            int x2 = (chunk2.wx + 1) * 8 - 1;
            int y2 = y;
            dist = NearestWalls.getNearestWallOnSameChunk(chunk2, x2, y2, z, 0, exterior);
            if (dist == -1) continue;
            return x2 - dist;
        }
        return -1;
    }

    private static int getNearestWallEast(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        IsoChunk chunk2;
        boolean wall = true;
        boolean dx = true;
        boolean dy = false;
        int dist = NearestWalls.getNearestWallOnSameChunk(chunk, x, y, z, 1, exterior);
        if (dist != -1) {
            return x + dist;
        }
        for (int d = 1; d <= 3 && (chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * 1, chunk.wy + d * 0)) != null; ++d) {
            int x2 = chunk2.wx * 8;
            int y2 = y;
            int n = dist = NearestWalls.hasWall(chunk2, x2, y2, z, 0, exterior) ? 0 : NearestWalls.getNearestWallOnSameChunk(chunk2, x2, y2, z, 1, exterior);
            if (dist == -1) continue;
            return x2 + dist;
        }
        return -1;
    }

    private static int getNearestWallNorth(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        IsoChunk chunk2;
        int wall = 2;
        boolean dx = false;
        int dy = -1;
        int dist = NearestWalls.getNearestWallOnSameChunk(chunk, x, y, z, 2, exterior);
        if (dist != -1) {
            return y - dist;
        }
        for (int d = 1; d <= 3 && (chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * 0, chunk.wy + d * -1)) != null; ++d) {
            int x2 = x;
            int y2 = (chunk2.wy + 1) * 8 - 1;
            dist = NearestWalls.getNearestWallOnSameChunk(chunk2, x2, y2, z, 2, exterior);
            if (dist == -1) continue;
            return y2 - dist;
        }
        return -1;
    }

    private static int getNearestWallSouth(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        IsoChunk chunk2;
        int wall = 3;
        boolean dx = false;
        boolean dy = true;
        int dist = NearestWalls.getNearestWallOnSameChunk(chunk, x, y, z, 3, exterior);
        if (dist != -1) {
            return y + dist;
        }
        for (int d = 1; d <= 3 && (chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * 0, chunk.wy + d * 1)) != null; ++d) {
            int x2 = x;
            int y2 = chunk2.wy * 8;
            int n = dist = NearestWalls.hasWall(chunk2, x2, y2, z, 2, exterior) ? 0 : NearestWalls.getNearestWallOnSameChunk(chunk2, x2, y2, z, 3, exterior);
            if (dist == -1) continue;
            return y2 + dist;
        }
        return -1;
    }

    public static void render(int x, int y, int z, boolean exterior) {
        int y2;
        int x2;
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(x, y, z);
        if (chunk == null) {
            return;
        }
        if (renderX != x || renderY != y || renderZ != z) {
            renderX = x;
            renderY = y;
            renderZ = z;
            System.out.println("ClosestWallDistance=" + NearestWalls.ClosestWallDistance(chunk, x, y, z, exterior));
        }
        if ((x2 = NearestWalls.getNearestWallWest(chunk, x, y, z, exterior)) != -1) {
            NearestWalls.DrawIsoLine(x2, (float)y + 0.5f, (float)x + 0.5f, (float)y + 0.5f, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
            NearestWalls.DrawIsoLine(x2, y, x2, y + 1, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
        }
        if ((x2 = NearestWalls.getNearestWallEast(chunk, x, y, z, exterior)) != -1) {
            NearestWalls.DrawIsoLine(x2, (float)y + 0.5f, (float)x + 0.5f, (float)y + 0.5f, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
            NearestWalls.DrawIsoLine(x2, y, x2, y + 1, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
        }
        if ((y2 = NearestWalls.getNearestWallNorth(chunk, x, y, z, exterior)) != -1) {
            NearestWalls.DrawIsoLine((float)x + 0.5f, y2, (float)x + 0.5f, (float)y + 0.5f, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
            NearestWalls.DrawIsoLine(x, y2, x + 1, y2, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
        }
        if ((y2 = NearestWalls.getNearestWallSouth(chunk, x, y, z, exterior)) != -1) {
            NearestWalls.DrawIsoLine((float)x + 0.5f, y2, (float)x + 0.5f, (float)y + 0.5f, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
            NearestWalls.DrawIsoLine(x, y2, x + 1, y2, z, 1.0f, 1.0f, 1.0f, 1.0f, 1);
        }
        float sx = IsoUtils.XToScreen(x, y, z, 0) - IsoCamera.frameState.offX;
        float sy = IsoUtils.YToScreen(x, y, z, 0) - IsoCamera.frameState.offY - (float)(16 * Core.tileScale);
        TextManager.instance.DrawStringCentre(UIFont.Small, sx, sy, String.format("%d", NearestWalls.ClosestWallDistance(chunk, x, y, z, exterior)), 1.0, 1.0, 1.0, 1.0);
    }

    private static void DrawIsoLine(float x, float y, float x2, float y2, float z, float r, float g, float b, float a, int thickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z, 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, a, thickness);
    }

    public static int ClosestWallDistance(IsoGridSquare square, boolean exterior) {
        if (square == null || square.chunk == null) {
            return 127;
        }
        return NearestWalls.ClosestWallDistance(square.chunk, square.x, square.y, square.z, exterior);
    }

    public static int ClosestWallDistance(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        int index;
        byte dist;
        if (chunk == null) {
            return 127;
        }
        ChunkLevelData levelData = NearestWalls.getOrCreateLevelData(chunk, z, exterior);
        if (levelData == null) {
            return 127;
        }
        byte[] closest = levelData.closest;
        if (levelData.changeCount != changeCount) {
            NearestWalls.calcDistanceOnThisChunkOnly(chunk, z, exterior);
            levelData.changeCount = changeCount;
        }
        if ((dist = closest[index = x - chunk.wx * 8 + (y - chunk.wy * 8) * 8]) != -1) {
            return dist;
        }
        int west = NearestWalls.getNearestWallWest(chunk, x, y, z, exterior);
        int east = NearestWalls.getNearestWallEast(chunk, x, y, z, exterior);
        int north = NearestWalls.getNearestWallNorth(chunk, x, y, z, exterior);
        int south = NearestWalls.getNearestWallSouth(chunk, x, y, z, exterior);
        if (west == -1 && east == -1 && north == -1 && south == -1) {
            closest[index] = 127;
            return 127;
        }
        if (exterior) {
            int min = 127;
            if (west != -1) {
                min = PZMath.min(min, x - west);
            }
            if (east != -1) {
                min = PZMath.min(min, east - x - 1);
            }
            if (north != -1) {
                min = PZMath.min(min, y - north);
            }
            if (south != -1) {
                min = PZMath.min(min, south - y - 1);
            }
            closest[index] = (byte)min;
            return closest[index];
        }
        int westEast = -1;
        if (west != -1 && east != -1) {
            westEast = east - west;
        }
        int northSouth = -1;
        if (north != -1 && south != -1) {
            northSouth = south - north;
        }
        if (westEast != -1 && northSouth != -1) {
            closest[index] = (byte)Math.min(westEast, northSouth);
            return closest[index];
        }
        if (westEast != -1) {
            closest[index] = (byte)westEast;
            return closest[index];
        }
        if (northSouth != -1) {
            closest[index] = (byte)northSouth;
            return closest[index];
        }
        IsoGridSquare square = chunk.getGridSquare(x - chunk.wx * 8, y - chunk.wy * 8, z);
        if (square != null && square.isOutside()) {
            west = west == -1 ? 127 : x - west;
            east = east == -1 ? 127 : east - x - 1;
            north = north == -1 ? 127 : y - north;
            south = south == -1 ? 127 : south - y - 1;
            closest[index] = (byte)Math.min(west, Math.min(east, Math.min(north, south)));
            return closest[index];
        }
        closest[index] = 127;
        return 127;
    }

    public static final class ChunkLevelData {
        int changeCount = -1;
        final byte[] distanceSelf = new byte[256];
        final byte[] closest = new byte[64];
    }
}

