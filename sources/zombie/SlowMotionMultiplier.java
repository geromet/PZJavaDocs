/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import zombie.core.math.PZMath;

public enum SlowMotionMultiplier {
    ONE,
    HALF,
    QUARTER,
    EIGHTH,
    SIXTEENTH,
    THIRTYTWOTH,
    SIXTYFORTH;

    private static final SlowMotionMultiplier[] values;
    private final float multiplier = 1.0f / PZMath.pow(2.0f, this.ordinal());

    public float getMultiplier() {
        return this.multiplier;
    }

    public SlowMotionMultiplier getHalfOf() {
        return this.ordinal() + 1 < values.length ? values[this.ordinal() + 1] : this;
    }

    public SlowMotionMultiplier getQuarterOf() {
        return this.getHalfOf().getHalfOf();
    }

    public SlowMotionMultiplier getEighthOf() {
        return this.getQuarterOf().getHalfOf();
    }

    public SlowMotionMultiplier getDoubleOf() {
        return this.ordinal() > 0 ? values[this.ordinal() - 1] : this;
    }

    static {
        values = SlowMotionMultiplier.values();
    }
}

