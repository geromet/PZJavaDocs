/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public final class Weather
extends OptionGroup {
    public final BooleanDebugOption fog = this.newDebugOnlyOption("Fog", true);
    public final BooleanDebugOption fx = this.newDebugOnlyOption("Fx", true);
    public final BooleanDebugOption snow = this.newDebugOnlyOption("Snow", true);
    public final BooleanDebugOption showUsablePuddles = this.newDebugOnlyOption("ShowUsablePuddles", false);
    public final BooleanDebugOption waterPuddles = this.newDebugOnlyOption("WaterPuddles", true);
}

