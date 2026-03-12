/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimTransition;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.popman.ObjectPool;
import zombie.util.StringUtils;

public final class TransitionNodeProxy {
    public ArrayList<NodeLayerPair> allNewNodes = new ArrayList();
    public List<NodeLayerPair> allOutgoingNodes = new ArrayList<NodeLayerPair>();
    public List<TransitionNodeProxyData> foundTransitions = new ArrayList<TransitionNodeProxyData>();
    private static final ObjectPool<NodeLayerPair> s_nodeLayerPairPool = new ObjectPool<NodeLayerPair>(NodeLayerPair::new);
    private static final ObjectPool<TransitionNodeProxyData> s_transitionNodeProxyDataPool = new ObjectPool<TransitionNodeProxyData>(TransitionNodeProxyData::new);

    public Boolean HasAnyPossibleTransitions() {
        return !this.allNewNodes.isEmpty() && !this.allOutgoingNodes.isEmpty();
    }

    public NodeLayerPair allocNodeLayerPair(LiveAnimNode liveAnimNode, AnimLayer animLayer) {
        if (Thread.currentThread() != GameWindow.gameThread) {
            boolean bl = true;
        }
        return s_nodeLayerPairPool.alloc().set(liveAnimNode, animLayer);
    }

    public TransitionNodeProxyData allocTransitionNodeProxyData() {
        if (Thread.currentThread() != GameWindow.gameThread) {
            boolean bl = true;
        }
        return s_transitionNodeProxyDataPool.alloc().reset();
    }

    public void reset() {
        if (Thread.currentThread() != GameWindow.gameThread) {
            boolean bl = true;
        }
        s_nodeLayerPairPool.releaseAll((List<NodeLayerPair>)this.allNewNodes);
        this.allNewNodes.clear();
        s_nodeLayerPairPool.releaseAll(this.allOutgoingNodes);
        this.allOutgoingNodes.clear();
        s_transitionNodeProxyDataPool.releaseAll(this.foundTransitions);
        this.foundTransitions.clear();
    }

    public static class NodeLayerPair {
        public LiveAnimNode liveAnimNode;
        public AnimLayer animLayer;

        protected NodeLayerPair() {
        }

        public NodeLayerPair set(LiveAnimNode liveAnimNode, AnimLayer animLayer) {
            this.liveAnimNode = liveAnimNode;
            this.animLayer = animLayer;
            return this;
        }
    }

    public static class TransitionNodeProxyData {
        public LiveAnimNode newAnimNode;
        public LiveAnimNode oldAnimNode;
        public AnimTransition transitionOut;
        public AnimLayer animLayerIn;
        public AnimLayer animLayerOut;

        public Boolean HasValidAnimNodes() {
            return this.newAnimNode != null && this.oldAnimNode != null;
        }

        public Boolean HasValidTransitions() {
            return this.transitionOut != null;
        }

        protected TransitionNodeProxyData() {
        }

        public TransitionNodeProxyData reset() {
            this.newAnimNode = null;
            this.oldAnimNode = null;
            this.transitionOut = null;
            this.animLayerIn = null;
            this.animLayerOut = null;
            return this;
        }

        private boolean isUsingAnimNodesDeferredInfo() {
            return this.transitionOut == null || StringUtils.isNullOrWhitespace(this.transitionOut.deferredBoneName);
        }

        public String getDeferredBoneName() {
            if (this.isUsingAnimNodesDeferredInfo()) {
                return this.oldAnimNode.getDeferredBoneName();
            }
            return this.transitionOut.deferredBoneName;
        }

        public BoneAxis getDeferredBoneAxis() {
            if (this.isUsingAnimNodesDeferredInfo()) {
                return this.oldAnimNode.getDeferredBoneAxis();
            }
            return this.transitionOut.deferredBoneAxis;
        }

        public boolean getUseDeferredRotation() {
            if (this.isUsingAnimNodesDeferredInfo()) {
                return this.oldAnimNode.getUseDeferredRotation();
            }
            return this.transitionOut.useDeferedRotation;
        }

        public boolean getUseDeferredMovement() {
            if (this.isUsingAnimNodesDeferredInfo()) {
                return this.oldAnimNode.getUseDeferredMovement();
            }
            return this.transitionOut.useDeferredMovement;
        }

        public float getDeferredRotationScale() {
            if (this.isUsingAnimNodesDeferredInfo()) {
                return this.oldAnimNode.getDeferredRotationScale();
            }
            return this.transitionOut.deferredRotationScale;
        }
    }
}

