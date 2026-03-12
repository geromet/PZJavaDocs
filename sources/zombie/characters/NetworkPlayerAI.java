/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.NetworkPlayerVariables;
import zombie.characters.Roles;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugOptions;
import zombie.debug.options.Multiplayer;
import zombie.input.GameKeyboard;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.fields.character.AnimalStateVariables;
import zombie.network.fields.character.Prediction;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.AnimalPacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;

public class NetworkPlayerAI
extends NetworkCharacterAI {
    public final UpdateLimit reliable = new UpdateLimit(2000L);
    IsoPlayer player;
    private final PathFindBehavior2 pfb2;
    private final UpdateLimit timerMax = new UpdateLimit(1000L);
    private final UpdateLimit timerMin = new UpdateLimit(200L);
    private boolean needUpdate;
    private final Vector2 tempo = new Vector2();
    private IsoGridSquare square;
    public boolean needToMovingUsingPathFinder;
    public boolean moving;
    public short lastBooleanVariables;
    private boolean pressedMovement;
    private boolean pressedCancelAction;
    private long accessLevelTimestamp;
    private byte shotID;
    boolean wasNonPvpZone;
    public boolean disconnected;
    public int hitsPerShot;
    private static final float ANIMAL_DETAILED_INFO_DIST = 4.0f;

    public NetworkPlayerAI(IsoGameCharacter character) {
        super(character);
        this.player = (IsoPlayer)character;
        this.pfb2 = this.player.getPathFindBehavior2();
        character.ulBeatenVehicle.Reset(200L);
        this.wasNonPvpZone = false;
        this.disconnected = false;
        this.player.role = Roles.animal;
        this.shotID = 0;
        this.hitsPerShot = 0;
    }

    @Override
    public IsoPlayer getRelatedPlayer() {
        IsoPlayer isoPlayer = this.player;
        if (isoPlayer instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)isoPlayer;
            IsoGameCharacter isoGameCharacter = isoAnimal.atkTarget;
            if (isoGameCharacter instanceof IsoPlayer) {
                IsoPlayer isoPlayer2 = (IsoPlayer)isoGameCharacter;
                return isoPlayer2;
            }
            return isoAnimal.getData().getAttachedPlayer();
        }
        return null;
    }

    @Override
    public Multiplayer.DebugFlagsOG.IsoGameCharacterOG getBooleanDebugOptions() {
        if (this.player instanceof IsoAnimal) {
            return DebugOptions.instance.multiplayer.debugFlags.animal;
        }
        if (this.player.isLocal()) {
            return DebugOptions.instance.multiplayer.debugFlags.localPlayer;
        }
        return DebugOptions.instance.multiplayer.debugFlags.remotePlayer;
    }

    public void needToUpdate() {
        this.needUpdate = true;
    }

    private void setStatic(Prediction prediction, IsoObject isoObject) {
        prediction.x = isoObject.getX();
        prediction.y = isoObject.getY();
        prediction.z = (byte)PZMath.fastfloor(isoObject.getZ());
        prediction.type = 0;
    }

    private void setMoving(Prediction prediction) {
        this.player.getDeferredMovement(this.tempo);
        float predictionInterval = 1000.0f;
        if (this.player.isAnimal()) {
            predictionInterval = this.player.getMovementSpeed();
        }
        this.tempo.x = this.player.getX() + this.tempo.x * 0.03f * predictionInterval;
        this.tempo.y = this.player.getY() + this.tempo.y * 0.03f * predictionInterval;
        if (PolygonalMap2.instance.lineClearCollide(this.player.getX(), this.player.getY(), this.tempo.x, this.tempo.y, PZMath.fastfloor(this.player.getZ()), null, false, true)) {
            prediction.x = this.player.getX();
            prediction.y = this.player.getY();
        } else {
            prediction.x = this.tempo.x;
            prediction.y = this.tempo.y;
        }
        prediction.z = this.player.getZ() == this.pfb2.getTargetZ() ? (byte)PZMath.fastfloor(this.pfb2.getTargetZ()) : (byte)PZMath.fastfloor(this.player.getZ());
        prediction.type = 1;
    }

    private void setPathFind(Prediction prediction) {
        prediction.x = this.pfb2.pathNextX;
        prediction.y = this.pfb2.pathNextY;
        prediction.z = (byte)PZMath.fastfloor(this.player.getZ());
        prediction.type = (byte)2;
    }

    public void set(AnimalPacket packet, UdpConnection receiver) {
        IsoPlayer isoPlayer = this.player;
        if (isoPlayer instanceof IsoAnimal) {
            IsoMovingObject isoMovingObject;
            IsoAnimal animal = (IsoAnimal)isoPlayer;
            packet.flags = 1;
            packet.variables = AnimalStateVariables.getVariables(animal);
            packet.realState = animal.realState;
            if (animal.isAlive() && animal.isMoving()) {
                this.setMoving(packet.prediction);
            } else {
                this.setStatic(packet.prediction, this.player);
                packet.prediction.direction = animal.getDirectionAngle();
            }
            packet.location = 0;
            if (animal.getItemID() != 0) {
                packet.location = (byte)3;
            }
            if (animal.getVehicle() != null) {
                packet.location = (byte)2;
                packet.vehicleId.set(animal.getVehicle());
            }
            if (animal.getHutch() != null) {
                packet.location = 1;
                packet.hutchNestBox = (byte)animal.nestBox;
                packet.hutchPosition = (byte)animal.getData().getHutchPosition();
                this.setStatic(packet.prediction, animal.getHutch());
            }
            packet.squareX = PZMath.fastfloor(animal.getX());
            packet.squareY = PZMath.fastfloor(animal.getY());
            packet.squareZ = (byte)PZMath.fastfloor(animal.getZ());
            packet.idleAction = animal.getVariableString("idleAction");
            if (!StringUtils.isNullOrEmpty(packet.idleAction)) {
                packet.flags = (short)(packet.flags | 2);
            }
            if (animal.getHook() != null) {
                packet.location = (byte)4;
                this.setStatic(packet.prediction, animal.getHook());
                return;
            }
            if (animal.isAlerted() && (isoMovingObject = animal.alertedChr) instanceof IsoPlayer) {
                IsoPlayer alerted = (IsoPlayer)isoMovingObject;
                packet.flags = (short)(packet.flags | 8);
                packet.alertedId = alerted.getOnlineID();
            }
            packet.type = animal.getAnimalType();
            packet.breed = animal.getBreed().getName();
            if (receiver != null) {
                boolean detailedInfoDistCheck;
                IsoPlayer playerReceiver = GameServer.getAnyPlayerFromConnection(receiver);
                boolean hasCheats = playerReceiver != null && playerReceiver.role.hasCapability(Capability.AnimalCheats);
                boolean bl = detailedInfoDistCheck = playerReceiver != null && IsoUtils.DistanceManhatten(playerReceiver.getX(), playerReceiver.getY(), animal.getX(), animal.getY()) <= 4.0f;
                if (hasCheats || detailedInfoDistCheck) {
                    int skillLvl = playerReceiver.getPerkLevel(PerkFactory.Perks.Husbandry);
                    packet.acceptance = (byte)animal.getPlayerAcceptance(playerReceiver);
                    packet.flags = (short)(packet.flags | 0x80);
                    if (hasCheats || skillLvl > 2) {
                        if (animal.getData().isFertilized()) {
                            packet.flags = (short)(packet.flags | 0x800);
                            packet.fertilizedTime = animal.getData().getFertilizedTime();
                        }
                        if (animal.getData().isPregnant()) {
                            packet.flags = (short)(packet.flags | 0x10);
                            packet.pregnantTime = animal.getData().getPregnancyTime();
                        }
                    }
                    if (animal.getData().canHaveMilk()) {
                        packet.flags = (short)(packet.flags | 0x20);
                        packet.milkQty = animal.getData().getMilkQuantity();
                        packet.lastTimeMilked = animal.getData().lastMilkTimer;
                    }
                    if (animal.getData().getWoolQuantity() > 0.0f) {
                        packet.woolQty = animal.getData().getWoolQuantity();
                        packet.flags = (short)(packet.flags | 0x40);
                    }
                    if (!animal.isFemale() && (hasCheats || skillLvl > 3)) {
                        packet.lastImpregnateTime = (byte)animal.getData().lastImpregnateTime;
                        packet.flags = (short)(packet.flags | 0x1000);
                    }
                    if (hasCheats) {
                        packet.flags = (short)(packet.flags | 0x400);
                        packet.maxMilkActual = animal.getData().getMaxMilkActual();
                    }
                }
            }
            if (!StringUtils.isNullOrEmpty(animal.getCustomName())) {
                packet.customName = animal.getCustomName();
                packet.flags = (short)(packet.flags | 0x100);
            }
            if (animal.getMother() != null) {
                packet.flags = (short)(packet.flags | 0x200);
                packet.mother.set(animal.getMother());
            }
            packet.age = animal.getData().getAge();
            packet.weight = animal.getWeight();
            packet.stress = (byte)animal.getStress();
            packet.health = (byte)(animal.getHealth() * 100.0f);
            packet.thirst = (byte)(animal.getThirst() * 100.0f);
            packet.hunger = (byte)(animal.getHunger() * 100.0f);
        }
    }

    public PacketTypes.PacketType set(PlayerPacket packet) {
        PacketTypes.PacketType result = null;
        boolean squareChanged = this.square != this.player.getCurrentSquare();
        this.square = this.player.getCurrentSquare();
        if (4 == ServerOptions.getInstance().antiCheatMovement.getValue()) {
            squareChanged = false;
        }
        if ((this.timerMin.Check() || this.needUpdate || squareChanged) && (this.player.isDead() || !this.player.isSeatedInVehicle())) {
            packet.disconnected = this.disconnected;
            packet.prediction.direction = this.player.getDirectionAngleRadians();
            if (this.pfb2.isMovingUsingPathFind() && this.pfb2.pathNextIsSet) {
                this.setPathFind(packet.prediction);
            } else if (this.player.isPlayerMoving()) {
                this.setStatic(packet.prediction, this.player);
            } else {
                this.setMoving(packet.prediction);
            }
            packet.booleanVariables = NetworkPlayerVariables.getBooleanVariables(this.player);
            boolean flagsChanged = this.lastBooleanVariables != packet.booleanVariables;
            this.lastBooleanVariables = packet.booleanVariables;
            boolean timerChanged = this.timerMax.Check();
            if (timerChanged || flagsChanged) {
                result = PacketTypes.PacketType.PlayerUpdateReliable;
            }
            if (squareChanged) {
                result = PacketTypes.PacketType.PlayerUpdateReliable;
            }
            if (this.needUpdate) {
                result = PacketTypes.PacketType.PlayerUpdateReliable;
                this.needUpdate = false;
            }
            if (PacketTypes.PacketType.PlayerUpdateReliable == result) {
                this.timerMax.Reset(600L);
            }
        }
        return result;
    }

    public void parse(AnimalPacket packet) {
        IsoPlayer isoPlayer = this.player;
        if (isoPlayer instanceof IsoAnimal) {
            IsoAnimal animal = (IsoAnimal)isoPlayer;
            this.targetX = PZMath.roundFromEdges(packet.prediction.x);
            this.targetY = PZMath.roundFromEdges(packet.prediction.y);
            this.targetZ = packet.prediction.z;
            this.predictionType = packet.prediction.type;
            this.direction.set((float)Math.cos(packet.prediction.direction), (float)Math.sin(packet.prediction.direction));
            this.distance.set(packet.prediction.x - animal.getX(), packet.prediction.y - animal.getY());
            if (this.usePathFind) {
                this.pfb2.pathToLocationF(packet.prediction.x, packet.prediction.y, packet.prediction.z);
                this.pfb2.walkingOnTheSpot.reset(animal.getX(), animal.getY());
            }
            AnimalStateVariables.setVariables(animal, packet.variables);
            animal.ensureOnTile();
            animal.setVariable("bPathfind", false);
            animal.realx = packet.prediction.position.x;
            animal.realy = packet.prediction.position.y;
            animal.realz = (byte)packet.prediction.position.z;
            float distToReal = IsoUtils.DistanceManhatten(animal.realx, animal.realy, this.getCharacter().getX(), this.getCharacter().getY());
            if (packet.isDead() && distToReal > 0.2f) {
                this.predictionType = 1;
                this.setNoCollision(5000L);
            }
            this.needToMovingUsingPathFinder = this.predictionType == 2;
            if (packet.location == 1) {
                animal.hutch = IsoHutch.getHutch(PZMath.fastfloor(packet.prediction.x), PZMath.fastfloor(packet.prediction.y), PZMath.fastfloor(packet.prediction.z));
                if (animal.hutch != null) {
                    IsoAnimal animalInNestBox;
                    if (animal.nestBox != -1) {
                        animal.hutch.getNestBox((Integer)Integer.valueOf((int)animal.nestBox)).animal = null;
                    }
                    if (animal.getData().getHutchPosition() != -1) {
                        animal.hutch.animalInside.put(animal.getData().getHutchPosition(), null);
                    }
                    if (packet.hutchPosition != -1) {
                        IsoAnimal animalInHutch = animal.hutch.animalInside.get(packet.hutchPosition);
                        if (animalInHutch != animal) {
                            animal.getData().setPreferredHutchPosition(packet.hutchPosition);
                            animal.getData().setHutchPosition(animal.getData().getPreferredHutchPosition());
                            animal.hutch.animalInside.put(animal.getData().getHutchPosition(), animal);
                        }
                    } else if (packet.hutchNestBox != -1 && (animalInNestBox = animal.hutch.getNestBox((Integer)Integer.valueOf((int)packet.hutchNestBox)).animal) != animal) {
                        animal.hutch.getNestBox((Integer)Integer.valueOf((int)packet.hutchNestBox)).animal = animal;
                        animal.nestBox = packet.hutchNestBox;
                    }
                    if (packet.isDead() && animal.hutch.deadBodiesInside.get(packet.hutchPosition) == null) {
                        IsoDeadBody deadAnimal = new IsoDeadBody(animal, false);
                        animal.hutch.deadBodiesInside.put(Integer.valueOf(packet.hutchPosition), deadAnimal);
                    }
                    animal.getHutch().tryRemoveAnimalFromWorld(animal);
                }
            }
            IsoHutch hutch = animal.getHutch();
            if (packet.location != 1 && hutch != null) {
                IsoGridSquare animalSq = IsoWorld.instance.currentCell.getGridSquare(hutch.savedX + hutch.getEnterSpotX(), hutch.savedY + hutch.getEnterSpotY(), hutch.savedZ);
                animal.getHutch().removeAnimal(animal);
                if (packet.location == 0 && !animal.isExistInTheWorld()) {
                    animal.addToWorld();
                    animal.setSquare(animalSq);
                    if (animalSq != null) {
                        animal.setX(animalSq.getX());
                        animal.setY(animalSq.getY());
                        animal.setZ(animalSq.getZ());
                    }
                    if (!animal.getCell().getObjectList().contains(animal) && !animal.getCell().getAddList().contains(animal)) {
                        animal.getCell().getAddList().add(animal);
                    }
                }
            }
            if ((packet.flags & 2) != 0) {
                animal.setVariable("idleAction", packet.idleAction);
            } else {
                animal.clearVariable("idleAction");
            }
            if (packet.location == 4) {
                animal.setOnHook(true);
                return;
            }
            if ((packet.flags & 0x20) != 0) {
                animal.getData().canHaveMilk = true;
                animal.getData().milkQty = packet.milkQty;
                animal.getData().lastMilkTimer = packet.lastTimeMilked;
            } else {
                animal.getData().canHaveMilk = false;
            }
            if ((packet.flags & 0x40) != 0) {
                animal.getData().setWoolQuantity(packet.woolQty, true);
            }
            animal.getData().setPregnant((packet.flags & 0x10) != 0);
            if ((packet.flags & 0x10) != 0) {
                animal.getData().setPregnancyTime(packet.pregnantTime);
            }
            animal.getData().setFertilized((packet.flags & 0x800) != 0);
            if ((packet.flags & 0x800) != 0) {
                animal.getData().setFertilizedTime(packet.fertilizedTime);
            }
            if ((packet.flags & 0x1000) != 0) {
                animal.getData().lastImpregnateTime = packet.lastImpregnateTime;
            }
            if ((packet.flags & 0x80) != 0) {
                animal.playerAcceptanceList.put(IsoPlayer.getInstance().getOnlineID(), Float.valueOf(packet.acceptance));
            }
            if ((packet.flags & 0x100) != 0 && !StringUtils.isNullOrEmpty(packet.customName)) {
                animal.setCustomName(packet.customName);
            }
            if ((packet.flags & 0x200) != 0 && packet.mother.isConsistent(null) && animal.getMother() == null) {
                animal.setMother(packet.mother.getAnimal());
                animal.motherId = packet.mother.getAnimal().getAnimalID();
            }
            if ((packet.flags & 0x400) != 0) {
                animal.getData().maxMilkActual = packet.maxMilkActual;
            }
            animal.getData().setAge(packet.age);
            animal.setHoursSurvived(packet.age * 24);
            animal.setWeight(packet.weight);
            animal.stressLevel = packet.stress;
            animal.getStats().set(CharacterStat.THIRST, (float)packet.thirst / 100.0f);
            animal.getStats().set(CharacterStat.HUNGER, (float)packet.hunger / 100.0f);
            if (!packet.isDead() || packet.location != 0) {
                animal.setHealth((float)packet.health / 100.0f);
            }
            animal.setIsAlerted(distToReal <= 0.2f && (packet.flags & 8) != 0);
            IsoMovingObject isoMovingObject = animal.alertedChr = animal.isAlerted() ? (IsoMovingObject)GameClient.IDToPlayerMap.get(packet.alertedId) : null;
            if (packet.location == 0 && distToReal > 10.0f && (this.player.getCurrentState() == null || !this.player.getCurrentState().isSyncOnSquare())) {
                this.player.teleportTo(animal.realx, animal.realy, (int)animal.realz);
            }
        }
    }

    public void parse(BaseVehicle vehicle) {
        this.player.setTimeSinceLastNetData(0);
        IsoGridSquare sq = vehicle.getCurrentSquare();
        if (sq != null) {
            if (this.player.isAlive() && !IsoWorld.instance.currentCell.getObjectList().contains(this.player)) {
                IsoWorld.instance.currentCell.getObjectList().add(this.player);
                this.player.setCurrent(sq);
            }
        } else if (IsoWorld.instance.currentCell.getObjectList().contains(this.player)) {
            IsoWorld.instance.currentCell.getObjectList().remove(this.player);
            this.player.removeFromWorld();
            this.player.removeFromSquare();
        }
    }

    public void parse(PlayerPacket packet) {
        this.targetX = PZMath.roundFromEdges(packet.prediction.x);
        this.targetY = PZMath.roundFromEdges(packet.prediction.y);
        this.targetZ = packet.prediction.z;
        this.predictionType = packet.prediction.type;
        this.needToMovingUsingPathFinder = 2 == packet.prediction.type;
        this.direction.set((float)Math.cos(packet.prediction.direction), (float)Math.sin(packet.prediction.direction));
        this.distance.set(packet.prediction.x - this.player.getX(), packet.prediction.y - this.player.getY());
        if (this.usePathFind) {
            this.pfb2.pathToLocationF(packet.prediction.x, packet.prediction.y, packet.prediction.z);
            this.pfb2.walkingOnTheSpot.reset(this.player.getX(), this.player.getY());
        }
        NetworkPlayerVariables.setBooleanVariables(this.player, packet.booleanVariables);
        this.player.setbSeenThisFrame(false);
        this.player.setbCouldBeSeenThisFrame(false);
        this.player.setTimeSinceLastNetData(0);
        this.player.ensureOnTile();
        this.player.realx = packet.prediction.position.x;
        this.player.realy = packet.prediction.position.y;
        this.player.realz = (byte)packet.prediction.position.z;
        if (GameServer.server) {
            this.player.setForwardDirection(this.direction);
        }
        packet.variables.apply(this.player);
        this.setPressedMovement(false);
        this.setPressedCancelAction(false);
    }

    public boolean isPressedMovement() {
        return this.pressedMovement;
    }

    public void setPressedMovement(boolean pressedMovement) {
        boolean send = !this.pressedMovement && pressedMovement;
        this.pressedMovement = pressedMovement;
    }

    public boolean isPressedCancelAction() {
        return this.pressedCancelAction;
    }

    public void setPressedCancelAction(boolean pressedCancelAction) {
        boolean send = !this.pressedCancelAction && pressedCancelAction;
        this.pressedCancelAction = pressedCancelAction;
    }

    public void setCheckAccessLevelDelay(long delay) {
        this.accessLevelTimestamp = System.currentTimeMillis() + delay;
    }

    public boolean doCheckAccessLevel() {
        if (this.accessLevelTimestamp == 0L) {
            return true;
        }
        if (System.currentTimeMillis() > this.accessLevelTimestamp) {
            this.accessLevelTimestamp = 0L;
            return true;
        }
        return false;
    }

    @Deprecated
    public void update() {
        if (!GameServer.server && GameClient.client) {
            if (!ServerOptions.getInstance().knockedDownAllowed.getValue() && this.player.isLocalPlayer() && this.player.getVehicle() == null && this.player.isUnderVehicleRadius(0.0f)) {
                this.player.setJustMoved(true);
                this.player.setMoveDelta(1.0f);
                this.player.setDir(IsoDirections.getRandom());
            }
            if (Core.debug && this.player == IsoPlayer.getInstance() && GameKeyboard.isKeyDown(29)) {
                if (GameKeyboard.isKeyPressed(44)) {
                    GameClient.SendCommandToServer(String.format("/createhorde2 -x %d -y %d -z %d -count %d -radius %d -crawler %s -isFallOnFront %s -isFakeDead %s -knockedDown %s -health %s -outfit %s ", PZMath.fastfloor(this.player.getX() + this.player.getForwardDirection().getX()), PZMath.fastfloor(this.player.getY() + this.player.getForwardDirection().getY()), PZMath.fastfloor(this.player.getZ()), 1, 0, "false", "false", "false", "false", "1", ""));
                }
                if (GameKeyboard.isKeyPressed(45)) {
                    GameClient.instance.sendClientCommandV(this.player, "animal", "add", "type", "bull", "breed", "angus", "x", PZMath.fastfloor(this.player.getX() + this.player.getForwardDirection().getX()), "y", PZMath.fastfloor(this.player.getY() + this.player.getForwardDirection().getY()), "z", PZMath.fastfloor(this.player.getZ()), "skeleton", false);
                }
                if (GameKeyboard.isKeyPressed(47)) {
                    GameClient.SendCommandToServer("/addvehicle Base.SportsCar");
                }
            }
        }
    }

    public boolean isDismantleAllowed() {
        return true;
    }

    public boolean isDisconnected() {
        return this.disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public boolean isReliable() {
        return this.reliable.Check();
    }

    @Override
    public void resetState() {
        super.resetState();
        this.player.setPerformingAnAction(false);
        this.player.overridePrimaryHandModel = null;
        this.player.overrideSecondaryHandModel = null;
        this.player.resetModelNextFrame();
    }

    @Override
    public void syncDamage() {
        UdpConnection connection;
        if (GameServer.server && (connection = GameServer.getConnectionFromPlayer(this.player)) != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
            this.player.updateSpeedModifiers();
            this.player.updateMovementRates();
            INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerInjuries, this.player);
            INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerDamage, this.player);
        }
    }

    @Override
    public void syncStats() {
        UdpConnection connection;
        if (GameServer.server && (connection = GameServer.getConnectionFromPlayer(this.player)) != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
            INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerStats, this.player);
            INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerEffects, this.player);
        }
    }

    @Override
    public void syncXp() {
        UdpConnection connection;
        if (GameServer.server && (connection = GameServer.getConnectionFromPlayer(this.player)) != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
            INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerXp, this.player);
        }
    }

    @Override
    public void syncHealth() {
        UdpConnection connection;
        if (GameServer.server && (connection = GameServer.getConnectionFromPlayer(this.player)) != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
            INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerHealth, this.player);
        }
    }

    @Override
    public void setAnimalPacket(UdpConnection receiver) {
        this.set(this.animalPacket, receiver);
    }

    public void setShotID(byte shotID) {
        this.shotID = shotID;
    }

    public byte getShotID() {
        return this.shotID;
    }

    public void onShot() {
        this.shotID = (byte)(this.shotID + 1);
    }

    public static class AnimalLocationFlags {
        public static final byte world = 0;
        public static final byte hutch = 1;
        public static final byte vehicle = 2;
        public static final byte container = 3;
        public static final byte hook = 4;
    }
}

