/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.SoundManager;
import zombie.audio.FMODGlobalParameter;

public final class ParameterMusicToggleMute
extends FMODGlobalParameter {
    public ParameterMusicToggleMute() {
        super("MusicToggleMute");
    }

    @Override
    public float calculateCurrentValue() {
        return SoundManager.instance.allowMusic ? 0.0f : 1.0f;
    }
}

