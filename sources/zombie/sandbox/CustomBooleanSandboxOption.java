/*
 * Decompiled with CFR 0.152.
 */
package zombie.sandbox;

import zombie.sandbox.CustomSandboxOption;
import zombie.scripting.ScriptParser;

public final class CustomBooleanSandboxOption
extends CustomSandboxOption {
    public final boolean defaultValue;

    CustomBooleanSandboxOption(String id, boolean defaultValue) {
        super(id);
        this.defaultValue = defaultValue;
    }

    static CustomBooleanSandboxOption parse(ScriptParser.Block block) {
        ScriptParser.Value vDefaultValue = block.getValue("default");
        if (vDefaultValue == null) {
            return null;
        }
        boolean defaultValue = Boolean.parseBoolean(vDefaultValue.getValue().trim());
        CustomBooleanSandboxOption option = new CustomBooleanSandboxOption(block.id, defaultValue);
        if (!option.parseCommon(block)) {
            return null;
        }
        return option;
    }
}

