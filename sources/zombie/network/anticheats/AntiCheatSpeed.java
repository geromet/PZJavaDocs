/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.NetworkCharacterAI;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.fields.IMovable;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.PlayerPacket;

public class AntiCheatSpeed
extends AbstractAntiCheat {
    private static final int MAX_SPEED = 10;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        float limit;
        PlayerPacket playerPacket;
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        if (packet instanceof PlayerPacket && !(playerPacket = (PlayerPacket)packet).getPlayer().isDead()) {
            ((NetworkCharacterAI.SpeedChecker)field.getMovable()).set(playerPacket.prediction.position.x, playerPacket.prediction.position.y, playerPacket.getPlayer().isSeatedInVehicle());
        }
        if (connection.getRole().hasCapability(Capability.TeleportToPlayer) || connection.getRole().hasCapability(Capability.TeleportToCoordinates) || connection.getRole().hasCapability(Capability.TeleportPlayerToAnotherPlayer) || connection.getRole().hasCapability(Capability.UseFastMoveCheat)) {
            return result;
        }
        float f = limit = field.getMovable().isVehicle() ? (float)ServerOptions.instance.speedLimit.getValue() : 10.0f;
        if (field.getMovable().getSpeed() > limit) {
            return String.format("speed=%f > limit=%f", Float.valueOf(field.getMovable().getSpeed()), Float.valueOf(limit));
        }
        return result;
    }

    public static interface IAntiCheat {
        public IMovable getMovable();
    }
}

