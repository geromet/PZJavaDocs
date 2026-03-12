/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterSeason
extends FMODGlobalParameter {
    public ParameterSeason() {
        super("Season");
    }

    @Override
    public float calculateCurrentValue() {
        ClimateManager.DayInfo currentDay = ClimateManager.getInstance().getCurrentDay();
        if (currentDay == null) {
            return 0.0f;
        }
        return switch (currentDay.season.getSeason()) {
            case 1 -> 0.0f;
            case 2, 3 -> 1.0f;
            case 4 -> 2.0f;
            case 5 -> 3.0f;
            default -> 1.0f;
        };
    }
}

