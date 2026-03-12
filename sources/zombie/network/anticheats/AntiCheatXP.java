/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.SandboxOptions;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatXP
extends AbstractAntiCheat {
    private static final float MAX_XP_GROWTH_RATE = 0.0f;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (connection.getRole().hasCapability(Capability.AddXP)) {
            return result;
        }
        double maxGrowthXP = 0.0 * SandboxOptions.instance.multipliersConfig.xpMultiplierGlobal.getValue();
        if ((double)field.getAmount() > maxGrowthXP) {
            return String.format("xp=%f > max=%f", Float.valueOf(field.getAmount()), maxGrowthXP);
        }
        return result;
    }

    public static interface IAntiCheat {
        public IsoPlayer getPlayer();

        public float getAmount();
    }
}

