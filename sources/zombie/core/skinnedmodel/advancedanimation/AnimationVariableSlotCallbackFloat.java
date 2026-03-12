/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallback;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.debug.DebugLog;

public final class AnimationVariableSlotCallbackFloat
extends AnimationVariableSlotCallback<Float> {
    private final PrimitiveFloatSupplier callbackGetPrimitive;
    private final PrimitiveFloatConsumer callbackSetPrimitive;
    private float defaultValue;

    public AnimationVariableSlotCallbackFloat(String key, PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
    }

    public AnimationVariableSlotCallbackFloat(String key, PrimitiveFloatSupplier callbackGet, PrimitiveFloatConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
    }

    public AnimationVariableSlotCallbackFloat(String key, float defaultVal, PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackFloat(String key, float defaultVal, PrimitiveFloatSupplier callbackGet, PrimitiveFloatConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
        this.defaultValue = defaultVal;
    }

    @Override
    public Float getValue() {
        return Float.valueOf(this.getValueFloat());
    }

    @Override
    public Float getDefaultValue() {
        return Float.valueOf(this.defaultValue);
    }

    @Override
    public boolean trySetValue(Float val) {
        return this.trySetValue(val.floatValue());
    }

    @Override
    public boolean trySetValue(float val) {
        if (this.callbackSetPrimitive == null) {
            return super.trySetValue(Float.valueOf(val));
        }
        if (this.isReadOnly()) {
            DebugLog.General.warn("Trying to set read-only variable \"%s\"", this.getKey());
            return false;
        }
        this.callbackSetPrimitive.accept(val);
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return this.callbackSetPrimitive == null && super.isReadOnly();
    }

    @Override
    public String getValueString() {
        return this.getValue().toString();
    }

    @Override
    public float getValueFloat() {
        if (this.callbackGetPrimitive == null) {
            return ((Float)super.getValue()).floatValue();
        }
        return this.callbackGetPrimitive.get();
    }

    @Override
    public int getValueInt() {
        return (int)this.getValueFloat();
    }

    @Override
    public boolean getValueBool() {
        return this.getValueFloat() != 0.0f;
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(PZMath.tryParseFloat(val, 0.0f));
    }

    @Override
    public void setValue(float val) {
        this.trySetValue(val);
    }

    @Override
    public void setValue(int val) {
        this.trySetValue(val);
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val ? 1.0f : 0.0f);
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Float;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    public static interface PrimitiveFloatSupplier {
        public float get();
    }

    public static interface PrimitiveFloatConsumer {
        public void accept(float var1);
    }
}

