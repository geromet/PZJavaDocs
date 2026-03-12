/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.vehicle;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.vehicle.VehicleField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

public class VehiclePassengers
extends VehicleField
implements INetworkPacketField {
    public VehiclePassengers(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        for (int i = 0; i < this.getVehicle().getMaxPassengers(); ++i) {
            short onlineId = b.getShort();
            if (!GameClient.client) continue;
            if (onlineId == -1) {
                IsoGameCharacter isoGameCharacter = this.getVehicle().getCharacter(i);
                if (isoGameCharacter instanceof IsoGameCharacter) {
                    IsoGameCharacter chr = isoGameCharacter;
                    if (chr.isLocal()) continue;
                    chr.setVehicle(null);
                }
                this.getVehicle().clearPassenger(i);
                continue;
            }
            IsoPlayer player = GameClient.IDToPlayerMap.get(onlineId);
            if (player == null) {
                INetworkPacket.send(PacketTypes.PacketType.PlayerDataRequest, onlineId);
                continue;
            }
            if (player.isLocalPlayer()) continue;
            if (this.getVehicle().enterRSync(i, player, this.getVehicle())) {
                player.networkAi.parse(this.getVehicle());
            }
            GameClient.rememberPlayerPosition(player, this.getVehicle().getX(), this.getVehicle().getY());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        for (int i = 0; i < this.getVehicle().getMaxPassengers(); ++i) {
            IsoGameCharacter isoGameCharacter;
            BaseVehicle.Passenger passenger = this.getVehicle().getPassenger(i);
            if (passenger != null && (isoGameCharacter = passenger.character) instanceof IsoPlayer) {
                IsoPlayer player = (IsoPlayer)isoGameCharacter;
                b.putShort(player.getOnlineID());
                continue;
            }
            b.putShort(-1);
        }
    }
}

