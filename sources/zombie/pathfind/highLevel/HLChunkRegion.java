/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.highLevel;

import java.nio.ByteBuffer;
import zombie.core.utils.BooleanGrid;
import zombie.debug.LineDrawer;
import zombie.pathfind.Chunk;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLGlobals;
import zombie.vehicles.Clipper;

public final class HLChunkRegion {
    static final int CPW = 8;
    HLChunkLevel levelData;
    final BooleanGrid squaresMask = new BooleanGrid(8, 8);
    int minX;
    int minY;
    int maxX;
    int maxY;
    final int[] edgeN = new int[8];
    final int[] edgeS = new int[8];
    final int[] edgeW = new int[8];
    final int[] edgeE = new int[8];

    Chunk getChunk() {
        return this.levelData.getChunk();
    }

    int getLevel() {
        return this.levelData.getLevel();
    }

    boolean containsSquare(int worldSquareX, int worldSquareY) {
        return this.squaresMask.getValue(worldSquareX - this.getChunk().getMinX(), worldSquareY - this.getChunk().getMinY());
    }

    boolean containsSquareLocal(int chunkSquareX, int chunkSquareY) {
        return this.squaresMask.getValue(chunkSquareX, chunkSquareY);
    }

    void initEdges() {
        for (int y = 0; y < 8; ++y) {
            this.edgeN[y] = 0;
            this.edgeS[y] = 0;
            for (int x = 0; x < 8; ++x) {
                if (!this.containsSquareLocal(x, y)) continue;
                if (!this.containsSquareLocal(x, y - 1)) {
                    int n = y;
                    this.edgeN[n] = this.edgeN[n] | 1 << x;
                }
                if (this.containsSquareLocal(x, y + 1)) continue;
                int n = y;
                this.edgeS[n] = this.edgeS[n] | 1 << x;
            }
        }
        for (int x = 0; x < 8; ++x) {
            this.edgeW[x] = 0;
            this.edgeE[x] = 0;
            for (int y = 0; y < 8; ++y) {
                if (!this.containsSquareLocal(x, y)) continue;
                if (!this.containsSquareLocal(x - 1, y)) {
                    int n = x;
                    this.edgeW[n] = this.edgeW[n] | 1 << y;
                }
                if (this.containsSquareLocal(x + 1, y)) continue;
                int n = x;
                this.edgeE[n] = this.edgeE[n] | 1 << y;
            }
        }
    }

    boolean isOnEdgeOfLoadedArea() {
        boolean bOnEdge = false;
        if (this.edgeN[0] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx, this.getChunk().wy - 1) == null;
        }
        if (this.edgeS[7] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx, this.getChunk().wy + 1) == null;
        }
        if (this.edgeW[0] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx - 1, this.getChunk().wy) == null;
        }
        if (this.edgeE[7] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx + 1, this.getChunk().wy) == null;
        }
        return bOnEdge;
    }

    void renderDebug() {
        Clipper clipper = HLGlobals.clipper;
        clipper.clear();
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                if (!this.squaresMask.getValue(x, y)) continue;
                clipper.addAABB(x, y, x + 1, y + 1);
            }
        }
        ByteBuffer polyBuffer = HLGlobals.clipperBuffer;
        int polyCount = clipper.generatePolygons(-0.1f, 3);
        for (int i = 0; i < polyCount; ++i) {
            polyBuffer.clear();
            clipper.getPolygon(i, polyBuffer);
            this.renderPolygon(polyBuffer, true);
            int holeCount = polyBuffer.getShort();
            for (int j = 0; j < holeCount; ++j) {
                this.renderPolygon(polyBuffer, false);
            }
        }
    }

    private void renderPolygon(ByteBuffer polyBuffer, boolean outer) {
        int pointCount = polyBuffer.getShort();
        if (pointCount < 3) {
            polyBuffer.position(polyBuffer.position() + pointCount * 4 * 2);
            return;
        }
        int left = this.getChunk().wx * 8;
        int top = this.getChunk().wy * 8;
        float xFirst = 0.0f;
        float yFirst = 0.0f;
        float xPrev = 0.0f;
        float yPrev = 0.0f;
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        if (!outer) {
            r *= 0.5f;
            g *= 0.5f;
            b *= 0.5f;
        }
        int level = this.getLevel() - 32;
        for (int j = 0; j < pointCount; ++j) {
            float x = polyBuffer.getFloat();
            float y = polyBuffer.getFloat();
            if (j == 0) {
                xFirst = x;
                yFirst = y;
            } else {
                LineDrawer.addLine((float)left + xPrev, (float)top + yPrev, (float)level, (float)left + x, (float)top + y, (float)level, r, g, b, 1.0f);
                if (j == pointCount - 1) {
                    LineDrawer.addLine((float)left + x, (float)top + y, (float)level, (float)left + xFirst, (float)top + yFirst, (float)level, r, g, b, 1.0f);
                }
            }
            xPrev = x;
            yPrev = y;
        }
    }
}

