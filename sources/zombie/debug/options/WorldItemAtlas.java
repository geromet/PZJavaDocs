/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public class WorldItemAtlas
extends OptionGroup {
    public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", true);
    public final BooleanDebugOption render = this.newDebugOnlyOption("Render", false);
}

