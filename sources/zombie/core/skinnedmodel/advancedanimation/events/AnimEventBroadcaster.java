/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerList;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerSetVariableWrapperString;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperBoolean;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperEnum;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperFloat;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperNoParam;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperNoTrack;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperNoTrackEnum;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperNoTrackString;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventListenerWrapperString;
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

public class AnimEventBroadcaster
implements IAnimEventListener {
    private final Map<String, AnimEventListenerList> listeners = new TreeMap<String, AnimEventListenerList>(String.CASE_INSENSITIVE_ORDER);

    public void addListener(String animEventName, IAnimEventListener listener) {
        AnimEventListenerList listenerList = this.getOrCreateListenerList(animEventName);
        listenerList.listeners.add(listener);
    }

    public void addListener(String animEventName, IAnimEventListenerNoTrack listener) {
        this.addListener(animEventName, AnimEventListenerWrapperNoTrack.wrapper(listener));
    }

    public void addListener(String animEventName, IAnimEventListenerNoTrackString listener) {
        this.addListener(animEventName, AnimEventListenerWrapperNoTrackString.wrapper(listener));
    }

    public void addListener(String animEventName, IAnimEventListenerBoolean listener) {
        this.addListener(animEventName, AnimEventListenerWrapperBoolean.wrapper(listener));
    }

    public void addListener(String animEventName, IAnimEventListenerString listener) {
        this.addListener(animEventName, AnimEventListenerWrapperString.wrapper(listener));
    }

    public void addListener(String animEventName, IAnimEventListenerNoParam listener) {
        this.addListener(animEventName, AnimEventListenerWrapperNoParam.wrapper(listener));
    }

    public void addListener(String animEventName, IAnimEventListenerFloat listener) {
        this.addListener(animEventName, AnimEventListenerWrapperFloat.wrapper(listener));
    }

    public <E extends Enum<E>> void addListener(String animEventName, IAnimEventListenerEnum<E> listener, E defaultValue) {
        this.addListener(animEventName, AnimEventListenerWrapperEnum.wrapper(listener, defaultValue));
    }

    public <E extends Enum<E>> void addListener(String animEventName, IAnimEventListenerNoTrackEnum<E> listener, E defaultValue) {
        this.addListener(animEventName, AnimEventListenerWrapperNoTrackEnum.wrapper(listener, defaultValue));
    }

    public void addListener(IAnimEventListenerSetVariableString listener) {
        this.addListener("SetVariable", AnimEventListenerSetVariableWrapperString.wrapper(listener));
    }

    private AnimEventListenerList getOrCreateListenerList(String animEventName) {
        AnimEventListenerList listenerList = this.getAnimEventListenerList(animEventName);
        if (listenerList == null) {
            listenerList = new AnimEventListenerList();
            this.listeners.put(animEventName, listenerList);
        }
        return listenerList;
    }

    private AnimEventListenerList getAnimEventListenerList(String animEventName) {
        return this.listeners.get(animEventName);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (this.listeners.isEmpty()) {
            return;
        }
        AnimEventListenerList listenerList = this.getAnimEventListenerList(event.eventName);
        if (listenerList == null) {
            return;
        }
        List<IAnimEventListener> listeners = listenerList.listeners;
        for (int i = 0; i < listeners.size(); ++i) {
            IAnimEventListener listener = listeners.get(i);
            listener.animEvent(owner, layer, track, event);
        }
    }
}

