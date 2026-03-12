/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;

public final class ParameterPlayerDistance
extends FMODLocalParameter {
    private final IsoZombie zombie;

    public ParameterPlayerDistance(IsoZombie zombie) {
        super("PlayerDistance");
        this.zombie = zombie;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.zombie.target == null) {
            return 1000.0f;
        }
        return (int)PZMath.ceil(this.zombie.DistToProper(this.zombie.target));
    }
}

