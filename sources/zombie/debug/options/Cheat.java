/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public class Cheat
extends OptionGroup {
    public final ClockOG clock = this.newOptionGroup(new ClockOG());
    public final DoorOG door = this.newOptionGroup(new DoorOG());
    public final PlayerOG player = this.newOptionGroup(new PlayerOG());
    public final TimedActionOG timedAction = this.newOptionGroup(new TimedActionOG());
    public final VehicleOG vehicle = this.newOptionGroup(new VehicleOG());
    public final WindowOG window = this.newOptionGroup(new WindowOG());
    public final FarmingOG farming = this.newOptionGroup(new FarmingOG());

    public static final class ClockOG
    extends OptionGroup {
        public final BooleanDebugOption visible = this.newDebugOnlyOption("Visible", false);
    }

    public static final class DoorOG
    extends OptionGroup {
        public final BooleanDebugOption unlock = this.newDebugOnlyOption("Unlock", false);
    }

    public static final class PlayerOG
    extends OptionGroup {
        public final BooleanDebugOption startInvisible = this.newDebugOnlyOption("StartInvisible", false);
        public final BooleanDebugOption invisibleSprint = this.newDebugOnlyOption("InvisibleSprint", false);
        public final BooleanDebugOption seeEveryone = this.newDebugOnlyOption("SeeEveryone", false);
        public final BooleanDebugOption unlimitedCondition = this.newDebugOnlyOption("UnlimitedCondition", false);
        public final BooleanDebugOption fastLooseXp = this.newDebugOnlyOption("FastLooseXp", false);
    }

    public static final class TimedActionOG
    extends OptionGroup {
        public final BooleanDebugOption instant = this.newDebugOnlyOption("Instant", false);
    }

    public static final class VehicleOG
    extends OptionGroup {
        public final BooleanDebugOption mechanicsAnywhere = this.newDebugOnlyOption("MechanicsAnywhere", false);
        public final BooleanDebugOption startWithoutKey = this.newDebugOnlyOption("StartWithoutKey", false);
    }

    public static final class WindowOG
    extends OptionGroup {
        public final BooleanDebugOption unlock = this.newDebugOnlyOption("Unlock", false);
    }

    public static final class FarmingOG
    extends OptionGroup {
        public final BooleanDebugOption fastGrow = this.newDebugOnlyOption("FastGrow", false);
    }
}

