/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHouseSurvivor
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        int daysSurvived;
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (!connection.getRole().hasCapability(Capability.CanSetupSafehouses) && (daysSurvived = ServerOptions.instance.safehouseDaySurvivedToClaim.getValue()) > 0 && field.getSurvivor().getHoursSurvived() < (double)(daysSurvived * 24)) {
            return String.format("player \"%s\" not survived enough", field.getSurvivor().getUsername());
        }
        return result;
    }

    public static interface IAntiCheat {
        public IsoPlayer getSurvivor();
    }
}

