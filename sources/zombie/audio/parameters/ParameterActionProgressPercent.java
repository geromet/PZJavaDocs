/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.IsoPlayer;

public final class ParameterActionProgressPercent
extends FMODLocalParameter {
    private final IsoPlayer character;
    private boolean wasAction;

    public ParameterActionProgressPercent(IsoPlayer character) {
        super("ActionProgressPercent");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.character.getCharacterActions().isEmpty()) {
            return this.checkWasAction();
        }
        BaseAction action = (BaseAction)this.character.getCharacterActions().get(0);
        if (action == null) {
            return this.checkWasAction();
        }
        if (!action.started) {
            return this.checkWasAction();
        }
        if (action.maxTime == 0) {
            return this.checkWasAction();
        }
        if (action.finished()) {
            return 100.0f;
        }
        this.wasAction = action.delta > 0.0f;
        return action.delta * 100.0f;
    }

    private float checkWasAction() {
        if (this.wasAction) {
            this.wasAction = false;
            return 100.0f;
        }
        return 0.0f;
    }
}

