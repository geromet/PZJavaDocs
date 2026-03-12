/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.ai.states.FitnessState;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterExercising
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterExercising(IsoGameCharacter character) {
        super("Exercising");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (!this.character.isCurrentState(FitnessState.instance())) {
            return 0.0f;
        }
        if (!this.character.getVariableBoolean("ExerciseStarted")) {
            return 0.0f;
        }
        return 1.0f;
    }
}

