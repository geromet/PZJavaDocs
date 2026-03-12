/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.audio.FMODAmbientWallLevelData;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.CorpseCount;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.NearestWalls;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.objects.RainManager;

public final class IsoChunkLevel {
    public static final byte CLDSF_NONE = 0;
    public static final byte CLDSF_SHOULD_RENDER = 1;
    public static final byte CLDSF_RAIN_RANDOM_XY = 2;
    public IsoChunk chunk;
    public int level;
    public final IsoGridSquare[] squares = new IsoGridSquare[64];
    public final boolean[] lightCheck = new boolean[4];
    public boolean physicsCheck;
    public final byte[] rainFlags = new byte[64];
    public final float[] rainSplashFrame = new float[64];
    public boolean raining;
    public int rainSplashFrameNum = -1;
    public CorpseCount.ChunkLevelData corpseCount;
    public FMODAmbientWallLevelData fmodAmbientWallLevelData;
    public NearestWalls.ChunkLevelData nearestWalls;
    public NearestWalls.ChunkLevelData nearestExteriorWalls;
    private static final ConcurrentLinkedQueue<IsoChunkLevel> pool = new ConcurrentLinkedQueue();

    public IsoChunkLevel init(IsoChunk chunk, int level) {
        this.chunk = chunk;
        this.level = level;
        Arrays.fill(this.rainSplashFrame, -1.0f);
        return this;
    }

    public IsoChunk getChunk() {
        return this.chunk;
    }

    public int getLevel() {
        return this.level;
    }

    public void updateRainSplashes() {
        boolean bRaining;
        if (this.rainSplashFrameNum == IsoCamera.frameState.frameCount) {
            return;
        }
        this.rainSplashFrameNum = IsoCamera.frameState.frameCount;
        boolean bl = bRaining = IsoWorld.instance.currentCell.getRainIntensity() > 0 || RainManager.isRaining() != false && RainManager.rainIntensity > 0.0f;
        if (bRaining) {
            this.raining = true;
            if (IsoCamera.frameState.paused) {
                return;
            }
            int intensity = IsoWorld.instance.currentCell.getRainIntensity();
            if (intensity == 0) {
                intensity = Math.min(PZMath.fastfloor(RainManager.rainIntensity / 0.2f) + 1, 5);
            }
            for (int i = 0; i < this.rainSplashFrame.length; ++i) {
                if (this.rainSplashFrame[i] < 0.0f) {
                    if (!Rand.NextBool(Rand.AdjustForFramerate((int)(5.0f / (float)intensity) * 100))) continue;
                    this.rainSplashFrame[i] = 0.0f;
                    int n = i;
                    this.rainFlags[n] = (byte)(this.rainFlags[n] | 2);
                    continue;
                }
                int n = i;
                this.rainSplashFrame[n] = this.rainSplashFrame[n] + 0.08f * (30.0f / (float)PerformanceSettings.getLockFPS());
                if (!(this.rainSplashFrame[i] >= 1.0f)) continue;
                this.rainSplashFrame[i] = -1.0f;
            }
            return;
        }
        if (this.raining) {
            this.raining = false;
            Arrays.fill(this.rainSplashFrame, -1.0f);
        }
    }

    public void renderRainSplashes(int playerIndex) {
        if (!this.raining) {
            return;
        }
        FBORenderCutaways.ChunkLevelData cutawayLevel = this.chunk.getCutawayDataForLevel(this.level);
        for (int i = 0; i < this.rainSplashFrame.length; ++i) {
            IsoGridSquare square;
            if (this.rainSplashFrame[i] < 0.0f || !cutawayLevel.shouldRenderSquare(playerIndex, square = this.chunk.getGridSquare(i % 8, i / 8, this.level))) continue;
            square.renderRainSplash(playerIndex, square.getLightInfo(playerIndex), this.rainSplashFrame[i], (this.rainFlags[i] & 2) != 0);
            int n = i;
            this.rainFlags[n] = (byte)(this.rainFlags[n] & 0xFFFFFFFD);
        }
    }

    public void clear() {
        Arrays.fill(this.squares, null);
        Arrays.fill(this.lightCheck, true);
        this.physicsCheck = false;
        Arrays.fill(this.rainFlags, (byte)0);
        Arrays.fill(this.rainSplashFrame, -1.0f);
        this.raining = false;
        this.rainSplashFrameNum = -1;
    }

    public static IsoChunkLevel alloc() {
        IsoChunkLevel obj = pool.poll();
        if (obj == null) {
            obj = new IsoChunkLevel();
        }
        return obj;
    }

    public void release() {
        if (this.fmodAmbientWallLevelData != null) {
            this.fmodAmbientWallLevelData.release();
            this.fmodAmbientWallLevelData = null;
        }
        pool.add(this);
    }

    public void checkPhysicsLaterForActiveRagdoll() {
        this.chunk.checkPhysicsLaterForActiveRagdoll(this);
    }

    public boolean containsIsoGridSquare(IsoGridSquare isoGridSquare) {
        return Arrays.stream(this.squares).anyMatch(square -> square == isoGridSquare);
    }
}

