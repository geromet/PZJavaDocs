/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallback;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.util.StringUtils;

public final class AnimationVariableSlotCallbackBool
extends AnimationVariableSlotCallback<Boolean> {
    private boolean defaultValue;

    public AnimationVariableSlotCallbackBool(String key, CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, descriptor);
    }

    public AnimationVariableSlotCallbackBool(String key, CallbackGetStrongTyped callbackGet, CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, callbackSet, descriptor);
    }

    public AnimationVariableSlotCallbackBool(String key, boolean defaultVal, CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, descriptor);
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackBool(String key, boolean defaultVal, CallbackGetStrongTyped callbackGet, CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, callbackSet, descriptor);
        this.defaultValue = defaultVal;
    }

    @Override
    public Boolean getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getValueString() {
        return (Boolean)this.getValue() != false ? "true" : "false";
    }

    @Override
    public float getValueFloat() {
        return (Boolean)this.getValue() != false ? 1.0f : 0.0f;
    }

    @Override
    public int getValueInt() {
        return (Boolean)this.getValue() != false ? 1 : 0;
    }

    @Override
    public boolean getValueBool() {
        return (Boolean)this.getValue();
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(StringUtils.tryParseBoolean(val));
    }

    @Override
    public void setValue(float val) {
        this.trySetValue((double)val != 0.0);
    }

    @Override
    public void setValue(int val) {
        this.trySetValue((double)val != 0.0);
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val);
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Boolean;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return StringUtils.tryParseBoolean(val);
    }

    public static interface CallbackSetStrongTyped
    extends Consumer<Boolean> {
    }

    public static interface CallbackGetStrongTyped
    extends Supplier<Boolean> {
    }
}

