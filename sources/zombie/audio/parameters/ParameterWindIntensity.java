/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterWindIntensity
extends FMODGlobalParameter {
    public ParameterWindIntensity() {
        super("WindIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        float value = ClimateManager.getInstance().getWindIntensity();
        return (float)((int)(value * 1000.0f)) / 1000.0f;
    }
}

