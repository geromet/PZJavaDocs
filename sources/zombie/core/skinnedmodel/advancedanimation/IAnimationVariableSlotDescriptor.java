/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;

public interface IAnimationVariableSlotDescriptor {
    public static final IAnimationVariableSlotDescriptor Null = owner -> null;

    public String getDescription(IAnimationVariableSource var1);
}

