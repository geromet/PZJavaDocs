/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.core.Core;

public final class ParameterMusicActionStyle
extends FMODGlobalParameter {
    public ParameterMusicActionStyle() {
        super("MusicActionStyle");
    }

    @Override
    public float calculateCurrentValue() {
        return Core.getInstance().getOptionMusicActionStyle() == 2 ? (float)State.Legacy.label : (float)State.Official.label;
    }

    public static enum State {
        Official(0),
        Legacy(1);

        final int label;

        private State(int label) {
            this.label = label;
        }
    }
}

