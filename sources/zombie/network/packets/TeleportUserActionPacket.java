/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
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

@PacketSetting(ordering=0, priority=1, reliability=3, requiredCapability=Capability.TeleportPlayerToAnotherPlayer, handlingType=1)
public class TeleportUserActionPacket
implements INetworkPacket {
    @JSONField
    public Command action;
    @JSONField
    String username;
    @JSONField
    String argument;

    @Override
    public void setData(Object ... values2) {
        this.action = Command.valueOf((String)values2[0]);
        this.username = (String)values2[1];
        this.argument = (String)values2[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.action);
        b.putUTF(this.username);
        b.putUTF(this.argument);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.action = b.getEnum(Command.class);
        this.username = b.getUTF();
        this.argument = b.getUTF();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer pl = GameServer.getPlayerByUserName(this.username);
        IsoPlayer admin = connection.players[0];
        UdpConnection c = GameServer.getConnectionFromPlayer(pl);
        switch (this.action.ordinal()) {
            case 0: {
                GameServer.sendTeleport(pl, admin.getX(), admin.getY(), admin.getZ());
            }
        }
        if (connection.getRole().hasCapability(Capability.SeeNetworkUsers)) {
            INetworkPacket.send(connection, PacketTypes.PacketType.NetworkUsers, new Object[0]);
        }
    }

    public static enum Command {
        Teleport;

    }
}

