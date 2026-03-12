/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;

public class AntiCheatXPUpdate
extends AbstractAntiCheat {
    private static final float MAX_XP_GROWTH_RATE = 1000.0f;

    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);
        boolean result = true;
        for (IsoPlayer player : connection.players) {
            if (player == null) continue;
            result &= this.update(connection, player);
        }
        return result;
    }

    private boolean update(UdpConnection connection, IsoGameCharacter character) {
        float xpMultiplier;
        float xpGrowthRate;
        IsoGameCharacter.XP field = character.getXp();
        return !this.antiCheat.isEnabled() || !field.intervalCheck() || !((xpGrowthRate = field.getGrowthRate()) > 1000.0f * (xpMultiplier = field.getMultiplier()));
    }

    public static interface IAntiCheatUpdate {
        public boolean intervalCheck();

        public float getGrowthRate();

        public float getMultiplier();
    }
}

