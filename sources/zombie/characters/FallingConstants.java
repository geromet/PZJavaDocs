/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.FallSeverity;
import zombie.core.math.PZMath;

public final class FallingConstants {
    public static final float IsoWorldToPhysicsZScale = 2.44949f;
    public static final float PhysicsToIsoWorldZScale = 0.40824825f;
    public static final float FallAcceleration = 9.8f;
    public static final float IsoFallAcceleration = 5.0010414f;
    public static final float isFallingThreshold = FallingConstants.getIsoImpactSpeedFromHeight(0.35f);
    public static final float noDamageThreshold = FallingConstants.getIsoImpactSpeedFromHeight(0.5f);
    public static final float hardFallThreshold = FallingConstants.getIsoImpactSpeedFromHeight(1.5f);
    public static final float severeFallThreshold = FallingConstants.getIsoImpactSpeedFromHeight(2.5f);
    public static final float lethalFallThreshold = FallingConstants.getIsoImpactSpeedFromHeight(3.5f);
    public static final float zombieLethalFallThreshold = FallingConstants.getIsoImpactSpeedFromHeight(20.0f);
    public static final float fallDamageMultiplier = 115.0f;
    public static final float fallDamageInjuryMultiplier = 55.0f;

    public static float getIsoImpactSpeedFromHeight(float fallHeight) {
        float impactTime = PZMath.sqrt(fallHeight * 2.0f / 5.0010414f);
        return 5.0010414f * impactTime;
    }

    public static boolean isLethalFall(float isoFallSpeed) {
        return isoFallSpeed >= lethalFallThreshold;
    }

    public static boolean isSevereFall(float isoFallSpeed) {
        return isoFallSpeed >= severeFallThreshold && isoFallSpeed < lethalFallThreshold;
    }

    public static boolean isHardFall(float isoFallSpeed) {
        return isoFallSpeed >= hardFallThreshold && isoFallSpeed < severeFallThreshold;
    }

    public static boolean isMoreThanHardFall(float isoFallSpeed) {
        return isoFallSpeed >= severeFallThreshold;
    }

    public static boolean isLightFall(float isoFallSpeed) {
        return isoFallSpeed >= isFallingThreshold && isoFallSpeed < hardFallThreshold;
    }

    public static boolean isMoreThanLightFall(float isoFallSpeed) {
        return isoFallSpeed >= hardFallThreshold;
    }

    public static boolean isFall(float isoFallSpeed) {
        return isoFallSpeed >= isFallingThreshold;
    }

    public static boolean isDamagingFall(float isoFallSpeed) {
        return isoFallSpeed >= noDamageThreshold;
    }

    public static FallSeverity getFallSeverity(float isoFallSpeed) {
        if (!FallingConstants.isFall(isoFallSpeed)) {
            return FallSeverity.None;
        }
        if (FallingConstants.isLightFall(isoFallSpeed)) {
            return FallSeverity.Light;
        }
        if (FallingConstants.isHardFall(isoFallSpeed)) {
            return FallSeverity.Hard;
        }
        if (FallingConstants.isSevereFall(isoFallSpeed)) {
            return FallSeverity.Severe;
        }
        if (FallingConstants.isLethalFall(isoFallSpeed)) {
            return FallSeverity.Lethal;
        }
        return FallSeverity.None;
    }
}

