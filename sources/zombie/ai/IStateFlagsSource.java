/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai;

import zombie.characters.IsoGameCharacter;
import zombie.vehicles.BaseVehicle;

public interface IStateFlagsSource {
    default public boolean isAttacking(IsoGameCharacter owner) {
        return false;
    }

    default public boolean isMoving(IsoGameCharacter owner) {
        return false;
    }

    default public boolean isDoingActionThatCanBeCancelled() {
        return false;
    }

    default public boolean canBeHitByVehicle(IsoGameCharacter owner, BaseVehicle impactingVehicle) {
        return true;
    }

    default public boolean causesDamageToVehicleWhenHit(IsoGameCharacter owner, BaseVehicle impactingVehicle) {
        return true;
    }

    default public boolean canSlowDownVehicleWhenHit(IsoGameCharacter owner, BaseVehicle impactingVehicle) {
        return true;
    }

    default public boolean canRagdoll(IsoGameCharacter owner) {
        return true;
    }

    default public boolean isSyncOnEnter() {
        return false;
    }

    default public boolean isSyncOnExit() {
        return false;
    }

    default public boolean isSyncOnSquare() {
        return false;
    }

    default public boolean isSyncInIdle() {
        return false;
    }
}

