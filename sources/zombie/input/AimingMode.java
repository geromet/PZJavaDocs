/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import zombie.GameTime;
import zombie.core.math.PZMath;
import zombie.core.math.interpolators.LerpType;

public enum AimingMode {
    REGULAR(0.035f, 0.025f),
    TARGET_FOUND(0.01f, 0.0075f),
    NOT_AIMING(1.0f, 1.0f);

    final float lerpRateOutwards;
    final float lerpRateInwards;

    private AimingMode(float outwards, float inwards) {
        this.lerpRateOutwards = outwards;
        this.lerpRateInwards = inwards;
    }

    float lerpAxis(float src, float target, float middle) {
        float absDiffSrc = PZMath.abs(middle - src);
        float absDiffTarget = PZMath.abs(middle - target);
        float aimingLerpRate = absDiffTarget > absDiffSrc ? this.lerpRateOutwards : this.lerpRateInwards;
        float aimingLerpRateClamped = PZMath.clamp(aimingLerpRate * GameTime.getInstance().getMultiplier(), 0.0f, 1.0f);
        return PZMath.lerp(src, target, aimingLerpRateClamped, LerpType.Linear);
    }
}

