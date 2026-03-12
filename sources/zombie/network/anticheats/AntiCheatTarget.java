/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import java.util.Arrays;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.fields.hit.Character;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTarget
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (Arrays.stream(connection.players).noneMatch(p -> p.getOnlineID() == field.getTargetCharacter().getCharacter().getOnlineID())) {
            return "invalid target";
        }
        return result;
    }

    public static interface IAntiCheat {
        public Character getTargetCharacter();
    }
}

