/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.AnswerTickets, handlingType=1)
public class ViewedTicketPacket
implements INetworkPacket {
    @JSONField
    String author;
    @JSONField
    int ticketId;

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.author);
        b.putInt(this.ticketId);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.author = b.getUTF();
        this.ticketId = b.getInt();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            ServerWorldDatabase.instance.viewedTicket(this.ticketId, true);
            GameServer.sendTickets(null, connection);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setData(Object ... values2) {
        this.author = (String)values2[0];
        this.ticketId = (Integer)values2[1];
    }
}

