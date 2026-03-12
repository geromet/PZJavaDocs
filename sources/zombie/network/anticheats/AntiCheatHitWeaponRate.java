/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.characters.AttackRateChecker;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitWeaponRate
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        HandWeapon weapon = field.getHandWeapon();
        if (weapon == null) {
            return "weapon not found";
        }
        AttackRateChecker checker = field.getWielder().getNetworkCharacterAI().attackRateChecker;
        int combatSpeed = 0;
        if (weapon.isAimedFirearm()) {
            combatSpeed = "Auto".equals(weapon.getFireMode()) ? 100 : (!weapon.isTwoHandWeapon() ? 200 : 300);
        }
        if (!(weapon.isAimedFirearm() || "Auto".equals(weapon.getFireMode()) || weapon.isTwoHandWeapon() || weapon.isBareHands() || weapon.isMelee() || !checker.check(weapon.getProjectileCount(), combatSpeed))) {
            return String.format("rate < speed=%d", combatSpeed);
        }
        return result;
    }

    public static interface IAntiCheat {
        public HandWeapon getHandWeapon();

        public float getDistance();

        public IsoPlayer getWielder();

        public IsoGameCharacter getTarget();
    }
}

