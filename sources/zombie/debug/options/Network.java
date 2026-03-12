/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import java.util.Objects;
import zombie.debug.BooleanDebugOption;
import zombie.debug.options.IDebugOptionGroup;
import zombie.debug.options.OptionGroup;

public final class Network
extends OptionGroup {
    public final Client client;
    public final Server server;
    public final PublicServerUtil publicServerUtil;

    public Network() {
        this.client = new Client(this, this.group);
        this.server = new Server(this, this.group);
        this.publicServerUtil = new PublicServerUtil(this, this.group);
    }

    public final class Client
    extends OptionGroup {
        public final BooleanDebugOption mainLoop;
        public final BooleanDebugOption updateZombiesFromPacket;
        public final BooleanDebugOption syncIsoObject;

        public Client(Network this$0, IDebugOptionGroup parent) {
            Objects.requireNonNull(this$0);
            super(parent, "Client");
            this.mainLoop = this.newDebugOnlyOption("MainLoop", true);
            this.updateZombiesFromPacket = this.newDebugOnlyOption("UpdateZombiesFromPacket", true);
            this.syncIsoObject = this.newDebugOnlyOption("SyncIsoObject", true);
        }
    }

    public final class Server
    extends OptionGroup {
        public final BooleanDebugOption syncIsoObject;

        public Server(Network this$0, IDebugOptionGroup parent) {
            Objects.requireNonNull(this$0);
            super(parent, "Server");
            this.syncIsoObject = this.newDebugOnlyOption("SyncIsoObject", true);
        }
    }

    public final class PublicServerUtil
    extends OptionGroup {
        public final BooleanDebugOption enabled;

        public PublicServerUtil(Network this$0, IDebugOptionGroup parent) {
            Objects.requireNonNull(this$0);
            super(parent, "PublicServerUtil");
            this.enabled = this.newDebugOnlyOption("Enabled", true);
        }
    }
}

