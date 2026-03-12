/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.audio.FMODGlobalParameter;

public final class ParameterWaterSupply
extends FMODGlobalParameter {
    public ParameterWaterSupply() {
        super("Water");
    }

    @Override
    public float calculateCurrentValue() {
        return (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30)) < (float)SandboxOptions.instance.getWaterShutModifier() ? 1.0f : 0.0f;
    }
}

