/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import gnu.trove.map.hash.THashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import zombie.AmbientStreamManager;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.GameSound;
import zombie.audio.parameters.ParameterCurrentZone;
import zombie.audio.parameters.ParameterInside;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.entity.util.TimSort;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

public final class ObjectAmbientEmitters {
    private final HashMap<String, PowerPolicy> powerPolicyMap = new HashMap();
    private static ObjectAmbientEmitters instance;
    static final Vector2 tempVector2;
    private final THashMap<IsoObject, ObjectWithDistance> added = new THashMap();
    private final ObjectPool<ObjectWithDistance> objectPool = new ObjectPool<ObjectWithDistance>(ObjectWithDistance::new);
    private final PZArrayList<ObjectWithDistance> objects = new PZArrayList<ObjectWithDistance>(ObjectWithDistance.class, 16);
    private final TimSort timSort = new TimSort();
    private final Slot[] slots;
    private final Comparator<ObjectWithDistance> comp = new Comparator<ObjectWithDistance>(this){
        {
            Objects.requireNonNull(this$0);
        }

        @Override
        public int compare(ObjectWithDistance a, ObjectWithDistance b) {
            return Float.compare(a.distSq, b.distSq);
        }
    };

    public static ObjectAmbientEmitters getInstance() {
        if (instance == null) {
            instance = new ObjectAmbientEmitters();
        }
        return instance;
    }

    private ObjectAmbientEmitters() {
        int numSlots = 16;
        this.slots = PZArrayUtil.newInstance(Slot.class, 16, Slot::new);
        this.added.setAutoCompactionFactor(0.0f);
        this.powerPolicyMap.put("FactoryMachineAmbiance", PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("HotdogMachineAmbiance", PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("PayPhoneAmbiance", PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("StreetLightAmbiance", PowerPolicy.StreetLight);
        this.powerPolicyMap.put("NeonLightAmbiance", PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("NeonSignAmbiance", PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("JukeboxAmbiance", PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("ControlStationAmbiance", PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("ClockAmbiance", PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("GasPumpAmbiance", PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("LightBulbAmbiance", PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("ArcadeMachineAmbiance", PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("PylonTowerAmbience", PowerPolicy.ExteriorOK);
    }

    private static boolean addObjectLambda(IsoObject object, PerObjectLogic logic) {
        ObjectAmbientEmitters.getInstance().addObject(object, logic);
        return true;
    }

    private void addObject(IsoObject object, PerObjectLogic logic) {
        if (GameServer.server) {
            return;
        }
        if (this.added.containsKey(object)) {
            return;
        }
        boolean bListener = false;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null || object.getObjectIndex() == -1) continue;
            int maxDist = 15;
            if (logic instanceof DoorLogic || logic instanceof WindowLogic) {
                maxDist = 10;
            }
            if (logic instanceof AmbientSoundLogic) {
                AmbientSoundLogic ambientSoundLogic = (AmbientSoundLogic)logic;
                if (ambientSoundLogic.powerPolicy != PowerPolicy.NotRequired) {
                    maxDist = 30;
                }
            }
            if (object.square.z != PZMath.fastfloor(player.getZ()) && (logic instanceof DoorLogic || logic instanceof WindowLogic) || player.DistToSquared((float)object.square.x + 0.5f, (float)object.square.y + 0.5f) > (float)(maxDist * maxDist)) continue;
            bListener = true;
            break;
        }
        if (!bListener) {
            return;
        }
        ObjectWithDistance owd = this.objectPool.alloc();
        owd.object = object;
        owd.logic = logic;
        this.objects.add(owd);
        this.added.put(object, owd);
    }

    void removeObject(IsoObject object) {
        if (GameServer.server) {
            return;
        }
        ObjectWithDistance owd = this.added.remove(object);
        if (owd == null) {
            return;
        }
        this.objects.remove(owd);
        this.objectPool.release(owd);
    }

    public void update() {
        int j;
        int i;
        PerObjectLogic logic;
        IsoObject object;
        int i2;
        if (GameServer.server) {
            return;
        }
        this.addObjectsFromChunks();
        for (i2 = 0; i2 < this.slots.length; ++i2) {
            this.slots[i2].playing = false;
        }
        if (this.objects.isEmpty()) {
            this.stopNotPlaying();
            return;
        }
        for (i2 = 0; i2 < this.objects.size(); ++i2) {
            ObjectWithDistance owd = this.objects.get(i2);
            object = owd.object;
            logic = this.objects.get((int)i2).logic;
            if (!this.shouldPlay(object, logic)) {
                this.added.remove(object);
                this.objects.remove(i2--);
                this.objectPool.release(owd);
                continue;
            }
            object.getFacingPosition(tempVector2);
            owd.distSq = this.getClosestListener(ObjectAmbientEmitters.tempVector2.x, ObjectAmbientEmitters.tempVector2.y, object.square.z);
        }
        this.timSort.doSort(this.objects.getElements(), this.comp, 0, this.objects.size());
        int count = Math.min(this.objects.size(), this.slots.length);
        for (i = 0; i < count; ++i) {
            object = this.objects.get((int)i).object;
            logic = this.objects.get((int)i).logic;
            if (!this.shouldPlay(object, logic) || (j = this.getExistingSlot(object)) == -1) continue;
            this.slots[j].playSound(object, logic);
        }
        for (i = 0; i < count; ++i) {
            object = this.objects.get((int)i).object;
            logic = this.objects.get((int)i).logic;
            if (!this.shouldPlay(object, logic) || (j = this.getExistingSlot(object)) != -1) continue;
            j = this.getFreeSlot();
            if (this.slots[j].object != null) {
                this.slots[j].stopPlaying();
                this.slots[j].object = null;
            }
            this.slots[j].playSound(object, logic);
        }
        this.stopNotPlaying();
        this.added.clear();
        this.objectPool.release((List<ObjectWithDistance>)this.objects);
        this.objects.clear();
    }

    void addObjectsFromChunks() {
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[i];
            if (chunkMap.ignore) continue;
            int midX = IsoChunkMap.chunkGridWidth / 2;
            int midY = IsoChunkMap.chunkGridWidth / 2;
            for (int dy = -3; dy <= 3; ++dy) {
                for (int dx = -3; dx <= 3; ++dx) {
                    IsoChunk chunk = chunkMap.getChunk(midX + dx, midY + dy);
                    if (chunk == null || chunk.objectEmitterData.objects.isEmpty()) continue;
                    chunk.objectEmitterData.objects.forEachEntry(ObjectAmbientEmitters::addObjectLambda);
                }
            }
        }
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

    boolean shouldPlay(IsoObject object, PerObjectLogic logic) {
        if (object == null) {
            return false;
        }
        if (object.getObjectIndex() == -1) {
            return false;
        }
        return logic.shouldPlaySound();
    }

    int getExistingSlot(IsoObject object) {
        for (int i = 0; i < this.slots.length; ++i) {
            if (this.slots[i].object != object) continue;
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
            slot.object = null;
        }
    }

    public void render() {
        if (!DebugOptions.instance.objectAmbientEmitterRender.getValue()) {
            return;
        }
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[IsoCamera.frameState.playerIndex];
        if (!chunkMap.ignore) {
            int midX = IsoChunkMap.chunkGridWidth / 2;
            int midY = IsoChunkMap.chunkGridWidth / 2;
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    IsoChunk chunk = chunkMap.getChunk(midX + dx, midY + dy);
                    if (chunk == null) continue;
                    Set<IsoObject> objects = chunk.objectEmitterData.objects.keySet();
                    for (IsoObject object : objects) {
                        if (object.square.z != PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) continue;
                        object.getFacingPosition(tempVector2);
                        float x = ObjectAmbientEmitters.tempVector2.x;
                        float y = ObjectAmbientEmitters.tempVector2.y;
                        float z = object.square.z;
                        LineDrawer.addLine(x - 0.45f, y - 0.45f, z, x + 0.45f, y + 0.45f, z, 0.5f, 0.5f, 0.5f, null, false);
                    }
                }
            }
        }
        for (int i = 0; i < this.slots.length; ++i) {
            Slot slot = this.slots[i];
            if (!slot.playing) continue;
            IsoObject object = slot.object;
            object.getFacingPosition(tempVector2);
            float x = ObjectAmbientEmitters.tempVector2.x;
            float y = ObjectAmbientEmitters.tempVector2.y;
            float z = object.square.z;
            LineDrawer.addLine(x - 0.45f, y - 0.45f, z, x + 0.45f, y + 0.45f, z, 0.0f, 0.0f, 1.0f, null, false);
        }
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        for (int i = 0; i < ObjectAmbientEmitters.instance.slots.length; ++i) {
            ObjectAmbientEmitters.instance.slots[i].stopPlaying();
            ObjectAmbientEmitters.instance.slots[i].object = null;
            ObjectAmbientEmitters.instance.slots[i].playing = false;
        }
    }

    static {
        tempVector2 = new Vector2();
    }

    static final class ObjectWithDistance {
        IsoObject object;
        PerObjectLogic logic;
        float distSq;

        ObjectWithDistance() {
        }
    }

    static final class Slot {
        IsoObject object;
        PerObjectLogic logic;
        BaseSoundEmitter emitter;
        long instance;
        boolean playing;

        Slot() {
        }

        void playSound(IsoObject object, PerObjectLogic logic) {
            if (this.emitter == null) {
                this.emitter = Core.soundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter();
            }
            object.getFacingPosition(tempVector2);
            this.emitter.setPos(tempVector2.getX(), tempVector2.getY(), object.square.z);
            this.object = object;
            this.logic = logic;
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
            this.logic.stopPlaying(this.emitter, this.instance);
            if (this.emitter.hasSustainPoints(this.instance)) {
                this.emitter.triggerCue(this.instance);
                this.instance = 0L;
                return;
            }
            this.emitter.stopAll();
            this.instance = 0L;
        }
    }

    static enum PowerPolicy {
        NotRequired,
        InteriorHydro,
        ExteriorOK,
        StreetLight;

    }

    public static abstract class PerObjectLogic {
        public IsoObject object;
        public float parameterValue1 = Float.NaN;

        public PerObjectLogic init(IsoObject object) {
            this.object = object;
            this.parameterValue1 = Float.NaN;
            return this;
        }

        void setParameterValue1(BaseSoundEmitter emitter, long instance, String paramName, float value) {
            if (value == this.parameterValue1) {
                return;
            }
            this.parameterValue1 = value;
            FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription = FMODManager.instance.getParameterDescription(paramName);
            emitter.setParameterValue(instance, parameterDescription, value);
        }

        void setParameterValue1(BaseSoundEmitter emitter, long instance, FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription, float value) {
            if (value == this.parameterValue1) {
                return;
            }
            this.parameterValue1 = value;
            emitter.setParameterValue(instance, parameterDescription, value);
        }

        public abstract boolean shouldPlaySound();

        public abstract String getSoundName();

        public abstract void startPlaying(BaseSoundEmitter var1, long var2);

        public abstract void stopPlaying(BaseSoundEmitter var1, long var2);

        public abstract void checkParameters(BaseSoundEmitter var1, long var2);
    }

    public static final class DoorLogic
    extends PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return AmbientStreamManager.instance.isParameterInsideTrue() && this.isReachableSquare();
        }

        @Override
        public String getSoundName() {
            return "DoorAmbiance";
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
            IsoDoor door1 = Type.tryCastTo(this.object, IsoDoor.class);
            IsoThumpable door2 = Type.tryCastTo(this.object, IsoThumpable.class);
            float value = 0.0f;
            if (door1 != null && door1.IsOpen()) {
                value = 1.0f;
            }
            if (door2 != null && door2.IsOpen()) {
                value = 1.0f;
            }
            this.setParameterValue1(emitter, instance, "DoorWindowOpen", value);
        }

        boolean isReachableSquare() {
            if (this.object.getSquare() == null) {
                return false;
            }
            IsoDoor door1 = Type.tryCastTo(this.object, IsoDoor.class);
            IsoThumpable door2 = Type.tryCastTo(this.object, IsoThumpable.class);
            boolean north = door1 == null ? door2.getNorth() : door1.getNorth();
            return ParameterInside.isAdjacentToReachableSquare(this.object.getSquare(), north);
        }
    }

    public static final class WindowLogic
    extends PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return AmbientStreamManager.instance.isParameterInsideTrue() && this.isReachableSquare();
        }

        @Override
        public String getSoundName() {
            return "WindowAmbiance";
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
            float value;
            IsoWindow window = Type.tryCastTo(this.object, IsoWindow.class);
            float f = value = window.IsOpen() || window.isDestroyed() ? 1.0f : 0.0f;
            if (value == 1.0f) {
                int numPlanks2;
                IsoBarricade barricade1 = window.getBarricadeOnSameSquare();
                IsoBarricade barricade2 = window.getBarricadeOnOppositeSquare();
                int numPlanks1 = barricade1 == null ? 0 : barricade1.getNumPlanks();
                int n = numPlanks2 = barricade2 == null ? 0 : barricade2.getNumPlanks();
                if (barricade1 != null && barricade1.isMetal() || barricade2 != null && barricade2.isMetal()) {
                    value = 0.0f;
                } else if (numPlanks1 > 0 || numPlanks2 > 0) {
                    value = 1.0f - (float)PZMath.max(numPlanks1, numPlanks2) / 4.0f;
                }
            }
            this.setParameterValue1(emitter, instance, "DoorWindowOpen", value);
        }

        boolean isReachableSquare() {
            return this.object.getSquare() != null && ParameterInside.isAdjacentToReachableSquare(this.object.getSquare(), ((IsoWindow)this.object).isNorth());
        }
    }

    public static final class AmbientSoundLogic
    extends PerObjectLogic {
        PowerPolicy powerPolicy = PowerPolicy.NotRequired;
        boolean hasGeneratorParameter;

        @Override
        public PerObjectLogic init(IsoObject object) {
            super.init(object);
            String soundName = this.getSoundName();
            this.powerPolicy = ObjectAmbientEmitters.getInstance().powerPolicyMap.getOrDefault(soundName, PowerPolicy.NotRequired);
            if (this.powerPolicy != PowerPolicy.NotRequired) {
                GameSound gameSound = GameSounds.getSound(soundName);
                this.hasGeneratorParameter = gameSound != null && gameSound.numClipsUsingParameter("Generator") > 0;
            }
            return this;
        }

        @Override
        public boolean shouldPlaySound() {
            boolean isPowered;
            IsoObject isoObject = this.object;
            if (isoObject instanceof IsoLightSwitch) {
                IsoLightSwitch lightSwitch = (IsoLightSwitch)isoObject;
                if (!lightSwitch.isActivated()) {
                    return false;
                }
                if (this.object.hasSpriteGrid() && (this.object.getSpriteGrid().getSpriteGridPosX(this.object.sprite) != 0 || this.object.getSpriteGrid().getSpriteGridPosY(this.object.sprite) != 0)) {
                    return false;
                }
            }
            if (this.powerPolicy == PowerPolicy.InteriorHydro) {
                boolean isPowered2;
                boolean bl = isPowered2 = this.object.square.haveElectricity() || this.object.hasGridPower() && (this.object.square.isInARoom() || this.object.square.associatedBuilding != null);
                if (!isPowered2) {
                    return false;
                }
            }
            if (this.powerPolicy == PowerPolicy.ExteriorOK) {
                boolean isPowered3;
                boolean bl = isPowered3 = this.object.square.haveElectricity() || this.object.hasGridPower();
                if (!isPowered3) {
                    return false;
                }
            }
            if (this.powerPolicy == PowerPolicy.StreetLight && (!(isPowered = this.object.hasGridPower()) || GameTime.getInstance().getNight() < 0.5f)) {
                return false;
            }
            if (this.powerPolicy != PowerPolicy.NotRequired && !this.object.hasGridPower() && !this.hasGeneratorParameter) {
                return false;
            }
            PropertyContainer props = this.object.getProperties();
            return props != null && props.has("AmbientSound");
        }

        @Override
        public String getSoundName() {
            return this.object.getProperties().get("AmbientSound");
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
            if (this.powerPolicy != PowerPolicy.NotRequired) {
                this.setParameterValue1(emitter, instance, "Generator", this.object.hasGridPower() ? 0.0f : 1.0f);
            }
        }
    }

    public static final class ChunkData {
        final THashMap<IsoObject, PerObjectLogic> objects = new THashMap();

        public ChunkData() {
            this.objects.setAutoCompactionFactor(0.0f);
        }

        public boolean hasObject(IsoObject object) {
            return this.objects.containsKey(object);
        }

        public void addObject(IsoObject object, PerObjectLogic logic) {
            if (this.objects.containsKey(object)) {
                return;
            }
            this.objects.put(object, logic);
        }

        public void removeObject(IsoObject object) {
            this.objects.remove(object);
        }

        public void reset() {
            this.objects.clear();
        }
    }

    public static final class WaterDripLogic
    extends PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return this.object.sprite != null && this.object.sprite.getProperties().has(IsoFlagType.waterPiped) && this.object.getFluidAmount() > 0.0f;
        }

        @Override
        public String getSoundName() {
            return "WaterDrip";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
            if (this.object.sprite != null && this.object.sprite.getProperties().has("SinkType")) {
                String sinkTypeStr;
                int sinkType = switch (sinkTypeStr = this.object.sprite.getProperties().get("SinkType")) {
                    case "Ceramic" -> 1;
                    case "Metal" -> 2;
                    default -> 0;
                };
                this.setParameterValue1(emitter, instance, "SinkType", (float)sinkType);
            }
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            this.parameterValue1 = Float.NaN;
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
        }
    }

    public static final class TreeAmbianceLogic
    extends PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return true;
        }

        @Override
        public String getSoundName() {
            return "TreeAmbiance";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
            if (emitter instanceof FMODSoundEmitter) {
                FMODSoundEmitter fmodSoundEmitter = (FMODSoundEmitter)emitter;
                fmodSoundEmitter.addParameter(new ParameterCurrentZone(this.object));
            }
            emitter.playAmbientLoopedImpl("BirdInTree");
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            emitter.stopOrTriggerSoundByName("BirdInTree");
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
        }
    }

    public static final class TentAmbianceLogic
    extends PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return this.object.sprite != null && this.object.sprite.getName() != null && this.object.sprite.getName().startsWith("camping_01") && (this.object.sprite.tileSheetIndex == 0 || this.object.sprite.tileSheetIndex == 3);
        }

        @Override
        public String getSoundName() {
            return "TentAmbiance";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
        }
    }

    public static final class FridgeHumLogic
    extends PerObjectLogic {
        static String[] soundNames = new String[]{"FridgeHumA", "FridgeHumB", "FridgeHumC", "FridgeHumD", "FridgeHumE", "FridgeHumF"};
        int choice = -1;

        @Override
        public PerObjectLogic init(IsoObject object) {
            super.init(object);
            this.choice = Rand.Next(6);
            return this;
        }

        @Override
        public boolean shouldPlaySound() {
            ItemContainer container = this.object.getContainerByEitherType("fridge", "freezer");
            return container != null && container.isPowered();
        }

        @Override
        public String getSoundName() {
            return soundNames[this.choice];
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
            this.setParameterValue1(emitter, instance, "Generator", this.object.hasGridPower() ? 0.0f : 1.0f);
        }
    }
}

