/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import java.util.List;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class StartAnimTrackParameters
extends PooledObject {
    public String animName;
    public int priority;
    public boolean isPrimary;
    public boolean isRagdoll;
    public float ragdollStartTime;
    public float ragdollMaxTime;
    public List<AnimBoneWeight> subLayerBoneWeights;
    public boolean syncTrackingEnabled;
    public String trackTimeToVariable;
    public float speedScale;
    public float initialWeight;
    public boolean isLooped;
    public boolean isReversed;
    public String deferredBoneName;
    public BoneAxis deferredBoneAxis;
    public boolean useDeferredMovement;
    public boolean useDeferredRotation;
    public float deferredRotationScale;
    public String matchingGrappledAnimNode;
    private static final Pool<StartAnimTrackParameters> s_pool = new Pool<StartAnimTrackParameters>(StartAnimTrackParameters::new);

    private void reset() {
        this.animName = null;
        this.priority = 0;
        this.isPrimary = false;
        this.isRagdoll = false;
        this.ragdollStartTime = -1.0f;
        this.ragdollMaxTime = -1.0f;
        this.subLayerBoneWeights = null;
        this.syncTrackingEnabled = false;
        this.trackTimeToVariable = null;
        this.speedScale = 1.0f;
        this.initialWeight = 0.0f;
        this.isLooped = false;
        this.isReversed = false;
        this.deferredBoneName = null;
        this.deferredBoneAxis = BoneAxis.Y;
        this.useDeferredMovement = true;
        this.useDeferredRotation = false;
        this.deferredRotationScale = 1.0f;
        this.matchingGrappledAnimNode = "";
    }

    @Override
    public void onReleased() {
        this.reset();
    }

    protected StartAnimTrackParameters() {
    }

    public static StartAnimTrackParameters alloc() {
        return s_pool.alloc();
    }
}

