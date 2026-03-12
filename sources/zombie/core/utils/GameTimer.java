/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import zombie.GameTime;
import zombie.SandboxOptions;

public class GameTimer {
    double start = GameTime.getInstance().getWorldAgeHours();
    double period;

    public GameTimer(int period) {
        this.period = (double)period / (double)SandboxOptions.getInstance().getDayLengthMinutes() * 24.0 / 60.0;
    }

    public boolean check() {
        boolean result;
        boolean bl = result = GameTime.getInstance().getWorldAgeHours() - this.start > this.period;
        if (result) {
            this.start = GameTime.getInstance().getWorldAgeHours();
        }
        return result;
    }
}

