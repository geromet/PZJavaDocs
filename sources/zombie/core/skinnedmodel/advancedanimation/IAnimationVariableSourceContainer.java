/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;

public interface IAnimationVariableSourceContainer
extends IAnimationVariableSource {
    public IAnimationVariableSource getGameVariablesInternal();

    @Override
    default public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        return this.getGameVariablesInternal().getVariable(handle);
    }

    @Override
    default public IAnimationVariableSlot getVariable(String key) {
        return this.getGameVariablesInternal().getVariable(key);
    }

    @Override
    default public String getVariableString(String name) {
        return this.getGameVariablesInternal().getVariableString(name);
    }

    @Override
    default public float getVariableFloat(String name, float defaultVal) {
        return this.getGameVariablesInternal().getVariableFloat(name, defaultVal);
    }

    @Override
    default public boolean getVariableBoolean(String name) {
        return this.getGameVariablesInternal().getVariableBoolean(name);
    }

    @Override
    default public boolean getVariableBoolean(String key, boolean defaultVal) {
        return this.getGameVariablesInternal().getVariableBoolean(key, defaultVal);
    }

    @Override
    default public Iterable<IAnimationVariableSlot> getGameVariables() {
        return this.getGameVariablesInternal().getGameVariables();
    }

    @Override
    default public boolean isVariable(String name, String val) {
        return this.getGameVariablesInternal().isVariable(name, val);
    }

    @Override
    default public boolean containsVariable(String name) {
        return this.getGameVariablesInternal().containsVariable(name);
    }
}

