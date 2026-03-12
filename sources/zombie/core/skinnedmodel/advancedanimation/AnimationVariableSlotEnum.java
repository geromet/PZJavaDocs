/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AbstractAnimationVariableSlotEnum;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;

public class AnimationVariableSlotEnum<EnumType extends Enum<EnumType>>
extends AbstractAnimationVariableSlotEnum<EnumType> {
    private EnumType value;

    public AnimationVariableSlotEnum(Class<EnumType> enumTypeClass, String key, EnumType defaultVal, IAnimationVariableSlotDescriptor descriptor) {
        super(enumTypeClass, key, defaultVal, descriptor);
        this.value = defaultVal;
    }

    @Override
    public EnumType getValue() {
        return this.value;
    }

    @Override
    public void setValue(EnumType newValue) {
        if (!this.isReadOnly()) {
            this.value = newValue;
        }
    }
}

