/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterElevation
extends FMODGlobalParameter {
    public ParameterCharacterElevation() {
        super("CharacterElevation");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return 0.0f;
        }
        return character.getZ();
    }
}

