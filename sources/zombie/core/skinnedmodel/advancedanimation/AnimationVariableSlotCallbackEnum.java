/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.skinnedmodel.advancedanimation.AbstractAnimationVariableSlotEnum;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.debug.DebugLog;

public class AnimationVariableSlotCallbackEnum<EnumType extends Enum<EnumType>>
extends AbstractAnimationVariableSlotEnum<EnumType> {
    private final Supplier<EnumType> callbackGet;
    private final Consumer<EnumType> callbackSet;

    protected AnimationVariableSlotCallbackEnum(Class<EnumType> enumTypeClass, String key, EnumType defaultVal, Supplier<EnumType> callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this(enumTypeClass, key, defaultVal, callbackGet, null, descriptor);
    }

    protected AnimationVariableSlotCallbackEnum(Class<EnumType> enumTypeClass, String key, EnumType defaultVal, Supplier<EnumType> callbackGet, Consumer<EnumType> callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        super(enumTypeClass, key, defaultVal, descriptor);
        this.callbackGet = callbackGet;
        this.callbackSet = callbackSet;
    }

    @Override
    public EnumType getValue() {
        return (EnumType)((Enum)this.callbackGet.get());
    }

    @Override
    public void setValue(EnumType newValue) {
        this.trySetValue(newValue);
    }

    public boolean trySetValue(EnumType val) {
        if (this.isReadOnly()) {
            DebugLog.General.warn("Trying to set read-only variable \"%s\"", this.getKey());
            return false;
        }
        this.callbackSet.accept(val);
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return this.callbackSet == null;
    }

    @Override
    public void clear() {
        if (!this.isReadOnly()) {
            this.trySetValue(this.getDefaultValue());
        }
    }
}

