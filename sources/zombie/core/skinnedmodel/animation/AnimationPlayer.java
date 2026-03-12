/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.math.VectorUtil;
import zombie.core.physics.RagdollController;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationBoneBinding;
import zombie.core.skinnedmodel.animation.AnimationBoneBindingPair;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.AnimatorsBoneTransform;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntries;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntry;
import zombie.core.skinnedmodel.animation.ModelTransformSampler;
import zombie.core.skinnedmodel.animation.StartAnimTrackParameters;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.animation.sharedskele.SharedSkeleAnimationRepository;
import zombie.core.skinnedmodel.animation.sharedskele.SharedSkeleAnimationTrack;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class AnimationPlayer
extends PooledObject {
    private Model model;
    private final Matrix4f propTransforms = new Matrix4f();
    private boolean boneTransformsNeedFirstFrame = true;
    private float boneTransformsTimeDelta = -1.0f;
    public AnimatorsBoneTransform[] boneTransforms;
    private Matrix4f[] modelTransforms;
    private SkinTransformData skinTransformData;
    private SkinTransformData skinTransformDataPool;
    private SkinningData skinningData;
    private AnimationClip ragdollAnimationClip;
    private float ragdollAnimationWeight = 0.0f;
    private SharedSkeleAnimationRepository sharedSkeleAnimationRepo;
    private SharedSkeleAnimationTrack currentSharedTrack;
    private AnimationClip currentSharedTrackClip;
    private float angle;
    private float targetAngle;
    private boolean characterAllowsTwist = true;
    private float twistAngle;
    private float shoulderTwistAngle;
    private float shoulderTwistWeight = 1.0f;
    private float targetTwistAngle;
    private float maxTwistAngle = PZMath.degToRad(70.0f);
    private float excessTwist;
    private static final float angleStepBase = 0.15f;
    public float angleStepDelta = 1.0f;
    public float angleTwistDelta = 1.0f;
    public boolean doBlending = true;
    public boolean updateBones = true;
    private final Vector2 targetDir = new Vector2();
    private final ArrayList<AnimationBoneBindingPair> reparentedBoneBindings = new ArrayList();
    private final List<AnimationBoneBinding> twistBones = new ArrayList<AnimationBoneBinding>();
    private AnimationBoneBinding counterRotationBone;
    public final ArrayList<Integer> dismembered = new ArrayList();
    private final float minimumValidAnimWeight = 0.001f;
    private final LiveAnimationTrackEntries liveAnimationTrackEntries = new LiveAnimationTrackEntries();
    public AnimationPlayer parentPlayer;
    private final Vector2 deferredMovement = new Vector2();
    private final Object deferredMovementLock = new Object();
    private final Vector2 deferredMovementAccum = new Vector2();
    private final Object deferredMovementAccumLock = new Object();
    private final Vector3 deferredMovementFromRagdoll = new Vector3();
    private float deferredRotationWeight;
    private float deferredAngleDelta;
    private final org.joml.Vector3f targetGrapplePos = new org.joml.Vector3f();
    private final Vector2 targetGrappleRotation = new Vector2(1.0f, 0.0f);
    private final org.joml.Vector3f grappleOffset = new org.joml.Vector3f();
    private AnimationPlayerRecorder recorder;
    private static final ThreadLocal<AnimationTrack[]> tempTracks = ThreadLocal.withInitial(() -> new AnimationTrack[0]);
    private static final Vector2 tempo = new Vector2();
    private RagdollController ragdollController;
    private final Vector3f ragdollWorldPosition = new Vector3f();
    private final Quaternion ragdollWorldRotation = new Quaternion();
    private IsoGameCharacter character;
    private static final Pool<AnimationPlayer> s_pool = new Pool<AnimationPlayer>(AnimationPlayer::new);
    private final AnimationMultiTrack multiTrack = new AnimationMultiTrack();

    private AnimationPlayer() {
    }

    public static AnimationPlayer alloc(Model model) {
        AnimationPlayer animPlayer = s_pool.alloc();
        animPlayer.setModel(model);
        return animPlayer;
    }

    public AnimationClip getAnimationClip() {
        return this.currentSharedTrackClip;
    }

    public static float lerpBlendWeight(float from, float to, float fadeTimeTo1) {
        if (PZMath.equal(from, to, 1.0E-4f)) {
            return to;
        }
        float fadeSpeed = 1.0f / fadeTimeTo1;
        float dt = GameTime.getInstance().getTimeDelta();
        float fadeDiff = to - from;
        float fadeDir = PZMath.sign(fadeDiff);
        float newPos = from + fadeDir * fadeSpeed * dt;
        float newDiff = to - newPos;
        float newDir = PZMath.sign(newDiff);
        if (newDir != fadeDir) {
            newPos = to;
        }
        return newPos;
    }

    public void setModel(Model model) {
        Objects.requireNonNull(model);
        if (model == this.model) {
            return;
        }
        this.model = model;
        this.initSkinningData();
    }

    public Model getModel() {
        return this.model;
    }

    public int getNumBones() {
        if (!this.isReady()) {
            return 0;
        }
        return this.boneTransforms.length;
    }

    public AnimatorsBoneTransform getBoneTransformAt(int i) {
        if (i < 0 || this.getNumBones() <= i) {
            throw new IndexOutOfBoundsException("Bone index " + i + " out of range. NumBones:" + this.getNumBones());
        }
        return this.boneTransforms[i];
    }

    public <T extends BoneTransform> T getBoneTransformAt(int i, T result) {
        if (i < 0 || this.getNumBones() <= i) {
            throw new IndexOutOfBoundsException("Bone index " + i + " out of range. NumBones:" + this.getNumBones());
        }
        result.set(this.boneTransforms[i]);
        return result;
    }

    private void initSkinningData() {
        if (this.model == null || !this.model.isReady()) {
            return;
        }
        SkinningData skinningData = (SkinningData)this.model.tag;
        if (skinningData == null) {
            return;
        }
        if (this.skinningData == skinningData) {
            return;
        }
        if (this.skinningData != null) {
            this.skinningData = null;
            this.multiTrack.reset();
        }
        this.skinningData = skinningData;
        Lambda.forEachFrom(PZArrayUtil::forEach, this.reparentedBoneBindings, this.skinningData, AnimationBoneBindingPair::setSkinningData);
        Lambda.forEachFrom(PZArrayUtil::forEach, this.twistBones, this.skinningData, AnimationBoneBinding::setSkinningData);
        if (this.counterRotationBone != null) {
            this.counterRotationBone.setSkinningData(this.skinningData);
        }
        int boneCount = skinningData.numBones();
        this.modelTransforms = PZArrayUtil.newInstance(Matrix4f.class, this.modelTransforms, boneCount, Matrix4f::new);
        this.boneTransforms = PZArrayUtil.newInstance(AnimatorsBoneTransform.class, this.boneTransforms, boneCount, AnimatorsBoneTransform::alloc);
        for (int i = 0; i < boneCount; ++i) {
            if (this.boneTransforms[i] == null) {
                this.boneTransforms[i] = AnimatorsBoneTransform.alloc();
            }
            this.boneTransforms[i].setIdentity();
        }
        this.boneTransformsNeedFirstFrame = true;
    }

    public boolean isReady() {
        this.initSkinningData();
        return this.hasSkinningData();
    }

    public boolean hasSkinningData() {
        return this.skinningData != null;
    }

    public void addBoneReparent(String boneName, String newParentBone) {
        if (PZArrayUtil.contains(this.reparentedBoneBindings, Lambda.predicate(boneName, newParentBone, AnimationBoneBindingPair::matches))) {
            return;
        }
        AnimationBoneBindingPair newBindingPair = new AnimationBoneBindingPair(boneName, newParentBone);
        newBindingPair.setSkinningData(this.skinningData);
        this.reparentedBoneBindings.add(newBindingPair);
    }

    public void setTwistBones(String ... bones) {
        ArrayList<String> boneNames = L_setTwistBones.boneNames;
        PZArrayUtil.listConvert(this.twistBones, boneNames, bone -> bone.boneName);
        if (PZArrayUtil.sequenceEqual(bones, boneNames, PZArrayUtil.Comparators::equalsIgnoreCase)) {
            return;
        }
        this.twistBones.clear();
        Lambda.forEachFrom(PZArrayUtil::forEach, bones, this, (boneName, lThis) -> {
            AnimationBoneBinding binding = new AnimationBoneBinding((String)boneName);
            binding.setSkinningData(lThis.skinningData);
            lThis.twistBones.add(binding);
        });
    }

    public int getNumTwistBones() {
        return this.twistBones.size();
    }

    public AnimatorsBoneTransform getTwistBoneAt(int twistBoneIdx) {
        AnimationBoneBinding twistBoneBinding = this.twistBones.get(twistBoneIdx);
        SkinningBone twistBone = twistBoneBinding.getBone();
        int boneIdx = twistBone.index;
        return this.boneTransforms[boneIdx];
    }

    public String getTwistBoneNameAt(int twistBoneIdx) {
        return this.twistBones.get((int)twistBoneIdx).boneName;
    }

    public void setCounterRotationBone(String boneName) {
        if (this.counterRotationBone == null || StringUtils.equals(this.counterRotationBone.boneName, boneName)) {
            // empty if block
        }
        this.counterRotationBone = new AnimationBoneBinding(boneName);
        this.counterRotationBone.setSkinningData(this.skinningData);
    }

    public AnimationBoneBinding getCounterRotationBone() {
        return this.counterRotationBone;
    }

    public void reset() {
        this.multiTrack.reset();
        this.releaseRagdollController();
    }

    @Override
    public void onReleased() {
        this.model = null;
        this.skinningData = null;
        this.propTransforms.setIdentity();
        this.boneTransformsNeedFirstFrame = true;
        this.boneTransformsTimeDelta = -1.0f;
        this.boneTransforms = (AnimatorsBoneTransform[])IPooledObject.tryReleaseAndBlank((IPooledObject[])this.boneTransforms);
        PZArrayUtil.forEach(this.modelTransforms, Matrix::setIdentity);
        this.resetSkinTransforms();
        this.setAngle(0.0f);
        this.setTargetAngle(0.0f);
        this.twistAngle = 0.0f;
        this.shoulderTwistAngle = 0.0f;
        this.targetTwistAngle = 0.0f;
        this.maxTwistAngle = PZMath.degToRad(70.0f);
        this.excessTwist = 0.0f;
        this.angleStepDelta = 1.0f;
        this.angleTwistDelta = 1.0f;
        this.doBlending = true;
        this.updateBones = true;
        this.targetDir.set(0.0f, 0.0f);
        this.reparentedBoneBindings.clear();
        this.twistBones.clear();
        this.counterRotationBone = null;
        this.dismembered.clear();
        this.liveAnimationTrackEntries.clear();
        this.parentPlayer = null;
        this.deferredMovement.set(0.0f, 0.0f);
        this.deferredMovementAccum.set(0.0f, 0.0f);
        this.deferredMovementFromRagdoll.set(0.0f, 0.0f, 0.0f);
        this.deferredRotationWeight = 0.0f;
        this.deferredAngleDelta = 0.0f;
        this.recorder = null;
        this.multiTrack.reset();
        this.releaseRagdollController();
        this.ragdollAnimationClip = null;
        this.ragdollAnimationWeight = 0.0f;
        this.character = null;
    }

    public SkinningData getSkinningData() {
        return this.skinningData;
    }

    public HashMap<String, Integer> getSkinningBoneIndices() {
        if (this.skinningData != null) {
            return this.skinningData.boneIndices;
        }
        return null;
    }

    public int getSkinningBoneIndex(String boneName, int defaultVal) {
        HashMap<String, Integer> boneIndices = this.getSkinningBoneIndices();
        if (boneIndices != null && boneIndices.containsKey(boneName)) {
            return boneIndices.get(boneName);
        }
        return defaultVal;
    }

    private synchronized SkinTransformData getSkinTransformData(SkinningData skinnedTo) {
        SkinTransformData current = this.skinTransformData;
        while (current != null) {
            if (skinnedTo == current.skinnedTo) {
                return current;
            }
            current = current.next;
        }
        current = this.getOrCreateSkinTransformData(skinnedTo);
        current.next = this.skinTransformData;
        this.skinTransformData = current;
        return current;
    }

    private synchronized SkinTransformData getOrCreateSkinTransformData(SkinningData skinnedTo) {
        SkinTransformData data = this.skinTransformDataPool;
        SkinTransformData prev = null;
        while (data != null) {
            if (data.transforms != null && data.transforms.length == skinnedTo.numBones()) {
                if (prev == null) {
                    this.skinTransformDataPool = data.next;
                } else {
                    prev.next = data.next;
                }
                data.setSkinnedTo(skinnedTo);
                data.dirty = true;
                return data;
            }
            prev = data;
            data = data.next;
        }
        return SkinTransformData.alloc(skinnedTo);
    }

    private synchronized void resetSkinTransforms() {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("resetSkinTransforms");){
            this.resetSkinTransformsInternal();
        }
    }

    private void resetSkinTransformsInternal() {
        if (this.skinTransformDataPool != null) {
            SkinTransformData last = this.skinTransformDataPool;
            while (last.next != null) {
                last = last.next;
            }
            last.next = this.skinTransformData;
        } else {
            this.skinTransformDataPool = this.skinTransformData;
        }
        this.skinTransformData = null;
    }

    public Matrix4f GetPropBoneMatrix(int bone) {
        this.propTransforms.load(this.modelTransforms[bone]);
        return this.propTransforms;
    }

    public AnimationTrack startClip(AnimationClip clip, boolean loop, float ragdollMaxTime) {
        if (clip == null) {
            throw new NullPointerException("Supplied clip is null.");
        }
        AnimationTrack track = AnimationTrack.alloc();
        track.startClip(clip, loop, ragdollMaxTime);
        track.setName(clip.name);
        track.isPlaying = true;
        this.multiTrack.addTrack(track);
        DebugType.AnimationDetailed.debugln("startClip: %s", clip.name);
        return track;
    }

    public static void releaseTracks(List<AnimationTrack> tracks) {
        AnimationTrack[] temp = tempTracks.get();
        AnimationTrack[] tracksToRelease = tracks.toArray(temp);
        PZArrayUtil.forEach(tracksToRelease, PooledObject::release);
    }

    public AnimationTrack play(String animName, boolean looped) {
        return this.play(animName, looped, false, -1.0f);
    }

    public AnimationTrack play(String animName, boolean looped, boolean isRagdoll, float ragdollMaxTime) {
        if (!this.isReady()) {
            DebugType.Animation.warn("AnimationPlayer is not ready. Cannot play animation: %s%s", animName, isRagdoll ? "(Ragdoll)" : "");
            return null;
        }
        if (this.skinningData == null) {
            DebugType.Animation.warn("Skinning Data not found. AnimName: %s%s", animName, isRagdoll ? "(Ragdoll)" : "");
            return null;
        }
        AnimationClip chosenClip = isRagdoll ? this.getOrCreateRagdollAnimationClip() : this.skinningData.animationClips.get(animName);
        if (chosenClip == null) {
            DebugType.Animation.warn("Anim Clip %snot found: %s", isRagdoll ? "(Ragdoll)" : "", animName);
            return null;
        }
        return this.startClip(chosenClip, looped, ragdollMaxTime);
    }

    public AnimationTrack play(StartAnimTrackParameters params, AnimLayer animLayer) {
        AnimationTrack track = this.play(params.animName, params.isLooped, params.isRagdoll, params.ragdollMaxTime);
        if (track == null) {
            return null;
        }
        track.isPrimary = params.isPrimary;
        SkinningData skinningData = this.getSkinningData();
        if (animLayer.isSubLayer()) {
            track.setBoneWeights(params.subLayerBoneWeights);
            track.initBoneWeights(skinningData);
        } else {
            track.setBoneWeights(null);
        }
        SkinningBone deferredBone = skinningData.getBone(params.deferredBoneName);
        if (deferredBone == null) {
            DebugType.Animation.error("Deferred bone not found: \"%s\"", params.deferredBoneName);
        }
        track.setSpeedDelta(params.speedScale);
        track.syncTrackingEnabled = params.syncTrackingEnabled;
        track.trackTimeToVariable = params.trackTimeToVariable;
        track.setDeferredBone(deferredBone, params.deferredBoneAxis);
        track.setUseDeferredRotation(params.useDeferredRotation);
        track.setDeferredRotationScale(params.deferredRotationScale);
        track.setBlendWeight(params.initialWeight);
        track.reverse = params.isReversed;
        track.priority = params.priority;
        track.ragdollStartTime = params.ragdollStartTime;
        track.setMatchingGrappledAnimNode(params.matchingGrappledAnimNode);
        track.setAnimLayer(animLayer);
        return track;
    }

    public AnimationClip getOrCreateRagdollAnimationClip() {
        if (!this.isReady()) {
            return null;
        }
        SkinningBoneHierarchy skeletonBoneHierarchy = this.getSkeletonBoneHierarchy();
        int numberOfBones = skeletonBoneHierarchy.numBones();
        if (this.ragdollAnimationClip == null) {
            ArrayList<Keyframe> keyframeList = new ArrayList<Keyframe>();
            for (int i = 0; i < numberOfBones; ++i) {
                SkinningBone bone = skeletonBoneHierarchy.getBoneAt(i);
                int boneIndex = bone.index;
                Keyframe keyframe = new Keyframe();
                keyframe.none = boneIndex;
                keyframe.time = 0.0f;
                keyframe.position = new Vector3f();
                keyframe.rotation = new Quaternion();
                keyframe.scale = new Vector3f();
                keyframeList.add(keyframe);
                keyframe = new Keyframe();
                keyframe.none = boneIndex;
                keyframe.time = 1.0f;
                keyframe.position = new Vector3f();
                keyframe.rotation = new Quaternion();
                keyframe.scale = new Vector3f();
                keyframeList.add(keyframe);
            }
            this.ragdollAnimationClip = new AnimationClip(1.0f, keyframeList, "RagdollAnimationClip", true, true);
        }
        return this.ragdollAnimationClip;
    }

    public SkinningBoneHierarchy getSkeletonBoneHierarchy() {
        if (!this.isReady()) {
            return null;
        }
        return this.getSkinningData().getSkeletonBoneHierarchy();
    }

    public void Update() {
        this.Update(GameTime.instance.getTimeDelta());
    }

    public void Update(float deltaT) {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("AnimationPlayer.Update");){
            this.updateInternal(deltaT);
        }
    }

    private void updateInternal(float deltaT) {
        if (!this.isReady()) {
            return;
        }
        this.updateRagdoll(deltaT);
        this.multiTrack.Update(deltaT);
        if (!this.updateBones) {
            this.updateAnimation_NonVisualOnly(deltaT);
            return;
        }
        if (this.multiTrack.getTrackCount() <= 0) {
            return;
        }
        SharedSkeleAnimationTrack sharedSkeleTrack = this.determineCurrentSharedSkeleTrack();
        if (sharedSkeleTrack != null) {
            float trackTime = this.multiTrack.getTrackAt(0).getCurrentTrackTime();
            this.updateAnimation_SharedSkeleTrack(sharedSkeleTrack, deltaT, trackTime);
            return;
        }
        this.updateAnimation_StandardAnimation(deltaT);
        this.postUpdateRagdoll(deltaT);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private SharedSkeleAnimationTrack determineCurrentSharedSkeleTrack() {
        if (this.isRagdolling()) {
            return null;
        }
        if (this.sharedSkeleAnimationRepo == null) {
            return null;
        }
        if (this.doBlending) {
            return null;
        }
        if (!DebugOptions.instance.animation.sharedSkeles.enabled.getValue()) {
            return null;
        }
        if (this.multiTrack.getTrackCount() != 1) {
            return null;
        }
        if (!PZMath.equal(this.twistAngle, 0.0f, 114.59155f)) {
            return null;
        }
        if (this.parentPlayer != null) {
            return null;
        }
        AnimationTrack animTrack = this.multiTrack.getTrackAt(0);
        if (animTrack.isRagdoll()) {
            return null;
        }
        float trackWeight = animTrack.getBlendFieldWeight();
        if (!PZMath.equal(trackWeight, 0.0f, 0.1f)) {
            return null;
        }
        AnimationClip clip = animTrack.getClip();
        if (clip == this.currentSharedTrackClip) {
            return this.currentSharedTrack;
        }
        SharedSkeleAnimationTrack sharedTrack = this.sharedSkeleAnimationRepo.getTrack(clip);
        if (sharedTrack == null) {
            DebugType.Animation.debugln("Caching SharedSkeleAnimationTrack: %s", animTrack.getName());
            sharedTrack = new SharedSkeleAnimationTrack();
            ModelTransformSampler sampler = ModelTransformSampler.alloc(this, animTrack);
            try {
                sharedTrack.set(sampler, 5.0f);
            }
            finally {
                sampler.release();
            }
            this.sharedSkeleAnimationRepo.setTrack(clip, sharedTrack);
        }
        this.currentSharedTrackClip = clip;
        this.currentSharedTrack = sharedTrack;
        return sharedTrack;
    }

    private void updateAnimation_NonVisualOnly(float deltaT) {
        this.updateMultiTrackBoneTransforms_DeferredMovementOnly();
        this.DoAngles(deltaT);
        this.calculateDeferredMovement();
    }

    public void setSharedAnimRepo(SharedSkeleAnimationRepository repo) {
        this.sharedSkeleAnimationRepo = repo;
    }

    private void updateAnimation_SharedSkeleTrack(SharedSkeleAnimationTrack sharedSkeleTrack, float deltaT, float trackTime) {
        this.updateMultiTrackBoneTransforms_DeferredMovementOnly();
        this.DoAngles(deltaT);
        this.calculateDeferredMovement();
        sharedSkeleTrack.moveToTime(trackTime);
        for (int boneIdx = 0; boneIdx < this.modelTransforms.length; ++boneIdx) {
            sharedSkeleTrack.getBoneMatrix(boneIdx, this.modelTransforms[boneIdx]);
        }
        this.UpdateSkinTransforms();
    }

    private void updateAnimation_StandardAnimation(float deltaT) {
        if (this.parentPlayer == null) {
            this.updateMultiTrackBoneTransforms(deltaT);
        } else {
            this.copyBoneTransformsFromParentPlayer();
        }
        this.DoAngles(deltaT);
        this.calculateDeferredMovement();
        this.updateTwistBone();
        this.applyBoneReParenting();
        this.updateModelTransforms();
        this.UpdateSkinTransforms();
    }

    private void updateRagdoll(float deltaT) {
        if (!this.updateBones) {
            this.releaseRagdollController();
            return;
        }
        if (!this.canRagdoll()) {
            this.releaseRagdollController();
            return;
        }
        if (!this.multiTrack.containsAnyRagdollTracks()) {
            this.releaseRagdollController();
            return;
        }
        AnimationTrack ragdollTrack = this.multiTrack.getActiveRagdollTrack();
        if (ragdollTrack == null) {
            this.releaseRagdollController();
            return;
        }
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("AnimationPlayer.updateRagdoll");){
            this.updateRagdollInternal(deltaT);
        }
    }

    private void postUpdateRagdoll(float deltaT) {
        if (!this.updateBones) {
            this.releaseRagdollController();
            return;
        }
        if (this.getIsoGameCharacter() == null) {
            this.releaseRagdollController();
            return;
        }
        if (!this.multiTrack.containsAnyRagdollTracks()) {
            this.releaseRagdollController();
            return;
        }
        AnimationTrack ragdollTrack = this.multiTrack.getActiveRagdollTrack();
        if (ragdollTrack == null) {
            this.releaseRagdollController();
            return;
        }
        this.postUpdateRagdollInternal(deltaT);
    }

    private void updateRagdollInternal(float deltaT) {
        RagdollController ragdollController = this.getOrCreateRagdollController();
        if (ragdollController == null) {
            return;
        }
        if (this.isBoneTransformsNeedFirstFrame()) {
            return;
        }
        if (this.multiTrack.anyRagdollFirstFrame()) {
            DebugType.Animation.debugln("Initiating radgoll first-frames to boneTransforms...");
            this.multiTrack.initRagdollTransforms(this.boneTransforms, false);
        }
        this.updateTotalRagdollWeight();
        this.deferredMovementFromRagdoll.set(0.0f, 0.0f, 0.0f);
        if (ragdollController.isFirstFrame()) {
            DebugType.Animation.debugln("Initiating radgoll first-frames to boneTransforms...");
            this.multiTrack.initRagdollTransforms(this.boneTransforms, true);
        }
        if (this.character != null && this.isSimulationDirectionCalculated() && this.isFullyRagdolling()) {
            this.calculateDeferredMovementFromRagdolls(this.deferredMovementFromRagdoll);
            this.character.doDeferredMovementFromRagdoll(this.deferredMovementFromRagdoll);
        }
        ragdollController.update(deltaT, this.ragdollWorldPosition, this.ragdollWorldRotation);
    }

    private void updateTotalRagdollWeight() {
        float blendWeight;
        this.ragdollAnimationWeight = 0.0f;
        float remainingWeight = 1.0f;
        for (int animBlendIdx = this.liveAnimationTrackEntries.count() - 1; animBlendIdx >= 0 && remainingWeight > 0.0f; remainingWeight -= blendWeight, --animBlendIdx) {
            LiveAnimationTrackEntry liveTrackEntry = this.liveAnimationTrackEntries.get(animBlendIdx);
            blendWeight = liveTrackEntry.getBlendWeight();
            if (!liveTrackEntry.getTrack().isRagdoll()) continue;
            this.ragdollAnimationWeight = PZMath.clamp(this.ragdollAnimationWeight + blendWeight, 0.0f, 1.0f);
        }
    }

    private void postUpdateRagdollInternal(float deltaT) {
        RagdollController ragdollController = this.getRagdollController();
        if (ragdollController == null) {
            return;
        }
        ragdollController.postUpdate(deltaT);
    }

    private void copyBoneTransformsFromParentPlayer() {
        this.boneTransformsNeedFirstFrame = false;
        for (int n = 0; n < this.boneTransforms.length; ++n) {
            this.boneTransforms[n].set(this.parentPlayer.boneTransforms[n]);
        }
    }

    public static float calculateAnimPlayerAngle(float dirX, float dirY) {
        return Vector2.getDirection(dirX, dirY);
    }

    public void setTargetDirection(float dirX, float dirY) {
        if (this.targetDir.x != dirX || this.targetDir.y != dirY) {
            this.setTargetAngle(AnimationPlayer.calculateAnimPlayerAngle(dirX, dirY));
            this.targetTwistAngle = PZMath.getClosestAngle(this.angle, this.targetAngle);
            float targetTwistClamped = PZMath.clamp(this.targetTwistAngle, -this.maxTwistAngle, this.maxTwistAngle);
            this.excessTwist = PZMath.getClosestAngle(targetTwistClamped, this.targetTwistAngle);
            this.targetDir.set(dirX, dirY);
        }
    }

    public void setTargetAndCurrentDirection(Vector2 dir) {
        this.setTargetAndCurrentDirection(dir.x, dir.y);
    }

    public void setTargetAndCurrentDirection(float dirX, float dirY) {
        this.setTargetAngle(AnimationPlayer.calculateAnimPlayerAngle(dirX, dirY));
        this.setAngleToTarget();
        this.targetTwistAngle = 0.0f;
        this.targetDir.set(dirX, dirY);
    }

    public void updateForwardDirection(IsoGameCharacter character) {
        if (character != null) {
            this.setTargetDirection(character.getForwardDirectionX(), character.getForwardDirectionY());
            this.characterAllowsTwist = character.allowsTwist();
            this.shoulderTwistWeight = character.getShoulderTwistWeight();
        }
    }

    public void updateVerticalAimAngle(IsoGameCharacter character) {
        float targetAngle;
        if (character == null) {
            return;
        }
        float prevAngle = character.getCurrentVerticalAimAngle() * ((float)Math.PI / 180);
        if (PZMath.equal(prevAngle, targetAngle = character.getTargetVerticalAimAngle() * ((float)Math.PI / 180), 0.01f)) {
            return;
        }
        float deltaT = GameTime.instance.getTimeDelta();
        float angleStepBase = 0.08f;
        float angleScaledStepBase = 0.08f * GameTime.instance.getMultiplierFromTimeDelta(deltaT);
        float diff = PZMath.getClosestAngle(prevAngle, targetAngle);
        if (PZMath.equal(diff, 0.0f, 0.001f)) {
            character.setCurrentVerticalAimAngle(targetAngle * 57.295776f);
            return;
        }
        float diffSign = PZMath.sign(diff);
        float angleStep = angleScaledStepBase * diffSign;
        float stepSign = PZMath.sign(angleStep);
        float nextAngleUnclamped = prevAngle + angleStep;
        float newDiffUnclamped = PZMath.getClosestAngle(nextAngleUnclamped, targetAngle);
        float newDiffUnclampedSign = PZMath.sign(newDiffUnclamped);
        if (newDiffUnclampedSign == diffSign || stepSign != diffSign) {
            character.setCurrentVerticalAimAngle(nextAngleUnclamped * 57.295776f);
        } else {
            character.setCurrentVerticalAimAngle(targetAngle * 57.295776f);
        }
    }

    public void DoAngles(float deltaT) {
        if (!this.isRagdolling()) {
            try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("AnimationPlayer.doAngles");){
                this.doAnglesInternal(deltaT);
            }
        }
    }

    private void doAnglesInternal(float deltaT) {
        float angleScaledStepBase = 0.15f * GameTime.instance.getMultiplierFromTimeDelta(deltaT);
        angleScaledStepBase = PZMath.min(angleScaledStepBase, (float)Math.PI);
        this.interpolateBodyAngle(angleScaledStepBase);
        this.interpolateBodyTwist(angleScaledStepBase);
        this.interpolateShoulderTwist(angleScaledStepBase);
    }

    private void interpolateBodyAngle(float angleScaledStepBase) {
        float targetAngle = this.targetAngle;
        float diff = PZMath.getClosestAngle(this.angle, targetAngle);
        if (PZMath.equal(diff, 0.0f, 0.001f)) {
            this.setAngleToTarget();
            this.targetTwistAngle = 0.0f;
            return;
        }
        float diffSign = PZMath.sign(diff);
        float angleStepUndeferred = angleScaledStepBase * diffSign * this.angleStepDelta;
        float angleStep = DebugOptions.instance.character.debug.animate.deferredRotationsOnly.getValue() ? this.deferredAngleDelta : (this.deferredRotationWeight > 0.0f ? this.deferredAngleDelta * this.deferredRotationWeight + angleStepUndeferred * (1.0f - this.deferredRotationWeight) : angleStepUndeferred);
        float stepSign = PZMath.sign(angleStep);
        float prevAngle = this.angle;
        float nextAngleUnclamped = prevAngle + angleStep;
        float newDiffUnclamped = PZMath.getClosestAngle(nextAngleUnclamped, targetAngle);
        float newDiffUnclampedSign = PZMath.sign(newDiffUnclamped);
        if (newDiffUnclampedSign == diffSign || stepSign != diffSign) {
            this.setAngle(nextAngleUnclamped);
            this.targetTwistAngle = newDiffUnclamped;
        } else {
            this.setAngleToTarget();
            this.targetTwistAngle = 0.0f;
        }
    }

    private void interpolateBodyTwist(float angleScaledStepBase) {
        float targetTwistUnclamped = PZMath.wrap(this.targetTwistAngle, (float)(-Math.PI), (float)Math.PI);
        float targetTwist = PZMath.clamp(targetTwistUnclamped, -this.maxTwistAngle, this.maxTwistAngle);
        this.excessTwist = PZMath.getClosestAngle(targetTwist, targetTwistUnclamped);
        float twistDiff = PZMath.getClosestAngle(this.twistAngle, targetTwist);
        if (PZMath.equal(twistDiff, 0.0f, 0.001f)) {
            this.twistAngle = targetTwist;
            return;
        }
        float prevTwist = this.twistAngle;
        float twistDiffSign = PZMath.sign(twistDiff);
        float twistAngleStep = angleScaledStepBase * twistDiffSign * PZMath.abs(this.angleTwistDelta);
        float nextTwistUnclamped = prevTwist + twistAngleStep;
        float newDiffUnclamped = PZMath.getClosestAngle(nextTwistUnclamped, targetTwist);
        float newDiffUnclampedSign = PZMath.sign(newDiffUnclamped);
        this.twistAngle = newDiffUnclampedSign == twistDiffSign ? nextTwistUnclamped : targetTwist;
    }

    private void interpolateShoulderTwist(float angleScaledStepBase) {
        float targetTwist = PZMath.wrap(this.twistAngle, (float)(-Math.PI), (float)Math.PI);
        float twistDiff = PZMath.getClosestAngle(this.shoulderTwistAngle, targetTwist);
        if (PZMath.equal(twistDiff, 0.0f, 0.001f)) {
            this.shoulderTwistAngle = targetTwist;
            return;
        }
        float prevTwist = this.shoulderTwistAngle;
        float twistDiffSign = PZMath.sign(twistDiff);
        float twistAngleStep = angleScaledStepBase * twistDiffSign * PZMath.abs(this.angleTwistDelta) * 0.55f;
        float nextTwistUnclamped = prevTwist + twistAngleStep;
        float newDiffUnclamped = PZMath.getClosestAngle(nextTwistUnclamped, targetTwist);
        float newDiffUnclampedSign = PZMath.sign(newDiffUnclamped);
        this.shoulderTwistAngle = newDiffUnclampedSign == twistDiffSign ? nextTwistUnclamped : targetTwist;
    }

    private void updateTwistBone() {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("updateTwistBone");){
            this.updateTwistBoneInternal();
        }
    }

    private void updateTwistBoneInternal() {
        Vector2 dir;
        if (this.twistBones.isEmpty()) {
            return;
        }
        if (DebugOptions.instance.character.debug.animate.noBoneTwists.getValue()) {
            return;
        }
        if (!this.characterAllowsTwist) {
            return;
        }
        int count = this.twistBones.size();
        int headBoneIdx = count - 1;
        int shoulderBoneIdx = PZArrayUtil.indexOf(this.twistBones, "Bip01_Spine1", (boneName, bone) -> StringUtils.equalsIgnoreCase(bone.boneName, boneName));
        if (shoulderBoneIdx < 0) {
            shoulderBoneIdx = headBoneIdx - 2;
        }
        if (shoulderBoneIdx < 0) {
            shoulderBoneIdx = headBoneIdx;
        }
        float shoulderTwistAngle = this.shoulderTwistAngle;
        if (DebugOptions.instance.character.debug.animate.alwaysAimTwist.getValue() && (dir = IsoPlayer.getInstance().getAimVector(new Vector2())).getLengthSquared() > 1.0E-4f) {
            float worldAngle = AnimationPlayer.calculateAnimPlayerAngle(dir.x, dir.y);
            shoulderTwistAngle = PZMath.getClosestAngle(this.angle, worldAngle);
            shoulderTwistAngle = PZMath.clamp(shoulderTwistAngle, -this.maxTwistAngle, this.maxTwistAngle);
        }
        float headTwistAngle = shoulderTwistAngle;
        SkinningBone headBone = this.twistBones.get(headBoneIdx).getBone();
        Quaternion twistTurnAdjustRot = this.calculateDesiredTwist(headBone, headTwistAngle, L_applyTwistBone.twistTurnAdjustRot);
        Quaternion twistTurnIdentity = L_applyTwistBone.twistTurnIdentity;
        twistTurnIdentity.setIdentity();
        float twistWeightDelta = this.shoulderTwistWeight / (float)(count - 1);
        Quaternion twistTurnStep = L_applyTwistBone.twistTurnStep;
        PZMath.slerp(twistTurnStep, twistTurnIdentity, twistTurnAdjustRot, twistWeightDelta);
        for (int i = 0; i < headBoneIdx; ++i) {
            SkinningBone twistBone = this.twistBones.get(i).getBone();
            this.applyTwistBone(twistBone, twistTurnStep);
        }
        if (this.isAiming()) {
            SkinningBone shoulderBone = this.twistBones.get(shoulderBoneIdx).getBone();
            this.applyTwistBone(shoulderBone, twistTurnStep);
        } else {
            this.applyTwistBone(headBone, twistTurnStep);
        }
    }

    private boolean isAiming() {
        IsoGameCharacter character = this.getIsoGameCharacter();
        if (character != null) {
            return character.isAiming();
        }
        return false;
    }

    private void applyTwistBone(SkinningBone twistBone, Quaternion twistRot) {
        if (twistBone == null) {
            return;
        }
        int boneIndex = twistBone.index;
        int parentBoneIndex = twistBone.parent.index;
        Matrix4f twistParentBoneTrans = this.getBoneModelTransform(parentBoneIndex, L_applyTwistBone.twistParentBoneTrans);
        Matrix4f twistParentBoneTransInv = Matrix4f.invert(twistParentBoneTrans, L_applyTwistBone.twistParentBoneTransInv);
        if (twistParentBoneTransInv == null) {
            return;
        }
        Matrix4f twistBoneModelTrans = this.getBoneModelTransform(boneIndex, L_applyTwistBone.twistBoneTrans);
        Vector3f twistBonePos = HelperFunctions.getPosition(twistBoneModelTrans, L_applyTwistBone.twistBonePos);
        Matrix4f twistBoneNewTrans = L_applyTwistBone.twistBoneNewTrans;
        twistBoneNewTrans.load(twistBoneModelTrans);
        HelperFunctions.setPosition(twistBoneNewTrans, 0.0f, 0.0f, 0.0f);
        Matrix4f twistBoneAdjustTrans = L_applyTwistBone.twistBoneAdjustTrans;
        twistBoneAdjustTrans.setIdentity();
        HelperFunctions.CreateFromQuaternion(twistRot, twistBoneAdjustTrans);
        Matrix4f.mul(twistBoneNewTrans, twistBoneAdjustTrans, twistBoneNewTrans);
        HelperFunctions.setPosition(twistBoneNewTrans, twistBonePos);
        this.boneTransforms[boneIndex].twist = PZMath.wrap(HelperFunctions.getRotationY(twistBoneNewTrans) - (float)Math.PI, (float)(-Math.PI), (float)Math.PI);
        this.boneTransforms[boneIndex].mul(twistBoneNewTrans, twistParentBoneTransInv);
    }

    private Quaternion calculateDesiredTwist(SkinningBone twistBone, float twistAngle, Quaternion twistRot) {
        if (twistBone == null) {
            return twistRot.setIdentity();
        }
        int boneIndex = twistBone.index;
        int parentBoneIndex = twistBone.parent.index;
        Matrix4f twistParentBoneTrans = this.getBoneModelTransform(parentBoneIndex, L_applyTwistBone.twistParentBoneTrans);
        Matrix4f twistParentBoneTransInv = Matrix4f.invert(twistParentBoneTrans, L_applyTwistBone.twistParentBoneTransInv);
        if (twistParentBoneTransInv == null) {
            return twistRot.setIdentity();
        }
        Matrix4f twistBoneModelTrans = this.getBoneModelTransform(boneIndex, L_applyTwistBone.twistBoneTrans);
        Matrix4f twistBoneNewTrans = L_applyTwistBone.twistBoneNewTrans;
        twistBoneNewTrans.load(twistBoneModelTrans);
        Vector3f desiredForward = L_applyTwistBone.desiredForward;
        desiredForward.set(0.0f, 0.0f, 1.0f);
        HelperFunctions.transform(HelperFunctions.setFromAxisAngle(0.0f, 1.0f, 0.0f, twistAngle, L_applyTwistBone.twistTurnRot), desiredForward, desiredForward);
        Vector3f currentForward = L_applyTwistBone.forward;
        currentForward.set(0.0f, 0.0f, -1.0f);
        HelperFunctions.transformVector(twistBoneNewTrans, currentForward, currentForward);
        currentForward.y = 0.0f;
        currentForward.normalise();
        Vector3f twistRotateAxis = L_applyTwistBone.twistRotateAxis;
        Vector3f.cross(desiredForward, currentForward, twistRotateAxis);
        twistRotateAxis.normalise();
        float dotAngle = Vector3f.dot(desiredForward, currentForward);
        float dotAngleClamped = PZMath.clamp(dotAngle, -1.0f, 1.0f);
        float twistRotateAngle = PZMath.acosf(dotAngleClamped);
        HelperFunctions.setFromAxisAngle(twistRotateAxis.x, twistRotateAxis.y, twistRotateAxis.z, -twistRotateAngle, twistRot);
        return twistRot;
    }

    public void resetBoneModelTransforms() {
        if (this.skinningData == null || this.modelTransforms == null) {
            return;
        }
        this.boneTransformsNeedFirstFrame = true;
        this.boneTransformsTimeDelta = -1.0f;
        int boneCount = this.boneTransforms.length;
        for (int boneIdx = 0; boneIdx < boneCount; ++boneIdx) {
            this.boneTransforms[boneIdx].reset();
            this.modelTransforms[boneIdx].setIdentity();
        }
    }

    public boolean isBoneTransformsNeedFirstFrame() {
        return this.boneTransformsNeedFirstFrame;
    }

    private void updateMultiTrackBoneTransforms(float timeDelta) {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("updateMultiTrackBoneTransforms");){
            this.updateMultiTrackBoneTransformsInternal(timeDelta);
        }
    }

    private void updateMultiTrackBoneTransformsInternal(float timeDelta) {
        int boneIdx;
        this.boneTransformsTimeDelta = timeDelta;
        for (boneIdx = 0; boneIdx < this.boneTransforms.length; ++boneIdx) {
            AnimatorsBoneTransform boneTransform = this.boneTransforms[boneIdx];
            boneTransform.nextFrame(timeDelta);
        }
        for (boneIdx = 0; boneIdx < this.modelTransforms.length; ++boneIdx) {
            this.modelTransforms[boneIdx].setIdentity();
        }
        this.updateLayerBlendWeightings();
        if (this.liveAnimationTrackEntries.count() == 0) {
            return;
        }
        if (this.isRecording()) {
            this.recorder.logAnimWeights(this.liveAnimationTrackEntries, this.deferredMovement, this.deferredMovementFromRagdoll);
        }
        for (boneIdx = 0; boneIdx < this.boneTransforms.length; ++boneIdx) {
            if (this.isBoneReparented(boneIdx)) continue;
            this.updateBoneAnimationTransform(boneIdx, null);
        }
        this.boneTransformsNeedFirstFrame = false;
    }

    private void updateLayerBlendWeightings() {
        List<AnimationTrack> tracks = this.multiTrack.getTracks();
        this.liveAnimationTrackEntries.setTracks(tracks, 0.001f, this.boneTransformsNeedFirstFrame);
    }

    private void calculateDeferredMovement() {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("calculateDeferredMovement");){
            this.calculateDeferredMovementInternal();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void calculateDeferredMovementInternal() {
        Object object = this.deferredMovementAccumLock;
        synchronized (object) {
            this.calculateDeferredMovementAccumInternal(this.deferredMovementAccum);
            this.pushDeferredMovementAccumToDeferredMovement();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void pushDeferredMovementAccumToDeferredMovement() {
        Object object = this.deferredMovementLock;
        synchronized (object) {
            this.deferredMovement.set(this.deferredMovementAccum);
        }
    }

    private void calculateDeferredMovementAccumInternal(Vector2 deferredMovementAccum) {
        this.deferredAngleDelta = 0.0f;
        this.deferredRotationWeight = 0.0f;
        float remainingWeight = 1.0f;
        for (int animBlendTrackIdx = this.liveAnimationTrackEntries.count() - 1; animBlendTrackIdx >= 0 && !(remainingWeight <= 0.001f); --animBlendTrackIdx) {
            float rawAnimWeight;
            float boneWeight;
            LiveAnimationTrackEntry liveTrackEntry = this.liveAnimationTrackEntries.get(animBlendTrackIdx);
            AnimationTrack track = liveTrackEntry.getTrack();
            if (track.isFinished() || (boneWeight = track.getDeferredBoneWeight()) <= 0.001f || (rawAnimWeight = liveTrackEntry.getBlendWeight() * boneWeight) <= 0.001f) continue;
            float animWeight = PZMath.clamp(rawAnimWeight, 0.0f, remainingWeight);
            remainingWeight -= rawAnimWeight;
            remainingWeight = org.joml.Math.max(0.0f, remainingWeight);
            if (track.isRagdoll()) continue;
            if (track.getUseDeferredMovement()) {
                Vector2.addScaled(deferredMovementAccum, track.getDeferredMovementDiff(tempo), animWeight, deferredMovementAccum);
            }
            if (!track.getUseDeferredRotation()) continue;
            this.deferredAngleDelta += track.getDeferredRotationDiff() * animWeight;
            this.deferredRotationWeight += animWeight;
        }
        this.applyRotationToDeferredMovement(deferredMovementAccum);
        deferredMovementAccum.x *= AdvancedAnimator.motionScale;
        deferredMovementAccum.y *= AdvancedAnimator.motionScale;
        this.deferredAngleDelta *= AdvancedAnimator.rotationScale;
        this.targetGrapplePos.x += deferredMovementAccum.x;
        this.targetGrapplePos.y += deferredMovementAccum.y;
    }

    private float calculateDeferredMovementFromRagdolls(Vector3 deferredMovement) {
        float ragdollWeight = 0.0f;
        float remainingWeight = 1.0f;
        for (int animBlendTrackIdx = this.liveAnimationTrackEntries.count() - 1; animBlendTrackIdx >= 0 && !(remainingWeight <= 0.001f); --animBlendTrackIdx) {
            float rawAnimWeight;
            float boneWeight;
            LiveAnimationTrackEntry liveTrackEntry = this.liveAnimationTrackEntries.get(animBlendTrackIdx);
            AnimationTrack track = liveTrackEntry.getTrack();
            if (track.isFinished() || (boneWeight = track.getDeferredBoneWeight()) <= 0.001f || (rawAnimWeight = liveTrackEntry.getBlendWeight() * boneWeight) <= 0.001f) continue;
            float animWeight = PZMath.clamp(rawAnimWeight, 0.0f, remainingWeight);
            remainingWeight -= rawAnimWeight;
            remainingWeight = org.joml.Math.max(0.0f, remainingWeight);
            if (!track.isRagdoll()) continue;
            ragdollWeight += animWeight;
        }
        deferredMovement.set(0.0f, 0.0f, 0.0f);
        ragdollWeight = PZMath.clamp(ragdollWeight, 0.0f, 1.0f) * 0.5f;
        if (this.character != null && this.isSimulationDirectionCalculated()) {
            float lerpedAngle;
            float deferredWeight = PZMath.clamp(ragdollWeight * GameTime.getInstance().getMultiplier(), 0.0f, 1.0f);
            RagdollController ragdollController = this.getRagdollController();
            deferredMovement.x = (ragdollController.getDesiredCharacterPositionX() - this.character.getX()) * deferredWeight;
            deferredMovement.y = (ragdollController.getDesiredCharacterPositionY() - this.character.getY()) * deferredWeight;
            deferredMovement.z = (ragdollController.getDesiredCharacterPositionZ() - this.character.getZ()) * deferredWeight;
            float sourceAngle = this.targetAngle;
            float simulationAngle = ragdollController.getCalculatedSimulationDirectionAngle();
            float angleDiff = PZMath.getClosestAngle(sourceAngle, simulationAngle);
            this.targetAngle = lerpedAngle = PZMath.lerpAngle(sourceAngle, simulationAngle, deferredWeight);
            this.setAngleToTarget();
            float simulationCharacterForwardAngle = this.getRagdollController().getSimulationCharacterForwardAngle();
            if (this.isRecording()) {
                this.recorder.logVariable("anm_simulationCharacterForwardAngle", simulationCharacterForwardAngle * 57.295776f);
                this.recorder.logVariable("anm_sourceAngle", sourceAngle * 57.295776f);
                this.recorder.logVariable("anm_simulationAngle", simulationAngle * 57.295776f);
                this.recorder.logVariable("anm_angleDiff", angleDiff * 57.295776f);
                this.recorder.logVariable("anm_ragdollWeight", ragdollWeight);
                this.recorder.logVariable("anm_lerpedAngle", lerpedAngle * 57.295776f);
                this.recorder.logVariable("anm_newTargetAngle", this.targetAngle * 57.295776f);
                this.recorder.logVariable("anm_deferredMovement.x", deferredMovement.x);
                this.recorder.logVariable("anm_deferredMovement.y", deferredMovement.y);
                this.recorder.logVariable("anm_deferredMovement.z", deferredMovement.z);
            }
        }
        return ragdollWeight;
    }

    private boolean isSimulationDirectionCalculated() {
        return this.isRagdolling() && this.getRagdollController().isSimulationDirectionCalculated();
    }

    private boolean isSimulationActive() {
        return this.isRagdolling() && this.getRagdollController().isSimulationActive();
    }

    private void applyRotationToDeferredMovement(Vector2 result) {
        float angle = this.getRenderedAngle();
        AnimationPlayer.applyRotationToDeferredMovement(result, angle);
    }

    private static void applyRotationToDeferredMovement(Vector2 result, float angle) {
        float len = result.normalize();
        result.rotate(angle);
        result.setLength(-len);
    }

    private void applyBoneReParenting() {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("applyBoneReParenting");){
            this.applyBoneReParentingInternal();
        }
    }

    private void applyBoneReParentingInternal() {
        int reparentCount = this.reparentedBoneBindings.size();
        for (int reparentIdx = 0; reparentIdx < reparentCount; ++reparentIdx) {
            AnimationBoneBindingPair reparentPair = this.reparentedBoneBindings.get(reparentIdx);
            if (!reparentPair.isValid()) {
                DebugType.Animation.warn("Animation binding pair is not valid: %s", reparentPair);
                continue;
            }
            this.updateBoneAnimationTransform(reparentPair.getBoneIdxA(), reparentPair);
        }
    }

    private void updateBoneAnimationTransform(int boneIdx, AnimationBoneBindingPair reparentPair) {
        this.updateBoneAnimationTransform_Internal(boneIdx, reparentPair);
    }

    private void updateBoneAnimationTransform_Internal(int boneIdx, AnimationBoneBindingPair reparentPair) {
        Vector3f pos = L_updateBoneAnimationTransform.pos;
        Quaternion rot = L_updateBoneAnimationTransform.rot;
        Vector3f scale = L_updateBoneAnimationTransform.scale;
        Keyframe key = L_updateBoneAnimationTransform.key;
        int totalAnimBlendCount = this.liveAnimationTrackEntries.count();
        AnimationBoneBinding crBone = this.counterRotationBone;
        boolean isCounterRotationBone = crBone != null && crBone.getBone() != null && crBone.getBone().index == boneIdx;
        key.setIdentity();
        float totalWeight = 0.0f;
        boolean isFirst = true;
        float remainingWeight = 1.0f;
        for (int animBlendIdx = totalAnimBlendCount - 1; animBlendIdx >= 0 && remainingWeight > 0.0f && !(remainingWeight <= 0.001f); --animBlendIdx) {
            boolean isDeferredMovementBone;
            float rawAnimWeight;
            LiveAnimationTrackEntry liveTrackEntry = this.liveAnimationTrackEntries.get(animBlendIdx);
            AnimationTrack track = liveTrackEntry.getTrack();
            float boneWeight = track.getBoneWeight(boneIdx);
            if (boneWeight <= 0.001f || (rawAnimWeight = liveTrackEntry.getBlendWeight() * boneWeight) <= 0.001f) continue;
            float animWeight = PZMath.clamp(rawAnimWeight, 0.0f, remainingWeight);
            remainingWeight -= rawAnimWeight;
            remainingWeight = org.joml.Math.max(0.0f, remainingWeight);
            this.getTrackTransform(boneIdx, track, reparentPair, pos, rot, scale);
            if (isCounterRotationBone && !track.isRagdoll() && track.getUseDeferredRotation()) {
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
            boolean bl = isDeferredMovementBone = !track.isRagdoll() && track.getDeferredMovementBoneIdx() == boneIdx;
            if (isDeferredMovementBone) {
                Vector3f deferredCounterPosition = track.getCurrentDeferredCounterPosition(L_updateBoneAnimationTransform.deferredPos);
                pos.x += deferredCounterPosition.x;
                pos.y += deferredCounterPosition.y;
                pos.z += deferredCounterPosition.z;
            }
            if (isFirst) {
                VectorUtil.setScaled(pos, animWeight, key.position);
                key.rotation.set(rot);
                totalWeight = animWeight;
                isFirst = false;
                continue;
            }
            float animRotationWeight = animWeight / (animWeight + totalWeight);
            totalWeight += animWeight;
            VectorUtil.addScaled(key.position, pos, animWeight, key.position);
            PZMath.slerp(key.rotation, key.rotation, rot, animRotationWeight);
        }
        if (remainingWeight > 0.0f && !this.boneTransformsNeedFirstFrame) {
            this.boneTransforms[boneIdx].getPRS(pos, rot, scale);
            VectorUtil.addScaled(key.position, pos, remainingWeight, key.position);
            PZMath.slerp(key.rotation, rot, key.rotation, totalWeight);
            PZMath.lerp(key.scale, scale, key.scale, totalWeight);
        }
        this.boneTransforms[boneIdx].set(key.position, key.rotation, key.scale);
        this.boneTransforms[boneIdx].blendWeight = totalWeight;
    }

    private void getTrackTransform(int boneIdx, AnimationTrack track, AnimationBoneBindingPair reparentPair, Vector3f pos, Quaternion rot, Vector3f scale) {
        if (boneIdx == SkeletonBone.Bip01.index() && !track.isRagdoll()) {
            if (!track.isInitialAdjustmentCalculated) {
                track.initialAdjustment.set(0.0f, 0.0f, 0.0f);
                if (this.isRagdolling() && DebugOptions.instance.character.debug.animate.keepAtOrigin.getValue()) {
                    int adjustingBone = SkeletonBone.Bip01.index();
                    Matrix4f existingBone = this.getBoneModelTransform(adjustingBone, new Matrix4f());
                    Matrix4f trackBone = this.getUnweightedModelTransform(track, adjustingBone, new Matrix4f());
                    Vector3f existingBonePos = HelperFunctions.getPosition(existingBone, new Vector3f());
                    Vector3f trackBonePos = HelperFunctions.getPosition(trackBone, new Vector3f());
                    Vector3f.sub(track.initialAdjustment, trackBonePos, track.initialAdjustment);
                }
                track.isInitialAdjustmentCalculated = true;
            }
            track.get(boneIdx, pos, rot, scale);
            pos.x += track.initialAdjustment.x;
            pos.y -= track.initialAdjustment.z;
            return;
        }
        if (reparentPair == null) {
            track.get(boneIdx, pos, rot, scale);
            return;
        }
        Matrix4f result = L_getTrackTransform.result;
        SkinningBone bone = reparentPair.getBoneA();
        Matrix4f pa = AnimationPlayer.getUnweightedBoneTransform(track, bone.index, L_getTrackTransform.Pa);
        SkinningBone boneA = bone.parent;
        SkinningBone boneB = reparentPair.getBoneB();
        Matrix4f mA = this.getBoneModelTransform(boneA.index, L_getTrackTransform.mA);
        Matrix4f mAinv = Matrix4f.invert(mA, L_getTrackTransform.mAinv);
        Matrix4f mB = this.getBoneModelTransform(boneB.index, L_getTrackTransform.mB);
        Matrix4f umA = this.getUnweightedModelTransform(track, boneA.index, L_getTrackTransform.umA);
        Matrix4f umB = this.getUnweightedModelTransform(track, boneB.index, L_getTrackTransform.umB);
        Matrix4f umBinv = Matrix4f.invert(umB, L_getTrackTransform.umBinv);
        Matrix4f.mul(pa, umA, result);
        Matrix4f.mul(result, umBinv, result);
        Matrix4f.mul(result, mB, result);
        Matrix4f.mul(result, mAinv, result);
        HelperFunctions.getPosition(result, pos);
        HelperFunctions.getRotation(result, rot);
        scale.set(1.0f, 1.0f, 1.0f);
    }

    public boolean isBoneReparented(int boneIdx) {
        return PZArrayUtil.contains(this.reparentedBoneBindings, Lambda.predicate(boneIdx, (binding, lBoneIdx) -> binding.getBoneIdxA() == lBoneIdx.intValue()));
    }

    private void initRagdollController() {
        if (this.ragdollController != null) {
            return;
        }
        if (!this.canRagdoll()) {
            return;
        }
        RagdollController ragdollController = RagdollController.alloc();
        ragdollController.setGameCharacterObject(this.getIsoGameCharacter());
        if (this.getIsoGameCharacter() != null) {
            this.getIsoGameCharacter().onRagdollSimulationStarted();
        }
        this.ragdollController = ragdollController;
    }

    public boolean isRagdolling() {
        RagdollController ragdollController = this.getRagdollController();
        if (ragdollController == null) {
            return false;
        }
        return ragdollController.isInitialized();
    }

    public RagdollController getRagdollController() {
        return this.ragdollController;
    }

    private RagdollController getOrCreateRagdollController() {
        this.initRagdollController();
        return this.getRagdollController();
    }

    public boolean canRagdoll() {
        return this.character != null && this.character.canRagdoll();
    }

    public void stopAll() {
        this.getMultiTrack().reset();
        this.releaseRagdollController();
    }

    public void releaseRagdollController() {
        this.ragdollController = Pool.tryRelease(this.ragdollController);
        if (this.ragdollAnimationClip != null) {
            this.ragdollAnimationClip.setRagdollSimulationActive(false);
        }
        this.ragdollAnimationWeight = 0.0f;
    }

    public AnimationClip getRagdollSimulationAnimationClip() {
        return this.ragdollAnimationClip;
    }

    public void setIsoGameCharacter(IsoGameCharacter character) {
        this.character = character;
    }

    public IsoGameCharacter getIsoGameCharacter() {
        return this.character;
    }

    public int getModelTransformsCount() {
        return PZArrayUtil.lengthOf(this.modelTransforms);
    }

    public Matrix4f getModelTransformAt(int idx) {
        return this.modelTransforms[idx];
    }

    public float getBoneTransformsTimeDelta() {
        return this.boneTransformsTimeDelta;
    }

    public boolean isRagdollSimulationActive() {
        return this.ragdollController != null && this.ragdollController.isSimulationActive();
    }

    public boolean isFullyRagdolling() {
        return this.isRagdolling() && this.isRagdollSimulationActive() && this.ragdollAnimationWeight > 0.9f;
    }

    public void updateMultiTrackBoneTransforms_DeferredMovementOnly() {
        this.deferredMovementFromRagdoll.set(0.0f, 0.0f, 0.0f);
        if (this.parentPlayer != null) {
            return;
        }
        this.updateLayerBlendWeightings();
        if (this.liveAnimationTrackEntries.count() == 0) {
            return;
        }
        int[] boneIndices = updateMultiTrackBoneTransforms_DeferredMovementOnly.boneIndices;
        int boneCount = 0;
        List<AnimationTrack> tracks = this.multiTrack.getTracks();
        int tracksCount = tracks.size();
        for (int trackIdx = 0; trackIdx < tracksCount; ++trackIdx) {
            AnimationTrack track = tracks.get(trackIdx);
            int boneIdx = track.getDeferredMovementBoneIdx();
            if (boneIdx == -1 || PZArrayUtil.contains(boneIndices, boneCount, boneIdx)) continue;
            boneIndices[boneCount++] = boneIdx;
        }
        for (int i = 0; i < boneCount; ++i) {
            this.updateBoneAnimationTransform(boneIndices[i], null);
        }
    }

    public boolean isRecording() {
        return this.recorder != null && this.recorder.isRecording();
    }

    public void setRecorder(AnimationPlayerRecorder recorder) {
        this.recorder = recorder;
    }

    public AnimationPlayerRecorder getRecorder() {
        return this.recorder;
    }

    public void dismember(int bone) {
        this.dismembered.add(bone);
    }

    private void updateModelTransforms() {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("updateModelTransforms");){
            this.updateModelTransformsInternal();
        }
    }

    private void updateModelTransformsInternal() {
        this.boneTransforms[0].getMatrix(this.modelTransforms[0]);
        for (int boneIdx = 1; boneIdx < this.modelTransforms.length; ++boneIdx) {
            SkinningBone bone = this.skinningData.getBoneAt(boneIdx);
            SkinningBone parentBone = bone.parent;
            BoneTransform.mul((BoneTransform)this.boneTransforms[bone.index], this.modelTransforms[parentBone.index], this.modelTransforms[bone.index]);
        }
    }

    public void transformRootChildBones(String boneName, Quaternion rotation) {
        Matrix4f rotationMatrix = HelperFunctions.CreateFromQuaternion(rotation, HelperFunctions.getMatrix());
        for (int boneIdx = 0; boneIdx < this.modelTransforms.length; ++boneIdx) {
            SkinningBone bone = this.skinningData.getBoneAt(boneIdx);
            if (!StringUtils.equalsIgnoreCase(bone.name, boneName)) continue;
            BoneTransform.mul(rotationMatrix, (BoneTransform)this.boneTransforms[bone.index], (BoneTransform)this.boneTransforms[bone.index]);
            break;
        }
        this.updateModelTransformsInternal();
        HelperFunctions.returnMatrix(rotationMatrix);
    }

    public Matrix4f getBoneModelTransform(int boneIdx, Matrix4f modelTransform) {
        SkinningBone bone;
        Matrix4f boneTransform = L_getBoneModelTransform.boneTransform;
        modelTransform.setIdentity();
        SkinningBone current = bone = this.skinningData.getBoneAt(boneIdx);
        while (current != null) {
            this.getBoneTransform(current.index, boneTransform);
            Matrix4f.mul(modelTransform, boneTransform, modelTransform);
            current = current.parent;
        }
        return modelTransform;
    }

    private Vector3f getBoneModelPosition(SkeletonBone bone, Vector3f pos) {
        return HelperFunctions.getPosition(this.getBoneModelTransform(bone.index(), new Matrix4f()), pos);
    }

    public Vector3f getBoneWorldPosition(SkeletonBone bone, Vector3f pos) {
        this.getBoneModelPosition(bone, pos);
        Vector3 pos3 = new Vector3(pos.x, pos.y, pos.z);
        Model.vectorToWorldCoords(this.character, pos3);
        pos.set(pos3.x, pos3.y, pos3.z);
        return pos;
    }

    public Matrix4f getBindPoseBoneModelTransform(int boneIdx, Matrix4f modelTransform) {
        SkinningBone bone;
        Matrix4f boneTransform = L_getBoneModelTransform.boneTransform;
        modelTransform.setIdentity();
        SkinningBone current = bone = this.skinningData.getBoneAt(boneIdx);
        while (current != null) {
            boneTransform.load(this.skinningData.bindPose.get(current.index));
            Matrix4f.mul(modelTransform, boneTransform, modelTransform);
            current = current.parent;
        }
        return modelTransform;
    }

    public Matrix4f getBoneTransform(int boneIdx, Matrix4f boneTransform) {
        this.boneTransforms[boneIdx].getMatrix(boneTransform);
        return boneTransform;
    }

    public TwistableBoneTransform getBone(int boneIdx) {
        return this.boneTransforms[boneIdx];
    }

    public Matrix4f getUnweightedModelTransform(AnimationTrack track, int boneIdx, Matrix4f modelTransform) {
        SkinningBone bone;
        Matrix4f boneTransform = L_getUnweightedModelTransform.boneTransform;
        boneTransform.setIdentity();
        modelTransform.setIdentity();
        SkinningBone current = bone = this.skinningData.getBoneAt(boneIdx);
        while (current != null) {
            AnimationPlayer.getUnweightedBoneTransform(track, current.index, boneTransform);
            Matrix4f.mul(modelTransform, boneTransform, modelTransform);
            current = current.parent;
        }
        return modelTransform;
    }

    public static Matrix4f getUnweightedBoneTransform(AnimationTrack track, int boneIdx, Matrix4f boneTransform) {
        Vector3f pos = L_getUnweightedBoneTransform.pos;
        Quaternion rot = L_getUnweightedBoneTransform.rot;
        Vector3f scale = L_getUnweightedBoneTransform.scale;
        track.get(boneIdx, pos, rot, scale);
        HelperFunctions.CreateFromQuaternionPositionScale(pos, rot, scale, boneTransform);
        return boneTransform;
    }

    public void UpdateSkinTransforms() {
        this.resetSkinTransforms();
    }

    public Matrix4f[] getSkinTransforms(SkinningData skinnedTo) {
        if (skinnedTo == null) {
            return this.modelTransforms;
        }
        SkinTransformData data = this.getSkinTransformData(skinnedTo);
        Matrix4f[] skinTransforms = data.transforms;
        if (data.dirty) {
            data.checkBoneMap(this.getSkinningData());
            for (int bone = 0; bone < this.modelTransforms.length; ++bone) {
                int boneTo = data.boneMap[bone];
                if (boneTo == -1) continue;
                if (skinnedTo.boneOffset != null && skinnedTo.boneOffset.get(boneTo) != null) {
                    Matrix4f.mul(skinnedTo.boneOffset.get(boneTo), this.modelTransforms[bone], skinTransforms[boneTo]);
                    continue;
                }
                skinTransforms[boneTo].setIdentity();
            }
            data.dirty = false;
        }
        return skinTransforms;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Vector2 getDeferredMovement(Vector2 result, boolean reset) {
        Object object = this.deferredMovementLock;
        synchronized (object) {
            result.set(this.deferredMovement);
        }
        if (reset) {
            object = this.deferredMovementAccumLock;
            synchronized (object) {
                this.deferredMovementAccum.set(0.0f, 0.0f);
            }
        }
        return result;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void resetDeferredMovementAccum() {
        Object object = this.deferredMovementAccumLock;
        synchronized (object) {
            this.deferredMovementAccum.set(0.0f, 0.0f);
        }
    }

    public Vector3 getDeferredMovementFromRagdoll(Vector3 result) {
        return result.set(this.deferredMovementFromRagdoll);
    }

    public float getDeferredAngleDelta() {
        return this.deferredAngleDelta;
    }

    public float getDeferredRotationWeight() {
        return this.deferredRotationWeight;
    }

    public org.joml.Vector3f getTargetGrapplePos(org.joml.Vector3f result) {
        result.set(this.targetGrapplePos);
        return result;
    }

    public Vector3 getTargetGrapplePos(Vector3 result) {
        result.set(this.targetGrapplePos.x, this.targetGrapplePos.y, this.targetGrapplePos.z);
        return result;
    }

    public void setTargetGrapplePos(float x, float y, float z) {
        this.targetGrapplePos.set(x, y, z);
    }

    public void setTargetGrappleRotation(float x, float y) {
        this.targetGrappleRotation.set(x, y);
    }

    public Vector2 getTargetGrappleRotation(Vector2 result) {
        result.set(this.targetGrappleRotation);
        return result;
    }

    public org.joml.Vector3f getGrappleOffset(org.joml.Vector3f result) {
        result.set(this.grappleOffset);
        return result;
    }

    public Vector3 getGrappleOffset(Vector3 result) {
        result.set(this.grappleOffset.x, this.grappleOffset.y, this.grappleOffset.z);
        return result;
    }

    public void setGrappleOffset(float x, float y, float z) {
        this.grappleOffset.set(x, y, z);
    }

    public AnimationMultiTrack getMultiTrack() {
        return this.multiTrack;
    }

    public void setRecording(boolean val) {
        this.recorder.setRecording(val);
    }

    public void discardRecording() {
        if (this.recorder != null) {
            this.recorder.discardRecording();
        }
    }

    public float getRenderedAngle() {
        return this.angle + 1.5707964f;
    }

    public float getAngle() {
        return this.angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public void setAngleToTarget() {
        this.setAngle(this.targetAngle);
    }

    public void setTargetToAngle() {
        float angle = this.getAngle();
        this.setTargetAngle(angle);
    }

    public float getTargetAngle() {
        return this.targetAngle;
    }

    public void setTargetAngle(float targetAngle) {
        this.targetAngle = targetAngle;
    }

    public float getMaxTwistAngle() {
        return this.maxTwistAngle;
    }

    public void setMaxTwistAngle(float radians) {
        this.maxTwistAngle = radians;
    }

    public float getExcessTwistAngle() {
        return this.excessTwist;
    }

    public float getTwistAngle() {
        return this.twistAngle;
    }

    public float getShoulderTwistAngle() {
        return this.shoulderTwistAngle;
    }

    public float getTargetTwistAngle() {
        return this.targetTwistAngle;
    }

    private static final class L_setTwistBones {
        static final ArrayList<String> boneNames = new ArrayList();

        private L_setTwistBones() {
        }
    }

    private static class SkinTransformData
    extends PooledObject {
        public Matrix4f[] transforms;
        private SkinningData skinnedTo;
        public boolean dirty;
        private SkinningData animPlayerSkinningData;
        private int[] boneMap;
        private SkinTransformData next;
        private static final Pool<SkinTransformData> s_pool = new Pool<SkinTransformData>(SkinTransformData::new);

        private SkinTransformData() {
        }

        public void setSkinnedTo(SkinningData skinnedTo) {
            if (this.skinnedTo == skinnedTo) {
                return;
            }
            this.dirty = true;
            this.skinnedTo = skinnedTo;
            this.transforms = PZArrayUtil.newInstance(Matrix4f.class, this.transforms, skinnedTo.numBones(), Matrix4f::new);
            this.animPlayerSkinningData = null;
        }

        public void checkBoneMap(SkinningData animPlayerSkinningData) {
            if (this.animPlayerSkinningData == animPlayerSkinningData) {
                return;
            }
            this.animPlayerSkinningData = animPlayerSkinningData;
            int numBones = animPlayerSkinningData.numBones();
            if (this.boneMap == null || this.boneMap.length < numBones) {
                this.boneMap = new int[numBones];
            }
            for (int i = 0; i < numBones; ++i) {
                SkinningBone skinningBone = animPlayerSkinningData.getBoneAt(i);
                Integer boneIndexObj = this.skinnedTo.boneIndices.get(skinningBone.name);
                this.boneMap[i] = boneIndexObj == null ? -1 : boneIndexObj;
            }
        }

        public static SkinTransformData alloc(SkinningData skinnedTo) {
            SkinTransformData newInstance = s_pool.alloc();
            newInstance.setSkinnedTo(skinnedTo);
            newInstance.dirty = true;
            return newInstance;
        }
    }

    private static class L_applyTwistBone {
        static final Matrix4f twistParentBoneTrans = new Matrix4f();
        static final Matrix4f twistParentBoneTransInv = new Matrix4f();
        static final Matrix4f twistBoneTrans = new Matrix4f();
        static final Vector3f twistBonePos = new Vector3f();
        static final Matrix4f twistBoneNewTrans = new Matrix4f();
        static final Matrix4f twistBoneAdjustTrans = new Matrix4f();
        static final Vector3f twistRotateAxis = new Vector3f();
        static final Vector3f forward = new Vector3f();
        static final Quaternion twistTurnRot = new Quaternion();
        static final Quaternion twistTurnAdjustRot = new Quaternion();
        static final Quaternion twistTurnStep = new Quaternion();
        static final Quaternion twistTurnIdentity = new Quaternion();
        static final Vector3f desiredForward = new Vector3f();

        private L_applyTwistBone() {
        }
    }

    private static final class L_updateBoneAnimationTransform {
        static final Quaternion rot = new Quaternion();
        static final Vector3f pos = new Vector3f();
        static final Vector3f scale = new Vector3f();
        static final Keyframe key = new Keyframe(new Vector3f(0.0f, 0.0f, 0.0f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f));
        static final Matrix4f boneMat = new Matrix4f();
        static final Matrix4f rotMat = new Matrix4f();
        static final Vector3f rotAxis = new Vector3f(1.0f, 0.0f, 0.0f);
        static final Quaternion crRot = new Quaternion();
        static final Vector4f crRotAA = new Vector4f();
        static final Matrix4f crMat = new Matrix4f();
        static final Vector3f rotEulers = new Vector3f();
        static final Vector3f deferredPos = new Vector3f();

        private L_updateBoneAnimationTransform() {
        }
    }

    private static final class L_getTrackTransform {
        static final Matrix4f Pa = new Matrix4f();
        static final Matrix4f mA = new Matrix4f();
        static final Matrix4f mB = new Matrix4f();
        static final Matrix4f umA = new Matrix4f();
        static final Matrix4f umB = new Matrix4f();
        static final Matrix4f mAinv = new Matrix4f();
        static final Matrix4f umBinv = new Matrix4f();
        static final Matrix4f result = new Matrix4f();

        private L_getTrackTransform() {
        }
    }

    private static final class updateMultiTrackBoneTransforms_DeferredMovementOnly {
        static int[] boneIndices = new int[60];

        private updateMultiTrackBoneTransforms_DeferredMovementOnly() {
        }
    }

    private static class L_getBoneModelTransform {
        static final Matrix4f boneTransform = new Matrix4f();
        static final Matrix4f modelTransform = new Matrix4f();

        private L_getBoneModelTransform() {
        }
    }

    private static class L_getUnweightedModelTransform {
        static final Matrix4f boneTransform = new Matrix4f();

        private L_getUnweightedModelTransform() {
        }
    }

    private static class L_getUnweightedBoneTransform {
        static final Vector3f pos = new Vector3f();
        static final Quaternion rot = new Quaternion();
        static final Vector3f scale = new Vector3f();

        private L_getUnweightedBoneTransform() {
        }
    }
}

