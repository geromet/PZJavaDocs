/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerEnum;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.StringUtils;

public class AnimEventListenerWrapperEnum<E extends Enum<E>>
implements IAnimEventListener,
IAnimEventListenerEnum<E> {
    private final IAnimEventListenerEnum<E> wrapped;
    private final E defaultValue;
    private final Class<E> enumClass;

    private AnimEventListenerWrapperEnum(IAnimEventListenerEnum<E> wrapped, E defaultValue) {
        this.wrapped = wrapped;
        this.defaultValue = defaultValue;
        this.enumClass = this.defaultValue.getClass();
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, layer, track, StringUtils.tryParseEnum(this.enumClass, event.parameterValue, this.defaultValue));
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer animLayer, AnimationTrack animTrack, E param) {
        this.wrapped.animEvent(owner, animLayer, animTrack, param);
    }

    public static <E extends Enum<E>> IAnimEventListener wrapper(IAnimEventListenerEnum<E> wrapped, E defaultValue) {
        if (wrapped instanceof IAnimEventListener) {
            IAnimEventListener iAnimEventListener = (IAnimEventListener)((Object)wrapped);
            return iAnimEventListener;
        }
        return new AnimEventListenerWrapperEnum<E>(wrapped, defaultValue);
    }
}

