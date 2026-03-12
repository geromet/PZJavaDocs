/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.advancedanimation.AnimCondition;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimTransition;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.animation.IAnimListener;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.Vector3;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.util.list.PZEmptyIterable;

public class LiveAnimNode
extends PooledObject
implements IAnimListener {
    private AnimNode sourceNode;
    private AnimLayer animLayer;
    private boolean active;
    private boolean wasActive;
    boolean transitioningOut;
    private float weight;
    private float rawWeight;
    private boolean isNew;
    public boolean isBlendField;
    public AnimationTrack runningRagdollTrack;
    private final TransitionIn transitionIn = new TransitionIn();
    private final List<AnimationTrack> animationTracks = new ArrayList<AnimationTrack>();
    final List<AnimationTrack> ragdollTracks = new ArrayList<AnimationTrack>();
    float nodeAnimTime;
    float prevNodeAnimTime;
    private boolean blendingIn;
    private boolean blendingOut;
    private AnimTransition transitionOut;
    private boolean tweeningInGrapple;
    private boolean tweeningInGrappleFinished;
    private final Vector3f grappleTweenStartPos = new Vector3f();
    private final ArrayList<WhileAliveFlag> whileAliveFlags = new ArrayList();
    private static final Pool<LiveAnimNode> s_pool = new Pool<LiveAnimNode>(LiveAnimNode::new);
    private String cachedRandomAnim = "";

    protected LiveAnimNode() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static LiveAnimNode alloc(AnimLayer animLayer, AnimNode sourceNode) {
        LiveAnimNode newNode;
        Pool<LiveAnimNode> pool = s_pool;
        synchronized (pool) {
            newNode = s_pool.alloc();
            newNode.reset();
            newNode.sourceNode = sourceNode;
            newNode.animLayer = animLayer;
        }
        return newNode;
    }

    private void reset() {
        this.decrementWhileAliveFlags();
        this.sourceNode = null;
        this.animLayer = null;
        this.active = false;
        this.wasActive = false;
        this.transitioningOut = false;
        this.weight = 0.0f;
        this.rawWeight = 0.0f;
        this.isNew = true;
        this.isBlendField = false;
        this.transitionIn.reset();
        this.animationTracks.clear();
        this.ragdollTracks.clear();
        this.runningRagdollTrack = null;
        this.nodeAnimTime = 0.0f;
        this.prevNodeAnimTime = 0.0f;
        this.blendingIn = false;
        this.blendingOut = false;
        this.transitionOut = null;
        this.tweeningInGrapple = false;
        this.tweeningInGrappleFinished = false;
        Pool.tryRelease(this.whileAliveFlags);
        this.cachedRandomAnim = "";
    }

    @Override
    public void onReleased() {
        this.removeAllTracks();
        this.reset();
    }

    public String getName() {
        return this.sourceNode.name;
    }

    public boolean isBlendingIn() {
        return this.blendingIn;
    }

    public boolean isBlendingOut() {
        return this.blendingOut;
    }

    public boolean isTransitioningIn() {
        return this.transitionIn.active && this.transitionIn.track != null;
    }

    public void startTransitionIn(LiveAnimNode transitionFrom, AnimTransition transitionIn, AnimationTrack track) {
        this.startTransitionIn(transitionFrom.getSourceNode(), transitionIn, track);
    }

    public void startTransitionIn(AnimNode transitionFrom, AnimTransition transitionIn, AnimationTrack track) {
        if (this.transitionIn.track != null) {
            DebugType.Animation.debugln("Removing existing TransitioningIn track: %s. Replaced by: %s", this.transitionIn.track.getName(), track != null ? track.getName() : "null");
            this.stopTransitionIn();
        }
        this.transitionIn.active = track != null;
        this.transitionIn.transitionedFrom = transitionFrom.name;
        this.transitionIn.data = transitionIn;
        this.transitionIn.track = track;
        this.transitionIn.weight = 0.0f;
        this.transitionIn.rawWeight = 0.0f;
        this.transitionIn.blendingIn = true;
        this.transitionIn.blendingOut = false;
        this.transitionIn.time = 0.0f;
        if (this.transitionIn.track != null) {
            this.transitionIn.track.addListener(this);
        }
        this.setMainTracksPlaying(false);
    }

    public void stopTransitionIn() {
        this.removeTrack(this.transitionIn.track);
        this.transitionIn.reset();
    }

    private void removeTrack(AnimationTrack track) {
        AnimationMultiTrack rootTrack = this.animLayer.getAnimationTrack();
        if (rootTrack != null) {
            rootTrack.removeTrack(track);
        }
    }

    public void removeAllTracks() {
        AnimationMultiTrack multiTrack = this.animLayer.getAnimationTrack();
        if (multiTrack != null) {
            multiTrack.removeTracks(this.animationTracks);
            multiTrack.removeTracks(this.ragdollTracks);
            multiTrack.removeTrack(this.runningRagdollTrack);
            multiTrack.removeTrack(this.getTransitionInTrack());
        } else {
            IPooledObject.release(this.animationTracks);
            IPooledObject.release(this.ragdollTracks);
            Pool.tryRelease(this.runningRagdollTrack);
            Pool.tryRelease(this.getTransitionInTrack());
        }
    }

    public void setTransitionOut(AnimTransition transitionOut) {
        this.transitionOut = transitionOut;
    }

    public float getTrackTimeToVariable(float defaultTime) {
        if (this.animLayer == null) {
            return defaultTime;
        }
        IAnimationVariableSource variableSource = this.animLayer.getVariableSource();
        if (variableSource == null) {
            return defaultTime;
        }
        float animLength = this.getMaxDuration();
        float timeFraction = variableSource.getVariableFloat(this.getSourceNode().trackTimeToVariable, defaultTime / animLength);
        return animLength * timeFraction;
    }

    public void update(float timeDelta) {
        boolean mainAnimStarted;
        boolean mainAnimActive;
        this.isNew = false;
        if (this.active != this.wasActive) {
            this.blendingIn = this.active;
            boolean bl = this.blendingOut = !this.active;
            if (this.transitionIn.active) {
                this.transitionIn.blendingIn = this.active;
                this.transitionIn.blendingOut = !this.active;
            }
            this.wasActive = this.active;
        }
        boolean wasMainAnimActive = this.isMainAnimActive();
        if (this.isTransitioningIn()) {
            this.updateTransitioningIn(timeDelta);
        }
        if (mainAnimActive = this.isMainAnimActive()) {
            if (this.blendingOut && this.sourceNode.stopAnimOnExit) {
                this.setMainTracksPlaying(false);
            } else {
                this.setMainTracksPlaying(true);
            }
        } else {
            this.setMainTracksPlaying(false);
        }
        if (!mainAnimActive) {
            return;
        }
        boolean bl = mainAnimStarted = !wasMainAnimActive;
        if (mainAnimStarted && this.isLooped()) {
            float rewindAmount = this.getMainInitialRewindTime();
            PZArrayUtil.forEach(this.animationTracks, Lambda.consumer(Float.valueOf(rewindAmount), AnimationTrack::scaledRewind));
        }
        if (this.blendingIn) {
            this.updateBlendingIn(timeDelta);
        } else if (this.blendingOut) {
            this.updateBlendingOut(timeDelta);
        }
        this.prevNodeAnimTime = this.nodeAnimTime;
        if (StringUtils.isNullOrEmpty(this.getSourceNode().trackTimeToVariable)) {
            this.nodeAnimTime += timeDelta;
        } else {
            this.nodeAnimTime = this.getTrackTimeToVariable(this.nodeAnimTime + timeDelta);
            if (this.prevNodeAnimTime < this.nodeAnimTime - timeDelta * 1.1f) {
                this.prevNodeAnimTime = this.nodeAnimTime;
            }
        }
        if (!this.transitionIn.active && this.transitionIn.track != null && this.transitionIn.track.getBlendWeight() <= 0.0f) {
            this.stopTransitionIn();
        }
        this.updateRagdollTracks();
    }

    private void updateRagdollTracks() {
        if (this.ragdollTracks.isEmpty()) {
            return;
        }
        if (this.runningRagdollTrack == null) {
            AnimationTrack foundRagdollTrack = null;
            for (int i = 0; i < this.ragdollTracks.size(); ++i) {
                AnimationTrack track = this.ragdollTracks.get(i);
                if (track.ragdollStartTime > this.nodeAnimTime) continue;
                if (foundRagdollTrack == null) {
                    foundRagdollTrack = track;
                    continue;
                }
                if (!(track.ragdollStartTime < foundRagdollTrack.ragdollStartTime)) continue;
                foundRagdollTrack = track;
            }
            if (foundRagdollTrack != null) {
                this.runningRagdollTrack = foundRagdollTrack;
                this.runningRagdollTrack.setBlendFieldWeight(0.0f);
            }
        }
        if (this.runningRagdollTrack == null) {
            return;
        }
        if (this.animationTracks.isEmpty()) {
            this.runningRagdollTrack.setBlendFieldWeight(1.0f);
            return;
        }
        if (this.runningRagdollTrack.getBlendFieldWeight() == 1.0f) {
            return;
        }
        AnimationTrack ragdollTrack = this.runningRagdollTrack;
        float timeDiff = this.nodeAnimTime - ragdollTrack.ragdollStartTime;
        float ragdollBlendInTime = this.sourceNode.blendTime;
        float newRagdollTrackWeight = timeDiff > 0.0f && timeDiff <= ragdollBlendInTime ? timeDiff / ragdollBlendInTime : (timeDiff > ragdollBlendInTime ? 1.0f : 0.0f);
        this.runningRagdollTrack.setBlendFieldWeight(PZMath.max(this.runningRagdollTrack.getBlendFieldWeight(), newRagdollTrackWeight));
    }

    private void updateTransitioningIn(float timeDelta) {
        float rawWeight;
        float nextTime;
        float blendTimeMax;
        float blendOutTime;
        boolean conditionsPass;
        float speedDelta = this.transitionIn.track.getSpeedDelta();
        float transitionDuration = this.transitionIn.track.getDuration();
        this.transitionIn.time = this.transitionIn.track.getCurrentTimeValue();
        if (this.transitionIn.time >= transitionDuration || DebugOptions.instance.animation.disableAnimationBlends.getValue()) {
            this.stopTransitionIn();
            return;
        }
        if (!this.transitionIn.blendingOut && !(conditionsPass = AnimCondition.pass(this.animLayer.getVariableSource(), this.transitionIn.data.conditions))) {
            this.transitionIn.blendingIn = false;
            this.transitionIn.blendingOut = true;
        }
        if (this.transitionIn.time >= transitionDuration - (blendOutTime = this.getTransitionInBlendOutTime() * speedDelta)) {
            this.transitionIn.blendingIn = false;
            this.transitionIn.blendingOut = true;
        }
        if (this.transitionIn.blendingIn) {
            blendTimeMax = this.getTransitionInBlendInTime() * speedDelta;
            nextTime = this.incrementBlendTime(this.transitionIn.rawWeight, blendTimeMax, timeDelta * speedDelta);
            this.transitionIn.rawWeight = rawWeight = PZMath.clamp(nextTime / blendTimeMax, 0.0f, 1.0f);
            this.transitionIn.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
            this.transitionIn.blendingIn = nextTime < blendTimeMax;
            boolean bl = this.transitionIn.active = nextTime < transitionDuration;
        }
        if (this.transitionIn.blendingOut) {
            blendTimeMax = this.getTransitionInBlendOutTime() * speedDelta;
            nextTime = this.incrementBlendTime(1.0f - this.transitionIn.rawWeight, blendTimeMax, timeDelta * speedDelta);
            this.transitionIn.rawWeight = rawWeight = PZMath.clamp(1.0f - nextTime / blendTimeMax, 0.0f, 1.0f);
            this.transitionIn.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
            this.transitionIn.active = this.transitionIn.blendingOut = nextTime < blendTimeMax;
        }
    }

    public void addMainTrack(AnimationTrack track) {
        float blendOutTime;
        if (!this.isLooped() && !this.sourceNode.stopAnimOnExit && this.sourceNode.earlyTransitionOut && (blendOutTime = this.getBlendOutTime()) > 0.0f && Float.isFinite(blendOutTime)) {
            track.earlyBlendOutTime = blendOutTime;
            track.triggerOnNonLoopedAnimFadeOutEvent = true;
        }
        if (track.isRagdoll()) {
            this.ragdollTracks.add(track);
        } else {
            this.animationTracks.add(track);
        }
    }

    private void setMainTracksPlaying(boolean arePlaying) {
        for (int i = 0; i < this.animationTracks.size(); ++i) {
            AnimationTrack track = this.animationTracks.get(i);
            track.isPlaying = arePlaying;
        }
        if (this.runningRagdollTrack != null) {
            this.runningRagdollTrack.isPlaying = arePlaying;
        }
    }

    private void updateBlendingIn(float timeDelta) {
        float rawWeight;
        float blendTimeMax = this.getBlendInTime();
        if (blendTimeMax <= 0.0f || DebugOptions.instance.animation.disableAnimationBlends.getValue()) {
            this.stopBlendingIn();
            return;
        }
        float nextTime = this.incrementBlendTime(this.rawWeight, blendTimeMax, timeDelta);
        this.rawWeight = rawWeight = PZMath.clamp(nextTime / blendTimeMax, 0.0f, 1.0f);
        this.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
        this.blendingIn = nextTime < blendTimeMax;
    }

    private void updateBlendingOut(float timeDelta) {
        float rawWeight;
        float blendTimeMax = this.getBlendOutTime();
        if (blendTimeMax <= 0.0f || DebugOptions.instance.animation.disableAnimationBlends.getValue()) {
            this.stopBlendingOut();
            return;
        }
        float nextTime = this.incrementBlendTime(1.0f - this.rawWeight, blendTimeMax, timeDelta);
        this.rawWeight = rawWeight = PZMath.clamp(1.0f - nextTime / blendTimeMax, 0.0f, 1.0f);
        this.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
        this.blendingOut = nextTime < blendTimeMax;
    }

    private void stopBlendingOut() {
        this.setWeightsToZero();
        this.blendingOut = false;
    }

    private void stopBlendingIn() {
        this.setWeightsToFull();
        this.blendingIn = false;
    }

    public void setWeightsToZero() {
        this.weight = 0.0f;
        this.rawWeight = 0.0f;
    }

    public void setWeightsToFull() {
        this.weight = 1.0f;
        this.rawWeight = 1.0f;
    }

    private float incrementBlendTime(float initialWeight, float blendTimeMax, float timeDelta) {
        float prevTime = initialWeight * blendTimeMax;
        return prevTime + timeDelta;
    }

    public float getTransitionInBlendInTime() {
        if (this.transitionIn.data != null && this.transitionIn.data.blendInTime != Float.POSITIVE_INFINITY) {
            return this.transitionIn.data.blendInTime;
        }
        return 0.0f;
    }

    public float getMainInitialRewindTime() {
        float randomAdvanceTime = 0.0f;
        if (this.sourceNode.randomAdvanceFraction > 0.0f) {
            float advanceFrac = Rand.Next(0.0f, this.sourceNode.randomAdvanceFraction);
            randomAdvanceTime = advanceFrac * this.getMaxDuration();
        }
        if (this.transitionIn.data == null) {
            return 0.0f - randomAdvanceTime;
        }
        float blendInTime = this.getTransitionInBlendOutTime();
        float syncAdjustTime = this.transitionIn.data.syncAdjustTime;
        if (this.transitionIn.track != null) {
            return blendInTime - syncAdjustTime;
        }
        return blendInTime - syncAdjustTime - randomAdvanceTime;
    }

    private float getMaxDuration() {
        float duration;
        AnimationTrack track;
        int i;
        float maxDuration = 0.0f;
        int count = this.animationTracks.size();
        for (i = 0; i < count; ++i) {
            track = this.animationTracks.get(i);
            duration = track.getDuration();
            maxDuration = PZMath.max(duration, maxDuration);
        }
        count = this.ragdollTracks.size();
        for (i = 0; i < count; ++i) {
            track = this.ragdollTracks.get(i);
            duration = track.getDuration();
            maxDuration = PZMath.max(duration, maxDuration);
        }
        return maxDuration;
    }

    public float getTransitionInBlendOutTime() {
        return this.getBlendInTime();
    }

    public float getBlendInTime() {
        if (this.transitionIn.data == null) {
            return this.sourceNode.blendTime;
        }
        if (this.transitionIn.track != null && this.transitionIn.data.blendOutTime != Float.POSITIVE_INFINITY) {
            return this.transitionIn.data.blendOutTime;
        }
        if (this.transitionIn.track == null) {
            if (this.transitionIn.data.blendInTime != Float.POSITIVE_INFINITY) {
                return this.transitionIn.data.blendInTime;
            }
            if (this.transitionIn.data.blendOutTime != Float.POSITIVE_INFINITY) {
                return this.transitionIn.data.blendOutTime;
            }
        }
        return this.sourceNode.blendTime;
    }

    public float getBlendOutTime() {
        if (this.transitionOut == null) {
            return this.sourceNode.getBlendOutTime();
        }
        if (!StringUtils.isNullOrWhitespace(this.transitionOut.animName) && this.transitionOut.blendInTime != Float.POSITIVE_INFINITY) {
            return this.transitionOut.blendInTime;
        }
        if (StringUtils.isNullOrWhitespace(this.transitionOut.animName)) {
            if (this.transitionOut.blendOutTime != Float.POSITIVE_INFINITY) {
                return this.transitionOut.blendOutTime;
            }
            if (this.transitionOut.blendInTime != Float.POSITIVE_INFINITY) {
                return this.transitionOut.blendInTime;
            }
        }
        return this.sourceNode.getBlendOutTime();
    }

    @Override
    public void onAnimStarted(AnimationTrack track) {
        this.invokeAnimStartTimeEvent(track);
    }

    @Override
    public void onLoopedAnim(AnimationTrack track) {
        if (this.transitioningOut) {
            return;
        }
        this.invokeAnimEndTimeEvent(track);
    }

    @Override
    public void onNonLoopedAnimFadeOut(AnimationTrack track) {
        if (!DebugOptions.instance.animation.allowEarlyTransitionOut.getValue()) {
            return;
        }
        this.invokeAnimEndTimeEvent(track);
        this.transitioningOut = true;
        this.decrementWhileAliveFlags();
    }

    @Override
    public void onNonLoopedAnimFinished(AnimationTrack track) {
        if (this.transitioningOut) {
            return;
        }
        this.invokeAnimEndTimeEvent(track);
        this.decrementWhileAliveFlags();
    }

    @Override
    public void onTrackDestroyed(AnimationTrack track) {
        this.animationTracks.remove(track);
        this.ragdollTracks.remove(track);
        if (this.runningRagdollTrack == track) {
            this.runningRagdollTrack = null;
        }
        if (this.transitionIn.track == track) {
            this.transitionIn.track = null;
            this.transitionIn.active = false;
            this.transitionIn.weight = 0.0f;
            this.setMainTracksPlaying(true);
        }
    }

    @Override
    public void onNoAnimConditionsPass() {
    }

    private void invokeAnimStartTimeEvent(AnimationTrack track) {
        this.invokeAnimTimeEvent(track, AnimEvent.AnimEventTime.START);
    }

    private void invokeAnimEndTimeEvent(AnimationTrack track) {
        this.invokeAnimTimeEvent(track, AnimEvent.AnimEventTime.END);
    }

    private void invokeAnimTimeEvent(AnimationTrack track, AnimEvent.AnimEventTime eventTime) {
        if (this.sourceNode == null) {
            return;
        }
        List<AnimEvent> events = this.getSourceNode().events;
        int eventCount = events.size();
        for (int i = 0; i < eventCount; ++i) {
            AnimEvent event = events.get(i);
            if (event.time != eventTime) continue;
            this.animLayer.invokeAnimEvent(this, track, event);
        }
    }

    public AnimNode getSourceNode() {
        return this.sourceNode;
    }

    public boolean isIdleAnimActive() {
        return this.active && this.sourceNode.isIdleAnim();
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
        }
    }

    public boolean isLooped() {
        return this.sourceNode.isLooped;
    }

    public float getWeight() {
        return this.weight;
    }

    public float getTransitionInWeight() {
        return this.transitionIn.weight;
    }

    public boolean wasActivated() {
        return this.active != this.wasActive && this.active;
    }

    public boolean wasDeactivated() {
        return this.active != this.wasActive && this.wasActive;
    }

    public boolean isNew() {
        return this.isNew;
    }

    public int getPlayingTrackCount() {
        int playingTrackCount = 0;
        if (this.isMainAnimActive()) {
            playingTrackCount += this.animationTracks.size();
        }
        if (this.runningRagdollTrack != null) {
            ++playingTrackCount;
        }
        if (this.isTransitioningIn()) {
            ++playingTrackCount;
        }
        return playingTrackCount;
    }

    public AnimationTrack getPlayingTrackAt(int trackIdx) {
        if (trackIdx < 0) {
            throw new IndexOutOfBoundsException("TrackIdx is negative. Out of bounds: " + trackIdx);
        }
        int playingTrackCount = 0;
        if (this.isMainAnimActive() && trackIdx < (playingTrackCount += this.animationTracks.size())) {
            return this.animationTracks.get(trackIdx);
        }
        if (this.runningRagdollTrack != null && trackIdx < ++playingTrackCount) {
            return this.runningRagdollTrack;
        }
        if (this.isTransitioningIn() && trackIdx < ++playingTrackCount) {
            return this.transitionIn.track;
        }
        throw new IndexOutOfBoundsException("TrackIdx out of bounds 0 - " + playingTrackCount);
    }

    public boolean isMainAnimActive() {
        return !this.isTransitioningIn() || this.transitionIn.blendingOut;
    }

    public String getTransitionFrom() {
        return this.transitionIn.transitionedFrom;
    }

    public void setTransitionInBlendDelta(float blendDelta) {
        if (this.transitionIn.track != null) {
            this.transitionIn.track.setBlendWeight(blendDelta);
        }
    }

    public AnimationTrack getTransitionInTrack() {
        return this.transitionIn.track;
    }

    public int getTransitionLayerIdx() {
        return this.transitionIn.track != null ? this.transitionIn.track.getLayerIdx() : -1;
    }

    public int getLayerIdx() {
        return this.animLayer.getDepth();
    }

    public int getPriority() {
        return this.sourceNode.getPriority();
    }

    public String getDeferredBoneName() {
        return this.sourceNode.getDeferredBoneName();
    }

    public BoneAxis getDeferredBoneAxis() {
        return this.sourceNode.getDeferredBoneAxis();
    }

    public List<AnimBoneWeight> getSubStateBoneWeights() {
        return this.sourceNode.subStateBoneWeights;
    }

    public AnimTransition findTransitionTo(IAnimationVariableSource varSource, AnimNode toNode) {
        return this.sourceNode.findTransitionTo(varSource, toNode);
    }

    public float getSpeedScale(IAnimationVariableSource varSource) {
        return this.sourceNode.getSpeedScale(varSource);
    }

    public boolean isGrappler() {
        return this.sourceNode.isGrappler();
    }

    public String getMatchingGrappledAnimNode() {
        return this.sourceNode.getMatchingGrappledAnimNode();
    }

    public GrappleOffsetBehaviour getGrapplerOffsetBehaviour() {
        return this.sourceNode.grapplerOffsetBehaviour;
    }

    public float getGrappleOffsetForward() {
        return this.sourceNode.grappleOffsetForward;
    }

    public float getGrappledOffsetYaw() {
        return this.sourceNode.grappleOffsetYaw;
    }

    public String getAnimName() {
        if (!StringUtils.isNullOrWhitespace(this.cachedRandomAnim)) {
            return this.cachedRandomAnim;
        }
        return this.sourceNode.animName;
    }

    public void selectRandomAnim() {
        this.cachedRandomAnim = this.sourceNode.getRandomAnim();
    }

    public boolean isTweeningInGrapple() {
        return this.tweeningInGrapple;
    }

    public void setTweeningInGrapple(boolean tweeningInGrapple) {
        this.tweeningInGrapple = tweeningInGrapple;
    }

    public boolean isTweeningInGrappleFinished() {
        return this.tweeningInGrappleFinished;
    }

    public void setTweeningInGrappleFinished(boolean tweeningInGrappleFinished) {
        this.tweeningInGrappleFinished = tweeningInGrappleFinished;
    }

    public Vector3f getGrappleTweenStartPos(Vector3f result) {
        result.set(this.grappleTweenStartPos);
        return result;
    }

    public void setGrappleTweenStartPos(Vector3f pos) {
        this.grappleTweenStartPos.set(pos);
    }

    public Vector3 getGrappleTweenStartPos(Vector3 result) {
        result.set(this.grappleTweenStartPos.x, this.grappleTweenStartPos.y, this.grappleTweenStartPos.z);
        return result;
    }

    public void setGrappleTweenStartPos(Vector3 pos) {
        this.grappleTweenStartPos.set(pos.x, pos.y, pos.z);
    }

    public float getGrappleTweenInTime() {
        return this.sourceNode.grappleTweenInTime;
    }

    public Iterable<AnimationTrack> getMainAnimationTracks() {
        if (this.isMainAnimActive()) {
            return this.animationTracks;
        }
        return PZEmptyIterable.getInstance();
    }

    public int getMainAnimationTracksCount() {
        return this.animationTracks.size();
    }

    public AnimationTrack getMainAnimationTrackAt(int idx) {
        return this.animationTracks.get(idx);
    }

    public boolean containsMainAnimationTrack(AnimationTrack track) {
        return this.animationTracks.contains(track);
    }

    public boolean hasMainAnimationTracks() {
        return !this.animationTracks.isEmpty();
    }

    public boolean incrementWhileAliveFlagOnce(AnimationVariableReference variableReference, boolean whileAliveFlagValue) {
        boolean alreadyExists = PZArrayUtil.contains(this.whileAliveFlags, variableReference, (reference, entry) -> entry.variableReference == reference);
        if (alreadyExists) {
            return false;
        }
        return this.whileAliveFlags.add(WhileAliveFlag.alloc(variableReference, whileAliveFlagValue));
    }

    public ArrayList<WhileAliveFlag> getWhileAliveFlags() {
        return this.whileAliveFlags;
    }

    private void decrementWhileAliveFlags() {
        if (this.animLayer == null) {
            return;
        }
        this.animLayer.decrementWhileAliveFlags(this);
    }

    public boolean getUseDeferredRotation() {
        return this.sourceNode.useDeferedRotation;
    }

    public boolean getUseDeferredMovement() {
        return this.sourceNode.useDeferredMovement;
    }

    public float getDeferredRotationScale() {
        return this.sourceNode.deferredRotationScale;
    }

    public void onTransferredToLayer(AnimLayer newParentLayer) {
        AnimationTrack track;
        int i;
        this.animLayer = newParentLayer;
        boolean useBoneMasks = this.animLayer.isSubLayer();
        for (i = 0; i < this.animationTracks.size(); ++i) {
            track = this.animationTracks.get(i);
            track.setAnimLayer(this.animLayer);
            this.initTrackBoneWeights(track, useBoneMasks);
        }
        for (i = 0; i < this.ragdollTracks.size(); ++i) {
            track = this.ragdollTracks.get(i);
            track.setAnimLayer(this.animLayer);
            this.initTrackBoneWeights(track, useBoneMasks);
        }
    }

    private void initTrackBoneWeights(AnimationTrack track, boolean useBoneMasks) {
        AnimNode node = this.getSourceNode();
        if (node == null) {
            return;
        }
        SkinningData skinningData = this.getSkinningData();
        if (skinningData == null) {
            return;
        }
        if (useBoneMasks) {
            track.setBoneWeights(node.subStateBoneWeights);
            track.initBoneWeights(skinningData);
        } else {
            track.setBoneWeights(null);
        }
    }

    private SkinningData getSkinningData() {
        if (this.animLayer == null) {
            return null;
        }
        return this.animLayer.getSkinningData();
    }

    private static class TransitionIn {
        private float time;
        private String transitionedFrom;
        private boolean active;
        private AnimationTrack track;
        private AnimTransition data;
        private float weight;
        private float rawWeight;
        private boolean blendingIn;
        private boolean blendingOut;

        private TransitionIn() {
        }

        private void reset() {
            this.time = 0.0f;
            this.transitionedFrom = null;
            this.active = false;
            this.track = null;
            this.data = null;
            this.weight = 0.0f;
            this.rawWeight = 0.0f;
            this.blendingIn = false;
            this.blendingOut = false;
        }
    }

    public static class WhileAliveFlag
    extends PooledObject {
        public AnimationVariableReference variableReference;
        public boolean whileAliveValue;
        private static final Pool<WhileAliveFlag> s_pool = new Pool<WhileAliveFlag>(WhileAliveFlag::new);

        private WhileAliveFlag() {
        }

        @Override
        public void onReleased() {
            this.variableReference = null;
            this.whileAliveValue = false;
        }

        public static WhileAliveFlag alloc(AnimationVariableReference variableReference, boolean whileAliveFlagValue) {
            WhileAliveFlag newInstance = s_pool.alloc();
            newInstance.variableReference = variableReference;
            newInstance.whileAliveValue = whileAliveFlagValue;
            return newInstance;
        }
    }
}

