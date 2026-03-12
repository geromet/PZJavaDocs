/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class LiveAnimationTrackEntry
extends PooledObject {
    private float animBlendWeight = -1.0f;
    private int animBlendPriority = -1;
    private AnimationTrack animTrack;
    private static final Pool<LiveAnimationTrackEntry> s_pool = new Pool<LiveAnimationTrackEntry>(LiveAnimationTrackEntry::new);

    @Override
    public void onReleased() {
        this.reset();
    }

    public void reset() {
        this.animBlendWeight = -1.0f;
        this.animBlendPriority = -1;
        this.animTrack = null;
    }

    public static LiveAnimationTrackEntry alloc() {
        return s_pool.alloc();
    }

    public static LiveAnimationTrackEntry alloc(AnimationTrack track) {
        LiveAnimationTrackEntry newEntry = LiveAnimationTrackEntry.alloc();
        newEntry.animBlendWeight = track.getBlendWeight();
        newEntry.animBlendPriority = track.getPriority();
        newEntry.animTrack = track;
        return newEntry;
    }

    public float getBlendWeight() {
        return this.animBlendWeight;
    }

    public void setBlendWeight(float blendWeight) {
        this.animBlendWeight = blendWeight;
    }

    public int getLayer() {
        return this.animTrack.getLayerIdx();
    }

    public int getPriority() {
        return this.animBlendPriority;
    }

    public AnimationTrack getTrack() {
        return this.animTrack;
    }
}

