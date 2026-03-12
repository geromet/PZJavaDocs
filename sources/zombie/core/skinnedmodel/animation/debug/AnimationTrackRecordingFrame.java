/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation.debug;

import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntries;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntry;
import zombie.core.skinnedmodel.animation.debug.GenericNameWeightRecordingFrame;

public final class AnimationTrackRecordingFrame
extends GenericNameWeightRecordingFrame {
    public AnimationTrackRecordingFrame(String fileKey) {
        super(fileKey);
    }

    public void logAnimWeights(LiveAnimationTrackEntries trackEntries) {
        for (int i = 0; i < trackEntries.count(); ++i) {
            LiveAnimationTrackEntry trackEntry = trackEntries.get(i);
            float animWeight = trackEntry.getBlendWeight();
            AnimationTrack track = trackEntry.getTrack();
            String animName = track.getName();
            int layer = track.getLayerIdx();
            this.logWeight(animName, layer, animWeight);
        }
    }
}

