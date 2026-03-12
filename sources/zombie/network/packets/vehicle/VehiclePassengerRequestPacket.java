/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.vehicle;

import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class VehiclePassengerRequestPacket
implements INetworkPacket {
    @JSONField
    protected int playerIndex;
    @JSONField
    protected int wx;
    @JSONField
    protected int wy;
    @JSONField
    protected long loaded;

    public void set(int playerIndex, int wx, int wy, long loaded) {
        this.playerIndex = playerIndex;
        this.wx = wx;
        this.wy = wy;
        this.loaded = loaded;
    }

    @Override
    public void setData(Object ... values2) {
        this.set((Integer)values2[0], (Integer)values2[1], (Integer)values2[2], (Long)values2[3]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerIndex = b.getInt();
        this.wx = b.getInt();
        this.wy = b.getInt();
        this.loaded = b.getLong();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.playerIndex);
        b.putInt(this.wx);
        b.putInt(this.wy);
        b.putLong(this.loaded);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer isoPlayer;
        IsoPlayer player;
        block6: {
            block5: {
                player = connection.players[this.playerIndex];
                if (player == null || player.getVehicle() == null) {
                    return;
                }
                IsoGameCharacter driver = player.getVehicle().getDriver();
                if (!(driver instanceof IsoPlayer)) break block5;
                isoPlayer = (IsoPlayer)driver;
                if (driver != player) break block6;
            }
            return;
        }
        UdpConnection c = GameServer.getConnectionFromPlayer(isoPlayer);
        if (c == null) {
            return;
        }
        INetworkPacket.send(c, PacketTypes.PacketType.VehiclePassengerResponse, player.getVehicle(), player.getVehicle().getSeat(player), this.wx, this.wy, this.loaded);
    }
}

