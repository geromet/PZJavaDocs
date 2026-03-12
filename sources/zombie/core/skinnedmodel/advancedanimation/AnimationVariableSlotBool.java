/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.util.StringUtils;

public final class AnimationVariableSlotBool
extends AnimationVariableSlot {
    private boolean value;
    private boolean isReadOnly;

    public AnimationVariableSlotBool(String key, IAnimationVariableSlotDescriptor descriptor) {
        super(key, descriptor);
    }

    @Override
    public String getValueString() {
        return this.value ? "true" : "false";
    }

    @Override
    public float getValueFloat() {
        return this.value ? 1.0f : 0.0f;
    }

    @Override
    public int getValueInt() {
        return this.value ? 1 : 0;
    }

    @Override
    public boolean getValueBool() {
        return this.value;
    }

    @Override
    public void setValue(String val) {
        if (!this.isReadOnly()) {
            this.value = StringUtils.tryParseBoolean(val);
        }
    }

    @Override
    public void setValue(float val) {
        if (!this.isReadOnly()) {
            this.value = (double)val != 0.0;
        }
    }

    @Override
    public void setValue(int val) {
        if (!this.isReadOnly()) {
            this.value = val != 0;
        }
    }

    @Override
    public void setValue(boolean val) {
        if (!this.isReadOnly()) {
            this.value = val;
        }
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Boolean;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return StringUtils.isBoolean(val);
    }

    @Override
    public void clear() {
        if (!this.isReadOnly()) {
            this.value = false;
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

