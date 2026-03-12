/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;

public final class AnimationVariableSlotFloat
extends AnimationVariableSlot {
    private float value;
    private boolean isReadOnly;

    public AnimationVariableSlotFloat(String key, IAnimationVariableSlotDescriptor descriptor) {
        super(key, descriptor);
    }

    @Override
    public String getValueString() {
        return String.valueOf(this.value);
    }

    @Override
    public float getValueFloat() {
        return this.value;
    }

    @Override
    public int getValueInt() {
        return (int)this.value;
    }

    @Override
    public boolean getValueBool() {
        return this.value != 0.0f;
    }

    @Override
    public void setValue(String val) {
        if (!this.isReadOnly()) {
            this.value = PZMath.tryParseFloat(val, 0.0f);
        }
    }

    @Override
    public void setValue(float val) {
        if (!this.isReadOnly()) {
            this.value = val;
        }
    }

    @Override
    public void setValue(int val) {
        if (!this.isReadOnly()) {
            this.value = val;
        }
    }

    @Override
    public void setValue(boolean val) {
        if (!this.isReadOnly()) {
            this.value = val ? 1.0f : 0.0f;
        }
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Float;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return PZMath.canParseFloat(val);
    }

    @Override
    public void clear() {
        if (!this.isReadOnly()) {
            this.value = 0.0f;
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

