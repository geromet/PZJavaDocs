/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;

public final class ParameterWeatherEvent
extends FMODGlobalParameter {
    private final Event event = Event.None;

    public ParameterWeatherEvent() {
        super("WeatherEvent");
    }

    @Override
    public float calculateCurrentValue() {
        return this.event.value;
    }

    public static enum Event {
        None(0),
        FreshSnow(1);

        final int value;

        private Event(int value) {
            this.value = value;
        }
    }
}

