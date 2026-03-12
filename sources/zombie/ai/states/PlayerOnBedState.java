/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.AttackType;
import zombie.ai.State;
import zombie.ai.states.IdleState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.util.StringUtils;

public final class PlayerOnBedState
extends State {
    private static final PlayerOnBedState INSTANCE = new PlayerOnBedState();

    public static PlayerOnBedState instance() {
        return INSTANCE;
    }

    private PlayerOnBedState() {
        super(false, false, false, true);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setCollidable(false);
        owner.setOnBed(true);
        if (!(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon)) {
            owner.setHideWeaponModel(true);
        }
        if (owner.getStateMachine().getPrevious() == IdleState.instance()) {
            owner.clearVariable("forceGetUp");
            owner.clearVariable("OnBedAnim");
            owner.clearVariable("OnBedStarted");
        }
        IsoDirections dir = IsoDirections.fromAngle(owner.getAnimAngleRadians());
        switch (dir) {
            case N: {
                owner.setY((float)((int)owner.getY()) + 0.3f);
                break;
            }
            case S: {
                owner.setY((float)((int)owner.getY()) + 0.7f);
                break;
            }
            case W: {
                owner.setX((float)((int)owner.getX()) + 0.3f);
                break;
            }
            case E: {
                owner.setX((float)((int)owner.getX()) + 0.7f);
            }
        }
        owner.blockTurning = true;
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedMovement(false)) {
            owner.StopAllActionQueue();
            owner.setVariable("forceGetUp", true);
        }
        if (owner.getVariableBoolean("OnBedStarted")) {
            // empty if block
        }
        player.setInitiateAttack(false);
        player.setAttackStarted(false);
        player.setAttackType(AttackType.NONE);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setHideWeaponModel(false);
        if (StringUtils.isNullOrEmpty(owner.getVariableString("HitReaction"))) {
            owner.clearVariable("forceGetUp");
            owner.clearVariable("OnBedAnim");
            owner.clearVariable("OnBedStarted");
            owner.setIgnoreMovement(false);
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("OnBedStarted")) {
            owner.setVariable("OnBedStarted", true);
            owner.setVariable("OnBedAnim", "Awake");
        }
    }
}

