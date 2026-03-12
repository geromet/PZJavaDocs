/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterRainIntensity
extends FMODGlobalParameter {
    public ParameterRainIntensity() {
        super("RainIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        return ClimateManager.getInstance().getRainIntensity();
    }
}

