/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterFogIntensity
extends FMODGlobalParameter {
    public ParameterFogIntensity() {
        super("FogIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        return ClimateManager.getInstance().getFogIntensity();
    }
}

