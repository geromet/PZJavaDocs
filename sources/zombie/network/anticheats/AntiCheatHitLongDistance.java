/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitLongDistance
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        int range;
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        float distance = field.getDistance();
        if (distance > (float)(range = connection.getRelevantRange() * 8)) {
            return String.format("distance=%f > range=%d", Float.valueOf(distance), range);
        }
        return result;
    }

    public static interface IAntiCheat {
        public float getDistance();
    }
}

