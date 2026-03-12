/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import fmod.fmod.FMODSoundEmitter;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.ai.State;
import zombie.ai.states.ZombieIdleState;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.properties.IsoObjectChange;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoMovingObject;
import zombie.network.GameServer;
import zombie.scripting.objects.SoundKey;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

public final class AttackVehicleState
extends State {
    private static final AttackVehicleState INSTANCE = new AttackVehicleState();
    private BaseSoundEmitter emitter;
    private final Vector3f worldPos = new Vector3f();

    public static AttackVehicleState instance() {
        return INSTANCE;
    }

    private AttackVehicleState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoMovingObject isoMovingObject = zombie.target;
        if (!(isoMovingObject instanceof IsoGameCharacter)) {
            return;
        }
        IsoGameCharacter target = (IsoGameCharacter)isoMovingObject;
        if (target.isDead()) {
            if (target.getLeaveBodyTimedown() > 3600.0f) {
                zombie.changeState(ZombieIdleState.instance());
                zombie.setTarget(null);
            } else {
                target.setLeaveBodyTimedown(target.getLeaveBodyTimedown() + GameTime.getInstance().getThirtyFPSMultiplier());
                if (!GameServer.server && !Core.soundDisabled && Rand.Next(Rand.AdjustForFramerate(15)) == 0) {
                    String soundName;
                    if (this.emitter == null) {
                        this.emitter = new FMODSoundEmitter();
                    }
                    if (!this.emitter.isPlaying(soundName = zombie.getDescriptor().getVoicePrefix() + "Eating")) {
                        this.emitter.playSound(soundName);
                    }
                }
            }
            zombie.timeSinceSeenFlesh = 0.0f;
            return;
        }
        BaseVehicle vehicle = target.getVehicle();
        if (vehicle == null || !vehicle.isCharacterAdjacentTo(owner)) {
            return;
        }
        Vector3f v = vehicle.chooseBestAttackPosition(target, owner, this.worldPos);
        if (v == null) {
            if (zombie.allowRepathDelay <= 0.0f) {
                owner.pathToCharacter(target);
                zombie.allowRepathDelay = 6.25f;
            }
            return;
        }
        if (v != null && (Math.abs(v.x - owner.getX()) > 0.1f || Math.abs(v.y - owner.getY()) > 0.1f)) {
            if (Math.abs(vehicle.getCurrentSpeedKmHour()) > 0.8f && (vehicle.isCharacterAdjacentTo(owner) || vehicle.DistToSquared(owner) < 16.0f)) {
                return;
            }
            if (zombie.allowRepathDelay <= 0.0f) {
                owner.pathToCharacter(target);
                zombie.allowRepathDelay = 6.25f;
            }
            return;
        }
        owner.faceThisObject(target);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoMovingObject isoMovingObject = zombie.target;
        if (!(isoMovingObject instanceof IsoGameCharacter)) {
            return;
        }
        IsoGameCharacter target = (IsoGameCharacter)isoMovingObject;
        IsoPlayer targetPlayer = Type.tryCastTo(target, IsoPlayer.class);
        BaseVehicle vehicle = target.getVehicle();
        if (vehicle == null) {
            return;
        }
        if (target.isDead()) {
            return;
        }
        if (event.eventName.equalsIgnoreCase("AttackCollisionCheck")) {
            target.getBodyDamage().AddRandomDamageFromZombie(zombie, null);
            target.getBodyDamage().Update();
            if (target.isDead()) {
                if (targetPlayer == null) {
                    if (target.isFemale()) {
                        zombie.getEmitter().playVocals("FemaleBeingEatenDeath");
                    } else {
                        zombie.getEmitter().playVocals("MaleBeingEatenDeath");
                    }
                } else {
                    targetPlayer.setPlayingDeathSound(true);
                    targetPlayer.playerVoiceSound("DeathEaten");
                }
                target.setHealth(0.0f);
            } else if (target.isAsleep()) {
                if (GameServer.server) {
                    target.sendObjectChange(IsoObjectChange.WAKE_UP);
                    target.setAsleep(false);
                } else {
                    target.forceAwake();
                }
            }
        } else if (event.eventName.equalsIgnoreCase("ThumpFrame")) {
            VehicleWindow window = null;
            VehiclePart part = null;
            int seat = vehicle.getSeat(target);
            String areaId = vehicle.getPassengerArea(seat);
            if (vehicle.isInArea(areaId, owner)) {
                VehiclePart door = vehicle.getPassengerDoor(seat);
                if (door != null && door.getDoor() != null && door.getInventoryItem() != null && !door.getDoor().isOpen()) {
                    window = door.findWindow();
                    if (window != null && !window.isHittable()) {
                        window = null;
                    }
                    if (window == null) {
                        part = door;
                    }
                }
            } else {
                part = vehicle.getNearestBodyworkPart(owner);
                if (part != null) {
                    window = part.getWindow();
                    if (window == null) {
                        window = part.findWindow();
                    }
                    if (window != null && !window.isHittable()) {
                        window = null;
                    }
                    if (window != null) {
                        part = null;
                    }
                }
            }
            if (window != null) {
                window.damage(zombie.strength);
                vehicle.setBloodIntensity(window.part.getId(), vehicle.getBloodIntensity(window.part.getId()) + 0.025f);
                if (!GameServer.server) {
                    zombie.setVehicleHitLocation(vehicle);
                    owner.getEmitter().playSound(SoundKey.ZOMBIE_THUMP_VEHICLE_WINDOW.toString(), vehicle);
                }
                zombie.setThumpFlag(3);
            } else {
                if (!GameServer.server) {
                    zombie.setVehicleHitLocation(vehicle);
                    owner.getEmitter().playSound(SoundKey.ZOMBIE_THUMP_VEHICLE.toString(), vehicle);
                }
                zombie.setThumpFlag(1);
            }
            vehicle.setAddThumpWorldSound(true);
            if (part != null && part.getWindow() == null && part.getCondition() > 0) {
                part.setCondition(part.getCondition() - zombie.strength);
                part.doInventoryItemStats((InventoryItem)part.getInventoryItem(), 0);
                vehicle.transmitPartCondition(part);
                vehicle.transmitPartItem(part);
            }
            if (target.isAsleep()) {
                if (GameServer.server) {
                    target.sendObjectChange(IsoObjectChange.WAKE_UP);
                    target.setAsleep(false);
                } else {
                    target.forceAwake();
                }
            }
        }
    }

    @Override
    public boolean isAttacking(IsoGameCharacter owner) {
        return true;
    }

    public boolean isPassengerExposed(IsoGameCharacter owner) {
        VehiclePart door;
        if (!(owner instanceof IsoZombie)) {
            return false;
        }
        IsoZombie zombie = (IsoZombie)owner;
        IsoMovingObject isoMovingObject = zombie.target;
        if (!(isoMovingObject instanceof IsoGameCharacter)) {
            return false;
        }
        IsoGameCharacter target = (IsoGameCharacter)isoMovingObject;
        BaseVehicle vehicle = target.getVehicle();
        if (vehicle == null) {
            return false;
        }
        boolean canAttackTarget = false;
        int seat = vehicle.getSeat(target);
        String areaId = vehicle.getPassengerArea(seat);
        if (vehicle.isInArea(areaId, owner) && (door = vehicle.getPassengerDoor(seat)) != null && door.getDoor() != null) {
            if (door.getInventoryItem() == null || door.getDoor().isOpen()) {
                canAttackTarget = true;
            } else {
                VehicleWindow window = door.findWindow();
                if (window != null) {
                    if (!window.isHittable()) {
                        window = null;
                    }
                    canAttackTarget = window == null;
                } else {
                    canAttackTarget = false;
                }
            }
        }
        return canAttackTarget;
    }
}

