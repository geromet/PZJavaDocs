/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatPlayer
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        connection.getValidator().playerUpdateTimeoutReset();
        IAntiCheat field = (IAntiCheat)((Object)packet);
        for (IsoPlayer player : connection.players) {
            if (player != field.getPlayer()) continue;
            return result;
        }
        return "invalid player";
    }

    public static interface IAntiCheat {
        public IsoPlayer getPlayer();
    }
}

