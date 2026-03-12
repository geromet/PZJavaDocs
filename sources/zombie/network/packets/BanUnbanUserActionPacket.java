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
import zombie.network.BanSystem;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=3, requiredCapability=Capability.BanUnbanUser, handlingType=1)
public class BanUnbanUserActionPacket
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
        try {
            IsoPlayer pl = GameServer.getPlayerByUserName(this.username);
            IsoPlayer admin = connection.players[0];
            UdpConnection c = GameServer.getConnectionFromPlayer(pl);
            switch (this.action.ordinal()) {
                case 0: {
                    if (!admin.getRole().hasCapability(Capability.KickUser)) break;
                    LoggerManager.getLogger("admin").write(connection.getUserName() + " kicked user " + this.username);
                    ServerWorldDatabase.instance.addUserlog(this.username, Userlog.UserlogType.Kicked, this.argument, connection.getUserName(), 1);
                    if (c == null) break;
                    if ("".equals(this.argument)) {
                        GameServer.kick(c, "UI_Policy_Kick", null);
                    } else {
                        GameServer.kick(c, "UI_Policy_KickReason", this.argument);
                    }
                    c.forceDisconnect("command-kick");
                    break;
                }
                case 1: {
                    BanSystem.BanUser(this.username, connection, this.argument, true);
                    break;
                }
                case 2: {
                    BanSystem.BanUser(this.username, connection, this.argument, false);
                    break;
                }
                case 3: {
                    BanSystem.BanUserByIP(this.username, connection, this.argument, true);
                    break;
                }
                case 4: {
                    BanSystem.BanUserByIP(this.username, connection, this.argument, false);
                    break;
                }
                case 5: {
                    BanSystem.BanIP(this.username, connection, this.argument, false);
                    break;
                }
                case 6: {
                    BanSystem.BanUserBySteamID(this.username, connection, this.argument, true);
                    break;
                }
                case 7: {
                    BanSystem.BanUserBySteamID(this.username, connection, this.argument, false);
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
        Kick,
        Ban,
        UnBan,
        BanIP,
        UnBanIP,
        UnBanIPOnly,
        BanSteamID,
        UnBanSteamID;

    }
}

