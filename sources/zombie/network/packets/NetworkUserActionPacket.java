/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.secure.PZcrypt;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.ModifyNetworkUsers, handlingType=1)
public class NetworkUserActionPacket
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
        if (this.action == Command.SetPassword) {
            this.argument = PZcrypt.hash(ServerWorldDatabase.encrypt(this.argument));
        }
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
        try {
            IsoPlayer pl = GameServer.getPlayerByUserName(this.username);
            IsoPlayer admin = connection.players[0];
            UdpConnection c = GameServer.getConnectionFromPlayer(pl);
            switch (this.action.ordinal()) {
                case 0: {
                    if (!admin.getRole().hasCapability(Capability.ModifyNetworkUsers)) break;
                    String password = PZcrypt.hash(ServerWorldDatabase.encrypt(this.argument));
                    ServerWorldDatabase.instance.addUser(this.username, password);
                    break;
                }
                case 1: {
                    if (!admin.getRole().hasCapability(Capability.ModifyNetworkUsers)) break;
                    ServerWorldDatabase.instance.removeWholeUserLog(this.username);
                    ServerWorldDatabase.instance.removeUser(this.username);
                    break;
                }
                case 2: {
                    if (!admin.getRole().hasCapability(Capability.ModifyNetworkUsers)) break;
                    ServerWorldDatabase.instance.resetUserGoogleKey(this.username);
                    break;
                }
                case 3: {
                    if (!admin.getRole().hasCapability(Capability.ModifyNetworkUsers)) break;
                    ServerWorldDatabase.instance.setPassword(this.username, "");
                    break;
                }
                case 4: {
                    if (!admin.getRole().hasCapability(Capability.ModifyNetworkUsers)) break;
                    ServerWorldDatabase.instance.setPassword(this.username, this.argument);
                    break;
                }
                case 5: {
                    if (!admin.getRole().hasCapability(Capability.ChangeAccessLevel)) break;
                    GameServer.changeRole(admin.getUsername(), connection, this.username, this.argument);
                }
            }
            if (connection.getRole().hasCapability(Capability.SeeNetworkUsers)) {
                INetworkPacket.send(connection, PacketTypes.PacketType.NetworkUsers, new Object[0]);
            }
        }
        catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static enum Command {
        Add,
        Delete,
        ResetTOTPSecret,
        ResetPassword,
        SetPassword,
        SetRole;

    }
}

