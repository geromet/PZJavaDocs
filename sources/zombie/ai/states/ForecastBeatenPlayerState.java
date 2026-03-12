/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

public final class ForecastBeatenPlayerState
extends State {
    private static final ForecastBeatenPlayerState INSTANCE = new ForecastBeatenPlayerState();

    public static ForecastBeatenPlayerState instance() {
        return INSTANCE;
    }

    private ForecastBeatenPlayerState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setReanimateTimer(30.0f);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.getCurrentSquare() == null) {
            return;
        }
        owner.setReanimateTimer(owner.getReanimateTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
        if (owner.getReanimateTimer() <= 0.0f) {
            owner.setReanimateTimer(0.0f);
            owner.setVariable("bKnockedDown", true);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
    }
}

