/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridOcclusionData;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.weather.ClimateManager;
import zombie.meta.Meta;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehiclePart;

public final class LightingJNI {
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();
    public static final int ROOM_SPAWN_DIST = 50;
    public static boolean init;
    public static final int[][] ForcedVis;
    private static final ArrayList<IsoGameCharacter.TorchInfo> torches;
    private static final ArrayList<IsoGameCharacter.TorchInfo> activeTorches;
    private static final ArrayList<IsoLightSource> JNILights;
    private static final int[] updateCounter;
    private static final int[] buildingsChangedCounter;
    private static boolean wasElecShut;
    private static boolean wasNight;
    private static final Vector2 tempVector2;
    private static final int MAX_PLAYERS = 256;
    private static final int MAX_LIGHTS_PER_PLAYER = 4;
    private static final int MAX_LIGHTS_PER_VEHICLE = 10;
    private static final ArrayList<InventoryItem> tempItems;
    private static final ArrayList<VisibleRoom>[] visibleRooms;
    private static long[] visibleRoomIDs;
    private static CompletableFuture<Void> checkLightsFuture;
    static float visionConeLerp;
    static float lumaInvertedLerp;
    static final ColorInfo lightTransmissionW;
    static final ColorInfo lightTransmissionN;
    static final ColorInfo lightTransmissionE;
    static final ColorInfo lightTransmissionS;

    public static void doInvalidateGlobalLights(int playerIndex) {
        ++Core.dirtyGlobalLightsCount;
    }

    public static void init() {
        if (init) {
            return;
        }
        String suffix = "";
        if ("1".equals(System.getProperty("zomboid.debuglibs.lighting"))) {
            DebugLog.log("***** Loading debug version of Lighting");
            suffix = "d";
        }
        try {
            if (System.getProperty("os.name").contains("OS X")) {
                System.loadLibrary("Lighting");
            } else if (System.getProperty("os.name").startsWith("Win")) {
                System.loadLibrary("Lighting64" + suffix);
            } else {
                System.loadLibrary("Lighting64");
            }
            for (int pn = 0; pn < 4; ++pn) {
                LightingJNI.updateCounter[pn] = -1;
            }
            LightingJNI.configure(0.005f);
            init = true;
        }
        catch (UnsatisfiedLinkError error) {
            error.printStackTrace();
            try {
                Thread.sleep(3000L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            System.exit(1);
        }
    }

    private static int getTorchIndexById(int id) {
        for (int i = 0; i < torches.size(); ++i) {
            IsoGameCharacter.TorchInfo torchInfo = torches.get(i);
            if (torchInfo.id != id) continue;
            return i;
        }
        return -1;
    }

    private static void checkTorch(IsoPlayer p, InventoryItem item, int id) {
        IsoGameCharacter.TorchInfo torchInfo;
        int torchIndex = LightingJNI.getTorchIndexById(id);
        if (torchIndex == -1) {
            torchInfo = IsoGameCharacter.TorchInfo.alloc();
            torches.add(torchInfo);
        } else {
            torchInfo = torches.get(torchIndex);
        }
        torchInfo.set(p, item);
        if (torchInfo.id == 0) {
            torchInfo.id = id;
        }
        LightingJNI.updateTorch(torchInfo.id, torchInfo.x, torchInfo.y, torchInfo.z + 32.0f, torchInfo.r, torchInfo.g, torchInfo.b, torchInfo.angleX, torchInfo.angleY, torchInfo.dist, torchInfo.strength, torchInfo.cone, torchInfo.dot, torchInfo.focusing);
        activeTorches.add(torchInfo);
    }

    private static int checkPlayerTorches(IsoPlayer player, int playerIndex) {
        ArrayList<InventoryItem> lightItems = tempItems;
        lightItems.clear();
        player.getActiveLightItems(lightItems);
        int numItems = Math.min(lightItems.size(), 4);
        for (int i = 0; i < numItems; ++i) {
            LightingJNI.checkTorch(player, lightItems.get(i), playerIndex * 4 + i + 1);
        }
        return numItems;
    }

    private static void clearPlayerTorches(int playerIndex, int numItems) {
        for (int i = numItems; i < 4; ++i) {
            int id = playerIndex * 4 + i + 1;
            int torchIndex = LightingJNI.getTorchIndexById(id);
            if (torchIndex == -1) continue;
            IsoGameCharacter.TorchInfo torchInfo = torches.get(torchIndex);
            LightingJNI.removeTorch(torchInfo.id);
            torchInfo.id = 0;
            IsoGameCharacter.TorchInfo.release(torchInfo);
            torches.remove(torchIndex);
            break;
        }
    }

    private static void checkTorch(VehiclePart part, int id) {
        VehicleLight light = part.getLight();
        if (light != null && light.getActive()) {
            IsoGameCharacter.TorchInfo torchInfo = null;
            for (int j = 0; j < torches.size(); ++j) {
                torchInfo = torches.get(j);
                if (torchInfo.id == id) break;
                torchInfo = null;
            }
            if (torchInfo == null) {
                torchInfo = IsoGameCharacter.TorchInfo.alloc();
                torches.add(torchInfo);
            }
            torchInfo.set(part);
            if (torchInfo.id == 0) {
                torchInfo.id = id;
            }
            LightingJNI.updateTorch(torchInfo.id, torchInfo.x, torchInfo.y, torchInfo.z + 32.0f, torchInfo.r, torchInfo.g, torchInfo.b, torchInfo.angleX, torchInfo.angleY, torchInfo.dist, torchInfo.strength, torchInfo.cone, torchInfo.dot, torchInfo.focusing);
            activeTorches.add(torchInfo);
        } else {
            for (int j = 0; j < torches.size(); ++j) {
                IsoGameCharacter.TorchInfo torchInfo = torches.get(j);
                if (torchInfo.id != id) continue;
                LightingJNI.removeTorch(torchInfo.id);
                torchInfo.id = 0;
                IsoGameCharacter.TorchInfo.release(torchInfo);
                torches.remove(j--);
            }
        }
    }

    private static void checkLights() {
        int i;
        int id;
        IsoLightSource lightSource;
        int i2;
        if (IsoWorld.instance.currentCell == null) {
            return;
        }
        if (GameClient.client) {
            IsoGenerator.updateSurroundingNow();
        }
        boolean bHydroPower = IsoWorld.instance.isHydroPowerOn();
        Stack<IsoLightSource> lights = IsoWorld.instance.currentCell.getLamppostPositions();
        for (i2 = 0; i2 < lights.size(); ++i2) {
            lightSource = (IsoLightSource)lights.get(i2);
            IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(lightSource.x, lightSource.y, lightSource.z);
            if (chunk != null && lightSource.chunk != null && lightSource.chunk != chunk) {
                lightSource.life = 0;
            }
            if (lightSource.life == 0 || !lightSource.isInBounds()) {
                lights.remove(i2);
                if (lightSource.id != 0) {
                    id = lightSource.id;
                    lightSource.id = 0;
                    JNILights.remove(lightSource);
                    LightingJNI.removeLight(id);
                    GameTime.instance.lightSourceUpdate = 100.0f;
                }
                --i2;
                continue;
            }
            if (lightSource.hydroPowered) {
                if (lightSource.switches.isEmpty()) {
                    assert (false);
                    hasPower = bHydroPower;
                    if (!hasPower) {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(lightSource.x, lightSource.y, lightSource.z);
                        boolean bl = hasPower = sq != null && sq.haveElectricity();
                    }
                    if (lightSource.active != hasPower) {
                        lightSource.active = hasPower;
                        GameTime.instance.lightSourceUpdate = 100.0f;
                    }
                } else {
                    IsoLightSwitch lightSwitch = lightSource.switches.get(0);
                    hasPower = lightSwitch.canSwitchLight();
                    if (lightSwitch.streetLight && (GameTime.getInstance().getNight() < 0.5f || !lightSwitch.hasGridPower())) {
                        hasPower = false;
                    }
                    if (lightSource.active && !hasPower) {
                        lightSource.active = false;
                        GameTime.instance.lightSourceUpdate = 100.0f;
                    } else if (!lightSource.active && hasPower && lightSwitch.isActivated()) {
                        lightSource.active = true;
                        GameTime.instance.lightSourceUpdate = 100.0f;
                    }
                }
            }
            float lightMult = 2.0f;
            if (lightSource.id == 0) {
                ++IsoLightSource.nextId;
                lightSource.id = lightSource.id;
                if (lightSource.life != -1) {
                    LightingJNI.addTempLight(lightSource.id, lightSource.x, lightSource.y, lightSource.z + 32, lightSource.radius, lightSource.r, lightSource.g, lightSource.b, (int)((float)(lightSource.life * PerformanceSettings.getLockFPS()) / 30.0f));
                    lights.remove(i2--);
                    continue;
                }
                lightSource.rJni = lightSource.r;
                lightSource.gJni = lightSource.g;
                lightSource.bJni = lightSource.b;
                lightSource.activeJni = lightSource.active;
                JNILights.add(lightSource);
                LightingJNI.addLight(lightSource.id, lightSource.x, lightSource.y, lightSource.z + 32, PZMath.min(lightSource.radius, 20), PZMath.clamp(lightSource.r * 2.0f, 0.0f, 1.0f), PZMath.clamp(lightSource.g * 2.0f, 0.0f, 1.0f), PZMath.clamp(lightSource.b * 2.0f, 0.0f, 1.0f), lightSource.localToBuilding == null ? -1 : lightSource.localToBuilding.id, lightSource.active);
                continue;
            }
            if (lightSource.r != lightSource.rJni || lightSource.g != lightSource.gJni || lightSource.b != lightSource.bJni) {
                lightSource.rJni = lightSource.r;
                lightSource.gJni = lightSource.g;
                lightSource.bJni = lightSource.b;
                LightingJNI.setLightColor(lightSource.id, PZMath.clamp(lightSource.r * 2.0f, 0.0f, 1.0f), PZMath.clamp(lightSource.g * 2.0f, 0.0f, 1.0f), PZMath.clamp(lightSource.b * 2.0f, 0.0f, 1.0f));
            }
            if (lightSource.activeJni == lightSource.active) continue;
            lightSource.activeJni = lightSource.active;
            LightingJNI.setLightActive(lightSource.id, lightSource.active);
        }
        for (i2 = 0; i2 < JNILights.size(); ++i2) {
            lightSource = JNILights.get(i2);
            if (lights.contains(lightSource)) continue;
            int id2 = lightSource.id;
            lightSource.id = 0;
            JNILights.remove(i2--);
            LightingJNI.removeLight(id2);
        }
        ArrayList<IsoRoomLight> roomLights = IsoWorld.instance.currentCell.roomLights;
        for (int i3 = 0; i3 < roomLights.size(); ++i3) {
            IsoRoomLight roomLight = roomLights.get(i3);
            if (roomLight.isInBounds()) {
                roomLight.active = roomLight.room.def.lightsActive;
                if (!bHydroPower) {
                    boolean switchHasPower = false;
                    for (int j = 0; !switchHasPower && j < roomLight.room.lightSwitches.size(); ++j) {
                        IsoLightSwitch lightSwitch = roomLight.room.lightSwitches.get(j);
                        if (lightSwitch.square == null || !lightSwitch.square.haveElectricity()) continue;
                        switchHasPower = true;
                    }
                    if (!switchHasPower && roomLight.active) {
                        roomLight.active = false;
                        if (roomLight.activeJni) {
                            IsoGridSquare.recalcLightTime = -1.0f;
                            if (PerformanceSettings.fboRenderChunk) {
                                ++Core.dirtyGlobalLightsCount;
                            }
                            GameTime.instance.lightSourceUpdate = 100.0f;
                        }
                    } else if (switchHasPower && roomLight.active && !roomLight.activeJni) {
                        IsoGridSquare.recalcLightTime = -1.0f;
                        if (PerformanceSettings.fboRenderChunk) {
                            ++Core.dirtyGlobalLightsCount;
                        }
                        GameTime.instance.lightSourceUpdate = 100.0f;
                    }
                }
                if (roomLight.id == 0) {
                    roomLight.id = 100000 + IsoRoomLight.nextId++;
                    LightingJNI.addRoomLight(roomLight.id, roomLight.room.building.def.id, roomLight.room.def.id, roomLight.x, roomLight.y, roomLight.z + 32, roomLight.width, roomLight.height, roomLight.active);
                    roomLight.activeJni = roomLight.active;
                    GameTime.instance.lightSourceUpdate = 100.0f;
                    continue;
                }
                if (roomLight.activeJni == roomLight.active) continue;
                LightingJNI.setRoomLightActive(roomLight.id, roomLight.active);
                roomLight.activeJni = roomLight.active;
                GameTime.instance.lightSourceUpdate = 100.0f;
                continue;
            }
            roomLights.remove(i3--);
            if (roomLight.id == 0) continue;
            id = roomLight.id;
            roomLight.id = 0;
            LightingJNI.removeRoomLight(id);
            GameTime.instance.lightSourceUpdate = 100.0f;
        }
        activeTorches.clear();
        if (GameClient.client) {
            ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
            for (int i4 = 0; i4 < players.size(); ++i4) {
                IsoPlayer p = players.get(i4);
                LightingJNI.checkPlayerTorches(p, p.onlineId + 1);
            }
        } else {
            for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
                IsoPlayer player = IsoPlayer.players[playerIndex];
                if (player == null || player.isDead() || player.getVehicle() != null && !player.isAiming()) {
                    LightingJNI.clearPlayerTorches(playerIndex, 0);
                    continue;
                }
                int numItems = LightingJNI.checkPlayerTorches(player, playerIndex);
                LightingJNI.clearPlayerTorches(playerIndex, numItems);
            }
        }
        for (i = 0; i < IsoWorld.instance.currentCell.getVehicles().size(); ++i) {
            BaseVehicle vehicle = IsoWorld.instance.currentCell.getVehicles().get(i);
            if (vehicle.vehicleId == -1) continue;
            for (int j = 0; j < vehicle.getLightCount(); ++j) {
                VehiclePart part = vehicle.getLightByIndex(j);
                LightingJNI.checkTorch(part, 1024 + vehicle.vehicleId * 10 + j);
            }
        }
        for (i = 0; i < torches.size(); ++i) {
            IsoGameCharacter.TorchInfo torchInfo = torches.get(i);
            if (activeTorches.contains(torchInfo)) continue;
            LightingJNI.removeTorch(torchInfo.id);
            torchInfo.id = 0;
            IsoGameCharacter.TorchInfo.release(torchInfo);
            torches.remove(i--);
        }
    }

    private static float coneToDegrees(float cone) {
        return 180.0f + cone * 180.0f;
    }

    private static float degreesToCone(float degrees) {
        return (degrees - 180.0f) / 180.0f;
    }

    public static float calculateVisionConeOld(IsoGameCharacter player) {
        float cone;
        if (player.getVehicle() == null) {
            cone = -0.2f;
            if ((cone -= player.getStats().get(CharacterStat.FATIGUE) - 0.6f) > -0.2f) {
                cone = -0.2f;
            }
            if (player.getStats().isAtMaximum(CharacterStat.FATIGUE)) {
                cone -= 0.2f;
            }
            if (player.getMoodles().getMoodleLevel(MoodleType.DRUNK) >= 2) {
                cone -= player.getStats().get(CharacterStat.INTOXICATION) * 0.002f;
            }
            if (player.getMoodles().getMoodleLevel(MoodleType.PANIC) == 4) {
                cone -= 0.2f;
            }
            if (player.isInARoom()) {
                ColorInfo light = player.square.getLightInfo(IsoPlayer.getPlayerIndex());
                float luma = light.r * 0.299f + light.g * 0.587f + light.b * 0.114f;
                cone -= 0.7f * (1.0f - luma);
            } else {
                cone -= 0.7f * (1.0f - ClimateManager.getInstance().getDayLightStrength());
            }
            if (cone < -0.9f) {
                cone = -0.9f;
            }
            if (player.hasTrait(CharacterTrait.EAGLE_EYED)) {
                cone += 0.2f * ClimateManager.getInstance().getDayLightStrength();
            }
            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 0.2f * (1.0f - ClimateManager.getInstance().getDayLightStrength());
            }
            DebugType.Lightning.debugln("cone 1 %f", Float.valueOf(cone));
            DebugType.Lightning.debugln("cone 2 %f", Float.valueOf(cone *= player.getWornItemsVisionModifier()));
            if (cone > 0.0f) {
                cone = 0.0f;
            } else if (cone < -0.9f) {
                cone = -0.9f;
            }
            DebugType.Lightning.debugln("cone 3 %f", Float.valueOf(cone));
        } else {
            cone = 0.8f - 3.0f * (1.0f - ClimateManager.getInstance().getDayLightStrength());
            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 0.2f * (1.0f - ClimateManager.getInstance().getDayLightStrength());
            }
            if ((cone *= player.getWornItemsVisionModifier()) > 1.0f) {
                cone = 1.0f;
            }
            if (player.getVehicle().getHeadlightsOn() && player.getVehicle().getHeadlightCanEmmitLight() && cone < -0.8f) {
                cone = -0.8f;
            } else if (cone < -0.95f) {
                cone = -0.95f;
            }
        }
        return cone;
    }

    public static float calculateVisionCone(IsoGameCharacter player) {
        float cone;
        float dayLightStrength = ClimateManager.getInstance().getDayLightStrength();
        float dayLightStrengthInverted = 1.0f - dayLightStrength;
        if (player.getVehicle() == null) {
            float lumaInverted;
            cone = 144.0f;
            if ((cone -= 72.0f * player.getStats().get(CharacterStat.FATIGUE)) > 144.0f) {
                cone = 144.0f;
            }
            if (player.getStats().isAtMaximum(CharacterStat.FATIGUE)) {
                cone -= 36.0f;
            }
            if (player.getMoodles().getMoodleLevel(MoodleType.DRUNK) >= 2) {
                cone -= 0.36f * player.getStats().get(CharacterStat.INTOXICATION);
            }
            if (player.getMoodles().getMoodleLevel(MoodleType.PANIC) == 4) {
                cone -= 36.0f;
            }
            if (!PZMath.equal(lumaInvertedLerp, lumaInverted = LightingJNI.getLumaInverted(player), 0.1f)) {
                lumaInvertedLerp = PZMath.lerp(lumaInvertedLerp, lumaInverted, 0.1f);
            }
            cone = player.isInARoom() ? (cone -= 126.0f * lumaInvertedLerp) : (cone -= 126.0f * PZMath.min(dayLightStrengthInverted, lumaInvertedLerp));
            cone = PZMath.clamp(cone, 18.0f, 180.0f);
            if (player.hasTrait(CharacterTrait.EAGLE_EYED)) {
                cone += 36.0f * dayLightStrength;
            }
            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 36.0f * dayLightStrengthInverted;
            }
        } else {
            cone = 324.0f;
            cone -= 540.0f * dayLightStrengthInverted;
            if (player.getStats().isAtMaximum(CharacterStat.FATIGUE)) {
                cone -= 36.0f;
            }
            if (player.getMoodles().getMoodleLevel(MoodleType.DRUNK) >= 2) {
                cone -= 0.36f * player.getStats().get(CharacterStat.INTOXICATION);
            }
            if (player.getMoodles().getMoodleLevel(MoodleType.PANIC) == 4) {
                cone -= 36.0f;
            }
            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 36.0f * dayLightStrengthInverted;
            }
            if (player.getVehicle().getHeadlightsOn() && player.getVehicle().getHeadlightCanEmmitLight() && cone < 36.0f) {
                cone = 36.0f;
            }
        }
        cone *= player.getWornItemsVisionMultiplier();
        cone = PZMath.clamp(cone, 18.0f, 360.0f);
        if (!PZMath.equal(visionConeLerp, cone, 0.033f)) {
            visionConeLerp = PZMath.lerp(visionConeLerp, cone, 0.033f);
        }
        return LightingJNI.degreesToCone(visionConeLerp);
    }

    private static float getLumaInverted(IsoGameCharacter player) {
        IsoGridSquare testSquare = player.getSquare() != null ? player.getSquare() : null;
        IsoGridSquare testSquare2 = testSquare != null && player.getDir() != null ? testSquare.getAdjacentSquare(player.getDir()) : null;
        ColorInfo light = testSquare != null ? testSquare.getLightInfo(IsoPlayer.getPlayerIndex()) : null;
        ColorInfo light2 = testSquare2 != null ? testSquare2.getLightInfo(IsoPlayer.getPlayerIndex()) : null;
        float lightValue = light != null ? light.r * 0.299f + light.g * 0.587f + light.b * 0.114f : 0.0f;
        float lightValue2 = light2 != null ? light2.r * 0.299f + light2.g * 0.587f + light2.b * 0.114f : 0.0f;
        return 1.0f - PZMath.max(lightValue, lightValue2);
    }

    public static float calculateRearZombieDistance(IsoGameCharacter player) {
        return player.getSeeNearbyCharacterDistance();
    }

    public static void updatePlayer(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player != null) {
            float tireddel = player.getStats().get(CharacterStat.FATIGUE) - 0.6f;
            if (tireddel < 0.0f) {
                tireddel = 0.0f;
            }
            tireddel *= 2.5f;
            float ndist = 2.0f;
            if (player.hasTrait(CharacterTrait.HARD_OF_HEARING)) {
                ndist -= 1.0f;
            }
            if (player.hasTrait(CharacterTrait.KEEN_HEARING)) {
                ndist += 3.0f;
            }
            ndist *= player.getWornItemsHearingMultiplier();
            float cone = LightingJNI.calculateVisionCone(player);
            Vector2 lookVector = player.getLookVector(tempVector2);
            BaseVehicle vehicle = player.getVehicle();
            if (vehicle != null && !player.isAiming() && !player.isLookingWhileInVehicle() && vehicle.isDriver(player) && vehicle.getCurrentSpeedKmHour() < -1.0f) {
                lookVector.rotate((float)Math.PI);
            }
            LightingJNI.playerSet(player.getX(), player.getY(), player.getZ() + 32.0f, lookVector.x, lookVector.y, false, player.reanimatedCorpse != null, player.isGhostMode(), player.hasTrait(CharacterTrait.SHORT_SIGHTED), tireddel, ndist, cone);
        }
    }

    public static void updateChunk(int playerIndex, IsoChunk mchunk) {
        LightingJNI.chunkBeginUpdate(mchunk.wx, mchunk.wy, mchunk.getMinLevel() + 32, mchunk.getMaxLevel() + 32);
        for (int z = mchunk.getMinLevel(); z <= mchunk.getMaxLevel(); ++z) {
            IsoChunkLevel chunkLevel = mchunk.getLevelData(z);
            if (!chunkLevel.lightCheck[playerIndex]) continue;
            chunkLevel.lightCheck[playerIndex] = false;
            LightingJNI.chunkLevelBeginUpdate(z + 32);
            for (int y = 0; y < 8; ++y) {
                for (int x = 0; x < 8; ++x) {
                    IsoGridSquare sq = chunkLevel.squares[x + y * 8];
                    if (sq != null) {
                        boolean isOpenAir;
                        LightingJNI.squareBeginUpdate(x, y, z + 32);
                        int visionMatrix = sq.visionMatrix;
                        if (sq.isSeen(playerIndex)) {
                            visionMatrix |= 1 << 27 + playerIndex;
                        }
                        if (isOpenAir = sq.getOpenAir()) {
                            sq.lightLevel = GameTime.getInstance().getSkyLightLevel();
                        }
                        boolean hasElevatedFloor = sq.has(IsoObjectType.stairsTN) || sq.has(IsoObjectType.stairsMN) || sq.has(IsoObjectType.stairsTW) || sq.has(IsoObjectType.stairsMW);
                        int visionUnblocked = 0;
                        for (int i = 0; i < DIRECTIONS.length; ++i) {
                            IsoDirections dir = DIRECTIONS[i];
                            if (sq.testVisionAdjacent(dir.dx(), dir.dy(), 0, true, false) == LosUtil.TestResults.Blocked) continue;
                            visionUnblocked |= 1 << i;
                        }
                        LightingJNI.squareSet(visionUnblocked, sq.testVisionAdjacent(0, 0, 1, true, false) != LosUtil.TestResults.Blocked, sq.testVisionAdjacent(0, 0, -1, true, false) != LosUtil.TestResults.Blocked, hasElevatedFloor, visionMatrix, sq.getRoom() == null ? -1L : sq.getBuilding().getDef().getID(), sq.getRoom() == null ? -1L : sq.getRoomID(), sq.lightLevel, isOpenAir);
                        float intensityW = Float.MAX_VALUE;
                        float intensityN = Float.MAX_VALUE;
                        float intensityE = Float.MAX_VALUE;
                        float intensityS = Float.MAX_VALUE;
                        float keepW = 0.0f;
                        float keepN = 0.0f;
                        float keepE = 0.0f;
                        float keepS = 0.0f;
                        ColorInfo ltW = lightTransmissionW.setRGB(0.0f);
                        ColorInfo ltN = lightTransmissionN.setRGB(0.0f);
                        ColorInfo ltE = lightTransmissionE.setRGB(0.0f);
                        ColorInfo ltS = lightTransmissionS.setRGB(0.0f);
                        for (int i = 0; i < sq.getSpecialObjects().size(); ++i) {
                            float curtainIntensity;
                            IsoObject object = sq.getSpecialObjects().get(i);
                            if (object instanceof IsoCurtain) {
                                String lightFilterMix;
                                IsoCurtain curtain = (IsoCurtain)object;
                                float curtainR = 0.0f;
                                float curtainG = 0.0f;
                                float curtainB = 0.0f;
                                String lightFilterR = curtain.getProperties().get("LightFilterR");
                                String lightFilterG = curtain.getProperties().get("LightFilterG");
                                String lightFilterB = curtain.getProperties().get("LightFilterB");
                                if (lightFilterR != null) {
                                    curtainR = (float)PZMath.clamp(PZMath.tryParseInt(lightFilterR, 0), 0, 255) / 255.0f;
                                }
                                if (lightFilterG != null) {
                                    curtainG = (float)PZMath.clamp(PZMath.tryParseInt(lightFilterG, 0), 0, 255) / 255.0f;
                                }
                                if (lightFilterB != null) {
                                    curtainB = (float)PZMath.clamp(PZMath.tryParseInt(lightFilterB, 0), 0, 255) / 255.0f;
                                }
                                curtainIntensity = 0.33f;
                                String lightFilterIntensity = curtain.getProperties().get("LightFilterIntensity");
                                if (lightFilterIntensity != null) {
                                    curtainIntensity = (float)PZMath.max(PZMath.tryParseInt(lightFilterIntensity, 0), 0) / 100.0f;
                                }
                                if ((lightFilterMix = curtain.getProperties().get("LightFilterMix")) != null) {
                                    curtainIntensity = (float)PZMath.max(PZMath.tryParseInt(lightFilterMix, 0), 0) / 100.0f;
                                }
                                int wnes = 0;
                                if (curtain.getType() == IsoObjectType.curtainW) {
                                    wnes |= 4;
                                    if (!curtain.IsOpen()) {
                                        intensityW = PZMath.min(intensityW, curtainIntensity);
                                        ltW.setRGB(curtainR, curtainG, curtainB);
                                    }
                                } else if (curtain.getType() == IsoObjectType.curtainN) {
                                    wnes |= 8;
                                    if (!curtain.IsOpen()) {
                                        intensityN = PZMath.min(intensityN, curtainIntensity);
                                        ltN.setRGB(curtainR, curtainG, curtainB);
                                    }
                                } else if (curtain.getType() == IsoObjectType.curtainE) {
                                    wnes |= 0x10;
                                    if (!curtain.IsOpen()) {
                                        intensityE = PZMath.min(intensityE, curtainIntensity);
                                        ltE.setRGB(curtainR, curtainG, curtainB);
                                    }
                                } else if (curtain.getType() == IsoObjectType.curtainS) {
                                    wnes |= 0x20;
                                    if (!curtain.IsOpen()) {
                                        intensityS = PZMath.min(intensityS, curtainIntensity);
                                        ltS.setRGB(curtainR, curtainG, curtainB);
                                    }
                                }
                                LightingJNI.squareAddCurtain(wnes, curtain.open);
                                continue;
                            }
                            if (object instanceof IsoDoor) {
                                boolean trans;
                                IsoDoor door = (IsoDoor)object;
                                boolean bl = trans = door.sprite != null && door.sprite.getProperties().has("doorTrans");
                                trans = door.open ? true : trans && (door.HasCurtains() == null || door.isCurtainOpen());
                                IsoBarricade barricade1 = door.getBarricadeOnSameSquare();
                                IsoBarricade barricade2 = door.getBarricadeOnOppositeSquare();
                                if (barricade1 != null && barricade1.isBlockVision()) {
                                    trans = false;
                                }
                                if (barricade2 != null && barricade2.isBlockVision()) {
                                    trans = false;
                                }
                                if (door.IsOpen() && IsoDoor.getGarageDoorIndex(door) != -1) {
                                    trans = true;
                                }
                                LightingJNI.squareAddDoor(door.north, door.open, trans);
                                if (!door.open && door.HasCurtains() == null) {
                                    if (door.getNorth()) {
                                        intensityN = PZMath.min(intensityN, 0.15f);
                                    } else {
                                        intensityW = PZMath.min(intensityW, 0.15f);
                                    }
                                }
                                if (door.open || door.HasCurtains() == null || door.isCurtainOpen()) continue;
                                float curtainR = 0.0f;
                                float curtainG = 0.0f;
                                float curtainB = 0.0f;
                                curtainIntensity = 0.33f;
                                if (door.getNorth()) {
                                    intensityN = PZMath.min(intensityN, 0.33f);
                                    ltN.setRGB(0.0f, 0.0f, 0.0f);
                                    continue;
                                }
                                intensityW = PZMath.min(intensityW, 0.33f);
                                ltW.setRGB(0.0f, 0.0f, 0.0f);
                                continue;
                            }
                            if (object instanceof IsoThumpable) {
                                IsoThumpable thump = (IsoThumpable)object;
                                boolean doorTrans = thump.getSprite().getProperties().has("doorTrans");
                                if (thump.isDoor() && thump.open) {
                                    doorTrans = true;
                                }
                                LightingJNI.squareAddThumpable(thump.north, thump.open, thump.isDoor(), doorTrans);
                                boolean opaque = false;
                                IsoBarricade barricade1 = thump.getBarricadeOnSameSquare();
                                IsoBarricade barricade2 = thump.getBarricadeOnOppositeSquare();
                                if (barricade1 != null) {
                                    opaque |= barricade1.isBlockVision();
                                    if (thump.getNorth()) {
                                        intensityN = PZMath.min(intensityN, barricade1.getLightTransmission());
                                    } else {
                                        intensityW = PZMath.min(intensityW, barricade1.getLightTransmission());
                                    }
                                }
                                if (barricade2 != null) {
                                    opaque |= barricade2.isBlockVision();
                                    if (thump.getNorth()) {
                                        intensityS = PZMath.min(intensityS, barricade2.getLightTransmission());
                                    } else {
                                        intensityE = PZMath.min(intensityE, barricade2.getLightTransmission());
                                    }
                                }
                                LightingJNI.squareAddWindow(thump.north, thump.open, opaque);
                                continue;
                            }
                            if (!(object instanceof IsoWindow)) continue;
                            IsoWindow window = (IsoWindow)object;
                            boolean opaque = false;
                            IsoBarricade barricade1 = window.getBarricadeOnSameSquare();
                            IsoBarricade barricade2 = window.getBarricadeOnOppositeSquare();
                            if (barricade1 != null) {
                                opaque |= barricade1.isBlockVision();
                                if (window.getNorth()) {
                                    intensityN = PZMath.min(intensityN, barricade1.getLightTransmission());
                                } else {
                                    intensityW = PZMath.min(intensityW, barricade1.getLightTransmission());
                                }
                            }
                            if (barricade2 != null) {
                                opaque |= barricade2.isBlockVision();
                                if (window.getNorth()) {
                                    intensityS = PZMath.min(intensityS, barricade2.getLightTransmission());
                                } else {
                                    intensityE = PZMath.min(intensityE, barricade2.getLightTransmission());
                                }
                            }
                            LightingJNI.squareAddWindow(window.isNorth(), window.IsOpen(), opaque);
                        }
                        if (intensityW == Float.MAX_VALUE) {
                            intensityW = 0.0f;
                        }
                        if (intensityN == Float.MAX_VALUE) {
                            intensityN = 0.0f;
                        }
                        if (intensityE == Float.MAX_VALUE) {
                            intensityE = 0.0f;
                        }
                        if (intensityS == Float.MAX_VALUE) {
                            intensityS = 0.0f;
                        }
                        LightingJNI.squareSetLightTransmission(ltW.r, ltW.g, ltW.b, intensityW, 0.0f, ltN.r, ltN.g, ltN.b, intensityN, 0.0f, ltE.r, ltE.g, ltE.b, intensityE, 0.0f, ltS.r, ltS.g, ltS.b, intensityS, 0.0f);
                        LightingJNI.squareEndUpdate();
                        continue;
                    }
                    LightingJNI.squareSetNull(x, y, z + 32);
                }
            }
            LightingJNI.chunkLevelEndUpdate();
        }
        LightingJNI.chunkEndUpdate();
    }

    public static void preUpdate() {
        if (DebugOptions.instance.threadLighting.getValue()) {
            checkLightsFuture = CompletableFuture.runAsync(LightingJNI::checkLights, PZForkJoinPool.commonPool());
        }
    }

    public static void update() {
        boolean bNight;
        GameProfiler.ProfileArea profileArea;
        if (IsoWorld.instance == null || IsoWorld.instance.currentCell == null) {
            return;
        }
        GameProfiler profiler = GameProfiler.getInstance();
        if (checkLightsFuture != null) {
            profileArea = profiler.profile("checkLights");
            try {
                checkLightsFuture.join();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
        profileArea = profiler.profile("checkLights");
        try {
            LightingJNI.checkLights();
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        checkLightsFuture = null;
        GameTime gameTime = GameTime.getInstance();
        RenderSettings renderSettings = RenderSettings.getInstance();
        boolean bElecShut = IsoWorld.instance.isHydroPowerOn();
        boolean bl = bNight = GameTime.getInstance().getNight() < 0.5f;
        if (bElecShut != wasElecShut || bNight != wasNight) {
            wasElecShut = bElecShut;
            wasNight = bNight;
            IsoGridSquare.recalcLightTime = -1.0f;
            if (PerformanceSettings.fboRenderChunk) {
                ++Core.dirtyGlobalLightsCount;
            }
            gameTime.lightSourceUpdate = 100.0f;
        }
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[playerIndex];
            if (cm == null || cm.ignore) continue;
            RenderSettings.PlayerRenderSettings plrSettings = renderSettings.getPlayerSettings(playerIndex);
            LightingJNI.stateBeginUpdate(playerIndex, cm.getWorldXMin(), cm.getWorldYMin(), IsoChunkMap.chunkGridWidth, IsoChunkMap.chunkGridWidth);
            LightingJNI.updatePlayer(playerIndex);
            LightingJNI.stateEndFrame(plrSettings.getRmod(), plrSettings.getGmod(), plrSettings.getBmod(), plrSettings.getAmbient(), plrSettings.getNight(), plrSettings.getViewDistance(), gameTime.getViewDistMax(), LosUtil.cachecleared[playerIndex], gameTime.lightSourceUpdate, GameTime.getInstance().getSkyLightLevel());
            if (LosUtil.cachecleared[playerIndex]) {
                LosUtil.cachecleared[playerIndex] = false;
                IsoWorld.instance.currentCell.invalidatePeekedRoom(playerIndex);
            }
            for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; ++cy) {
                for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; ++cx) {
                    IsoChunk mchunk = cm.getChunk(cx, cy);
                    if (mchunk == null || !mchunk.loaded) continue;
                    if (mchunk.lightCheck[playerIndex]) {
                        LightingJNI.updateChunk(playerIndex, mchunk);
                        mchunk.lightCheck[playerIndex] = false;
                    }
                    mchunk.lightingNeverDone[playerIndex] = !LightingJNI.chunkLightingDone(mchunk.wx, mchunk.wy);
                }
            }
            LightingJNI.stateEndUpdate();
            LightingJNI.updateCounter[playerIndex] = LightingJNI.stateUpdateCounter(playerIndex);
            if (!(gameTime.lightSourceUpdate > 0.0f) || IsoPlayer.players[playerIndex] == null) continue;
            IsoPlayer.players[playerIndex].dirtyRecalcGridStackTime = 20.0f;
        }
        try (GameProfiler.ProfileArea profileArea2 = profiler.profile("DeadBodyAtlas");){
            DeadBodyAtlas.instance.lightingUpdate(updateCounter[0], gameTime.lightSourceUpdate > 0.0f);
        }
        gameTime.lightSourceUpdate = 0.0f;
        LightingJNI.updateVisibleRooms();
        LightingJNI.checkChangedBuildings();
    }

    public static void getTorches(ArrayList<IsoGameCharacter.TorchInfo> out) {
        out.addAll(torches);
    }

    public static int getUpdateCounter(int playerIndex) {
        return updateCounter[playerIndex];
    }

    public static void stop() {
        torches.clear();
        JNILights.clear();
        LightingJNI.destroy();
        for (int i = 0; i < updateCounter.length; ++i) {
            LightingJNI.updateCounter[i] = -1;
        }
        wasElecShut = false;
        wasNight = false;
        IsoLightSource.nextId = 1;
        IsoRoomLight.nextId = 1;
    }

    public static native void configure(float var0);

    public static native void scrollLeft(int var0);

    public static native void scrollRight(int var0);

    public static native void scrollUp(int var0);

    public static native void scrollDown(int var0);

    public static native void stateBeginUpdate(int var0, int var1, int var2, int var3, int var4);

    public static native void stateEndFrame(float var0, float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, float var8, int var9);

    public static native void stateEndUpdate();

    public static native int stateUpdateCounter(int var0);

    public static native void teleport(int var0, int var1, int var2);

    public static native void DoLightingUpdateNew(long var0, boolean var2);

    public static native boolean WaitingForMain();

    public static native void playerSet(float var0, float var1, float var2, float var3, float var4, boolean var5, boolean var6, boolean var7, boolean var8, float var9, float var10, float var11);

    public static native boolean chunkLightingDone(int var0, int var1);

    public static native boolean getChunkDirty(int var0, int var1, int var2, int var3);

    public static native void chunkBeginUpdate(int var0, int var1, int var2, int var3);

    public static native void chunkEndUpdate();

    public static native void chunkLevelBeginUpdate(int var0);

    public static native void chunkLevelEndUpdate();

    public static native void squareSetNull(int var0, int var1, int var2);

    public static native void squareBeginUpdate(int var0, int var1, int var2);

    public static native void squareSet(int var0, boolean var1, boolean var2, boolean var3, int var4, long var5, long var7, int var9, boolean var10);

    public static native void squareSetLightTransmission(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, float var17, float var18, float var19);

    public static native void squareAddCurtain(int var0, boolean var1);

    public static native void squareAddDoor(boolean var0, boolean var1, boolean var2);

    public static native void squareAddThumpable(boolean var0, boolean var1, boolean var2, boolean var3);

    public static native void squareAddWindow(boolean var0, boolean var1, boolean var2);

    public static native void squareEndUpdate();

    public static native int getVertLight(int var0, int var1, int var2, int var3, int var4);

    public static native float getLightInfo(int var0, int var1, int var2, int var3, int var4);

    public static native float getDarkMulti(int var0, int var1, int var2, int var3);

    public static native float getTargetDarkMulti(int var0, int var1, int var2, int var3);

    public static native boolean getSeen(int var0, int var1, int var2, int var3);

    public static native boolean getCanSee(int var0, int var1, int var2, int var3);

    public static native boolean getCouldSee(int var0, int var1, int var2, int var3);

    public static native boolean getSquareLighting(int var0, int var1, int var2, int var3, int[] var4);

    public static native boolean getSquareDirty(int var0, int var1, int var2, int var3);

    public static native void addLight(int var0, int var1, int var2, int var3, int var4, float var5, float var6, float var7, int var8, boolean var9);

    public static native void addTempLight(int var0, int var1, int var2, int var3, int var4, float var5, float var6, float var7, int var8);

    public static native void removeLight(int var0);

    public static native void setLightActive(int var0, boolean var1);

    public static native void setLightColor(int var0, float var1, float var2, float var3);

    public static native void addRoomLight(int var0, long var1, long var3, int var5, int var6, int var7, int var8, int var9, boolean var10);

    public static native void removeRoomLight(int var0);

    public static native void setRoomLightActive(int var0, boolean var1);

    public static native void updateTorch(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, boolean var11, float var12, int var13);

    public static native void removeTorch(int var0);

    public static native int getVisibleRoomCount(int var0);

    public static native int getVisibleRooms(int var0, long[] var1);

    public static native void destroy();

    private static void updateVisibleRooms() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            int roomCount;
            if (buildingsChangedCounter[playerIndex] != -1) continue;
            VisibleRoom.releaseAll(visibleRooms[playerIndex]);
            visibleRooms[playerIndex].clear();
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
            if (chunkMap == null || chunkMap.ignore || (roomCount = LightingJNI.getVisibleRoomCount(playerIndex)) == 0) continue;
            if (visibleRoomIDs.length < roomCount) {
                visibleRoomIDs = new long[roomCount];
            }
            LightingJNI.getVisibleRooms(playerIndex, visibleRoomIDs);
            for (int i = 0; i < roomCount; ++i) {
                RoomDef roomDef;
                long roomID = visibleRoomIDs[i];
                int cellX = RoomID.getCellX(roomID);
                int cellY = RoomID.getCellY(roomID);
                IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(cellX, cellY);
                if (metaCell == null || (roomDef = metaCell.rooms.get(roomID)) == null) continue;
                VisibleRoom visibleRoom = VisibleRoom.alloc();
                visibleRoom.cellX = cellX;
                visibleRoom.cellY = cellY;
                visibleRoom.metaId = roomDef.metaId;
                visibleRooms[playerIndex].add(visibleRoom);
            }
        }
    }

    public static ArrayList<VisibleRoom> getVisibleRooms(int playerIndex) {
        return visibleRooms[playerIndex];
    }

    public static boolean isRoomVisible(int playerIndex, int cellX, int cellY, long metaID) {
        RoomDef roomDef;
        ArrayList<VisibleRoom> rooms = visibleRooms[playerIndex];
        for (int i = 0; i < rooms.size(); ++i) {
            VisibleRoom visibleRoom = rooms.get(i);
            if (!visibleRoom.equals(cellX, cellY, metaID)) continue;
            return true;
        }
        IsoPlayer player = IsoPlayer.players[playerIndex];
        return player != null && player.getCurrentRoomDef() != null && cellX == (roomDef = player.getCurrentRoomDef()).getBuilding().getCellX() && cellY == roomDef.getBuilding().getCellY() && metaID == roomDef.metaId;
    }

    public static void buildingsChanged() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            LightingJNI.buildingsChangedCounter[playerIndex] = updateCounter[playerIndex] + 2;
        }
        GameTime.instance.lightSourceUpdate = 100.0f;
        Arrays.fill(LosUtil.cachecleared, true);
        ++Core.dirtyGlobalLightsCount;
    }

    private static void checkChangedBuildings() {
        boolean bUpdated = false;
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            if (buildingsChangedCounter[playerIndex] == -1 || buildingsChangedCounter[playerIndex] > updateCounter[playerIndex]) continue;
            LightingJNI.buildingsChangedCounter[playerIndex] = -1;
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
            if (chunkMap == null || chunkMap.ignore) continue;
            bUpdated = true;
            for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; ++cy) {
                for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; ++cx) {
                    IsoChunk chunk = chunkMap.getChunk(cx, cy);
                    if (chunk == null) continue;
                    chunk.getRenderLevels(playerIndex).invalidateAll(18496L);
                    chunk.getCutawayData().invalidateAll();
                    chunk.checkLightingLater_OnePlayer_AllLevels(playerIndex);
                }
            }
        }
        if (bUpdated) {
            IsoGridOcclusionData.SquareChanged();
            FBORenderCutaways.getInstance().squareChanged(null);
        }
    }

    static {
        ForcedVis = new int[][]{{-1, 0, -1, -1, 0, -1, 1, -1, 1, 0, -2, -2, -1, -2, 0, -2, 1, -2, 2, -2}, {-1, 1, -1, 0, -1, -1, 0, -1, 1, -1, -2, 0, -2, -1, -2, -2, -1, -2, 0, -2}, {0, 1, -1, 1, -1, 0, -1, -1, 0, -1, -2, 2, -2, 1, -2, 0, -2, -1, -2, -2}, {1, 1, 0, 1, -1, 1, -1, 0, -1, -1, 0, 2, -1, 2, -2, 2, -2, 1, -2, 0}, {1, 0, 1, 1, 0, 1, -1, 1, -1, 0, 2, 2, 1, 2, 0, 2, -1, 2, -2, 2}, {-1, 1, 0, 1, 1, 1, 1, 0, 1, -1, 2, 0, 2, 1, 2, 2, 1, 2, 0, 2}, {0, 1, 1, 1, 1, 0, 1, -1, 0, -1, 2, -2, 2, -1, 2, 0, 2, 1, 2, 2}, {-1, -1, 0, -1, 1, -1, 1, 0, 1, 1, 0, -2, 1, -2, 2, -2, 2, -1, 2, 0}};
        torches = new ArrayList();
        activeTorches = new ArrayList();
        JNILights = new ArrayList();
        updateCounter = new int[4];
        buildingsChangedCounter = new int[4];
        tempVector2 = new Vector2();
        tempItems = new ArrayList();
        visibleRooms = new ArrayList[4];
        visibleRoomIDs = new long[32];
        for (int i = 0; i < 4; ++i) {
            LightingJNI.visibleRooms[i] = new ArrayList();
        }
        lightTransmissionW = new ColorInfo();
        lightTransmissionN = new ColorInfo();
        lightTransmissionE = new ColorInfo();
        lightTransmissionS = new ColorInfo();
    }

    public static final class VisibleRoom {
        public int cellX;
        public int cellY;
        public long metaId;
        private static final ObjectPool<VisibleRoom> pool = new ObjectPool<VisibleRoom>(VisibleRoom::new);

        VisibleRoom set(int cellX, int cellY, long metaID) {
            this.cellX = cellX;
            this.cellY = cellY;
            this.metaId = metaID;
            return this;
        }

        public VisibleRoom set(VisibleRoom other) {
            return this.set(other.cellX, other.cellY, other.metaId);
        }

        public boolean equals(Object rhs) {
            if (rhs instanceof VisibleRoom) {
                VisibleRoom other = (VisibleRoom)rhs;
                return this.equals(other.cellX, other.cellY, other.metaId);
            }
            return false;
        }

        boolean equals(int cellX, int cellY, long metaID) {
            return this.cellX == cellX && this.cellY == cellY && this.metaId == metaID;
        }

        public static VisibleRoom alloc() {
            return pool.alloc();
        }

        public void release() {
            pool.release(this);
        }

        public static void releaseAll(List<VisibleRoom> objs) {
            pool.releaseAll(objs);
        }
    }

    public static final class JNILighting
    implements IsoGridSquare.ILighting {
        private static final int RESULT_LIGHTS_PER_SQUARE = 6;
        private static final int[] lightInts = new int[49];
        private static final byte VIS_SEEN = 1;
        private static final byte VIS_CAN_SEE = 2;
        private static final byte VIS_COULD_SEE = 4;
        private final int playerIndex;
        private final IsoGridSquare square;
        private final ColorInfo lightInfo = new ColorInfo();
        private byte vis;
        private float cacheDarkMulti;
        private float cacheTargetDarkMulti;
        private final int[] cacheVertLight = new int[8];
        private int updateTick = -1;
        private int lightsCount;
        private IsoGridSquare.ResultLight[] lights;
        private int lightLevel;
        static int notDirty;
        static int dirty;

        public JNILighting(int playerIndex, IsoGridSquare square) {
            this.playerIndex = playerIndex;
            this.square = square;
            this.cacheDarkMulti = 0.0f;
            this.cacheTargetDarkMulti = 0.0f;
            for (int i = 0; i < 8; ++i) {
                this.cacheVertLight[i] = -16777216;
            }
        }

        @Override
        public int lightverts(int i) {
            return this.cacheVertLight[i];
        }

        @Override
        public float lampostTotalR() {
            return 0.0f;
        }

        @Override
        public float lampostTotalG() {
            return 0.0f;
        }

        @Override
        public float lampostTotalB() {
            return 0.0f;
        }

        @Override
        public boolean bSeen() {
            this.update();
            return (this.vis & 1) != 0;
        }

        @Override
        public boolean bCanSee() {
            this.update();
            return (this.vis & 2) != 0;
        }

        @Override
        public boolean bCouldSee() {
            this.update();
            return (this.vis & 4) != 0;
        }

        @Override
        public float darkMulti() {
            return this.cacheDarkMulti;
        }

        @Override
        public float targetDarkMulti() {
            return this.cacheTargetDarkMulti;
        }

        @Override
        public ColorInfo lightInfo() {
            this.update();
            return this.lightInfo;
        }

        @Override
        public void lightverts(int i, int value) {
            throw new IllegalStateException();
        }

        @Override
        public void lampostTotalR(float r) {
            throw new IllegalStateException();
        }

        @Override
        public void lampostTotalG(float g) {
            throw new IllegalStateException();
        }

        @Override
        public void lampostTotalB(float b) {
            throw new IllegalStateException();
        }

        @Override
        public void bSeen(boolean seen) {
            this.vis = seen ? (byte)(this.vis | 1) : (byte)(this.vis & 0xFFFFFFFE);
        }

        @Override
        public void bCanSee(boolean canSee) {
            throw new IllegalStateException();
        }

        @Override
        public void bCouldSee(boolean couldSee) {
            throw new IllegalStateException();
        }

        @Override
        public void darkMulti(float f) {
            throw new IllegalStateException();
        }

        @Override
        public void targetDarkMulti(float f) {
            throw new IllegalStateException();
        }

        @Override
        public int resultLightCount() {
            return this.lightsCount;
        }

        @Override
        public IsoGridSquare.ResultLight getResultLight(int index) {
            return this.lights[index];
        }

        @Override
        public void reset() {
            this.updateTick = -1;
            Arrays.fill(this.cacheVertLight, -16777216);
            this.vis = 0;
            this.cacheDarkMulti = 0.0f;
            this.cacheTargetDarkMulti = 0.0f;
            this.lightLevel = 0;
            this.lightInfo.set(0.0f, 0.0f, 0.0f, 1.0f);
        }

        private void update() {
            if (this.playerIndex != -1 && PerformanceSettings.fboRenderChunk) {
                this.updateFBORenderChunk();
                return;
            }
            if (this.playerIndex != -1 && updateCounter[this.playerIndex] == -1) {
                return;
            }
            if (this.playerIndex == -1 || this.updateTick != updateCounter[this.playerIndex] && LightingJNI.getSquareDirty(this.playerIndex, this.square.x, this.square.y, this.square.z + 32) && LightingJNI.getSquareLighting(this.playerIndex, this.square.x, this.square.y, this.square.z + 32, lightInts)) {
                int col;
                int i;
                IsoPlayer player = null;
                if (this.playerIndex != -1) {
                    player = IsoPlayer.players[this.playerIndex];
                }
                boolean wasSeen = (this.vis & 1) != 0;
                int kk = 0;
                this.vis = (byte)(lightInts[kk++] & 7);
                this.lightInfo.r = (float)(lightInts[kk] & 0xFF) / 255.0f;
                this.lightInfo.g = (float)(lightInts[kk] >> 8 & 0xFF) / 255.0f;
                this.lightInfo.b = (float)(lightInts[kk++] >> 16 & 0xFF) / 255.0f;
                this.lightInfo.a = 1.0f;
                this.cacheDarkMulti = (float)lightInts[kk++] / 100000.0f;
                this.cacheTargetDarkMulti = (float)lightInts[kk++] / 100000.0f;
                this.square.lightLevel = this.lightLevel = lightInts[kk++];
                float colorModUpper = 1.0f;
                float colorModLower = 1.0f;
                if (player != null) {
                    int dZ = this.square.z - PZMath.fastfloor(player.getZ());
                    if (dZ == -1) {
                        colorModUpper = 1.0f;
                        colorModLower = 0.85f;
                    } else if (dZ < -1) {
                        colorModUpper = 0.85f;
                        colorModLower = 0.85f;
                    }
                    if ((this.vis & 2) == 0 && (this.vis & 4) != 0) {
                        int px = PZMath.fastfloor(player.getX());
                        int py = PZMath.fastfloor(player.getY());
                        int dx = this.square.x - px;
                        int dy = this.square.y - py;
                        if (player.dir != null && Math.abs(dx) <= 2 && Math.abs(dy) <= 2) {
                            int[] fv = ForcedVis[player.dir.ordinal()];
                            for (int i2 = 0; i2 < fv.length; i2 += 2) {
                                if (dx != fv[i2] || dy != fv[i2 + 1]) continue;
                                this.vis = (byte)(this.vis | 2);
                                break;
                            }
                        }
                    }
                }
                for (i = 0; i < 4; ++i) {
                    col = lightInts[kk++];
                    float r = (float)(col & 0xFF) * colorModLower;
                    float g = (float)((col & 0xFF00) >> 8) * colorModLower;
                    float b = (float)((col & 0xFF0000) >> 16) * colorModLower;
                    this.cacheVertLight[i] = (int)r << 0 | (int)g << 8 | (int)b << 16 | 0xFF000000;
                }
                for (i = 4; i < 8; ++i) {
                    col = lightInts[kk++];
                    float r = (float)(col & 0xFF) * colorModUpper;
                    float g = (float)((col & 0xFF00) >> 8) * colorModUpper;
                    float b = (float)((col & 0xFF0000) >> 16) * colorModUpper;
                    this.cacheVertLight[i] = (int)r << 0 | (int)g << 8 | (int)b << 16 | 0xFF000000;
                }
                this.lightsCount = lightInts[kk++];
                for (i = 0; i < this.lightsCount; ++i) {
                    if (this.lights == null) {
                        this.lights = new IsoGridSquare.ResultLight[6];
                    }
                    if (this.lights[i] == null) {
                        this.lights[i] = new IsoGridSquare.ResultLight();
                    }
                    this.lights[i].id = lightInts[kk++];
                    this.lights[i].x = lightInts[kk++];
                    this.lights[i].y = lightInts[kk++];
                    this.lights[i].z = lightInts[kk++] - 32;
                    this.lights[i].radius = lightInts[kk++];
                    int rgb = lightInts[kk++];
                    this.lights[i].r = (float)(rgb & 0xFF) / 255.0f;
                    this.lights[i].g = (float)(rgb >> 8 & 0xFF) / 255.0f;
                    this.lights[i].b = (float)(rgb >> 16 & 0xFF) / 255.0f;
                    this.lights[i].flags = rgb >> 24 & 0xFF;
                }
                if (this.playerIndex == -1) {
                    return;
                }
                this.updateTick = updateCounter[this.playerIndex];
                if ((this.vis & 1) != 0) {
                    if (wasSeen && this.square.getRoom() != null && this.square.getRoom().def != null && !this.square.getRoom().def.explored) {
                        boolean bl = true;
                    }
                    this.square.checkRoomSeen(this.playerIndex);
                    if (!wasSeen) {
                        assert (!GameServer.server);
                        if (!GameClient.client) {
                            Meta.instance.dealWithSquareSeen(this.square);
                        }
                    }
                } else if (this.square.getRoom() != null && this.square.getRoom().def != null && !this.square.getRoom().def.explored && IsoUtils.DistanceToSquared(player.getX(), player.getY(), (float)this.square.x + 0.5f, (float)this.square.y + 0.5f) < 3.0f) {
                    this.square.checkRoomSeen(this.playerIndex);
                }
            }
        }

        private void updateFBORenderChunk() {
            if (this.square.chunk == null) {
                return;
            }
            if (updateCounter[this.playerIndex] == -1) {
                return;
            }
            if (this.updateTick == updateCounter[this.playerIndex]) {
                return;
            }
            if (!LightingJNI.getSquareDirty(this.playerIndex, this.square.x, this.square.y, this.square.z + 32)) {
                ++notDirty;
                return;
            }
            ++dirty;
            if (!LightingJNI.getSquareLighting(this.playerIndex, this.square.x, this.square.y, this.square.z + 32, lightInts)) {
                return;
            }
            IsoPlayer player = IsoPlayer.players[this.playerIndex];
            boolean wasCanSee = (this.vis & 2) != 0;
            boolean wasCouldSee = (this.vis & 4) != 0;
            boolean wasSeen = (this.vis & 1) != 0;
            int kk = 0;
            this.vis = (byte)(lightInts[kk++] & 7);
            int wasLightInfoR = (int)(this.lightInfo.r * 255.0f);
            int wasLightInfoG = (int)(this.lightInfo.g * 255.0f);
            int wasLightInfoB = (int)(this.lightInfo.b * 255.0f);
            int wasDarkMulti = (int)(this.cacheDarkMulti * 1000.0f);
            int wasDarkMultiTarget = (int)(this.cacheTargetDarkMulti * 1000.0f);
            int wasLightLevel = this.lightLevel;
            this.lightInfo.r = (float)(lightInts[kk] & 0xFF) / 255.0f;
            this.lightInfo.g = (float)(lightInts[kk] >> 8 & 0xFF) / 255.0f;
            this.lightInfo.b = (float)(lightInts[kk++] >> 16 & 0xFF) / 255.0f;
            this.lightInfo.a = 1.0f;
            this.cacheDarkMulti = (float)lightInts[kk++] / 100000.0f;
            this.cacheTargetDarkMulti = (float)lightInts[kk++] / 100000.0f;
            this.square.lightLevel = this.lightLevel = lightInts[kk++];
            if (player != null && (this.vis & 2) == 0 && (this.vis & 4) != 0) {
                int px = PZMath.fastfloor(player.getX());
                int py = PZMath.fastfloor(player.getY());
                int dx = this.square.x - px;
                int dy = this.square.y - py;
                if (player.dir != null && Math.abs(dx) <= 2 && Math.abs(dy) <= 2) {
                    int[] fv = ForcedVis[player.dir.ordinal()];
                    for (int i = 0; i < fv.length; i += 2) {
                        if (dx != fv[i] || dy != fv[i + 1]) continue;
                        this.vis = (byte)(this.vis | 2);
                        break;
                    }
                }
            }
            int wasVertLight1 = this.cacheVertLight[0];
            int wasVertLight2 = this.cacheVertLight[1];
            int wasVertLight3 = this.cacheVertLight[2];
            int wasVertLight4 = this.cacheVertLight[3];
            int wasVertLight5 = this.cacheVertLight[4];
            int wasVertLight6 = this.cacheVertLight[5];
            int wasVertLight7 = this.cacheVertLight[6];
            int wasVertLight8 = this.cacheVertLight[7];
            for (int i = 0; i < 8; ++i) {
                this.cacheVertLight[i] = lightInts[kk++];
            }
            int isLightInfoR = (int)(this.lightInfo.r * 255.0f);
            int isLightInfoG = (int)(this.lightInfo.g * 255.0f);
            int isLightInfoB = (int)(this.lightInfo.b * 255.0f);
            int isDarkMulti = (int)(this.cacheDarkMulti * 1000.0f);
            int isDarkMultiTarget = (int)(this.cacheTargetDarkMulti * 1000.0f);
            int isLightLevel = this.lightLevel;
            int isVertLight1 = this.cacheVertLight[0];
            int isVertLight2 = this.cacheVertLight[1];
            int isVertLight3 = this.cacheVertLight[2];
            int isVertLight4 = this.cacheVertLight[3];
            int isVertLight5 = this.cacheVertLight[4];
            int isVertLight6 = this.cacheVertLight[5];
            int isVertLight7 = this.cacheVertLight[6];
            int isVertLight8 = this.cacheVertLight[7];
            FBORenderLevels renderLevels = this.square.chunk.getRenderLevels(this.playerIndex);
            if (isDarkMulti != wasDarkMulti || isDarkMultiTarget != wasDarkMultiTarget || isLightLevel != wasLightLevel || isLightInfoR != wasLightInfoR || isLightInfoG != wasLightInfoG || isLightInfoB != wasLightInfoB) {
                if (!DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                    renderLevels.invalidateLevel(this.square.z, 32L);
                }
            } else if (!(isVertLight1 == wasVertLight1 && isVertLight2 == wasVertLight2 && isVertLight3 == wasVertLight3 && isVertLight4 == wasVertLight4 && isVertLight5 == wasVertLight5 && isVertLight6 == wasVertLight6 && isVertLight7 == wasVertLight7 && isVertLight8 == wasVertLight8 || DebugOptions.instance.fboRenderChunk.nolighting.getValue())) {
                renderLevels.invalidateLevel(this.square.z, 32L);
            }
            if (wasCouldSee != ((this.vis & 4) != 0)) {
                FBORenderCutaways.getInstance().squareChanged(this.square);
            }
            this.lightsCount = lightInts[kk++];
            for (int i = 0; i < this.lightsCount; ++i) {
                if (this.lights == null) {
                    this.lights = new IsoGridSquare.ResultLight[6];
                }
                if (this.lights[i] == null) {
                    this.lights[i] = new IsoGridSquare.ResultLight();
                }
                this.lights[i].id = lightInts[kk++];
                this.lights[i].x = lightInts[kk++];
                this.lights[i].y = lightInts[kk++];
                this.lights[i].z = lightInts[kk++] - 32;
                this.lights[i].radius = lightInts[kk++];
                int rgb = lightInts[kk++];
                this.lights[i].r = (float)(rgb & 0xFF) / 255.0f;
                this.lights[i].g = (float)(rgb >> 8 & 0xFF) / 255.0f;
                this.lights[i].b = (float)(rgb >> 16 & 0xFF) / 255.0f;
                this.lights[i].flags = rgb >> 24 & 0xFF;
            }
            if (this.updateTick == -1 && renderLevels.isOnScreen(this.square.z)) {
                renderLevels.invalidateLevel(this.square.z, 32L);
            }
            this.updateTick = updateCounter[this.playerIndex];
            if ((this.vis & 1) != 0) {
                this.square.checkRoomSeen(this.playerIndex);
                if (!wasSeen) {
                    assert (!GameServer.server);
                    if (!GameClient.client) {
                        Meta.instance.dealWithSquareSeen(this.square);
                    }
                }
            }
        }
    }
}

