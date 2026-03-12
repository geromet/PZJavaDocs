/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimTrackSampler;
import zombie.core.skinnedmodel.animation.AnimationBoneBinding;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugOptions;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.list.PZArrayUtil;

public class ModelTransformSampler
extends PooledObject
implements AnimTrackSampler {
    private AnimationPlayer sourceAnimPlayer;
    private AnimationTrack track;
    private float currentTime;
    private SkinningData skinningData;
    private BoneTransform[] boneTransforms;
    private Matrix4f[] boneModelTransforms;
    private static final Pool<ModelTransformSampler> s_pool = new Pool<ModelTransformSampler>(ModelTransformSampler::new);

    private void init(AnimationPlayer animPlayer, AnimationTrack track) {
        this.sourceAnimPlayer = animPlayer;
        this.track = AnimationTrack.createClone(track, AnimationTrack::alloc);
        SkinningData skinningData = this.sourceAnimPlayer.getSkinningData();
        int numBones = skinningData.numBones();
        this.skinningData = skinningData;
        this.boneModelTransforms = PZArrayUtil.newInstance(Matrix4f.class, this.boneModelTransforms, numBones, Matrix4f::new);
        this.boneTransforms = PZArrayUtil.newInstance(BoneTransform.class, this.boneTransforms, numBones, BoneTransform::alloc);
    }

    public static ModelTransformSampler alloc(AnimationPlayer animationPlayer, AnimationTrack animTrack) {
        ModelTransformSampler newItem = s_pool.alloc();
        newItem.init(animationPlayer, animTrack);
        return newItem;
    }

    @Override
    public void onReleased() {
        this.sourceAnimPlayer = null;
        this.track = Pool.tryRelease(this.track);
        this.skinningData = null;
        this.boneTransforms = Pool.tryRelease(this.boneTransforms);
    }

    @Override
    public float getTotalTime() {
        return this.track.getDuration();
    }

    @Override
    public boolean isLooped() {
        return this.track.isLooping();
    }

    @Override
    public void moveToTime(float time) {
        this.currentTime = time;
        this.track.setCurrentTimeValue(time);
        this.track.Update(0.0f);
        for (int boneIdx = 0; boneIdx < this.boneTransforms.length; ++boneIdx) {
            this.updateBoneAnimationTransform(boneIdx);
        }
    }

    private void updateBoneAnimationTransform(int boneIdx) {
        boolean isDeferredMovementBone;
        Vector3f pos = L_updateBoneAnimationTransform.pos;
        Quaternion rot = L_updateBoneAnimationTransform.rot;
        Vector3f scale = L_updateBoneAnimationTransform.scale;
        Keyframe key = L_updateBoneAnimationTransform.key;
        AnimationBoneBinding crBone = this.sourceAnimPlayer.getCounterRotationBone();
        boolean isCounterRotationBone = crBone != null && crBone.getBone() != null && crBone.getBone().index == boneIdx;
        key.setIdentity();
        AnimationTrack track = this.track;
        this.getTrackTransform(boneIdx, track, pos, rot, scale);
        if (isCounterRotationBone && track.getUseDeferredRotation()) {
            if (DebugOptions.instance.character.debug.animate.zeroCounterRotationBone.getValue()) {
                Vector3f rotAxis = L_updateBoneAnimationTransform.rotAxis;
                Matrix4f rotMat = L_updateBoneAnimationTransform.rotMat;
                rotMat.setIdentity();
                rotAxis.set(0.0f, 1.0f, 0.0f);
                rotMat.rotate(-1.5707964f, rotAxis);
                rotAxis.set(1.0f, 0.0f, 0.0f);
                rotMat.rotate(-1.5707964f, rotAxis);
                HelperFunctions.getRotation(rotMat, rot);
            } else {
                Vector3f rotEulers = HelperFunctions.ToEulerAngles(rot, L_updateBoneAnimationTransform.rotEulers);
                HelperFunctions.ToQuaternion(rotEulers.x, rotEulers.y, 1.5707963705062866, rot);
            }
        }
        boolean bl = isDeferredMovementBone = track.getDeferredMovementBoneIdx() == boneIdx;
        if (isDeferredMovementBone) {
            Vector3f deferredCounterPosition = track.getCurrentDeferredCounterPosition(L_updateBoneAnimationTransform.deferredPos);
            pos.x += deferredCounterPosition.x;
            pos.y += deferredCounterPosition.y;
            pos.z += deferredCounterPosition.z;
        }
        key.position.set(pos);
        key.rotation.set(rot);
        key.scale.set(scale);
        this.boneTransforms[boneIdx].set(key.position, key.rotation, key.scale);
    }

    private void getTrackTransform(int boneIdx, AnimationTrack track, Vector3f pos, Quaternion rot, Vector3f scale) {
        track.get(boneIdx, pos, rot, scale);
    }

    @Override
    public float getCurrentTime() {
        return this.currentTime;
    }

    @Override
    public void getBoneMatrix(int boneIdx, Matrix4f matrix) {
        if (boneIdx == 0) {
            this.boneTransforms[0].getMatrix(this.boneModelTransforms[0]);
            matrix.load(this.boneModelTransforms[0]);
            return;
        }
        SkinningBone bone = this.skinningData.getBoneAt(boneIdx);
        SkinningBone parentBone = bone.parent;
        BoneTransform.mul(this.boneTransforms[bone.index], this.boneModelTransforms[parentBone.index], this.boneModelTransforms[bone.index]);
        matrix.load(this.boneModelTransforms[bone.index]);
    }

    @Override
    public int getNumBones() {
        return this.skinningData.numBones();
    }

    public static class L_updateBoneAnimationTransform {
        public static final Vector3f pos = new Vector3f();
        public static final Quaternion rot = new Quaternion();
        public static final Vector3f scale = new Vector3f();
        public static final Keyframe key = new Keyframe(new Vector3f(0.0f, 0.0f, 0.0f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f));
        public static final Vector3f rotAxis = new Vector3f();
        public static final Matrix4f rotMat = new Matrix4f();
        public static final Vector3f rotEulers = new Vector3f();
        public static final Vector3f deferredPos = new Vector3f();
    }
}

