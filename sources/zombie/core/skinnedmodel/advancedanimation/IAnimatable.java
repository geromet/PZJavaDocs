/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.characters.action.ActionContext;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.ModelInstance;

public interface IAnimatable
extends IAnimationVariableSource {
    public ActionContext getActionContext();

    default public boolean canTransitionToState(String stateName) {
        ActionContext actionContext = this.getActionContext();
        return actionContext != null && actionContext.canTransitionToState(stateName);
    }

    public AnimationPlayer getAnimationPlayer();

    public AnimationPlayerRecorder getAnimationRecorder();

    public boolean isAnimationRecorderActive();

    public AdvancedAnimator getAdvancedAnimator();

    public ModelInstance getModelInstance();

    public String GetAnimSetName();

    public String getUID();

    default public short getOnlineID() {
        return -1;
    }

    public boolean hasAnimationPlayer();

    public IGrappleable getGrappleable();
}

