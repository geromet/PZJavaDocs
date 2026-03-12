/*
 * Decompiled with CFR 0.152.
 */
package zombie.sandbox;

import zombie.sandbox.CustomSandboxOption;
import zombie.scripting.ScriptParser;

public final class CustomDoubleSandboxOption
extends CustomSandboxOption {
    public final double min;
    public final double max;
    public final double defaultValue;

    CustomDoubleSandboxOption(String id, double min, double max, double defaultValue) {
        super(id);
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    static CustomDoubleSandboxOption parse(ScriptParser.Block block) {
        double min = CustomDoubleSandboxOption.getValueDouble(block, "min", Double.NaN);
        double max = CustomDoubleSandboxOption.getValueDouble(block, "max", Double.NaN);
        double defaultValue = CustomDoubleSandboxOption.getValueDouble(block, "default", Double.NaN);
        if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(defaultValue)) {
            return null;
        }
        CustomDoubleSandboxOption option = new CustomDoubleSandboxOption(block.id, min, max, defaultValue);
        if (!option.parseCommon(block)) {
            return null;
        }
        return option;
    }
}

