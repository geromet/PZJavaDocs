/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitShortDistance
extends AbstractAntiCheat {
    private static final int MAX_RELEVANT_RANGE = 10;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        float distance = field.getDistance();
        if (distance > 10.0f) {
            return String.format("distance=%f > range=%d", Float.valueOf(distance), 10);
        }
        return result;
    }

    public static interface IAntiCheat {
        public float getDistance();
    }
}

