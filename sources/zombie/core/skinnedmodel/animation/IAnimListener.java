/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import zombie.core.skinnedmodel.animation.AnimationTrack;

public interface IAnimListener {
    public void onAnimStarted(AnimationTrack var1);

    public void onLoopedAnim(AnimationTrack var1);

    public void onNonLoopedAnimFadeOut(AnimationTrack var1);

    public void onNonLoopedAnimFinished(AnimationTrack var1);

    public void onNoAnimConditionsPass();

    public void onTrackDestroyed(AnimationTrack var1);
}

