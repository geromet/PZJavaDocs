/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackBool;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackEnum;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackFloat;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackInt;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackString;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;

public interface IAnimationVariableCallbackMap
extends IAnimationVariableMap {
    default public void setVariable(String key, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackBool(key, callbackGet, descriptor));
    }

    default public void setVariable(String key, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, AnimationVariableSlotCallbackBool.CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackBool(key, callbackGet, callbackSet, descriptor));
    }

    default public void setVariable(String key, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackString(key, callbackGet, descriptor));
    }

    default public void setVariable(String key, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackString(key, callbackGet, callbackSet, descriptor));
    }

    default public void setVariable(String key, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackFloat(key, callbackGet, descriptor));
    }

    default public void setVariable(String key, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackFloat(key, callbackGet, callbackSet, descriptor));
    }

    default public void setVariable(String key, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackInt(key, callbackGet, descriptor));
    }

    default public void setVariable(String key, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, AnimationVariableSlotCallbackInt.PrimitiveIntConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackInt(key, callbackGet, callbackSet, descriptor));
    }

    default public void setVariable(String key, boolean defaultVal, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackBool(key, defaultVal, callbackGet, descriptor));
    }

    default public void setVariable(String key, boolean defaultVal, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, AnimationVariableSlotCallbackBool.CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackBool(key, defaultVal, callbackGet, callbackSet, descriptor));
    }

    default public void setVariable(String key, String defaultVal, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackString(key, defaultVal, callbackGet, descriptor));
    }

    default public void setVariable(String key, String defaultVal, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackString(key, defaultVal, callbackGet, callbackSet, descriptor));
    }

    default public void setVariable(String key, float defaultVal, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackFloat(key, defaultVal, callbackGet, descriptor));
    }

    default public void setVariable(String key, float defaultVal, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackFloat(key, defaultVal, callbackGet, callbackSet, descriptor));
    }

    default public void setVariable(String key, int defaultVal, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackInt(key, defaultVal, callbackGet, descriptor));
    }

    default public void setVariable(String key, int defaultVal, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, AnimationVariableSlotCallbackInt.PrimitiveIntConsumer callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackInt(key, defaultVal, callbackGet, callbackSet, descriptor));
    }

    default public <EnumType extends Enum<EnumType>> void setVariable(String key, Class<EnumType> enumTypeClass, Supplier<EnumType> callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackEnum<Enum>(enumTypeClass, key, (Enum)callbackGet.get(), callbackGet, descriptor));
    }

    default public <EnumType extends Enum<EnumType>> void setVariable(String key, Class<EnumType> enumTypeClass, Supplier<EnumType> callbackGet, Consumer<EnumType> callbackSet, IAnimationVariableSlotDescriptor descriptor) {
        this.setVariable(new AnimationVariableSlotCallbackEnum<Enum>(enumTypeClass, key, (Enum)callbackGet.get(), callbackGet, callbackSet, descriptor));
    }
}

