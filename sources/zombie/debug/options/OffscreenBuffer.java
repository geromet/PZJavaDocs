/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public final class OffscreenBuffer
extends OptionGroup {
    public final BooleanDebugOption render = this.newDebugOnlyOption("Render", true);
}

