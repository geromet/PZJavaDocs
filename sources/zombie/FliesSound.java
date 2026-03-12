/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ZomboidGlobals;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.CorpseCount;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;
import zombie.scripting.objects.CharacterTrait;
import zombie.util.list.PZArrayUtil;

public final class FliesSound {
    public static int maxCorpseCount = 25;
    public static final FliesSound instance = new FliesSound();
    private static final IsoGridSquare[] tempSquares = new IsoGridSquare[64];
    private final PlayerData[] playerData = new PlayerData[4];
    private final ArrayList<FadeEmitter> fadeEmitters = new ArrayList();
    private final float fliesVolume = -1.0f;

    public FliesSound() {
        for (int i = 0; i < this.playerData.length; ++i) {
            this.playerData[i] = new PlayerData(this);
        }
    }

    public void reset() {
        for (int i = 0; i < this.playerData.length; ++i) {
            this.playerData[i].reset();
        }
    }

    public void update() {
        int i;
        if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() == 1) {
            return;
        }
        for (i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null || player.getCurrentSquare() == null) continue;
            this.playerData[i].update(player);
        }
        for (i = 0; i < this.fadeEmitters.size(); ++i) {
            FadeEmitter emitter = this.fadeEmitters.get(i);
            if (!emitter.update()) continue;
            this.fadeEmitters.remove(i--);
        }
    }

    public void render(int playerIndex) {
        int chunkSizeInSquares = 8;
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
        for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; ++cy) {
            for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; ++cx) {
                ChunkData chunkData;
                IsoChunk chunk = chunkMap.getChunk(cx, cy);
                if (chunk == null || (chunkData = chunk.corpseData) == null) continue;
                int z = IsoPlayer.players[playerIndex].getZi();
                ChunkLevelData levelData = chunkData.levelData[z + 32];
                for (int i = 0; i < levelData.emitters.length; ++i) {
                    FadeEmitter emitter = levelData.emitters[i];
                    if (emitter != null && emitter.emitter != null) {
                        float alpha = 1.0f;
                        if (this.fadeEmitters.contains(emitter)) {
                            alpha = PZMath.max(emitter.volume, 0.1f);
                        }
                        this.paintSquare(emitter.sq.x, emitter.sq.y, emitter.sq.z, 0.0f, 1.0f, 0.0f, alpha);
                    }
                    if (!this.playerData[playerIndex].refs.contains(levelData)) continue;
                    this.paintSquare(chunk.wx * 8 + 4, chunk.wy * 8 + 4, 0, 0.0f, 0.0f, 1.0f, 1.0f);
                }
                IsoBuilding building = IsoPlayer.players[playerIndex].getCurrentBuilding();
                if (building == null || !CorpseCount.instance.hasBuildingCorpseCount(chunk, z, building)) continue;
                this.paintSquare(chunk.wx * 8 + 4, chunk.wy * 8 + 4, z, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }
    }

    private void paintSquare(int x, int y, int z, float r, float g, float b, float a) {
        int scale = Core.tileScale;
        int sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
        int sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32 * scale, sy - 16 * scale, sx + 64 * scale, sy, sx + 32 * scale, sy + 16 * scale, r, g, b, a);
    }

    public void chunkLoaded(IsoChunk chunk) {
        if (chunk.corpseData == null) {
            chunk.corpseData = new ChunkData(this, chunk.wx, chunk.wy);
        }
        chunk.corpseData.wx = chunk.wx;
        chunk.corpseData.wy = chunk.wy;
        chunk.corpseData.Reset();
        for (int i = 0; i < this.playerData.length; ++i) {
            this.playerData[i].forceUpdate = true;
        }
    }

    public void corpseAdded(int x, int y, int z) {
        if (z < -32 || z > 31) {
            DebugLog.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
            return;
        }
        ChunkData chunkData = this.getChunkData(x, y);
        if (chunkData == null) {
            return;
        }
        for (int i = 0; i < this.playerData.length; ++i) {
            ChunkLevelData levelData = chunkData.levelData[z + 32];
            if (!this.playerData[i].refs.contains(levelData)) continue;
            this.playerData[i].forceUpdate = true;
        }
    }

    public void corpseRemoved(int x, int y, int z) {
        if (z < -32 || z > 31) {
            DebugLog.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
            return;
        }
        ChunkData chunkData = this.getChunkData(x, y);
        if (chunkData == null) {
            return;
        }
        for (int i = 0; i < this.playerData.length; ++i) {
            ChunkLevelData levelData = chunkData.levelData[z + 32];
            if (!this.playerData[i].refs.contains(levelData)) continue;
            this.playerData[i].forceUpdate = true;
        }
    }

    private ChunkData getChunkData(int x, int y) {
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(x, y, 0);
        if (chunk != null) {
            return chunk.corpseData;
        }
        return null;
    }

    private final class PlayerData {
        int wx;
        int wy;
        int z;
        IsoBuilding building;
        boolean forceUpdate;
        boolean deaf;
        final ArrayList<ChunkLevelData> refs;
        final ArrayList<ChunkLevelData> refsPrev;
        final /* synthetic */ FliesSound this$0;

        PlayerData(FliesSound fliesSound) {
            FliesSound fliesSound2 = fliesSound;
            Objects.requireNonNull(fliesSound2);
            this.this$0 = fliesSound2;
            this.wx = Integer.MIN_VALUE;
            this.wy = Integer.MIN_VALUE;
            this.z = Integer.MIN_VALUE;
            this.refs = new ArrayList();
            this.refsPrev = new ArrayList();
        }

        boolean isSameLocation(IsoPlayer player) {
            IsoGridSquare playerSq = player.getCurrentSquare();
            if (playerSq != null && playerSq.getBuilding() != this.building) {
                return false;
            }
            int chunkSizeInSquares = 8;
            return PZMath.coorddivision(player.getXi(), 8) == this.wx && PZMath.coorddivision(player.getYi(), 8) == this.wy && player.getZi() == this.z;
        }

        void update(IsoPlayer player) {
            if (this.deaf != player.hasTrait(CharacterTrait.DEAF)) {
                this.forceUpdate = true;
                this.deaf = player.hasTrait(CharacterTrait.DEAF);
            }
            if (!this.forceUpdate && this.isSameLocation(player)) {
                return;
            }
            this.forceUpdate = false;
            int chunkSizeInSquares = 8;
            IsoGridSquare playerSq = player.getCurrentSquare();
            this.wx = PZMath.coorddivision(playerSq.getX(), 8);
            this.wy = PZMath.coorddivision(playerSq.getY(), 8);
            this.z = playerSq.getZ();
            this.building = playerSq.getBuilding();
            this.refs.clear();
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    ChunkData chunkData = this.this$0.getChunkData((this.wx + dx) * 8, (this.wy + dy) * 8);
                    if (chunkData == null) continue;
                    ChunkLevelData levelData = chunkData.levelData[this.z + 32];
                    levelData.update(this.wx + dx, this.wy + dy, this.z, player);
                    this.refs.add(levelData);
                }
            }
            for (int i = 0; i < this.refsPrev.size(); ++i) {
                ChunkLevelData levelData = this.refsPrev.get(i);
                if (this.refs.contains(levelData)) continue;
                levelData.deref(player);
            }
            this.refsPrev.clear();
            PZArrayUtil.addAll(this.refsPrev, this.refs);
        }

        void reset() {
            this.z = Integer.MIN_VALUE;
            this.wy = Integer.MIN_VALUE;
            this.wx = Integer.MIN_VALUE;
            this.building = null;
            this.forceUpdate = false;
            this.refs.clear();
            this.refsPrev.clear();
        }
    }

    private static final class FadeEmitter {
        private static final float FADE_IN_RATE = 0.01f;
        private static final float FADE_OUT_RATE = -0.01f;
        BaseSoundEmitter emitter;
        float volume = 1.0f;
        float targetVolume = 1.0f;
        IsoGridSquare sq;

        private FadeEmitter() {
        }

        boolean update() {
            if (this.emitter == null) {
                return true;
            }
            if (this.volume < this.targetVolume) {
                this.volume += 0.01f * GameTime.getInstance().getThirtyFPSMultiplier();
                if (this.volume >= this.targetVolume) {
                    this.volume = this.targetVolume;
                    return true;
                }
            } else {
                this.volume += -0.01f * GameTime.getInstance().getThirtyFPSMultiplier();
                if (this.volume <= 0.0f) {
                    this.volume = 0.0f;
                    this.emitter.stopAll();
                    this.emitter = null;
                    return true;
                }
            }
            this.emitter.setVolumeAll(this.volume);
            return false;
        }

        void Reset() {
            this.emitter = null;
            this.volume = 1.0f;
            this.targetVolume = 1.0f;
            this.sq = null;
        }
    }

    public final class ChunkData {
        private int wx;
        private int wy;
        private final ChunkLevelData[] levelData;

        private ChunkData(FliesSound this$0, int wx, int wy) {
            Objects.requireNonNull(this$0);
            this.levelData = new ChunkLevelData[64];
            this.wx = wx;
            this.wy = wy;
            for (int z = 0; z < this.levelData.length; ++z) {
                this.levelData[z] = new ChunkLevelData(this$0);
            }
        }

        public void removeFromWorld() {
            for (int z = 0; z < this.levelData.length; ++z) {
                ChunkLevelData levelData1 = this.levelData[z];
                for (int i = 0; i < 4; ++i) {
                    FadeEmitter fadeEmitter = levelData1.emitters[i];
                    if (fadeEmitter == null || fadeEmitter.emitter == null) continue;
                    fadeEmitter.emitter.stopAll();
                }
                levelData1.Reset();
            }
        }

        private void Reset() {
            for (int z = 0; z < this.levelData.length; ++z) {
                this.levelData[z].Reset();
            }
        }
    }

    private final class ChunkLevelData {
        final FadeEmitter[] emitters;
        final /* synthetic */ FliesSound this$0;

        ChunkLevelData(FliesSound fliesSound) {
            FliesSound fliesSound2 = fliesSound;
            Objects.requireNonNull(fliesSound2);
            this.this$0 = fliesSound2;
            this.emitters = new FadeEmitter[4];
        }

        IsoGridSquare calcSoundPos(int wx, int wy, int z, IsoBuilding building) {
            int chunkSizeInSquares = 8;
            IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(wx * 8, wy * 8, z);
            if (chunk == null) {
                return null;
            }
            int tempSquaresCount = 0;
            for (int cy = 0; cy < 8; ++cy) {
                for (int cx = 0; cx < 8; ++cx) {
                    IsoGridSquare sq = chunk.getGridSquare(cx, cy, z);
                    if (sq == null || sq.getStaticMovingObjects().isEmpty() || sq.getBuilding() != building) continue;
                    FliesSound.tempSquares[tempSquaresCount++] = sq;
                }
            }
            if (tempSquaresCount > 0) {
                return tempSquares[tempSquaresCount / 2];
            }
            return null;
        }

        void update(int wx, int wy, int z, IsoPlayer player) {
            int corpseCount = CorpseCount.instance.getCorpseCount(wx, wy, z, player.getCurrentBuilding());
            if (player.hasTrait(CharacterTrait.DEAF)) {
                corpseCount = 0;
            }
            if ((double)BodyDamage.getSicknessFromCorpsesRate(corpseCount) > ZomboidGlobals.foodSicknessDecrease) {
                IsoBuilding building = player.getCurrentBuilding();
                IsoGridSquare soundSq = this.calcSoundPos(wx, wy, z, building);
                if (soundSq == null) {
                    return;
                }
                if (this.emitters[player.playerIndex] == null) {
                    this.emitters[player.playerIndex] = new FadeEmitter();
                }
                FadeEmitter fadeEmitter = this.emitters[player.playerIndex];
                float targetVolume = 1.0f;
                if (fadeEmitter.emitter == null) {
                    fadeEmitter.emitter = IsoWorld.instance.getFreeEmitter(soundSq.x, soundSq.y, z);
                    fadeEmitter.emitter.playSoundLoopedImpl("CorpseFlies");
                    fadeEmitter.emitter.setVolumeAll(0.0f);
                    fadeEmitter.volume = 0.0f;
                    this.this$0.fadeEmitters.add(fadeEmitter);
                } else {
                    fadeEmitter.sq.setHasFlies(false);
                    fadeEmitter.emitter.setPos(soundSq.x, soundSq.y, z);
                    if (fadeEmitter.targetVolume != 1.0f && !this.this$0.fadeEmitters.contains(fadeEmitter)) {
                        this.this$0.fadeEmitters.add(fadeEmitter);
                    }
                }
                fadeEmitter.targetVolume = 1.0f;
                fadeEmitter.sq = soundSq;
                soundSq.setHasFlies(true);
            } else {
                FadeEmitter fadeEmitter = this.emitters[player.playerIndex];
                if (fadeEmitter != null && fadeEmitter.emitter != null) {
                    if (!this.this$0.fadeEmitters.contains(fadeEmitter)) {
                        this.this$0.fadeEmitters.add(fadeEmitter);
                    }
                    if (player.hasTrait(CharacterTrait.DEAF)) {
                        fadeEmitter.volume = 0.0f;
                    }
                    fadeEmitter.targetVolume = 0.0f;
                    fadeEmitter.sq.setHasFlies(false);
                }
            }
        }

        void deref(IsoPlayer player) {
            int pn = player.playerIndex;
            if (this.emitters[pn] != null && this.emitters[pn].emitter != null) {
                if (!this.this$0.fadeEmitters.contains(this.emitters[pn])) {
                    this.this$0.fadeEmitters.add(this.emitters[pn]);
                }
                this.emitters[pn].targetVolume = 0.0f;
                this.emitters[pn].sq.setHasFlies(false);
            }
        }

        void Reset() {
            for (int i = 0; i < 4; ++i) {
                if (this.emitters[i] == null) continue;
                this.emitters[i].Reset();
            }
        }
    }
}

