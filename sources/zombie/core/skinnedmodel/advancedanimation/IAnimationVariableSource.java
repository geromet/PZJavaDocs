/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;

public interface IAnimationVariableSource {
    public IAnimationVariableSlot getVariable(AnimationVariableHandle var1);

    default public IAnimationVariableSlot getVariable(String key) {
        AnimationVariableHandle handle = AnimationVariableHandle.alloc(key);
        return this.getVariable(handle);
    }

    default public String getVariableString(String key) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null ? slot.getValueString() : "";
    }

    default public float getVariableFloat(String key, float defaultVal) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null ? slot.getValueFloat() : defaultVal;
    }

    default public boolean getVariableBoolean(String key) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null && slot.getValueBool();
    }

    default public boolean getVariableBoolean(String key, boolean defaultVal) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null ? slot.getValueBool() : defaultVal;
    }

    default public boolean getVariableBoolean(AnimationVariableHandle handle) {
        IAnimationVariableSlot slot = this.getVariable(handle);
        return slot != null && slot.getValueBool();
    }

    default public <EnumType extends Enum<EnumType>> EnumType getVariableEnum(String key, EnumType defaultVal) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null ? slot.getEnumValue(defaultVal) : defaultVal;
    }

    public Iterable<IAnimationVariableSlot> getGameVariables();

    public boolean isVariable(String var1, String var2);

    public boolean containsVariable(String var1);

    default public IAnimationVariableSource getSubVariableSource(String subVariableSourceName) {
        return null;
    }
}

