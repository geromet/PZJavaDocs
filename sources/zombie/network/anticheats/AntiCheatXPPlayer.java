/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatXPPlayer
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (connection.getRole().hasCapability(Capability.AddXP)) {
            return result;
        }
        if (!connection.havePlayer(field.getPlayer())) {
            return "invalid player";
        }
        return result;
    }

    public static interface IAntiCheat {
        public IsoPlayer getPlayer();

        public float getAmount();
    }
}

