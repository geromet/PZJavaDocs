/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerFloat;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperFloat
implements IAnimEventListener,
IAnimEventListenerFloat {
    private final IAnimEventListenerFloat wrapped;

    private AnimEventListenerWrapperFloat(IAnimEventListenerFloat wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, PZMath.tryParseFloat(event.parameterValue, 0.0f));
    }

    @Override
    public void animEvent(IsoGameCharacter owner, float param) {
        this.wrapped.animEvent(owner, param);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerFloat wrapped) {
        if (wrapped instanceof IAnimEventListener) {
            IAnimEventListener iAnimEventListener = (IAnimEventListener)((Object)wrapped);
            return iAnimEventListener;
        }
        return new AnimEventListenerWrapperFloat(wrapped);
    }
}

