/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public interface IAnimEventCallback {
    public void OnAnimEvent(AnimLayer var1, AnimationTrack var2, AnimEvent var3);
}

