/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;

public final class ZombieTurnAlerted
extends State {
    private static final ZombieTurnAlerted INSTANCE = new ZombieTurnAlerted();
    public static final State.Param<Float> TARGET_ANGLE = State.Param.ofFloat("target_angle", 0.0f);

    public static ZombieTurnAlerted instance() {
        return INSTANCE;
    }

    private ZombieTurnAlerted() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        float targetAngle = owner.get(TARGET_ANGLE).floatValue();
        owner.getAnimationPlayer().setTargetAngle(targetAngle);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.pathToSound(owner.getPathTargetX(), owner.getPathTargetY(), owner.getPathTargetZ());
        ((IsoZombie)owner).alerted = false;
    }

    public void setParams(IsoGameCharacter owner, float targetAngle) {
        owner.clear(this);
        owner.set(TARGET_ANGLE, Float.valueOf(targetAngle));
    }
}

