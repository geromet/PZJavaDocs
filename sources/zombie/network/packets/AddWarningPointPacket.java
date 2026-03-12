/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
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

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.AddUserlog, handlingType=1)
public class AddWarningPointPacket
implements INetworkPacket {
    @JSONField
    String username;
    @JSONField
    String reason;
    @JSONField
    int amount;

    @Override
    public void setData(Object ... values2) {
        this.username = (String)values2[0];
        this.reason = (String)values2[1];
        this.amount = (Integer)values2[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.username);
        b.putUTF(this.reason);
        b.putInt(this.amount);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.username = b.getUTF();
        this.reason = b.getUTF();
        this.amount = b.getInt();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            ServerWorldDatabase.instance.addWarningPoint(this.username, this.reason, this.amount, connection.getUserName());
        }
        catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        LoggerManager.getLogger("admin").write(connection.getUserName() + " added " + this.amount + " warning point(s) on " + this.username + ", reason:" + this.reason);
        IsoPlayer user = GameServer.getPlayerByRealUserName(this.username);
        if (user != null) {
            INetworkPacket.send(user, PacketTypes.PacketType.WorldMessage, connection.getUserName(), " gave you " + this.amount + " warning point(s), reason: " + this.reason + " ");
        }
    }
}

