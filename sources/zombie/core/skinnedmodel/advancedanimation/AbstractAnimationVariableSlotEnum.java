/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.util.StringUtils;

public abstract class AbstractAnimationVariableSlotEnum<EnumType extends Enum<EnumType>>
extends AnimationVariableSlot {
    private final EnumType defaultValue;
    private final Class<EnumType> enumTypeClass;

    public AbstractAnimationVariableSlotEnum(Class<EnumType> enumTypeClass, String key, EnumType defaultVal, IAnimationVariableSlotDescriptor descriptor) {
        super(key, descriptor);
        this.enumTypeClass = enumTypeClass;
        this.defaultValue = defaultVal;
    }

    public Class<EnumType> getEnumTypeClass() {
        return this.enumTypeClass;
    }

    public abstract EnumType getValue();

    public abstract void setValue(EnumType var1);

    public <E extends Enum<E>> E getEnumValue(E defaultVal) {
        Class<EnumType> enumClass = this.getEnumTypeClass();
        Class<?> incomingValType = defaultVal.getClass();
        if (incomingValType != enumClass) {
            String strValue = this.getValueString();
            return (E)StringUtils.tryParseEnum(incomingValType, strValue, defaultVal);
        }
        return (E)this.getValue();
    }

    public <E extends Enum<E>> void setEnumValue(E val) {
        Class<EnumType> enumClass = this.getEnumTypeClass();
        Class<?> incomingValType = val.getClass();
        if (incomingValType != enumClass) {
            String strValue = val.toString();
            this.setValue(strValue);
            return;
        }
        this.setValue(val);
    }

    @Override
    public String getValueString() {
        EnumType valueEnum = this.getValue();
        return valueEnum != null ? ((Enum)valueEnum).name() : "";
    }

    @Override
    public float getValueFloat() {
        return PZMath.tryParseFloat(this.getValueString(), 0.0f);
    }

    @Override
    public int getValueInt() {
        return PZMath.tryParseInt(this.getValueString(), 0);
    }

    @Override
    public boolean getValueBool() {
        return StringUtils.tryParseBoolean(this.getValueString());
    }

    public EnumType getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void setValue(String val) {
        try {
            this.setEnumValue((E)((Enum)Enum.valueOf(this.enumTypeClass, val)));
        }
        catch (IllegalArgumentException ex) {
            this.setEnumValue((E)((Enum)this.defaultValue));
        }
    }

    @Override
    public void setValue(float val) {
        this.setValue(String.valueOf(val));
    }

    @Override
    public void setValue(int val) {
        this.setValue(String.valueOf(val));
    }

    @Override
    public void setValue(boolean val) {
        this.setValue(val ? "true" : "false");
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.String;
    }

    @Override
    public boolean canConvertFrom(String val) {
        try {
            EnumType parsedVal = Enum.valueOf(this.enumTypeClass, val);
            return parsedVal != null;
        }
        catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void clear() {
        this.setEnumValue((E)((Enum)this.defaultValue));
    }
}

