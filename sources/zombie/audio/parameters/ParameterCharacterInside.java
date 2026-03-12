/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterInside
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterCharacterInside(IsoGameCharacter character) {
        super("CharacterInside");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.character.getVehicle() == null) {
            if (this.character.getCurrentBuilding() == null) {
                return 0.0f;
            }
            return 1.0f;
        }
        return 2.0f;
    }
}

