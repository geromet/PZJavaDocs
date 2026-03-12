/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public final class Statistics
extends OptionGroup {
    public final BooleanDebugOption displayAllDebugStatistics = this.newDebugOnlyOption("DisplayAllDebugStats", false);
}

