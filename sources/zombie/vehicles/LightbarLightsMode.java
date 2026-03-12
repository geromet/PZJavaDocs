/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

public final class LightbarLightsMode {
    private long startTime;
    private int light;
    private final int modeMax = 3;
    private int mode;

    public int get() {
        return this.mode;
    }

    public void set(int v) {
        if (v > 3) {
            this.mode = 3;
            return;
        }
        if (v < 0) {
            this.mode = 0;
            return;
        }
        this.mode = v;
        if (this.mode != 0) {
            this.start();
        }
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        long d = System.currentTimeMillis() - this.startTime;
        switch (this.mode) {
            case 1: {
                if ((d %= 1000L) < 50L) {
                    this.light = 0;
                    break;
                }
                if (d < 450L) {
                    this.light = 1;
                    break;
                }
                if (d < 550L) {
                    this.light = 0;
                    break;
                }
                if (d < 950L) {
                    this.light = 2;
                    break;
                }
                this.light = 0;
                break;
            }
            case 2: {
                if ((d %= 1000L) < 50L) {
                    this.light = 0;
                    break;
                }
                if (d < 250L) {
                    this.light = 1;
                    break;
                }
                if (d < 300L) {
                    this.light = 0;
                    break;
                }
                if (d < 500L) {
                    this.light = 1;
                    break;
                }
                if (d < 550L) {
                    this.light = 0;
                    break;
                }
                if (d < 750L) {
                    this.light = 2;
                    break;
                }
                if (d < 800L) {
                    this.light = 0;
                    break;
                }
                this.light = 2;
                break;
            }
            case 3: {
                if ((d %= 300L) < 25L) {
                    this.light = 0;
                    break;
                }
                if (d < 125L) {
                    this.light = 1;
                    break;
                }
                if (d < 175L) {
                    this.light = 0;
                    break;
                }
                if (d < 275L) {
                    this.light = 2;
                    break;
                }
                this.light = 0;
                break;
            }
            default: {
                this.light = 0;
            }
        }
    }

    public int getLightTexIndex() {
        return this.light;
    }

    public boolean isEnable() {
        return this.mode != 0;
    }
}

