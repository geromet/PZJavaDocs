/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import zombie.GameTime;
import zombie.core.random.Rand;

public final class OnceEvery {
    private long initialDelayMillis;
    private final long triggerIntervalMillis;
    private static float milliFraction;
    private static long currentMillis;
    private static long prevMillis;

    public OnceEvery(float seconds) {
        this(seconds, false);
    }

    public OnceEvery(float seconds, boolean randomStart) {
        this.triggerIntervalMillis = (long)(seconds * 1000.0f);
        this.initialDelayMillis = 0L;
        if (randomStart) {
            this.initialDelayMillis = Rand.Next(this.triggerIntervalMillis);
        }
    }

    public static long getElapsedMillis() {
        return currentMillis;
    }

    public boolean Check() {
        if (currentMillis < this.initialDelayMillis) {
            return false;
        }
        if (this.triggerIntervalMillis == 0L) {
            return true;
        }
        long a = (prevMillis - this.initialDelayMillis) % this.triggerIntervalMillis;
        long b = (currentMillis - this.initialDelayMillis) % this.triggerIntervalMillis;
        if (a > b) {
            return true;
        }
        long deltaTmillis = currentMillis - prevMillis;
        return this.triggerIntervalMillis < deltaTmillis;
    }

    public static void update() {
        long prevMillis = currentMillis;
        float prevMilliFraction = milliFraction;
        float deltaTseconds = GameTime.instance.getTimeDelta();
        float deltaTmillis = deltaTseconds * 1000.0f + prevMilliFraction;
        long deltaTmillisWhole = (long)deltaTmillis;
        float deltaTmilliFraction = deltaTmillis - (float)deltaTmillisWhole;
        long currentMillis = prevMillis + deltaTmillisWhole;
        OnceEvery.prevMillis = prevMillis;
        OnceEvery.currentMillis = currentMillis;
        milliFraction = deltaTmilliFraction;
    }
}

