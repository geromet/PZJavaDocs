/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterTemperature
extends FMODGlobalParameter {
    public ParameterTemperature() {
        super("Temperature");
    }

    @Override
    public float calculateCurrentValue() {
        return (float)((int)(ClimateManager.getInstance().getTemperature() * 100.0f)) / 100.0f;
    }
}

