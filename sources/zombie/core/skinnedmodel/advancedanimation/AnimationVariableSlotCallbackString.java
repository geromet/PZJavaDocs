/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallback;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.util.StringUtils;

public final class AnimationVariableSlotCallbackString
extends AnimationVariableSlotCallback<String> {
    private String defaultValue = "";

    public AnimationVariableSlotCallbackString(String key, CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, descriptor);
    }

    public AnimationVariableSlotCallbackString(String key, CallbackGetStrongTyped callbackGet, CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, callbackSet, descriptor);
    }

    public AnimationVariableSlotCallbackString(String key, String defaultVal, CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, descriptor);
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackString(String key, String defaultVal, CallbackGetStrongTyped callbackGet, CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, callbackGet, callbackSet, descriptor);
        this.defaultValue = defaultVal;
    }

    @Override
    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getValueString() {
        return (String)this.getValue();
    }

    @Override
    public float getValueFloat() {
        return PZMath.tryParseFloat((String)this.getValue(), 0.0f);
    }

    @Override
    public int getValueInt() {
        return PZMath.tryParseInt((String)this.getValue(), 0);
    }

    @Override
    public boolean getValueBool() {
        return StringUtils.tryParseBoolean((String)this.getValue());
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(val);
    }

    @Override
    public void setValue(float val) {
        this.trySetValue(String.valueOf(val));
    }

    @Override
    public void setValue(int val) {
        this.trySetValue(String.valueOf(val));
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val ? "true" : "false");
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.String;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    public static interface CallbackSetStrongTyped
    extends Consumer<String> {
    }

    public static interface CallbackGetStrongTyped
    extends Supplier<String> {
    }
}

