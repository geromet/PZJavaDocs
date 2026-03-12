/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public interface IStateCharacter {
    public void changeState(State var1);

    public State getCurrentState();

    public boolean isCurrentState(State var1);

    default public boolean hasCurrentState() {
        return this.getCurrentState() != null;
    }

    default public boolean isCurrentStateAttacking() {
        return this.hasCurrentState() && this.getCurrentState().isAttacking(Type.tryCastTo(this, IsoGameCharacter.class));
    }

    default public boolean isCurrentStateMoving() {
        return this.hasCurrentState() && this.getCurrentState().isMoving(Type.tryCastTo(this, IsoGameCharacter.class));
    }

    default public boolean isDoingActionThatCanBeCancelled() {
        return this.hasCurrentState() && this.getCurrentState().isDoingActionThatCanBeCancelled();
    }

    default public boolean canBeHitByVehicle(BaseVehicle impactingVehicle) {
        return this.hasCurrentState() && this.getCurrentState().canBeHitByVehicle(Type.tryCastTo(this, IsoGameCharacter.class), impactingVehicle);
    }

    default public boolean causesDamageToVehicleWhenHit(BaseVehicle impactingVehicle) {
        return this.hasCurrentState() && this.getCurrentState().causesDamageToVehicleWhenHit(Type.tryCastTo(this, IsoGameCharacter.class), impactingVehicle);
    }

    default public boolean canSlowDownVehicleWhenHit(BaseVehicle impactingVehicle) {
        return this.hasCurrentState() && this.getCurrentState().canSlowDownVehicleWhenHit(Type.tryCastTo(this, IsoGameCharacter.class), impactingVehicle);
    }

    default public boolean canCurrentStateRagdoll() {
        return this.hasCurrentState() && this.getCurrentState().canRagdoll(Type.tryCastTo(this, IsoGameCharacter.class));
    }
}

