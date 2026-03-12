/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterCharacterVoiceType
extends FMODLocalParameter {
    private final IsoPlayer player;

    public ParameterCharacterVoiceType(IsoPlayer player) {
        super("CharacterVoiceType");
        this.player = player;
    }

    @Override
    public float calculateCurrentValue() {
        return this.player.getVoiceType();
    }
}

