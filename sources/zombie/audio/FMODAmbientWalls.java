/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import fmod.fmod.FMODSoundEmitter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import zombie.AmbientStreamManager;
import zombie.GameSounds;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.FMODAmbientWallLevelData;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.audio.ObjectAmbientEmitters;
import zombie.audio.parameters.ParameterInside;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.SoundKey;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class FMODAmbientWalls {
    public static final boolean ENABLE = true;
    private static FMODAmbientWalls instance;
    private final Vector2 tempVector2 = new Vector2();
    private final ObjectPool<ObjectWithDistance> objectPool = new ObjectPool<ObjectWithDistance>(ObjectWithDistance::new);
    private final ArrayList<ObjectWithDistance> objects = new ArrayList();
    private final Slot[] slots;
    private final ObjectPool<ObjectAmbientEmitters.DoorLogic> doorLogicPool = new ObjectPool<ObjectAmbientEmitters.DoorLogic>(ObjectAmbientEmitters.DoorLogic::new);
    private final ObjectPool<ObjectAmbientEmitters.WindowLogic> windowLogicPool = new ObjectPool<ObjectAmbientEmitters.WindowLogic>(ObjectAmbientEmitters.WindowLogic::new);
    private final ObjectPool<OpenWallLogic> openWallLogicPool = new ObjectPool<OpenWallLogic>(OpenWallLogic::new);
    private final HashMap<String, Integer> instanceCounts = new HashMap();
    private final Comparator<ObjectWithDistance> comp = (a, b) -> Float.compare(a.distSq, b.distSq);

    public static FMODAmbientWalls getInstance() {
        if (instance == null) {
            instance = new FMODAmbientWalls();
        }
        return instance;
    }

    private FMODAmbientWalls() {
        int numSlots = 16;
        this.slots = PZArrayUtil.newInstance(Slot.class, 16, Slot::new);
    }

    public void update() {
        this.addObjectsFromChunks();
        ParameterInside.calculateFloodFill();
        this.updateEmitters();
    }

    void addObjectsFromChunks() {
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player;
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[i];
            if (chunkMap.ignore || (player = IsoPlayer.players[i]) == null) continue;
            int midX = IsoChunkMap.chunkGridWidth / 2;
            int midY = IsoChunkMap.chunkGridWidth / 2;
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    this.addObjectsFromChunkLevel(i, midX + dx, midY + dy);
                }
            }
        }
    }

    private void addObjectsFromChunkLevel(int playerIndex, int chunkX, int chunkY) {
        IsoChunkMap chunkMap = IsoWorld.instance.getCell().getChunkMap(playerIndex);
        IsoChunk chunk = chunkMap.getChunk(chunkX, chunkY);
        if (chunk == null) {
            return;
        }
        int playerZ = PZMath.fastfloor(IsoPlayer.players[playerIndex].getZ());
        IsoChunkLevel chunkLevel = chunk.getLevelData(playerZ);
        if (chunkLevel == null) {
            return;
        }
        FMODAmbientWallLevelData levelData = chunkLevel.fmodAmbientWallLevelData;
        if (levelData == null) {
            levelData = chunkLevel.fmodAmbientWallLevelData = FMODAmbientWallLevelData.alloc().init(chunkLevel);
        }
        levelData.checkDirty();
        for (int i = 0; i < levelData.walls.size(); ++i) {
            FMODAmbientWallLevelData.FMODAmbientWall wall = levelData.walls.get(i);
            this.addObjects(wall);
        }
    }

    private void addObjects(FMODAmbientWallLevelData.FMODAmbientWall wall) {
        int z = wall.owner.chunkLevel.getLevel();
        if (wall.isHorizontal()) {
            for (int x = wall.x1; x < wall.x2; ++x) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, wall.y1, z);
                if (square == null) continue;
                ObjectWithDistance owd = this.objectPool.alloc();
                owd.x = square.x;
                owd.y = square.y;
                owd.z = square.z;
                owd.north = true;
                owd.setObjectAndLogic(square);
                this.objects.add(owd);
            }
        } else {
            for (int y = wall.y1; y < wall.y2; ++y) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(wall.x1, y, z);
                if (square == null) continue;
                ObjectWithDistance owd = this.objectPool.alloc();
                owd.x = square.x;
                owd.y = square.y;
                owd.z = square.z;
                owd.north = false;
                owd.setObjectAndLogic(square);
                this.objects.add(owd);
            }
        }
    }

    private void updateEmitters() {
        ObjectWithDistance owd;
        int i;
        ObjectWithDistance owd2;
        int i2;
        for (i2 = 0; i2 < this.slots.length; ++i2) {
            this.slots[i2].playing = false;
        }
        if (this.objects.isEmpty()) {
            this.stopNotPlaying();
            return;
        }
        for (i2 = 0; i2 < this.objects.size(); ++i2) {
            owd2 = this.objects.get(i2);
            owd2.getEmitterPosition(this.tempVector2);
            owd2.distSq = this.getClosestListener(this.tempVector2.x, this.tempVector2.y, owd2.z);
            if (!(owd2.distSq > 64.0f) && owd2.shouldPlay()) continue;
            this.objects.remove(i2--);
            this.objectPool.release(owd2);
        }
        this.objects.sort(this.comp);
        this.instanceCounts.clear();
        for (i2 = 0; i2 < this.objects.size(); ++i2) {
            int instanceCount;
            owd2 = this.objects.get(i2);
            if (owd2.logic == null) continue;
            String soundName = owd2.logic.getSoundName();
            GameSound gameSound = GameSounds.getSound(soundName);
            if (gameSound != null && !gameSound.clips.isEmpty()) {
                GameSoundClip clip0 = (GameSoundClip)gameSound.clips.getFirst();
                if (!StringUtils.isNullOrWhitespace(clip0.event)) {
                    soundName = clip0.event;
                }
            }
            if ((instanceCount = this.instanceCounts.getOrDefault(soundName, 0).intValue()) >= 3) {
                this.objects.remove(i2--);
                this.objectPool.release(owd2);
                continue;
            }
            this.instanceCounts.put(soundName, instanceCount + 1);
        }
        int count = Math.min(this.objects.size(), this.slots.length);
        for (i = 0; i < count; ++i) {
            owd = this.objects.get(i);
            int j = this.getExistingSlot(owd);
            if (j == -1) continue;
            Slot slot = this.slots[j];
            this.objects.remove(i--);
            --count;
            if (slot.owd != null) {
                this.objectPool.release(slot.owd);
            }
            slot.playSound(owd);
        }
        for (i = 0; i < count; ++i) {
            owd = this.objects.get(i);
            int j = this.getExistingSlot(owd);
            if (j != -1) continue;
            j = this.getFreeSlot();
            Slot slot = this.slots[j];
            if (slot.owd != null) {
                if (slot.emitter != null && !slot.emitter.isPlaying(owd.logic.getSoundName())) {
                    slot.stopPlaying();
                }
                this.objectPool.release(slot.owd);
                slot.owd = null;
            }
            this.objects.remove(i--);
            --count;
            slot.playSound(owd);
        }
        this.stopNotPlaying();
        this.objectPool.releaseAll((List<ObjectWithDistance>)this.objects);
        this.objects.clear();
    }

    float getClosestListener(float soundX, float soundY, float soundZ) {
        float minDist = Float.MAX_VALUE;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr == null || chr.getCurrentSquare() == null) continue;
            float px = chr.getX();
            float py = chr.getY();
            float pz = chr.getZ();
            float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0f, soundX, soundY, soundZ * 3.0f);
            if (!((distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0f)) < minDist)) continue;
            minDist = distSq;
        }
        return minDist;
    }

    int getExistingSlot(ObjectWithDistance owd) {
        for (int i = 0; i < this.slots.length; ++i) {
            ObjectWithDistance owd1 = this.slots[i].owd;
            if (owd1 == null || owd.x != owd1.x || owd.y != owd1.y || owd.z != owd1.z || owd.north != owd1.north) continue;
            return i;
        }
        return -1;
    }

    int getFreeSlot() {
        for (int i = 0; i < this.slots.length; ++i) {
            if (this.slots[i].playing) continue;
            return i;
        }
        return -1;
    }

    void stopNotPlaying() {
        for (int i = 0; i < this.slots.length; ++i) {
            Slot slot = this.slots[i];
            if (slot.playing) continue;
            slot.stopPlaying();
            slot.owd = null;
        }
    }

    public void squareChanged(IsoGridSquare square) {
        IsoChunkLevel chunkLevel;
        if (square != null && square.getChunk() != null && (chunkLevel = square.getChunk().getLevelData(square.getZ())) != null && chunkLevel.fmodAmbientWallLevelData != null) {
            chunkLevel.fmodAmbientWallLevelData.dirty = true;
        }
    }

    public void render() {
        if (!DebugOptions.instance.ambientWallEmittersRender.getValue()) {
            return;
        }
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoPlayer player;
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
            if (chunkMap.ignore || (player = IsoPlayer.players[playerIndex]) == null) continue;
            int playerZ = PZMath.fastfloor(player.getZ());
            int midX = IsoChunkMap.chunkGridWidth / 2;
            int midY = IsoChunkMap.chunkGridWidth / 2;
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    FMODAmbientWallLevelData levelData;
                    IsoChunkLevel chunkLevel;
                    IsoChunk chunk = chunkMap.getChunk(midX + dx, midY + dy);
                    if (chunk == null || (chunkLevel = chunk.getLevelData(playerZ)) == null || (levelData = chunkLevel.fmodAmbientWallLevelData) == null) continue;
                    for (int i = 0; i < levelData.walls.size(); ++i) {
                        FMODAmbientWallLevelData.FMODAmbientWall wall = levelData.walls.get(i);
                        LineDrawer.addLine((float)wall.x1, (float)wall.y1, (float)playerZ, (float)wall.x2, (float)wall.y2, (float)playerZ, 1.0f, 0.0f, 0.0f, 1.0f);
                    }
                }
            }
        }
        for (Slot slot : this.slots) {
            if (slot.owd == null) continue;
            ObjectWithDistance owd = slot.owd;
            int w = owd.north ? 1 : 0;
            int h = 1 - w;
            LineDrawer.addLine((float)owd.x, (float)owd.y, (float)owd.z, (float)(owd.x + w), (float)(owd.y + h), (float)owd.z, 0.0f, 1.0f, 0.0f, 1.0f);
        }
    }

    static final class Slot {
        ObjectWithDistance owd;
        BaseSoundEmitter emitter;
        long instance;
        boolean playing;

        Slot() {
        }

        void playSound(ObjectWithDistance owd) {
            this.owd = owd;
            ObjectAmbientEmitters.PerObjectLogic logic = owd.logic;
            if (this.emitter == null) {
                this.emitter = Core.soundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter();
            }
            Vector2 tempVector2 = owd.getEmitterPosition(FMODAmbientWalls.getInstance().tempVector2);
            this.emitter.setPos(tempVector2.getX(), tempVector2.getY(), owd.z);
            String soundName = logic.getSoundName();
            if (!this.emitter.isPlaying(soundName)) {
                this.emitter.stopAll();
                BaseSoundEmitter baseSoundEmitter = this.emitter;
                if (baseSoundEmitter instanceof FMODSoundEmitter) {
                    FMODSoundEmitter fmodSoundEmitter = (FMODSoundEmitter)baseSoundEmitter;
                    fmodSoundEmitter.clearParameters();
                }
                this.instance = this.emitter.playSoundImpl(soundName, (IsoObject)null);
                logic.startPlaying(this.emitter, this.instance);
            }
            logic.checkParameters(this.emitter, this.instance);
            this.playing = true;
            this.emitter.tick();
        }

        void stopPlaying() {
            if (this.emitter == null || this.instance == 0L) {
                return;
            }
            ObjectAmbientEmitters.PerObjectLogic logic = this.owd.logic;
            logic.stopPlaying(this.emitter, this.instance);
            if (this.emitter.hasSustainPoints(this.instance)) {
                this.emitter.triggerCue(this.instance);
                this.instance = 0L;
                return;
            }
            this.emitter.stopAll();
            this.instance = 0L;
        }
    }

    static final class ObjectWithDistance {
        int x;
        int y;
        int z;
        boolean north;
        IsoObject object;
        ObjectAmbientEmitters.PerObjectLogic logic;
        float distSq;

        ObjectWithDistance() {
        }

        Vector2 getEmitterPosition(Vector2 pos) {
            IsoObject isoObject = this.object;
            if (isoObject instanceof IsoDoor) {
                IsoDoor door = (IsoDoor)isoObject;
                return door.getFacingPosition(pos);
            }
            isoObject = this.object;
            if (isoObject instanceof IsoWindow) {
                IsoWindow window = (IsoWindow)isoObject;
                return window.getFacingPosition(pos);
            }
            if (this.north) {
                return pos.set((float)this.x + 0.5f, this.y);
            }
            return pos.set(this.x, (float)this.y + 0.5f);
        }

        void setObjectAndLogic(IsoGridSquare square) {
            this.release();
            this.object = square.getDoor(this.north);
            if (this.object != null) {
                this.logic = FMODAmbientWalls.getInstance().doorLogicPool.alloc().init(this.object);
                return;
            }
            this.object = square.getWindow(this.north);
            if (this.object != null) {
                this.logic = FMODAmbientWalls.getInstance().windowLogicPool.alloc().init(this.object);
                return;
            }
            this.logic = FMODAmbientWalls.getInstance().openWallLogicPool.alloc().init(square, this.north);
        }

        boolean shouldPlay() {
            if (this.object != null && this.object.getObjectIndex() == -1) {
                return false;
            }
            return this.logic.shouldPlaySound();
        }

        void release() {
            ObjectAmbientEmitters.PerObjectLogic perObjectLogic = this.logic;
            if (perObjectLogic instanceof ObjectAmbientEmitters.DoorLogic) {
                ObjectAmbientEmitters.DoorLogic logic1 = (ObjectAmbientEmitters.DoorLogic)perObjectLogic;
                FMODAmbientWalls.getInstance().doorLogicPool.release(logic1);
            } else {
                perObjectLogic = this.logic;
                if (perObjectLogic instanceof ObjectAmbientEmitters.WindowLogic) {
                    ObjectAmbientEmitters.WindowLogic logic1 = (ObjectAmbientEmitters.WindowLogic)perObjectLogic;
                    FMODAmbientWalls.getInstance().windowLogicPool.release(logic1);
                } else {
                    perObjectLogic = this.logic;
                    if (perObjectLogic instanceof OpenWallLogic) {
                        OpenWallLogic logic1 = (OpenWallLogic)perObjectLogic;
                        FMODAmbientWalls.getInstance().openWallLogicPool.release(logic1);
                    }
                }
            }
            this.object = null;
            this.logic = null;
        }
    }

    public static final class OpenWallLogic
    extends ObjectAmbientEmitters.PerObjectLogic {
        IsoGridSquare square;
        boolean north;

        public OpenWallLogic init(IsoGridSquare square, boolean north) {
            super.init(null);
            this.square = square;
            this.north = north;
            return this;
        }

        @Override
        public boolean shouldPlaySound() {
            return AmbientStreamManager.instance.isParameterInsideTrue() && this.isReachableSquare();
        }

        @Override
        public String getSoundName() {
            return SoundKey.OPEN_WALL_AMBIENCE.toString();
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            this.parameterValue1 = Float.NaN;
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
            this.setParameterValue1(emitter, instance, "DoorWindowOpen", 1.0f);
        }

        boolean isReachableSquare() {
            return this.square != null && ParameterInside.isAdjacentToReachableSquare(this.square, this.north);
        }
    }
}

