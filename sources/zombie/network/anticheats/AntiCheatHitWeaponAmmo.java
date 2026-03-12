/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitWeaponAmmo
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        int ammoCount;
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        HandWeapon weapon = field.getHandWeapon();
        if (weapon == null) {
            return "weapon not found";
        }
        if (weapon.isAimedFirearm() && !field.isIgnoreDamage() && (ammoCount = weapon.getCurrentAmmoCount() + (weapon.isRoundChambered() ? 1 : 0)) <= 0 && !field.getWielder().isUnlimitedAmmo()) {
            return String.format("ammo=%d", ammoCount);
        }
        return result;
    }

    public static interface IAntiCheat {
        public HandWeapon getHandWeapon();

        public IsoPlayer getWielder();

        public boolean isIgnoreDamage();
    }
}

