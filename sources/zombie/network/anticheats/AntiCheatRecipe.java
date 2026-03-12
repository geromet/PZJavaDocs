/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatRecipe
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        connection.getValidator().checksumTimeoutReset();
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (field.getClientChecksum() != field.getServerChecksum()) {
            return "invalid checksum";
        }
        return result;
    }

    public static interface IAntiCheat {
        public long getClientChecksum();

        public long getServerChecksum();
    }
}

