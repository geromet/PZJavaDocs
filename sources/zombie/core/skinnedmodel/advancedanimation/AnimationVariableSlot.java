/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;

public abstract class AnimationVariableSlot
implements IAnimationVariableSlot {
    private final String key;
    private final IAnimationVariableSlotDescriptor descriptor;

    protected AnimationVariableSlot(String key, IAnimationVariableSlotDescriptor descriptor) {
        this.key = key.toLowerCase().trim();
        this.descriptor = descriptor;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getDescription(IAnimationVariableSource owner) {
        return this.descriptor != null ? this.descriptor.getDescription(owner) : null;
    }
}

