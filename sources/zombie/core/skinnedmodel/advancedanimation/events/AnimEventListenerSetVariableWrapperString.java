/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimEventSetVariable;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerSetVariableString;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerSetVariableWrapperString
implements IAnimEventListener,
IAnimEventListenerSetVariableString {
    private final IAnimEventListenerSetVariableString wrapped;

    private AnimEventListenerSetVariableWrapperString(IAnimEventListenerSetVariableString wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event instanceof AnimEventSetVariable) {
            AnimEventSetVariable eventsv = (AnimEventSetVariable)event;
            this.animEvent(owner, eventsv.variableReference, eventsv.setVariableValue);
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimationVariableReference variableReference, String variableValue) {
        this.wrapped.animEvent(owner, variableReference, variableValue);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerSetVariableString wrapped) {
        if (wrapped instanceof IAnimEventListener) {
            IAnimEventListener iAnimEventListener = (IAnimEventListener)((Object)wrapped);
            return iAnimEventListener;
        }
        return new AnimEventListenerSetVariableWrapperString(wrapped);
    }
}

