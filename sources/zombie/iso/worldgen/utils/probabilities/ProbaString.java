/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.utils.probabilities;

import zombie.SandboxOptions;
import zombie.iso.worldgen.utils.probabilities.Probability;

public class ProbaString
implements Probability {
    private final Double value;
    private final String clazz;
    private final String field;

    public ProbaString(String value) {
        SandboxOptions.SandboxOption option;
        this.clazz = value.split("\\.")[0];
        this.field = value.split("\\.")[1];
        if ("Sandbox".equals(this.clazz)) {
            option = SandboxOptions.instance.getOptionByName(this.field);
            if (option == null) {
                throw new IllegalArgumentException("Invalid sandbox option: " + this.field);
            }
        } else {
            throw new IllegalArgumentException("Unknown class type: " + this.clazz);
        }
        this.value = ((SandboxOptions.DoubleSandboxOption)option).getValue();
    }

    @Override
    public float getValue() {
        return this.value.floatValue();
    }
}

