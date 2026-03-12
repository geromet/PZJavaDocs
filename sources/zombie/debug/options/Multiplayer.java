/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.IDebugOptionGroup;
import zombie.debug.options.OptionGroup;

public class Multiplayer
extends OptionGroup {
    public final DebugOG debug;
    public final DebugFlagsOG debugFlags;

    public Multiplayer() {
        this.debug = new DebugOG(this.group);
        this.debugFlags = new DebugFlagsOG(this.group);
    }

    public static final class DebugOG
    extends OptionGroup {
        public final BooleanDebugOption attackPlayer = this.newDebugOnlyOption("Attack.Player", false);
        public final BooleanDebugOption followPlayer = this.newDebugOnlyOption("Follow.Player", false);
        public final BooleanDebugOption seeNonPvpZones = this.newDebugOnlyOption("SeeNonPvpZones", false);
        public final BooleanDebugOption anticlippingAlgorithm = this.newDebugOnlyOption("AnticlippingAlgorithm", true);

        DebugOG(IDebugOptionGroup parentGroup) {
            super(parentGroup, "Debug");
        }
    }

    public static final class DebugFlagsOG
    extends OptionGroup {
        public final IsoGameCharacterOG localPlayer;
        public final IsoGameCharacterOG remotePlayer;
        public final IsoGameCharacterOG animal;
        public final IsoGameCharacterOG zombie;
        public final IsoDeadBodyOG deadBody;

        DebugFlagsOG(IDebugOptionGroup parentGroup) {
            super(parentGroup, "DebugFlags");
            this.localPlayer = new IsoGameCharacterOG(this.group, "LocalPlayer");
            this.remotePlayer = new IsoGameCharacterOG(this.group, "RemotePlayer");
            this.animal = new IsoGameCharacterOG(this.group, "Animal");
            this.zombie = new IsoGameCharacterOG(this.group, "Zombie");
            this.deadBody = new IsoDeadBodyOG(this.group);
        }

        public static final class IsoGameCharacterOG
        extends OptionGroup {
            public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", false);
            public final BooleanDebugOption position = this.newDebugOnlyOption("Position", false);
            public final BooleanDebugOption prediction = this.newDebugOnlyOption("Prediction", false);
            public final BooleanDebugOption state = this.newDebugOnlyOption("State", false);
            public final BooleanDebugOption stateVariables = this.newDebugOnlyOption("StateVariables", false);
            public final BooleanDebugOption animation = this.newDebugOnlyOption("Animation", false);
            public final BooleanDebugOption variables = this.newDebugOnlyOption("Variables", false);

            IsoGameCharacterOG(IDebugOptionGroup parentGroup, String name) {
                super(parentGroup, name);
            }
        }

        public static final class IsoDeadBodyOG
        extends OptionGroup {
            public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", false);
            public final BooleanDebugOption position = this.newDebugOnlyOption("Position", false);

            IsoDeadBodyOG(IDebugOptionGroup parentGroup) {
                super(parentGroup, "DeadBody");
            }
        }
    }
}

