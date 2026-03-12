/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterSnowIntensity
extends FMODGlobalParameter {
    public ParameterSnowIntensity() {
        super("SnowIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        return ClimateManager.getInstance().getSnowIntensity();
    }
}

