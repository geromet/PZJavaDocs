/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import org.joml.Random;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.random.Rand;

public enum RagdollBodyPart {
    BODYPART_PELVIS,
    BODYPART_SPINE,
    BODYPART_HEAD,
    BODYPART_LEFT_UPPER_LEG,
    BODYPART_LEFT_LOWER_LEG,
    BODYPART_RIGHT_UPPER_LEG,
    BODYPART_RIGHT_LOWER_LEG,
    BODYPART_LEFT_UPPER_ARM,
    BODYPART_LEFT_LOWER_ARM,
    BODYPART_RIGHT_UPPER_ARM,
    BODYPART_RIGHT_LOWER_ARM,
    BODYPART_COUNT;

    private static final RagdollBodyPart[] VALUES;
    private static final int RANDOM_BOUND;
    private static final Random RANDOM;

    public static RagdollBodyPart getRandomPart() {
        return VALUES[RANDOM.nextInt(RANDOM_BOUND)];
    }

    public static boolean isHead(int value) {
        return value == BODYPART_HEAD.ordinal();
    }

    public static boolean isLeg(int value) {
        return value >= BODYPART_LEFT_UPPER_LEG.ordinal() && value <= BODYPART_RIGHT_LOWER_LEG.ordinal();
    }

    public static boolean isArm(int value) {
        return value >= BODYPART_LEFT_UPPER_ARM.ordinal() && value <= BODYPART_RIGHT_LOWER_ARM.ordinal();
    }

    public static int getBodyPartType(int value) {
        boolean secondaryBodyPart = Rand.NextBool(2);
        return switch (VALUES[value].ordinal()) {
            case 2 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.Head);
                }
                yield BodyPartType.ToIndex(BodyPartType.Neck);
            }
            case 3 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.Groin);
                }
                yield BodyPartType.ToIndex(BodyPartType.UpperLeg_L);
            }
            case 4 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.LowerLeg_L);
                }
                yield BodyPartType.ToIndex(BodyPartType.Foot_L);
            }
            case 5 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.Groin);
                }
                yield BodyPartType.ToIndex(BodyPartType.UpperLeg_R);
            }
            case 6 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.LowerLeg_R);
                }
                yield BodyPartType.ToIndex(BodyPartType.Foot_R);
            }
            case 0 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.Torso_Lower);
                }
                yield BodyPartType.ToIndex(BodyPartType.Groin);
            }
            case 1 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.Torso_Upper);
                }
                yield BodyPartType.ToIndex(BodyPartType.Torso_Lower);
            }
            case 7 -> BodyPartType.ToIndex(BodyPartType.UpperArm_L);
            case 8 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.ForeArm_L);
                }
                yield BodyPartType.ToIndex(BodyPartType.Hand_L);
            }
            case 9 -> BodyPartType.ToIndex(BodyPartType.UpperArm_R);
            case 10 -> {
                if (secondaryBodyPart) {
                    yield BodyPartType.ToIndex(BodyPartType.ForeArm_R);
                }
                yield BodyPartType.ToIndex(BodyPartType.Hand_R);
            }
            default -> BodyPartType.ToIndex(BodyPartType.MAX);
        };
    }

    static {
        VALUES = RagdollBodyPart.values();
        RANDOM_BOUND = VALUES.length - 1;
        RANDOM = new Random();
    }
}

