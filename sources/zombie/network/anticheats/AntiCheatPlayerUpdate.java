/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.anticheats.PacketValidator;

public class AntiCheatPlayerUpdate
extends AbstractAntiCheat {
    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);
        PacketValidator field = connection.getValidator();
        for (IsoPlayer player : connection.players) {
            if (player == null || !player.isDead() && player.getVehicle() == null) continue;
            connection.getValidator().playerUpdateTimeoutReset();
            return true;
        }
        return !this.antiCheat.isEnabled() || !field.playerUpdateTimeoutCheck();
    }

    public static interface IAntiCheatUpdate {
        public boolean playerUpdateTimeoutCheck();

        public void playerUpdateTimeoutReset();
    }
}

