/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoFire;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

public class AntiCheatFire
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (!field.getSmoke()) {
            if (ServerOptions.instance.noFire.getValue()) {
                return "fire is disabled";
            }
            if (!IsoFire.CanAddFire(field.getSquare().getSquare(), field.getIgnition(), field.getSmoke())) {
                return "invalid square";
            }
        }
        if (!connection.isRelevantTo(field.getSquare().getX(), field.getSquare().getY())) {
            return "irrelevant square";
        }
        return result;
    }

    public static interface IAntiCheat {
        public boolean getSmoke();

        public boolean getIgnition();

        public Square getSquare();
    }
}

