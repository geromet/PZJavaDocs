/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.anticheats.PacketValidator;

public class AntiCheatRecipeUpdate
extends AbstractAntiCheat {
    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);
        PacketValidator field = connection.getValidator();
        if (field.checksumIntervalCheck()) {
            field.checksumSend(false, false);
        }
        return !this.antiCheat.isEnabled() || !field.checksumTimeoutCheck();
    }

    public static interface IAntiCheatUpdate {
        public void checksumSend(boolean var1, boolean var2);

        public boolean checksumIntervalCheck();

        public boolean checksumTimeoutCheck();

        public void checksumTimeoutReset();
    }
}

