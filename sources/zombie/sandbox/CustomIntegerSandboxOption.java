/*
 * Decompiled with CFR 0.152.
 */
package zombie.sandbox;

import zombie.sandbox.CustomSandboxOption;
import zombie.scripting.ScriptParser;

public final class CustomIntegerSandboxOption
extends CustomSandboxOption {
    public final int min;
    public final int max;
    public final int defaultValue;

    CustomIntegerSandboxOption(String id, int min, int max, int defaultValue) {
        super(id);
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    static CustomIntegerSandboxOption parse(ScriptParser.Block block) {
        int min = CustomIntegerSandboxOption.getValueInt(block, "min", Integer.MIN_VALUE);
        int max = CustomIntegerSandboxOption.getValueInt(block, "max", Integer.MIN_VALUE);
        int defaultValue = CustomIntegerSandboxOption.getValueInt(block, "default", Integer.MIN_VALUE);
        if (min == Integer.MIN_VALUE || max == Integer.MIN_VALUE || defaultValue == Integer.MIN_VALUE) {
            return null;
        }
        CustomIntegerSandboxOption option = new CustomIntegerSandboxOption(block.id, min, max, defaultValue);
        if (!option.parseCommon(block)) {
            return null;
        }
        return option;
    }
}

