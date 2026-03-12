/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;

public final class ParameterPlayerHealth
extends FMODLocalParameter {
    private final IsoPlayer player;

    public ParameterPlayerHealth(IsoPlayer player) {
        super("PlayerHealth");
        this.player = player;
    }

    @Override
    public float calculateCurrentValue() {
        return PZMath.clamp(this.player.getHealth() / 100.0f, 0.0f, 1.0f);
    }
}

