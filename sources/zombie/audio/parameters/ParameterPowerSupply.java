/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.IsoWorld;

public final class ParameterPowerSupply
extends FMODGlobalParameter {
    public ParameterPowerSupply() {
        super("Electricity");
    }

    @Override
    public float calculateCurrentValue() {
        return IsoWorld.instance != null && IsoWorld.instance.isHydroPowerOn() ? 1.0f : 0.0f;
    }
}

