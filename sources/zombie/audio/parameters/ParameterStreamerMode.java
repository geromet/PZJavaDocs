/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.core.Core;

public final class ParameterStreamerMode
extends FMODGlobalParameter {
    public ParameterStreamerMode() {
        super("StreamerMode");
    }

    @Override
    public float calculateCurrentValue() {
        return Core.getInstance().getOptionStreamerMode() ? 1.0f : 0.0f;
    }
}

