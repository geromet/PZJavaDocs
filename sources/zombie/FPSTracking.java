/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import zombie.GameTime;
import zombie.GameWindow;
import zombie.core.PerformanceSettings;

public final class FPSTracking {
    private final double[] lastFps = new double[20];
    private int lastFpsCount;
    private long timeAtLastUpdate;
    private final long[] last10 = new long[10];
    private int last10index;

    public void init() {
        for (int n = 0; n < 20; ++n) {
            this.lastFps[n] = PerformanceSettings.getLockFPS();
        }
        this.timeAtLastUpdate = System.nanoTime();
    }

    public long frameStep() {
        long timeNow = System.nanoTime();
        long timeDiff = timeNow - this.timeAtLastUpdate;
        if (timeDiff > 0L) {
            double frames;
            float averageFPS = 0.0f;
            double deltaTimeSeconds = (double)timeDiff / 1.0E9;
            this.lastFps[this.lastFpsCount] = frames = 1.0 / deltaTimeSeconds;
            ++this.lastFpsCount;
            if (this.lastFpsCount >= 5) {
                this.lastFpsCount = 0;
            }
            for (int n = 0; n < 5; ++n) {
                averageFPS = (float)((double)averageFPS + this.lastFps[n]);
            }
            GameWindow.averageFPS = averageFPS /= 5.0f;
            GameTime.instance.fpsMultiplier = (float)(60.0 / frames);
            if (GameTime.instance.fpsMultiplier > 5.0f) {
                GameTime.instance.fpsMultiplier = 5.0f;
            }
        }
        this.timeAtLastUpdate = timeNow;
        this.updateFPS(timeDiff);
        return timeDiff;
    }

    public void updateFPS(long timeDiff) {
        this.last10[this.last10index++] = timeDiff;
        if (this.last10index >= this.last10.length) {
            this.last10index = 0;
        }
        float lowest = 11110.0f;
        float highest = -11110.0f;
        for (long aLast10 : this.last10) {
            if (aLast10 == 0L) continue;
            if ((float)aLast10 < lowest) {
                lowest = aLast10;
            }
            if (!((float)aLast10 > highest)) continue;
            highest = aLast10;
        }
    }
}

