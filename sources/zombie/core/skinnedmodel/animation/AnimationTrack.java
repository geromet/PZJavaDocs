/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.PerformanceSettings;
import zombie.core.math.IInterpolator;
import zombie.core.math.PZMath;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.PooledAnimBoneWeightArray;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.IAnimListener;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledArrayObject;
import zombie.util.PooledFloatArrayObject;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.lambda.Consumers;
import zombie.util.list.IntMap;
import zombie.util.list.PZArrayUtil;

public final class AnimationTrack
extends PooledObject {
    public boolean isPlaying;
    public boolean isPrimary;
    public AnimationClip currentClip;
    public int priority;
    private boolean isRagdollFirstFrame;
    public float ragdollStartTime;
    public float ragdollMaxTime = 5.0f;
    private float currentTimeValue;
    private float previousTimeValue;
    public boolean syncTrackingEnabled;
    public String trackTimeToVariable;
    public boolean reverse;
    public boolean looping;
    private final KeyframeSpan[] pose = new KeyframeSpan[60];
    private final KeyframeSpan deferredPoseSpan = new KeyframeSpan();
    private IntMap<BoneTransform> poseAdjustments;
    private float speedDelta;
    private float blendWeight;
    private float blendFieldWeight;
    public IInterpolator blendCurve;
    private String name;
    private String matchingGrappledAnimNode;
    private boolean isGrappler;
    public float earlyBlendOutTime;
    public boolean triggerOnNonLoopedAnimFadeOutEvent;
    public AnimLayer animLayer;
    private PooledArrayObject<AnimBoneWeight> boneWeightBindings;
    private PooledFloatArrayObject boneWeights;
    private final ArrayList<IAnimListener> listeners = new ArrayList();
    private final ArrayList<IAnimListener> listenersInvoking = new ArrayList();
    private SkinningBone deferredBone;
    private BoneAxis deferredBoneAxis;
    private boolean useDeferredMovement = true;
    private boolean useDeferredRotation;
    private float deferredRotationScale = 1.0f;
    private final DeferredMotionData deferredMotion = new DeferredMotionData();
    public boolean isInitialAdjustmentCalculated;
    public final Vector3f initialAdjustment = new Vector3f();
    private static final Pool<AnimationTrack> s_pool = new Pool<AnimationTrack>(AnimationTrack::new);

    public static AnimationTrack alloc() {
        return s_pool.alloc();
    }

    protected AnimationTrack() {
        PZArrayUtil.arrayPopulate(this.pose, KeyframeSpan::new);
        this.resetInternal();
    }

    private AnimationTrack resetInternal() {
        this.isPlaying = false;
        this.isPrimary = false;
        this.currentClip = null;
        this.priority = 0;
        this.isRagdollFirstFrame = false;
        this.ragdollStartTime = 0.0f;
        this.ragdollMaxTime = 5.0f;
        this.currentTimeValue = 0.0f;
        this.previousTimeValue = 0.0f;
        this.syncTrackingEnabled = true;
        this.reverse = false;
        this.looping = false;
        PZArrayUtil.forEach(this.pose, KeyframeSpan::clear);
        this.deferredPoseSpan.clear();
        this.poseAdjustments = Pool.tryRelease(this.poseAdjustments);
        this.speedDelta = 1.0f;
        this.blendWeight = 0.0f;
        this.blendFieldWeight = 1.0f;
        this.blendCurve = null;
        this.name = "!Empty!";
        this.earlyBlendOutTime = 0.0f;
        this.triggerOnNonLoopedAnimFadeOutEvent = false;
        this.animLayer = null;
        this.boneWeightBindings = Pool.tryRelease(this.boneWeightBindings);
        this.boneWeights = Pool.tryRelease(this.boneWeights);
        this.listeners.clear();
        this.listenersInvoking.clear();
        this.deferredBone = null;
        this.deferredBoneAxis = BoneAxis.Y;
        this.useDeferredMovement = true;
        this.useDeferredRotation = false;
        this.deferredRotationScale = 1.0f;
        this.deferredMotion.reset();
        this.isInitialAdjustmentCalculated = false;
        return this;
    }

    public void get(int bone, Vector3f pos, Quaternion rot, Vector3f scale) {
        this.pose[bone].lerp(this.getCurrentAnimationTime(), pos, rot, scale);
        if (this.poseAdjustments == null || this.poseAdjustments.isEmpty()) {
            return;
        }
        BoneTransform poseAdjustment = this.poseAdjustments.get(bone);
        if (poseAdjustment == null) {
            return;
        }
        BoneTransform boneTransform = BoneTransform.alloc();
        boneTransform.set(pos, rot, scale);
        BoneTransform.mul(poseAdjustment, boneTransform, boneTransform);
        boneTransform.getPRS(pos, rot, scale);
        Pool.tryRelease(boneTransform);
    }

    public void setBonePoseAdjustment(int bone, Vector3f pos, Quaternion rot, Vector3f scale) {
        BoneTransform poseAdjustment = BoneTransform.alloc();
        poseAdjustment.set(pos, rot, scale);
        if (this.poseAdjustments == null) {
            this.poseAdjustments = IntMap.alloc();
        }
        this.poseAdjustments.set(bone, poseAdjustment);
    }

    private Keyframe getDeferredMovementFrameAt(int boneIdx, float time, Keyframe result) {
        KeyframeSpan span = this.getKeyframeSpan(boneIdx, time, this.deferredPoseSpan);
        return span.lerp(time, result);
    }

    private KeyframeSpan getKeyframeSpan(int boneIdx, float time, KeyframeSpan result) {
        Keyframe[] boneFrames;
        int numFrames;
        if (!result.isBone(boneIdx)) {
            result.clear();
        }
        if ((numFrames = (boneFrames = this.currentClip.getBoneFramesAt(boneIdx)).length) == 0) {
            result.clear();
            return result;
        }
        if (result.containsTime(time)) {
            return result;
        }
        Keyframe lastFrame = boneFrames[numFrames - 1];
        if (time >= lastFrame.time) {
            result.fromIdx = numFrames > 1 ? numFrames - 2 : 0;
            result.toIdx = numFrames - 1;
            result.from = boneFrames[result.fromIdx];
            result.to = boneFrames[result.toIdx];
            return result;
        }
        Keyframe firstFrame = boneFrames[0];
        if (time <= firstFrame.time) {
            result.clear();
            result.toIdx = 0;
            result.to = firstFrame;
            return result;
        }
        int startIdx = 0;
        if (result.isSpan() && result.to.time <= time) {
            startIdx = result.toIdx;
        }
        result.clear();
        for (int idx = startIdx; idx < numFrames - 1; ++idx) {
            Keyframe kcurr = boneFrames[idx];
            Keyframe knext = boneFrames[idx + 1];
            if (!(kcurr.time <= time) || !(time <= knext.time)) continue;
            result.fromIdx = idx;
            result.toIdx = idx + 1;
            result.from = kcurr;
            result.to = knext;
            break;
        }
        return result;
    }

    public void removeListener(IAnimListener listener) {
        this.listeners.remove(listener);
    }

    public void Update(float time) {
        try {
            this.UpdateKeyframes(time);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void UpdateKeyframes(float dt) {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.updateKeyframes.profile();){
            this.updateKeyframesInternal(dt);
        }
    }

    private void updateKeyframesInternal(float dt) {
        if (this.currentClip == null) {
            throw new RuntimeException("AnimationPlayer.Update was called before startClip");
        }
        if (dt > 0.0f) {
            this.TickCurrentTime(dt);
        }
        if (!GameServer.server || ServerGUI.isCreated()) {
            this.updatePose();
        }
        this.updateDeferredValues();
    }

    private void updatePose() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.updatePose.profile();){
            this.updatePoseInternal();
        }
    }

    private void updatePoseInternal() {
        float currentTime = this.getCurrentAnimationTime();
        for (int n = 0; n < 60; ++n) {
            this.getKeyframeSpan(n, currentTime, this.pose[n]);
        }
    }

    private void updateDeferredValues() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.updateDeferredValues.profile();){
            this.updateDeferredValuesInternal();
        }
    }

    private void updateDeferredValuesInternal() {
        if (this.deferredBone == null) {
            return;
        }
        DeferredMotionData dm = this.deferredMotion;
        dm.deferredRotationDiff = 0.0f;
        dm.deferredMovementDiff.set(0.0f, 0.0f);
        dm.counterRotatedMovementDiff.set(0.0f, 0.0f);
        float prevTime = this.getPreviousAnimationTime();
        float currentTime = this.getCurrentAnimationTime();
        if (this.isLooping() && prevTime > currentTime) {
            float endTime = this.getDuration();
            this.appendDeferredValues(dm, prevTime, endTime);
            prevTime = 0.0f;
        }
        this.appendDeferredValues(dm, prevTime, currentTime);
    }

    private void appendDeferredValues(DeferredMotionData dm, float prevTime, float currentTime) {
        int deferredBoneIdx = this.getDeferredMovementBoneIdx();
        Keyframe prevKeyFrame = this.getDeferredMovementFrameAt(deferredBoneIdx, prevTime, L_updateDeferredValues.prevKeyFrame);
        Keyframe keyFrame = this.getDeferredMovementFrameAt(deferredBoneIdx, currentTime, L_updateDeferredValues.keyFrame);
        if (!GameServer.server) {
            dm.prevDeferredRotation = this.getDeferredTwistRotation(prevKeyFrame.rotation);
            dm.targetDeferredRotationQ.set(keyFrame.rotation);
            dm.targetDeferredRotation = this.getDeferredTwistRotation(keyFrame.rotation);
            float angleDiff = PZMath.getClosestAngle(dm.prevDeferredRotation, dm.targetDeferredRotation);
            dm.deferredRotationDiff += angleDiff * this.getDeferredRotationScale();
        }
        this.getDeferredMovement(prevKeyFrame.position, dm.prevDeferredMovement);
        dm.targetDeferredPosition.set(keyFrame.position);
        this.getDeferredMovement(keyFrame.position, dm.targetDeferredMovement);
        Vector2 diff = L_updateDeferredValues.diff.set(dm.targetDeferredMovement.x - dm.prevDeferredMovement.x, dm.targetDeferredMovement.y - dm.prevDeferredMovement.y);
        Vector2 crDiff = L_updateDeferredValues.crDiff.set(diff);
        if (this.getUseDeferredRotation() && !this.isRagdoll()) {
            float len = crDiff.normalize();
            crDiff.rotate(-(dm.targetDeferredRotation + 1.5707964f));
            crDiff.scale(-len);
        }
        dm.deferredMovementDiff.x += diff.x;
        dm.deferredMovementDiff.y += diff.y;
        dm.counterRotatedMovementDiff.x += crDiff.x;
        dm.counterRotatedMovementDiff.y += crDiff.y;
    }

    private float getDeferredTwistRotation(Quaternion boneRotation) {
        if (this.deferredBoneAxis == BoneAxis.Z) {
            return HelperFunctions.getRotationZ(boneRotation);
        }
        if (this.deferredBoneAxis == BoneAxis.Y) {
            return HelperFunctions.getRotationY(boneRotation);
        }
        DebugType.Animation.error("BoneAxis unhandled: %s", String.valueOf((Object)this.deferredBoneAxis));
        return 0.0f;
    }

    private Vector2 getDeferredMovement(Vector3f bonePos, Vector2 deferredPos) {
        if (this.deferredBoneAxis == BoneAxis.Y) {
            deferredPos.set(bonePos.x, -bonePos.z);
        } else {
            deferredPos.set(bonePos.x, bonePos.y);
        }
        return deferredPos;
    }

    public Vector3f getCurrentDeferredCounterPosition(Vector3f result) {
        this.getCurrentDeferredPosition(result);
        if (this.deferredBoneAxis == BoneAxis.Y) {
            result.set(-result.x, 0.0f, result.z);
        } else {
            result.set(-result.x, -result.y, 0.0f);
        }
        return result;
    }

    public float getCurrentDeferredRotation() {
        return this.deferredMotion.targetDeferredRotation;
    }

    public Vector3f getCurrentDeferredPosition(Vector3f result) {
        result.set(this.deferredMotion.targetDeferredPosition);
        return result;
    }

    public int getDeferredMovementBoneIdx() {
        if (this.deferredBone != null) {
            return this.deferredBone.index;
        }
        return -1;
    }

    public float getCurrentTrackTime() {
        return this.getReversibleTimeValue(this.currentTimeValue);
    }

    public float getPreviousTrackTime() {
        return this.getReversibleTimeValue(this.previousTimeValue);
    }

    public float getCurrentAnimationTime() {
        if (this.isRagdoll()) {
            return this.currentClip.getDuration();
        }
        return this.getCurrentTrackTime();
    }

    public float getPreviousAnimationTime() {
        if (this.isRagdoll()) {
            return 0.0f;
        }
        return this.getPreviousTrackTime();
    }

    private float getReversibleTimeValue(float timeValue) {
        if (this.reverse) {
            return this.getDuration() - timeValue;
        }
        return timeValue;
    }

    protected void TickCurrentTime(float time) {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.tickCurrentTime.profile();){
            this.tickCurrentTimeInternal(time);
        }
    }

    private void tickCurrentTimeInternal(float time) {
        boolean isRagdoll;
        float earlyBlendOutAtTime;
        time *= this.speedDelta;
        if (!this.isPlaying) {
            time = 0.0f;
        }
        float endDuration = this.getDuration();
        this.previousTimeValue = this.currentTimeValue;
        this.currentTimeValue = StringUtils.isNullOrEmpty(this.trackTimeToVariable) ? (this.currentTimeValue += time) : this.getTrackTimeToVariable(this.currentTimeValue + time);
        if (this.looping) {
            if (this.previousTimeValue == 0.0f && this.currentTimeValue > 0.0f) {
                this.invokeOnAnimStartedEvent();
            }
            if (this.previousTimeValue > this.currentTimeValue || this.currentTimeValue >= endDuration) {
                this.invokeOnLoopedAnimEvent();
                this.currentTimeValue %= endDuration;
                this.invokeOnAnimStartedEvent();
            }
            return;
        }
        if (this.currentTimeValue < 0.0f) {
            this.currentTimeValue = 0.0f;
        }
        if (this.previousTimeValue == 0.0f && this.currentTimeValue > 0.0f) {
            this.invokeOnAnimStartedEvent();
        }
        if (this.triggerOnNonLoopedAnimFadeOutEvent && this.previousTimeValue < (earlyBlendOutAtTime = endDuration - this.earlyBlendOutTime) && earlyBlendOutAtTime <= this.currentTimeValue) {
            this.invokeOnNonLoopedAnimFadeOutEvent();
        }
        if (this.currentTimeValue > endDuration) {
            this.currentTimeValue = endDuration;
        }
        if (!(isRagdoll = this.isRagdoll()) && this.previousTimeValue < endDuration && this.currentTimeValue >= endDuration) {
            if (this.looping) {
                this.invokeOnLoopedAnimEvent();
            }
            this.invokeOnNonLoopedAnimFinishedEvent();
        }
    }

    private float getTrackTimeToVariable(float defaultTime) {
        if (this.animLayer == null) {
            return defaultTime;
        }
        IAnimationVariableSource variableSource = this.animLayer.getVariableSource();
        if (variableSource == null) {
            return defaultTime;
        }
        float animLength = this.getDuration();
        float timeFraction = variableSource.getVariableFloat(this.trackTimeToVariable, defaultTime / animLength);
        return animLength * timeFraction;
    }

    public float getDuration() {
        if (this.isRagdoll()) {
            return this.ragdollMaxTime;
        }
        if (this.hasClip()) {
            return this.currentClip.getDuration();
        }
        return 0.0f;
    }

    private void invokeListeners(Consumer<IAnimListener> invoker) {
        if (this.listeners.isEmpty()) {
            return;
        }
        this.listenersInvoking.clear();
        PZArrayUtil.addAll(this.listenersInvoking, this.listeners);
        for (int i = 0; i < this.listenersInvoking.size(); ++i) {
            IAnimListener listener = this.listenersInvoking.get(i);
            invoker.accept(listener);
        }
    }

    private <T1> void invokeListeners(T1 val1, Consumers.Params1.ICallback<IAnimListener, T1> invoker) {
        Lambda.capture(this, val1, invoker, (stack, lThis, lVal1, lInvoker) -> lThis.invokeListeners(stack.consumer(lVal1, lInvoker)));
    }

    protected void invokeOnAnimStartedEvent() {
        this.invokeListeners(this, IAnimListener::onAnimStarted);
    }

    protected void invokeOnLoopedAnimEvent() {
        this.invokeListeners(this, IAnimListener::onLoopedAnim);
    }

    protected void invokeOnNonLoopedAnimFadeOutEvent() {
        this.invokeListeners(this, IAnimListener::onNonLoopedAnimFadeOut);
    }

    protected void invokeOnNonLoopedAnimFinishedEvent() {
        this.invokeListeners(this, IAnimListener::onNonLoopedAnimFinished);
    }

    @Override
    public void onReleased() {
        if (!this.listeners.isEmpty()) {
            this.listenersInvoking.clear();
            PZArrayUtil.addAll(this.listenersInvoking, this.listeners);
            for (int i = 0; i < this.listenersInvoking.size(); ++i) {
                IAnimListener listener = this.listenersInvoking.get(i);
                listener.onTrackDestroyed(this);
            }
            this.listeners.clear();
            this.listenersInvoking.clear();
        }
        this.reset();
    }

    public Vector2 getDeferredMovementDiff(Vector2 result) {
        result.set(this.deferredMotion.counterRotatedMovementDiff);
        return result;
    }

    public float getDeferredRotationDiff() {
        return this.deferredMotion.deferredRotationDiff;
    }

    public void addListener(IAnimListener listener) {
        this.listeners.add(listener);
    }

    public void startClip(AnimationClip clip, boolean loop, float ragdollMaxTime) {
        if (clip == null) {
            throw new NullPointerException("Supplied clip is null.");
        }
        this.reset();
        if (!StringUtils.isNullOrEmpty(this.trackTimeToVariable)) {
            this.previousTimeValue = this.currentTimeValue = this.getTrackTimeToVariable(this.currentTimeValue);
        }
        this.isPlaying = true;
        this.looping = loop;
        this.currentClip = clip;
        this.isRagdollFirstFrame = this.isRagdoll();
        this.ragdollMaxTime = ragdollMaxTime;
    }

    public AnimationTrack reset() {
        return this.resetInternal();
    }

    public void setBoneWeights(List<AnimBoneWeight> boneWeights) {
        this.boneWeightBindings = PooledAnimBoneWeightArray.toArray(boneWeights);
        this.boneWeights = null;
    }

    public void initBoneWeights(SkinningData skinningData) {
        if (this.hasBoneMask()) {
            return;
        }
        if (this.boneWeightBindings == null) {
            return;
        }
        if (this.boneWeightBindings.isEmpty()) {
            this.boneWeights = PooledFloatArrayObject.alloc(0);
            return;
        }
        this.boneWeights = PooledFloatArrayObject.alloc(skinningData.numBones());
        PZArrayUtil.arraySet(this.boneWeights.array(), 0.0f);
        for (int i = 0; i < this.boneWeightBindings.length(); ++i) {
            AnimBoneWeight weightBinding = this.boneWeightBindings.get(i);
            this.initWeightBinding(skinningData, weightBinding);
        }
    }

    protected void initWeightBinding(SkinningData skinningData, AnimBoneWeight weightBinding) {
        if (weightBinding == null || StringUtils.isNullOrEmpty(weightBinding.boneName)) {
            return;
        }
        String boneName = weightBinding.boneName;
        SkinningBone bone = skinningData.getBone(boneName);
        if (bone == null) {
            DebugType.Animation.error("Bone not found: %s", boneName);
            return;
        }
        float boneWeight = weightBinding.weight;
        this.assignBoneWeight(boneWeight, bone.index);
        if (weightBinding.includeDescendants) {
            Lambda.forEach(bone::forEachDescendant, this, Float.valueOf(boneWeight), (descendantBone, lThis, lBoneWeight) -> lThis.assignBoneWeight(lBoneWeight.floatValue(), descendantBone.index));
        }
    }

    private void assignBoneWeight(float weight, int boneIdx) {
        if (!this.hasBoneMask()) {
            throw new NullPointerException("Bone weights array not initialized.");
        }
        float existingWeight = this.boneWeights.get(boneIdx);
        this.boneWeights.set(boneIdx, Math.max(weight, existingWeight));
    }

    public float getBoneWeight(int boneIdx) {
        if (!this.hasBoneMask()) {
            return 1.0f;
        }
        if (DebugOptions.instance.character.debug.animate.noBoneMasks.getValue()) {
            return 1.0f;
        }
        return PZArrayUtil.getOrDefault(this.boneWeights.array(), boneIdx, 0.0f);
    }

    public float getDeferredBoneWeight() {
        if (this.deferredBone == null) {
            return 0.0f;
        }
        return this.getBoneWeight(this.deferredBone.index);
    }

    public int getLayerIdx() {
        return this.animLayer != null ? this.animLayer.getDepth() : 0;
    }

    public boolean hasBoneMask() {
        return this.boneWeights != null;
    }

    public boolean isLooping() {
        return this.looping;
    }

    public void setDeferredBone(SkinningBone bone, BoneAxis axis) {
        this.deferredBone = bone;
        this.deferredBoneAxis = axis;
    }

    public void setUseDeferredMovement(boolean val) {
        this.useDeferredMovement = val;
    }

    public boolean getUseDeferredMovement() {
        return this.useDeferredMovement;
    }

    public void setUseDeferredRotation(boolean val) {
        this.useDeferredRotation = val;
    }

    public boolean getUseDeferredRotation() {
        return this.useDeferredRotation;
    }

    public void setDeferredRotationScale(float deferredRotationScale) {
        this.deferredRotationScale = deferredRotationScale;
    }

    public float getDeferredRotationScale() {
        return this.deferredRotationScale;
    }

    public boolean isFinished() {
        if (this.isRagdoll()) {
            return !this.isRagdollSimulationActive();
        }
        return !this.looping && this.getDuration() > 0.0f && this.currentTimeValue >= this.getDuration();
    }

    public float getCurrentTimeValue() {
        return this.currentTimeValue;
    }

    public void setCurrentTimeValue(float currentTimeValue) {
        this.currentTimeValue = currentTimeValue;
    }

    public float getPreviousTimeValue() {
        return this.previousTimeValue;
    }

    public void setPreviousTimeValue(float previousTimeValue) {
        this.previousTimeValue = previousTimeValue;
    }

    public void rewind(float rewindAmount) {
        this.advance(-rewindAmount);
    }

    public void scaledRewind(float rewindAmount) {
        this.scaledAdvance(-rewindAmount);
    }

    public void scaledAdvance(float advanceAmount) {
        this.advance(advanceAmount * this.speedDelta);
    }

    public void advance(float advanceAmount) {
        this.currentTimeValue = PZMath.wrap(this.currentTimeValue + advanceAmount, 0.0f, this.getDuration());
        this.previousTimeValue = PZMath.wrap(this.previousTimeValue + advanceAmount, 0.0f, this.getDuration());
    }

    public void advanceFraction(float advanceFraction) {
        this.advance(this.getDuration() * advanceFraction);
    }

    public void moveCurrentTimeValueTo(float target) {
        float diff = target - this.currentTimeValue;
        this.advance(diff);
    }

    public void moveCurrentTimeValueToFraction(float fraction) {
        float targetTime = this.getDuration() * fraction;
        this.moveCurrentTimeValueTo(targetTime);
    }

    public float getCurrentTimeFraction() {
        if (this.hasClip()) {
            return this.currentTimeValue / this.getDuration();
        }
        return 0.0f;
    }

    public boolean hasClip() {
        return this.currentClip != null;
    }

    public AnimationClip getClip() {
        return this.currentClip;
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean isGrappler() {
        return this.isGrappler;
    }

    public static AnimationTrack createClone(AnimationTrack source2, Supplier<AnimationTrack> allocator) {
        AnimationTrack newTrack = allocator.get();
        newTrack.isPlaying = source2.isPlaying;
        newTrack.currentClip = source2.currentClip;
        newTrack.priority = source2.priority;
        newTrack.isRagdollFirstFrame = source2.isRagdollFirstFrame;
        newTrack.currentTimeValue = source2.currentTimeValue;
        newTrack.previousTimeValue = source2.previousTimeValue;
        newTrack.syncTrackingEnabled = source2.syncTrackingEnabled;
        newTrack.reverse = source2.reverse;
        newTrack.looping = source2.looping;
        newTrack.speedDelta = source2.speedDelta;
        newTrack.blendWeight = source2.blendWeight;
        newTrack.blendFieldWeight = source2.blendFieldWeight;
        newTrack.name = source2.name;
        newTrack.earlyBlendOutTime = source2.earlyBlendOutTime;
        newTrack.triggerOnNonLoopedAnimFadeOutEvent = source2.triggerOnNonLoopedAnimFadeOutEvent;
        newTrack.setAnimLayer(source2.animLayer);
        newTrack.boneWeightBindings = PooledAnimBoneWeightArray.toArray(source2.boneWeightBindings);
        newTrack.boneWeights = PooledFloatArrayObject.toArray(source2.boneWeights);
        newTrack.deferredBone = source2.deferredBone;
        newTrack.deferredBoneAxis = source2.deferredBoneAxis;
        newTrack.useDeferredMovement = source2.useDeferredMovement;
        newTrack.useDeferredRotation = source2.useDeferredRotation;
        newTrack.deferredRotationScale = source2.deferredRotationScale;
        newTrack.matchingGrappledAnimNode = source2.matchingGrappledAnimNode;
        newTrack.isGrappler = source2.isGrappler();
        return newTrack;
    }

    public String getMatchingGrappledAnimNode() {
        return this.matchingGrappledAnimNode;
    }

    public void setMatchingGrappledAnimNode(String matchingGrappledAnimNode) {
        this.matchingGrappledAnimNode = matchingGrappledAnimNode;
        this.isGrappler = !StringUtils.isNullOrWhitespace(this.matchingGrappledAnimNode);
    }

    public void setAnimLayer(AnimLayer animLayer) {
        if (this.animLayer == animLayer) {
            return;
        }
        if (this.animLayer != null) {
            this.removeListener(this.animLayer);
        }
        this.animLayer = animLayer;
        if (this.animLayer != null) {
            this.addListener(animLayer);
        }
    }

    public boolean isRagdollFirstFrame() {
        return this.isRagdollFirstFrame;
    }

    public void initRagdollTransform(int bone, Vector3f pos, Quaternion rot, Vector3f scale) {
        Keyframe[] keyframes;
        if (!this.isRagdoll()) {
            DebugType.Animation.warn("This track is not a ragdoll track: %s", this.getName());
            return;
        }
        for (Keyframe key : keyframes = this.currentClip.getBoneFramesAt(bone)) {
            key.set(pos, rot, scale);
        }
    }

    public boolean isRagdoll() {
        return this.currentClip != null && this.currentClip.isRagdoll;
    }

    public boolean isRagdollSimulationActive() {
        return this.isRagdoll() && this.currentClip.isRagdollSimulationActive();
    }

    private void initRagdollTransform(int boneIdx, TwistableBoneTransform boneTransform) {
        Vector3f pos = new Vector3f();
        Quaternion rot = new Quaternion();
        Vector3f scale = new Vector3f();
        boneTransform.getPRS(pos, rot, scale);
        this.initRagdollTransform(boneIdx, pos, rot, scale);
    }

    public void initRagdollTransforms(TwistableBoneTransform[] boneTransforms) {
        for (int boneIdx = 0; boneIdx < boneTransforms.length; ++boneIdx) {
            this.initRagdollTransform(boneIdx, boneTransforms[boneIdx]);
        }
        this.isRagdollFirstFrame = false;
        this.updatePose();
    }

    public String getName() {
        return this.currentClip != null ? this.currentClip.name : "!Empty!";
    }

    public float getSpeedDelta() {
        return this.speedDelta;
    }

    public void setSpeedDelta(float speedDelta) {
        this.speedDelta = speedDelta;
    }

    public float getBlendWeight() {
        float curveWeight;
        if (this.blendCurve != null) {
            float currentTime = this.getCurrentTrackTime();
            float trackDuration = this.getDuration();
            float timeAlpha = currentTime / trackDuration;
            curveWeight = this.blendCurve.lerp(timeAlpha);
        } else {
            curveWeight = 1.0f;
        }
        return this.blendWeight * curveWeight * this.blendFieldWeight;
    }

    public void setBlendWeight(float blendWeight) {
        this.blendWeight = blendWeight;
    }

    public float getBlendFieldWeight() {
        return this.blendFieldWeight;
    }

    public void setBlendFieldWeight(float blendFieldWeight) {
        this.blendFieldWeight = blendFieldWeight;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static class KeyframeSpan {
        Keyframe from;
        Keyframe to;
        int fromIdx = -1;
        int toIdx = -1;

        private KeyframeSpan() {
        }

        void clear() {
            this.from = null;
            this.to = null;
            this.fromIdx = -1;
            this.toIdx = -1;
        }

        Keyframe lerp(float time, Keyframe result) {
            result.setIdentity();
            if (this.from == null && this.to == null) {
                return result;
            }
            if (this.to == null) {
                result.set(this.from);
                return result;
            }
            if (this.from == null) {
                result.set(this.to);
                return result;
            }
            if (this.from == this.to) {
                result.set(this.to);
                return result;
            }
            return Keyframe.lerp(this.from, this.to, time, result);
        }

        void lerp(float time, Vector3f pos, Quaternion rot, Vector3f scale) {
            if (this.from == null && this.to == null) {
                Keyframe.setIdentity(pos, rot, scale);
                return;
            }
            if (this.to == null) {
                this.from.get(pos, rot, scale);
                return;
            }
            if (this.from == null) {
                this.to.get(pos, rot, scale);
                return;
            }
            if (this.from == this.to) {
                this.to.get(pos, rot, scale);
                return;
            }
            if (!PerformanceSettings.interpolateAnims) {
                this.to.get(pos, rot, scale);
                return;
            }
            Keyframe.lerp(this.from, this.to, time, pos, rot, scale);
        }

        boolean isSpan() {
            return this.from != null && this.to != null;
        }

        boolean isPost() {
            return (this.from == null || this.to == null) && this.from != this.to;
        }

        boolean isEmpty() {
            return this.from == null && this.to == null;
        }

        boolean containsTime(float time) {
            return this.isSpan() && this.from.time <= time && time <= this.to.time;
        }

        public boolean isBone(int boneIdx) {
            return this.from != null && this.from.none == boneIdx || this.to != null && this.to.none == boneIdx;
        }
    }

    private static class DeferredMotionData {
        float targetDeferredRotation;
        float prevDeferredRotation;
        final Quaternion targetDeferredRotationQ = new Quaternion();
        final Vector3f targetDeferredPosition = new Vector3f();
        final Vector2 prevDeferredMovement = new Vector2();
        final Vector2 targetDeferredMovement = new Vector2();
        float deferredRotationDiff;
        final Vector2 deferredMovementDiff = new Vector2();
        final Vector2 counterRotatedMovementDiff = new Vector2();

        private DeferredMotionData() {
        }

        public void reset() {
            this.deferredRotationDiff = 0.0f;
            this.targetDeferredRotation = 0.0f;
            this.prevDeferredRotation = 0.0f;
            this.targetDeferredRotationQ.setIdentity();
            this.targetDeferredMovement.set(0.0f, 0.0f);
            this.targetDeferredPosition.set(0.0f, 0.0f, 0.0f);
            this.prevDeferredMovement.set(0.0f, 0.0f);
            this.deferredMovementDiff.set(0.0f, 0.0f);
            this.counterRotatedMovementDiff.set(0.0f, 0.0f);
        }
    }

    private static class s_performance {
        static final PerformanceProfileProbe tickCurrentTime = new PerformanceProfileProbe("AnimationTrack.tickCurrentTime");
        static final PerformanceProfileProbe updateKeyframes = new PerformanceProfileProbe("AnimationTrack.updateKeyframes");
        static final PerformanceProfileProbe updateDeferredValues = new PerformanceProfileProbe("AnimationTrack.updateDeferredValues");
        static final PerformanceProfileProbe updatePose = new PerformanceProfileProbe("AnimationTrack.updatePose");

        private s_performance() {
        }
    }

    private static class L_updateDeferredValues {
        static final Keyframe keyFrame = new Keyframe(new Vector3f(), new Quaternion(), new Vector3f(1.0f, 1.0f, 1.0f));
        static final Keyframe prevKeyFrame = new Keyframe(new Vector3f(), new Quaternion(), new Vector3f(1.0f, 1.0f, 1.0f));
        static final Vector2 crDiff = new Vector2();
        static final Vector2 diff = new Vector2();

        private L_updateDeferredValues() {
        }
    }
}

