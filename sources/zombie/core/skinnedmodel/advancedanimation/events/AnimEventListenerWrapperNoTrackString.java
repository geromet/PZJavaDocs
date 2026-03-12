/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerNoTrackString;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperNoTrackString
implements IAnimEventListener,
IAnimEventListenerNoTrackString {
    private final IAnimEventListenerNoTrackString wrapped;

    private AnimEventListenerWrapperNoTrackString(IAnimEventListenerNoTrackString wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, event.parameterValue);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, String param) {
        this.wrapped.animEvent(owner, param);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerNoTrackString wrapped) {
        if (wrapped instanceof IAnimEventListener) {
            IAnimEventListener iAnimEventListener = (IAnimEventListener)((Object)wrapped);
            return iAnimEventListener;
        }
        return new AnimEventListenerWrapperNoTrackString(wrapped);
    }
}

