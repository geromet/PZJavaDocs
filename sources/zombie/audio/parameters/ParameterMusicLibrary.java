/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.core.Core;

public final class ParameterMusicLibrary
extends FMODGlobalParameter {
    public ParameterMusicLibrary() {
        super("MusicLibrary");
    }

    @Override
    public float calculateCurrentValue() {
        return switch (Core.getInstance().getOptionMusicLibrary()) {
            case 2 -> Library.EarlyAccess.label;
            case 3 -> Library.Random.label;
            default -> Library.Official.label;
        };
    }

    public static enum Library {
        Official(0),
        EarlyAccess(1),
        Random(2);

        final int label;

        private Library(int label) {
            this.label = label;
        }
    }
}

