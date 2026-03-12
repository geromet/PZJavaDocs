/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerBoolean;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerEnum;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerFloat;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerNoParam;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerNoTrack;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerNoTrackEnum;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerNoTrackString;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerSetVariableString;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListenerString;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public interface IAnimEventWrappedBroadcaster
extends IAnimEventListener {
    public AnimEventBroadcaster getAnimEventBroadcaster();

    default public void addAnimEventListener(String animEventName, IAnimEventListener listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default public void addAnimEventListener(String animEventName, IAnimEventListenerNoTrack listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default public void addAnimEventListener(String animEventName, IAnimEventListenerNoTrackString listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default public void addAnimEventListener(String animEventName, IAnimEventListenerBoolean listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default public void addAnimEventListener(String animEventName, IAnimEventListenerString listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default public void addAnimEventListener(String animEventName, IAnimEventListenerNoParam listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default public void addAnimEventListener(String animEventName, IAnimEventListenerFloat listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default public void addAnimEventListener(IAnimEventListenerSetVariableString listener) {
        this.getAnimEventBroadcaster().addListener(listener);
    }

    default public <E extends Enum<E>> void addAnimEventListener(String animEventName, IAnimEventListenerEnum<E> listener, E defaultValue) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener, defaultValue);
    }

    default public <E extends Enum<E>> void addAnimEventListener(String animEventName, IAnimEventListenerNoTrackEnum<E> listener, E defaultValue) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener, defaultValue);
    }

    @Override
    default public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.getAnimEventBroadcaster().animEvent(owner, layer, track, event);
    }
}

