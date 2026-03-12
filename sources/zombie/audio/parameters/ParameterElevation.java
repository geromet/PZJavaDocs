/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterElevation
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterElevation(IsoGameCharacter character) {
        super("Elevation");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.character.getZ();
    }
}

