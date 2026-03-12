/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.scripting.objects.CharacterTrait;

public final class ParameterDeaf
extends FMODLocalParameter {
    private final IsoPlayer player;

    public ParameterDeaf(IsoPlayer player) {
        super("Deaf");
        this.player = player;
    }

    @Override
    public float calculateCurrentValue() {
        return this.player.hasTrait(CharacterTrait.DEAF) ? 1.0f : 0.0f;
    }
}

