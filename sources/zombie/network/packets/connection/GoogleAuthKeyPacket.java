/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.connection;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=0, reliability=2, requiredCapability=Capability.None, handlingType=7)
public class GoogleAuthKeyPacket
implements INetworkPacket {
    @JSONField
    protected String username;
    @JSONField
    protected String password;
    @JSONField
    protected String key;
    @JSONField
    String clientVersion;
    @JSONField
    int authType;
    @JSONField
    String message;

    @Override
    public void setData(Object ... values2) {
        this.set((String)values2[0]);
    }

    private void set(String message) {
        this.message = message;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        block14: {
            ConnectionManager.log("receive-packet", "GoogleAuthKey", connection);
            String serverVersion = Core.getInstance().getVersionNumber();
            if (!this.clientVersion.equals(serverVersion)) {
                LoggerManager.getLogger("user").write("access denied: user \"" + this.username + "\" client version (" + this.clientVersion + ") does not match server version (" + serverVersion + ")");
                INetworkPacket.send(connection, PacketTypes.PacketType.AccessDenied, "ClientVersionMismatch##" + this.clientVersion + "##" + serverVersion);
                connection.forceDisconnect("access-denied-client-version");
            }
            connection.setIP(connection.getInetSocketAddress().getHostString());
            connection.setUserName(this.username);
            LoggerManager.getLogger("user").write(connection.getIDStr() + " \"" + this.username + "\" sent google secret key");
            ServerWorldDatabase.LogonResult r = ServerWorldDatabase.instance.authClient(this.username, this.password, connection.getIP(), connection.getSteamId(), this.authType);
            if (r.authorized) {
                try {
                    String savedGoogleKey;
                    if (ServerOptions.instance.autoCreateUserInWhiteList.getValue() && !ServerWorldDatabase.instance.containsUser(this.username)) {
                        ServerWorldDatabase.instance.addUser(this.username, this.password, this.authType);
                    }
                    if ((savedGoogleKey = ServerWorldDatabase.instance.getUserGoogleKey(this.username)) == null || savedGoogleKey.isEmpty()) {
                        if (ServerWorldDatabase.instance.setUserGoogleKey(this.username, this.key)) {
                            INetworkPacket.send(connection, PacketTypes.PacketType.GoogleAuthKey, "QR key registered successfully");
                        } else {
                            INetworkPacket.send(connection, PacketTypes.PacketType.GoogleAuthKey, "The user " + this.username + " is not in whitelist");
                        }
                        connection.forceDisconnect("google-key-saved");
                        break block14;
                    }
                    INetworkPacket.send(connection, PacketTypes.PacketType.GoogleAuthKey, "Registration failed. Google key already exists");
                    connection.forceDisconnect("google-key-already-exists");
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                if (!r.role.hasCapability(Capability.LoginOnServer)) {
                    LoggerManager.getLogger("user").write("access denied: user \"" + this.username + "\" is banned");
                    if (r.bannedReason != null && !r.bannedReason.isEmpty()) {
                        INetworkPacket.send(connection, PacketTypes.PacketType.AccessDenied, "BannedReason##" + r.bannedReason);
                    } else {
                        INetworkPacket.send(connection, PacketTypes.PacketType.AccessDenied, "Banned");
                    }
                } else if (!r.authorized) {
                    LoggerManager.getLogger("user").write("access denied: user \"" + this.username + "\" reason \"" + r.dcReason + "\"");
                    INetworkPacket.send(connection, PacketTypes.PacketType.AccessDenied, r.dcReason != null ? r.dcReason : "AccessDenied");
                }
                connection.forceDisconnect("access-denied-unauthorized");
            }
        }
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        LuaEventManager.triggerEvent("OnQRReceived", this.message);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.processClientLoading(connection);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        if (!GameServer.server) {
            this.message = bb.getUTF();
            return;
        }
        this.username = bb.getUTF();
        this.password = bb.getUTF();
        this.key = bb.getUTF();
        this.clientVersion = bb.getUTF();
        this.authType = bb.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (GameServer.server) {
            b.putUTF(this.message);
            return;
        }
        b.putUTF(GameClient.username);
        b.putUTF(GameClient.password);
        b.putUTF(GameClient.googleKey);
        b.putUTF(Core.getInstance().getVersionNumber());
        b.putInt(GameClient.authType);
    }
}

