/*
 * Decompiled with CFR 0.152.
 */
package zombie.sandbox;

import zombie.sandbox.CustomSandboxOption;
import zombie.scripting.ScriptParser;

public final class CustomStringSandboxOption
extends CustomSandboxOption {
    public final String defaultValue;

    CustomStringSandboxOption(String id, String defaultValue) {
        super(id);
        this.defaultValue = defaultValue;
    }

    static CustomStringSandboxOption parse(ScriptParser.Block block) {
        ScriptParser.Value vDefaultValue = block.getValue("default");
        if (vDefaultValue == null) {
            return null;
        }
        CustomStringSandboxOption option = new CustomStringSandboxOption(block.id, vDefaultValue.getValue().trim());
        if (!option.parseCommon(block)) {
            return null;
        }
        return option;
    }
}

