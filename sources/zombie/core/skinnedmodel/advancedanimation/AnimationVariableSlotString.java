/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.util.StringUtils;

public final class AnimationVariableSlotString
extends AnimationVariableSlot {
    private String value;
    private boolean isReadOnly;

    public AnimationVariableSlotString(String key, IAnimationVariableSlotDescriptor descriptor) {
        super(key, descriptor);
    }

    @Override
    public String getValueString() {
        return this.value;
    }

    @Override
    public float getValueFloat() {
        return PZMath.tryParseFloat(this.value, 0.0f);
    }

    @Override
    public int getValueInt() {
        return PZMath.tryParseInt(this.value, 0);
    }

    @Override
    public boolean getValueBool() {
        return StringUtils.tryParseBoolean(this.value);
    }

    @Override
    public void setValue(String val) {
        if (!this.isReadOnly()) {
            this.value = val;
        }
    }

    @Override
    public void setValue(float val) {
        if (!this.isReadOnly()) {
            this.value = String.valueOf(val);
        }
    }

    @Override
    public void setValue(int val) {
        if (!this.isReadOnly()) {
            this.value = String.valueOf(val);
        }
    }

    @Override
    public void setValue(boolean val) {
        if (!this.isReadOnly()) {
            this.value = val ? "true" : "false";
        }
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.String;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    @Override
    public void clear() {
        if (!this.isReadOnly()) {
            this.value = "";
        }
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    public boolean setReadOnly(boolean set) {
        if (set != this.isReadOnly) {
            this.isReadOnly = set;
            return true;
        }
        return false;
    }
}

