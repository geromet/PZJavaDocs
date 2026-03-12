/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitWeaponRange
extends AbstractAntiCheat {
    private static final float predictedAdditionalRange = 1.0f;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        HandWeapon weapon = field.getHandWeapon();
        if (weapon == null) {
            return "weapon not found";
        }
        if (field.getDistance() - 1.0f > weapon.getMaxRange()) {
            return String.format("distance=%f > range=%f", Float.valueOf(field.getDistance()), Float.valueOf(weapon.getMaxRange()));
        }
        return result;
    }

    public static interface IAntiCheat {
        public HandWeapon getHandWeapon();

        public float getDistance();
    }
}

