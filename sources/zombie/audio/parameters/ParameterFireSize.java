/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;

public final class ParameterFireSize
extends FMODLocalParameter {
    private int size;

    public ParameterFireSize() {
        super("FireSize");
    }

    @Override
    public float calculateCurrentValue() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}

