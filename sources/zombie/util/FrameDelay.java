/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import zombie.GameTime;

public class FrameDelay {
    public int delay = 1;
    private int count;
    private float delta;
    private float multiplier;

    public FrameDelay() {
    }

    public FrameDelay(int delay) {
        this.delay = delay;
    }

    public boolean update() {
        if (this.count == 0) {
            this.delta = 0.0f;
            this.multiplier = 0.0f;
        }
        this.delta += GameTime.instance.getTimeDelta();
        this.multiplier += GameTime.instance.getMultiplier();
        this.count += Math.round(GameTime.instance.perObjectMultiplier);
        if (this.count > this.delay) {
            this.count = 0;
            return true;
        }
        return false;
    }

    public float getDelta() {
        return this.delta;
    }

    public float getMultiplier() {
        return this.multiplier;
    }

    public void reset() {
        this.count = 0;
        this.delta = 0.0f;
        this.multiplier = 0.0f;
    }
}

