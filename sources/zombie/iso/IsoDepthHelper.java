/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.core.math.PZMath;
import zombie.iso.fboRenderChunk.FBORenderLevels;

public class IsoDepthHelper {
    private static final ThreadLocal<Results> results = ThreadLocal.withInitial(Results::new);
    public static final float CHUNK_DEPTH = 0.023093667f;
    public static final float SQUARE_DEPTH = 0.0028867084f;
    public static final float LEVEL_DEPTH = 0.0028867084f;
    public static final int CHUNK_WIDTH_OF_DEPTH_BUFFER = 20;

    public static Results getChunkDepthData(int centreWX, int centreWY, int wx, int wy, int level) {
        Results r = results.get();
        int xscrMin = centreWX + 10;
        int yscrMin = centreWY + 10;
        int xscrMax = centreWX - 10;
        int yscrMax = centreWY - 10;
        int difX = Math.abs(xscrMax - xscrMin);
        int difY = Math.abs(yscrMax - yscrMin);
        int indexX = wx - xscrMax;
        int indexY = wy - yscrMax;
        int indexX2 = wx - 1 - xscrMax;
        int indexY2 = wy - 1 - yscrMax;
        indexX = difX - indexX;
        indexY = difY - indexY;
        indexX2 = difX - indexX2;
        indexY2 = difY - indexY2;
        indexX *= 8;
        indexY *= 8;
        float max = (difX *= 8) + (difY *= 8);
        r.sizeX = difX;
        r.sizeY = difY;
        r.indexX2 = indexX2 *= 8;
        r.indexY2 = indexY2 *= 8;
        r.indexX = indexX;
        r.indexY = indexY;
        r.maxDepth = max;
        r.depthStart = (float)(indexX + indexY) / max;
        r.depthEnd = (float)(indexX2 + indexY2) / max;
        int chunksPerWidth = 8;
        r.depthStart = (float)(indexX + indexY) / 8.0f / 40.0f;
        r.depthStart *= 0.46187335f;
        r.depthStart -= (float)FBORenderLevels.calculateMinLevel(PZMath.fastfloor(level)) * 0.0028867084f;
        return r;
    }

    public static Results getDepthSize() {
        Results r = results.get();
        int xscrMin = 10;
        int yscrMin = 10;
        int xscrMax = -10;
        int yscrMax = -10;
        int difX = Math.abs((xscrMax *= 8) - (xscrMin *= 8));
        int difY = Math.abs((yscrMax *= 8) - (yscrMin *= 8));
        r.sizeX = difX;
        r.sizeY = difY;
        return r;
    }

    public static float calculateDepth(float x, float y, float z) {
        float depth = (7.0f - z) * 2.5f;
        float xMod = PZMath.coordmodulof(x, 8);
        float yMod = PZMath.coordmodulof(y, 8);
        xMod = 8.0f - xMod;
        yMod = 8.0f - yMod;
        int xscrMin = 0;
        int yscrMin = 0;
        int xscrMax = -20;
        int yscrMax = -20;
        int difX = Math.abs((xscrMax *= 8) - (xscrMin *= 8));
        int difY = Math.abs((yscrMax *= 8) - (yscrMin *= 8));
        float max = difX + difY;
        float d = (xMod + yMod + depth) / max;
        int minLevel = FBORenderLevels.calculateMinLevel(PZMath.fastfloor(z));
        float zOffset = (2.0f - (z - (float)minLevel)) * 0.0028867084f;
        int chunksPerWidth = 8;
        return (xMod + yMod) / 16.0f * 0.023093667f + zOffset;
    }

    public static Results getSquareDepthData(int centreX, int centreY, float x, float y, float z) {
        centreX = PZMath.fastfloor((float)centreX / 8.0f);
        centreY = PZMath.fastfloor((float)centreY / 8.0f);
        Results r = IsoDepthHelper.getChunkDepthData(centreX, centreY, PZMath.fastfloor(x / 8.0f), PZMath.fastfloor(y / 8.0f), PZMath.fastfloor(z));
        r.depthStart += IsoDepthHelper.calculateDepth(x, y, z);
        return r;
    }

    public static class Results {
        public int indexX;
        public int indexY;
        public int indexX2;
        public int indexY2;
        public int sizeX;
        public int sizeY;
        public float depthStart;
        public float depthEnd;
        public float maxDepth;
    }
}

