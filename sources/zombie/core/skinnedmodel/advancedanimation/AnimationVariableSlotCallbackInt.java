/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallback;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.debug.DebugLog;

public final class AnimationVariableSlotCallbackInt
extends AnimationVariableSlotCallback<Integer> {
    private final PrimitiveIntSupplier callbackGetPrimitive;
    private final PrimitiveIntConsumer callbackSetPrimitive;
    private int defaultValue;

    public AnimationVariableSlotCallbackInt(String key, PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
    }

    public AnimationVariableSlotCallbackInt(String key, PrimitiveIntSupplier callbackGet, PrimitiveIntConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
    }

    public AnimationVariableSlotCallbackInt(String key, int defaultVal, PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackInt(String key, int defaultVal, PrimitiveIntSupplier callbackGet, PrimitiveIntConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
        this.defaultValue = defaultVal;
    }

    @Override
    public Integer getValue() {
        return this.getValueInt();
    }

    @Override
    public Integer getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getValueString() {
        return this.getValue().toString();
    }

    @Override
    public int getValueInt() {
        if (this.callbackGetPrimitive == null) {
            return (Integer)super.getValue();
        }
        return this.callbackGetPrimitive.get();
    }

    @Override
    public boolean trySetValue(Integer val) {
        return this.trySetValue((int)val);
    }

    @Override
    public boolean trySetValue(int val) {
        if (this.callbackSetPrimitive == null) {
            return super.trySetValue(val);
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
    public float getValueFloat() {
        return this.getValueInt();
    }

    @Override
    public boolean getValueBool() {
        return this.getValueInt() != 0;
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(PZMath.tryParseInt(val, 0));
    }

    @Override
    public void setValue(float val) {
        this.trySetValue((int)val);
    }

    @Override
    public void setValue(int val) {
        this.trySetValue(val);
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val ? 1 : 0);
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Int;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    public static interface PrimitiveIntSupplier {
        public int get();
    }

    public static interface PrimitiveIntConsumer {
        public void accept(int var1);
    }
}

