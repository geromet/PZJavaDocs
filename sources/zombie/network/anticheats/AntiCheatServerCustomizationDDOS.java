/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatServerCustomizationDDOS
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (System.currentTimeMillis() / 1000L - field.getLastConnect() <= 3600L) {
            return "invalid rate";
        }
        return result;
    }

    public static interface IAntiCheat {
        public long getLastConnect();
    }
}

