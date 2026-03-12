/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;

public interface IAnimEventListenerNoTrackEnum<E extends Enum<E>> {
    public void animEvent(IsoGameCharacter var1, E var2);
}

