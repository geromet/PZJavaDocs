/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterMoving
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterCharacterMoving(IsoGameCharacter character) {
        super("CharacterMoving");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.character.isPlayerMoving() ? 1.0f : 0.0f;
    }
}

