/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterLocalPlayer
extends FMODLocalParameter {
    private final IsoPlayer player;

    public ParameterLocalPlayer(IsoPlayer player) {
        super("LocalPlayer");
        this.player = player;
    }

    @Override
    public float calculateCurrentValue() {
        return this.player.isLocalPlayer() ? 1.0f : 0.0f;
    }
}

