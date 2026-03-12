/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHousePlayer
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (!(connection.getRole().hasCapability(Capability.CanSetupSafehouses) || connection.hasPlayer(field.getSafehouse().getOwner()) || connection.hasPlayer(field.getPlayer()))) {
            return "player not found";
        }
        return result;
    }

    public static interface IAntiCheat {
        public String getPlayer();

        public SafeHouse getSafehouse();
    }
}

