/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;

public interface IAnimationVariableMap
extends IAnimationVariableSource {
    public void setVariable(IAnimationVariableSlot var1);

    public IAnimationVariableSlot setVariable(String var1, String var2);

    public IAnimationVariableSlot setVariable(String var1, boolean var2);

    public IAnimationVariableSlot setVariable(String var1, float var2);

    public IAnimationVariableSlot setVariable(AnimationVariableHandle var1, boolean var2);

    public <EnumType extends Enum<EnumType>> IAnimationVariableSlot setVariableEnum(String var1, EnumType var2);

    public void clearVariable(String var1);

    public void clearVariables();
}

