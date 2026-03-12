/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.util.StringUtils;

public interface IAnimationVariableSlot {
    public String getKey();

    public String getValueString();

    public float getValueFloat();

    public int getValueInt();

    public boolean getValueBool();

    default public <EnumType extends Enum<EnumType>> EnumType getEnumValue(EnumType defaultVal) {
        String strValue = this.getValueString();
        return (EnumType)StringUtils.tryParseEnum(defaultVal.getClass(), strValue, defaultVal);
    }

    default public <EnumType extends Enum<EnumType>> void setEnumValue(EnumType val) {
        String strValue = val.toString();
        this.setValue(strValue);
    }

    public void setValue(String var1);

    public void setValue(float var1);

    public void setValue(int var1);

    public void setValue(boolean var1);

    public AnimationVariableType getType();

    public boolean canConvertFrom(String var1);

    public void clear();

    default public boolean isReadOnly() {
        return false;
    }

    default public boolean setReadOnly(boolean set) {
        return false;
    }

    default public AnimationVariableHandle getHandle() {
        return AnimationVariableHandle.alloc(this.getKey());
    }

    default public String getDescription(IAnimationVariableSource owner) {
        return null;
    }
}

