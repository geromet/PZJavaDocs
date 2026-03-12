/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.utils.probabilities;

import zombie.iso.worldgen.utils.probabilities.Probability;

public class ProbaDouble
implements Probability {
    private final Double value;

    public ProbaDouble(Double value) {
        this.value = value;
    }

    @Override
    public float getValue() {
        return this.value.floatValue();
    }
}

