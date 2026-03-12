/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterOnFire
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterCharacterOnFire(IsoGameCharacter character) {
        super("CharacterOnFire");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.character.isOnFire() ? 1.0f : 0.0f;
    }
}

