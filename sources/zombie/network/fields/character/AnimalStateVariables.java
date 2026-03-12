/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.character;

import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandles;

public class AnimalStateVariables {
    public static byte getVariables(IsoAnimal animal) {
        byte value = 0;
        value = (byte)(value | (animal.isOnFloor() ? Flags.isOnFloor : (byte)0));
        value = (byte)(value | (animal.isDead() ? Flags.isDead : (byte)0));
        value = (byte)(value | (animal.getVariableBoolean(AnimationVariableHandles.animalRunning) ? Flags.isRunning : (byte)0));
        value = (byte)(value | (animal.isAnimalAttacking() ? Flags.isAttacking : (byte)0));
        return value;
    }

    public static void setVariables(IsoAnimal animal, byte value) {
        animal.setOnFloor((value & Flags.isOnFloor) != 0);
        animal.setVariable(AnimationVariableHandles.animalRunning, (value & Flags.isRunning) != 0);
        animal.setAnimalAttackingOnClient((value & Flags.isAttacking) != 0);
    }

    public static class Flags {
        public static byte isOnFloor = 1;
        public static byte isDead = (byte)2;
        public static byte isRunning = (byte)4;
        public static byte isAttacking = (byte)8;
    }
}

