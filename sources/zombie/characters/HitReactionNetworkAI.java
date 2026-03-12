/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.GameTime;
import zombie.ai.states.PlayerFallDownState;
import zombie.ai.states.PlayerKnockedDown;
import zombie.ai.states.PlayerOnGroundState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.animals.AnimalOnGroundState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.debug.DebugType;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.pathfind.PolygonalMap2;
import zombie.vehicles.BaseVehicle;

public class HitReactionNetworkAI {
    private static final boolean isEnabled = true;
    private static final float G = 2.0f;
    private static final float DURATION = 600.0f;
    public final Vector2 startPosition = new Vector2();
    public final Vector2 finalPosition = new Vector2();
    public byte finalPositionZ;
    public final Vector2 startDirection = new Vector2();
    public final Vector2 finalDirection = new Vector2();
    private float startAngle;
    private float finalAngle;
    private final IsoGameCharacter character;
    private long startTime;

    public static boolean isEnabled(IsoGameCharacter character) {
        return !(character instanceof IsoPlayer);
    }

    public HitReactionNetworkAI(IsoGameCharacter character) {
        this.character = character;
        this.startTime = 0L;
    }

    public boolean isSetup() {
        return this.finalPosition.x != 0.0f && this.finalPosition.y != 0.0f;
    }

    public boolean isStarted() {
        return this.startTime > 0L;
    }

    public void start() {
        if (this.isSetup() && !this.isStarted()) {
            this.startTime = GameTime.getServerTimeMills();
            if (this.startPosition.x != this.character.getX() || this.startPosition.y != this.character.getY()) {
                DebugType.Multiplayer.warn("HitReaction start shifted");
            }
            DebugType.Damage.trace("id=%d: %s / %s => %s", this.character.getOnlineID(), this.getActualDescription(), this.getStartDescription(), this.getFinalDescription());
        }
    }

    public void reset() {
        if (this.startTime != 0L) {
            DebugType.Damage.noise("id=%d: %s / %s => %s", this.character.getOnlineID(), this.getActualDescription(), this.getStartDescription(), this.getFinalDescription());
        }
        this.startTime = 0L;
        this.setup(0.0f, 0.0f, (byte)0, Float.valueOf(0.0f));
    }

    public void setup(float dropPositionX, float dropPositionY, byte dropPositionZ, Float angle) {
        this.startPosition.set(this.character.getX(), this.character.getY());
        this.finalPosition.set(dropPositionX, dropPositionY);
        this.finalPositionZ = dropPositionZ;
        this.startDirection.set(this.character.getForwardDirection());
        this.startAngle = this.character.getAnimAngleRadians();
        Vector2 direction = new Vector2().set(this.finalPosition.x - this.startPosition.x, this.finalPosition.y - this.startPosition.y);
        if (angle == null) {
            direction.normalize();
            angle = Float.valueOf(direction.dot(this.character.getForwardDirection()));
            PZMath.lerp(this.finalDirection, direction, this.character.getForwardDirection(), Math.abs(angle.floatValue()));
            IsoMovingObject.getVectorFromDirection(this.finalDirection, IsoDirections.fromAngle(this.finalDirection));
        } else {
            this.finalDirection.setLengthAndDirection(angle.floatValue(), 1.0f);
        }
        this.finalAngle = angle.floatValue();
        if (this.isSetup()) {
            DebugType.Damage.noise("id=%d: %s / %s => %s", this.character.getOnlineID(), this.getActualDescription(), this.getStartDescription(), this.getFinalDescription());
        }
    }

    private void moveInternal(float posX, float posY, float dirX, float dirY) {
        this.character.setNextX(posX);
        this.character.setNextY(posY);
        if (!this.character.isAnimal()) {
            this.character.setDir(IsoDirections.fromAngle(dirX, dirY));
            this.character.setForwardDirection(dirX, dirY);
            this.character.getAnimationPlayer().setTargetAndCurrentDirection(this.character.getForwardDirectionX(), this.character.getForwardDirectionY());
        }
    }

    public void stop() {
        this.moveInternal(this.finalPosition.x, this.finalPosition.y, this.finalDirection.x, this.finalDirection.y);
        this.character.setForceX(this.finalPosition.x);
        this.character.setForceY(this.finalPosition.y);
        this.character.setCurrentSquareFromPosition(PZMath.fastfloor(this.finalPosition.x), PZMath.fastfloor(this.finalPosition.y), this.character.getZ());
        DebugType.Damage.trace("id=%d: %s / %s => %s", this.character.getOnlineID(), this.getActualDescription(), this.getStartDescription(), this.getFinalDescription());
    }

    public void move() {
        if (this.finalPositionZ != (byte)this.character.getZ()) {
            DebugType.Damage.trace("HitReaction interrupt id=%d: z-final:%d z-current=%d", this.character.getOnlineID(), this.finalPositionZ, (byte)this.character.getZ());
            this.reset();
            return;
        }
        float a = Math.min(1.0f, Math.max(0.0f, (float)(GameTime.getServerTimeMills() - this.startTime) / 600.0f));
        if (this.startPosition.x == this.finalPosition.x && this.startPosition.y == this.finalPosition.y) {
            a = 1.0f;
        }
        if (a < 1.0f) {
            a = (PZMath.gain(a * 0.5f + 0.5f, 2.0f) - 0.5f) * 2.0f;
            this.moveInternal(PZMath.lerp(this.startPosition.x, this.finalPosition.x, a), PZMath.lerp(this.startPosition.y, this.finalPosition.y, a), PZMath.lerp(this.startDirection.x, this.finalDirection.x, a), PZMath.lerp(this.startDirection.y, this.finalDirection.y, a));
        } else {
            this.stop();
            this.reset();
        }
    }

    public boolean isDoSkipMovement() {
        if (this.character instanceof IsoZombie) {
            return this.character.isCurrentState(ZombieFallDownState.instance()) || this.character.isCurrentState(ZombieOnGroundState.instance());
        }
        if (this.character instanceof IsoAnimal) {
            return this.character.isCurrentState(AnimalOnGroundState.instance());
        }
        if (this.character instanceof IsoPlayer) {
            return this.character.isCurrentState(PlayerFallDownState.instance()) || this.character.isCurrentState(PlayerKnockedDown.instance()) || this.character.isCurrentState(PlayerOnGroundState.instance());
        }
        return false;
    }

    private String getStartDescription() {
        return String.format("start=[ pos=( %f ; %f ) dir=( %f ; %f ) angle=%f ]", Float.valueOf(this.startPosition.x), Float.valueOf(this.startPosition.y), Float.valueOf(this.startDirection.x), Float.valueOf(this.startDirection.y), Float.valueOf(this.startAngle));
    }

    private String getFinalDescription() {
        return String.format("final=[ pos=( %f ; %f ) dir=( %f ; %f ) angle=%f ]", Float.valueOf(this.finalPosition.x), Float.valueOf(this.finalPosition.y), Float.valueOf(this.finalDirection.x), Float.valueOf(this.finalDirection.y), Float.valueOf(this.finalAngle));
    }

    private String getActualDescription() {
        return String.format("actual=[ pos=( %f ; %f ) dir=( %f ; %f ) angle=%f ]", Float.valueOf(this.character.getX()), Float.valueOf(this.character.getY()), Float.valueOf(this.character.getForwardDirectionX()), Float.valueOf(this.character.getForwardDirectionY()), Float.valueOf(this.character.getAnimAngleRadians()));
    }

    public String getDescription() {
        return String.format("start=%d | (x=%f,y=%f;a=%f;l=%f)", this.startTime, Float.valueOf(this.finalPosition.x), Float.valueOf(this.finalPosition.y), Float.valueOf(this.finalAngle), Float.valueOf(IsoUtils.DistanceTo(this.startPosition.x, this.startPosition.y, this.finalPosition.x, this.finalPosition.y)));
    }

    public static void CalcHitReactionWeapon(IsoGameCharacter wielder, IsoGameCharacter target, HandWeapon weapon) {
        if (!GameClient.client) {
            return;
        }
        if (!wielder.isLocal()) {
            return;
        }
        HitReactionNetworkAI hitReaction = target.getHitReactionNetworkAI();
        if (target.isOnFloor()) {
            hitReaction.setup(target.getX(), target.getY(), (byte)target.getZ(), Float.valueOf(target.getAnimAngleRadians()));
        } else {
            Vector2 hitPos = new Vector2();
            Float angle = target.calcHitDir(wielder, weapon, hitPos);
            if (target instanceof IsoAnimal) {
                hitPos.x += target.getX();
                hitPos.y += target.getY();
                hitPos.setLength(0.1f);
            } else if (target instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)target;
                hitPos.x = (hitPos.x + target.getX() + isoPlayer.networkAi.targetX) * 0.5f;
                hitPos.y = (hitPos.y + target.getY() + isoPlayer.networkAi.targetY) * 0.5f;
            } else {
                hitPos.x += target.getX();
                hitPos.y += target.getY();
            }
            hitPos.x = PZMath.roundFromEdges(hitPos.x);
            hitPos.y = PZMath.roundFromEdges(hitPos.y);
            if (PolygonalMap2.instance.lineClearCollide(target.getX(), target.getY(), hitPos.x, hitPos.y, PZMath.fastfloor(target.getZ()), null, false, true)) {
                hitPos.x = target.getX();
                hitPos.y = target.getY();
            }
            hitReaction.setup(hitPos.x, hitPos.y, (byte)target.getZ(), angle);
        }
        if (hitReaction.isSetup()) {
            hitReaction.start();
        }
    }

    public static void CalcHitReactionVehicle(IsoGameCharacter target, BaseVehicle vehicle) {
        HitReactionNetworkAI hitReaction = target.getHitReactionNetworkAI();
        if (!hitReaction.isStarted()) {
            if (target.isOnFloor()) {
                hitReaction.setup(target.getX(), target.getY(), (byte)target.getZ(), Float.valueOf(target.getAnimAngleRadians()));
            } else {
                Vector2 hitPos = new Vector2();
                target.calcHitDir(hitPos);
                if (target instanceof IsoPlayer) {
                    IsoPlayer isoPlayer = (IsoPlayer)target;
                    hitPos.x = (hitPos.x + target.getX() + isoPlayer.networkAi.targetX) * 0.5f;
                    hitPos.y = (hitPos.y + target.getY() + isoPlayer.networkAi.targetY) * 0.5f;
                } else {
                    hitPos.x += target.getX();
                    hitPos.y += target.getY();
                }
                hitPos.x = PZMath.roundFromEdges(hitPos.x);
                hitPos.y = PZMath.roundFromEdges(hitPos.y);
                if (PolygonalMap2.instance.lineClearCollide(target.getX(), target.getY(), hitPos.x, hitPos.y, PZMath.fastfloor(target.getZ()), vehicle, false, true)) {
                    hitPos.x = target.getX();
                    hitPos.y = target.getY();
                }
                hitReaction.setup(hitPos.x, hitPos.y, (byte)target.getZ(), null);
            }
        }
        if (hitReaction.isSetup()) {
            hitReaction.start();
        }
    }

    public void process(float dropPositionX, float dropPositionY, float dropPositionZ, float dropDirection) {
        this.setup(dropPositionX, dropPositionY, (byte)dropPositionZ, Float.valueOf(dropDirection));
        this.start();
        if (GameServer.server) {
            this.stop();
            this.reset();
        }
    }
}

