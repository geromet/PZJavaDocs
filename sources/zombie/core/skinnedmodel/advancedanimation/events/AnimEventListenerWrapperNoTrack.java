/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerNoTrack;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperNoTrack
implements IAnimEventListener,
IAnimEventListenerNoTrack {
    private final IAnimEventListenerNoTrack wrapped;

    private AnimEventListenerWrapperNoTrack(IAnimEventListenerNoTrack wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, event);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimEvent event) {
        this.wrapped.animEvent(owner, event);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerNoTrack wrapped) {
        if (wrapped instanceof IAnimEventListener) {
            IAnimEventListener iAnimEventListener = (IAnimEventListener)((Object)wrapped);
            return iAnimEventListener;
        }
        return new AnimEventListenerWrapperNoTrack(wrapped);
    }
}

