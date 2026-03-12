/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkUser;
import zombie.characters.NetworkUsers;
import zombie.characters.Roles;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.RolesPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.SeeNetworkUsers, handlingType=2)
public class NetworkUsersPacket
extends RolesPacket {
    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        ArrayList<IsoPlayer> players = GameServer.getPlayers();
        HashMap<String, NetworkUser> users = new HashMap<String, NetworkUser>();
        ServerWorldDatabase.instance.getWhitelistUsers(users);
        ServerWorldDatabase.instance.getUserlogUsers(users);
        for (IsoPlayer player : players) {
            if (!users.containsKey(player.username)) {
                UdpConnection connection = GameServer.getConnectionFromPlayer(player);
                if (connection == null) continue;
                NetworkUser user = new NetworkUser(GameServer.serverName, player.username, "", player.getRole(), 1, String.valueOf(connection.getSteamId()), player.getDisplayName(), true);
                user.setInWhitelist(false);
                users.put(user.getUsername(), user);
                continue;
            }
            users.get((Object)player.username).online = true;
        }
        ServerWorldDatabase.instance.updateUserCounters(users.values());
        NetworkUsers.send(b, users.values());
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        Roles.setRoles(this.roles, defaultForBanned, defaultForNewUser, defaultForUser, defaultForPriorityUser, defaultForObserver, defaultForGM, defaultForOverseer, defaultForModerator, defaultForAdmin);
        NetworkUsers.instance.parse(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.SeeNetworkUsers)) {
            return;
        }
        Roles.setRoles(this.roles, defaultForBanned, defaultForNewUser, defaultForUser, defaultForPriorityUser, defaultForObserver, defaultForGM, defaultForOverseer, defaultForModerator, defaultForAdmin);
        LuaEventManager.triggerEvent("OnNetworkUsersReceived");
    }
}

