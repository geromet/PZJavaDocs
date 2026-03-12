/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerNoParam;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperNoParam
implements IAnimEventListener,
IAnimEventListenerNoParam {
    private final IAnimEventListenerNoParam wrapped;

    private AnimEventListenerWrapperNoParam(IAnimEventListenerNoParam wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner);
    }

    @Override
    public void animEvent(IsoGameCharacter owner) {
        this.wrapped.animEvent(owner);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerNoParam wrapped) {
        if (wrapped instanceof IAnimEventListener) {
            IAnimEventListener iAnimEventListener = (IAnimEventListener)((Object)wrapped);
            return iAnimEventListener;
        }
        return new AnimEventListenerWrapperNoParam(wrapped);
    }
}

