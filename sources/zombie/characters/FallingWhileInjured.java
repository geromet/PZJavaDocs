/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.debug.DebugType;
import zombie.entity.util.Predicate;

public enum FallingWhileInjured {
    Head(1.01f, 1.01f, 1.05f, bodyPart -> bodyPart == BodyPartType.Head),
    Leg(1.01f, 1.2f, 2.5f, bodyPart -> bodyPart == BodyPartType.UpperLeg_L || bodyPart == BodyPartType.UpperLeg_R || bodyPart == BodyPartType.LowerLeg_L || bodyPart == BodyPartType.LowerLeg_R),
    Foot(1.01f, 1.05f, 1.75f, bodyPart -> bodyPart == BodyPartType.Foot_L || bodyPart == BodyPartType.Foot_R),
    Arm(1.01f, 1.15f, 1.5f, bodyPart -> bodyPart == BodyPartType.UpperArm_L || bodyPart == BodyPartType.UpperArm_R || bodyPart == BodyPartType.ForeArm_L || bodyPart == BodyPartType.ForeArm_R),
    Hand(1.01f, 1.05f, 1.25f, bodyPart -> bodyPart == BodyPartType.Hand_L || bodyPart == BodyPartType.Hand_R),
    Spine(1.01f, 1.2f, 2.2f, bodyPart -> bodyPart == BodyPartType.Neck || bodyPart == BodyPartType.Torso_Upper || bodyPart == BodyPartType.Torso_Lower || bodyPart == BodyPartType.Groin);

    private final float injured;
    private final float deepInjured;
    private final float fractured;
    private final Predicate<BodyPartType> isBodyPart;

    private FallingWhileInjured(float injured, float deepInjured, float fractured, Predicate<BodyPartType> isBodyPart) {
        this.injured = injured;
        this.deepInjured = deepInjured;
        this.fractured = fractured;
        this.isBodyPart = isBodyPart;
    }

    public boolean isBodyPart(BodyPartType bodyPartType) {
        return this.isBodyPart.evaluate(bodyPartType);
    }

    public static FallingWhileInjured fromBodyPartType(BodyPartType bodyPartType) {
        for (FallingWhileInjured elem : FallingWhileInjured.values()) {
            if (!elem.isBodyPart(bodyPartType)) continue;
            return elem;
        }
        DebugType.FallDamage.debugln("No FallingWhileInjured found for: %s", new Object[]{bodyPartType});
        return null;
    }

    public static float getDamageMultiplier(BodyPart bodyPart) {
        FallingWhileInjured bodyPartInjuryElem = FallingWhileInjured.fromBodyPartType(bodyPart.type);
        if (bodyPartInjuryElem == null) {
            return 1.0f;
        }
        if (bodyPart.getFractureTime() > 0.0f) {
            DebugType.FallDamage.debugln("Impact with fractured %s. Damage multiplier: %f", new Object[]{bodyPart.type, Float.valueOf(bodyPartInjuryElem.fractured)});
            return bodyPartInjuryElem.fractured;
        }
        if (bodyPart.isDeepWounded()) {
            DebugType.FallDamage.debugln("Impact with deep injured %s. Damage multiplier: %f", new Object[]{bodyPart.type, Float.valueOf(bodyPartInjuryElem.deepInjured)});
            return bodyPartInjuryElem.deepInjured;
        }
        if (bodyPart.HasInjury()) {
            DebugType.FallDamage.debugln("Impact with injured %s. Damage multiplier: %f", new Object[]{bodyPart.type, Float.valueOf(bodyPartInjuryElem.injured)});
            return bodyPartInjuryElem.injured;
        }
        return 1.0f;
    }
}

