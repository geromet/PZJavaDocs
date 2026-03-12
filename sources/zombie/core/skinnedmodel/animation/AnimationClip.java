/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.util.list.PZArrayUtil;

public final class AnimationClip {
    public final String name;
    public final boolean isRagdoll;
    public final boolean keepLastFrame;
    private final float duration;
    private final KeyframeByBoneIndexElement[] keyFramesByBoneIndex;
    private final List<Keyframe> rootMotionKeyframes = new ArrayList<Keyframe>();
    private final Keyframe[] keyframeArray;
    private boolean isRagdollSimulationActive;

    public AnimationClip(float duration, List<Keyframe> keyframes, String name, boolean bKeepLastFrame) {
        this(duration, keyframes, name, bKeepLastFrame, false);
    }

    public AnimationClip(float duration, List<Keyframe> keyframes, String name, boolean bKeepLastFrame, boolean isRagdoll) {
        this.name = name;
        this.isRagdoll = isRagdoll;
        this.duration = duration;
        this.keepLastFrame = bKeepLastFrame;
        this.keyframeArray = keyframes.toArray(new Keyframe[0]);
        this.keyFramesByBoneIndex = new KeyframeByBoneIndexElement[60];
        this.recalculateKeyframesByBoneIndex();
    }

    public Keyframe getKeyframe(int keyframeIndex) {
        return this.keyframeArray[keyframeIndex];
    }

    public Keyframe[] getBoneFramesAt(int idx) {
        return this.keyFramesByBoneIndex[idx].keyframes;
    }

    public int getRootMotionFrameCount() {
        return this.rootMotionKeyframes.size();
    }

    public Keyframe getRootMotionFrameAt(int idx) {
        return this.rootMotionKeyframes.get(idx);
    }

    public Keyframe[] getKeyframes() {
        return this.keyframeArray;
    }

    public float getDuration() {
        return this.duration;
    }

    private KeyframeByBoneIndexElement getKeyframesForBone(int boneIdx) {
        return this.keyFramesByBoneIndex[boneIdx];
    }

    public Keyframe[] getKeyframesForBone(int boneIdx, Keyframe[] keyframesForBone) {
        KeyframeByBoneIndexElement allFrames = this.getKeyframesForBone(boneIdx);
        int numKeyframes = allFrames.keyframes.length;
        if (PZArrayUtil.lengthOf(keyframesForBone) < numKeyframes) {
            keyframesForBone = PZArrayUtil.newInstance(Keyframe.class, keyframesForBone, numKeyframes, false, Keyframe::new);
        }
        PZArrayUtil.arrayCopy(keyframesForBone, allFrames.keyframes);
        return keyframesForBone;
    }

    public boolean isRagdollSimulationActive() {
        return this.isRagdollSimulationActive;
    }

    public void setRagdollSimulationActive(boolean val) {
        this.isRagdollSimulationActive = val;
    }

    public float getTranslationLength(BoneAxis deferredBoneAxis) {
        float x = this.keyframeArray[this.keyframeArray.length - 1].position.x - this.keyframeArray[0].position.x;
        float y = deferredBoneAxis == BoneAxis.Y ? -this.keyframeArray[this.keyframeArray.length - 1].position.z + this.keyframeArray[0].position.z : this.keyframeArray[this.keyframeArray.length - 1].position.y - this.keyframeArray[0].position.y;
        return (float)Math.sqrt(x * x + y * y);
    }

    public void recalculateKeyframesByBoneIndex() {
        ArrayList<Keyframe> bkf = new ArrayList<Keyframe>();
        int frameCount = this.keyframeArray.length > 1 ? this.keyframeArray.length - (this.keepLastFrame ? 0 : 1) : 1;
        for (int boneIdx = 0; boneIdx < 60; ++boneIdx) {
            bkf.clear();
            for (int k = 0; k < frameCount; ++k) {
                Keyframe keyframe = this.keyframeArray[k];
                if (keyframe.none != boneIdx) continue;
                bkf.add(keyframe);
            }
            this.keyFramesByBoneIndex[boneIdx] = new KeyframeByBoneIndexElement(bkf);
        }
    }

    private static class KeyframeByBoneIndexElement {
        final Keyframe[] keyframes;

        KeyframeByBoneIndexElement(List<Keyframe> keyframes) {
            this.keyframes = keyframes.toArray(new Keyframe[0]);
        }
    }
}

