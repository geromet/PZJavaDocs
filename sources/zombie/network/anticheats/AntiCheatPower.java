/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatPower
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (connection.getRole().hasCapability(Capability.ToggleGodModHimself)) {
            return result;
        }
        int mask = 3072;
        if (field.getPlayer().networkAi.doCheckAccessLevel() && (field.getBooleanVariables() & 0xC00) != 0) {
            return "invalid mode";
        }
        return result;
    }

    public static interface IAntiCheat {
        public short getBooleanVariables();

        public IsoPlayer getPlayer();
    }
}

