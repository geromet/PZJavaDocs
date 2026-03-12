/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.AddUserlog, handlingType=1)
public class AddUserlogPacket
implements INetworkPacket {
    @JSONField
    String username;
    @JSONField
    String type;
    @JSONField
    String text;

    @Override
    public void setData(Object ... values2) {
        this.username = (String)values2[0];
        this.type = (String)values2[1];
        this.text = (String)values2[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.username);
        b.putUTF(this.type);
        b.putUTF(this.text);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.username = b.getUTF();
        this.type = b.getUTF();
        this.text = b.getUTF();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ServerWorldDatabase.instance.addUserlog(this.username, Userlog.UserlogType.FromString(this.type), this.text, connection.getUserName(), 1);
        LoggerManager.getLogger("admin").write(connection.getUserName() + " added log on user " + this.username + ", log: " + this.text);
    }
}

