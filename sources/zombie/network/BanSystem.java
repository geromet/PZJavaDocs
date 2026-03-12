/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkUser;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;

public class BanSystem {
    private static final boolean unbanAllUsersWithSteamID = false;

    public static String BanUser(String username, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
            return "You don't have capability to ban/unban users.";
        }
        Role role = ServerWorldDatabase.instance.getUserRoleNameByUsername(username);
        if (role != null && role.hasCapability(Capability.CantBeBannedByUser)) {
            return "This user can't be banned.";
        }
        String usernameAdmin = adminConnection != null ? adminConnection.getUserName() : "System";
        ServerWorldDatabase.instance.banUser(username, ban);
        String message = usernameAdmin + (ban ? " banned" : " unbanned") + " user " + username;
        ServerWorldDatabase.instance.addUserlog(username, Userlog.UserlogType.Banned, argument, usernameAdmin, 1);
        if (ban) {
            LoggerManager.getLogger("admin").write(usernameAdmin + " banned user " + username + (argument != null ? argument : ""), "IMPORTANT");
            BanSystem.KickUser(username, "You were banned", "command-banid");
        } else {
            if (SteamUtils.isSteamModeEnabled()) {
                String steamID = "";
                HashMap<String, NetworkUser> users = new HashMap<String, NetworkUser>();
                ServerWorldDatabase.instance.getWhitelistUsers(users);
                if (users.containsKey(username)) {
                    steamID = users.get((Object)username).steamid;
                }
                ServerWorldDatabase.instance.banSteamID(steamID, argument, false);
            } else {
                String ip = ServerWorldDatabase.instance.getFirstBannedIPForUser(username);
                ServerWorldDatabase.instance.banIp(ip, username, argument, false);
            }
            LoggerManager.getLogger("admin").write(message, "IMPORTANT");
            DebugLog.Multiplayer.println(message);
        }
        return message;
    }

    public static void KickUser(String username, String reason, String description) {
        UdpConnection c;
        IsoPlayer pl = GameServer.getPlayerByUserName(username);
        if (pl != null && (c = GameServer.getConnectionFromPlayer(pl)) != null) {
            GameServer.kick(c, reason, null);
            c.forceDisconnect(description);
        }
    }

    public static String BanUserBySteamID(String steamID, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        Object message = "";
        if (SteamUtils.isSteamModeEnabled()) {
            if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
                return "You don't have capability to ban/unban users.";
            }
            String usernameAdmin = adminConnection != null ? adminConnection.getUserName() : "System";
            ServerWorldDatabase.instance.banSteamID(steamID, argument, ban);
            message = usernameAdmin + (ban ? " banned" : " unbanned") + " SteamID " + steamID + "(";
            HashMap<String, NetworkUser> users = new HashMap<String, NetworkUser>();
            ServerWorldDatabase.instance.getWhitelistUsers(users);
            for (Map.Entry<String, NetworkUser> user : users.entrySet()) {
                String username;
                String userSteamID = user.getValue().getSteamid();
                if (userSteamID == null || !userSteamID.equals(steamID) || (username = user.getKey()).equals(usernameAdmin) && !usernameAdmin.equals("System")) continue;
                ServerWorldDatabase.instance.banUser(username, ban);
                message = (String)message + username + ", ";
                if (!ban) continue;
                BanSystem.KickUser(username, "You were banned", "command-banid");
            }
            message = (String)message + ")" + (argument != null ? argument : "");
            LoggerManager.getLogger("admin").write((String)message, "IMPORTANT");
            DebugLog.Multiplayer.println((String)message);
        }
        return message;
    }

    public static String BanIP(String ip, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        Object message = "";
        if (!SteamUtils.isSteamModeEnabled()) {
            if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
                return "You don't have capability to ban/unban users.";
            }
            String usernameAdmin = adminConnection != null ? adminConnection.getUserName() : "System";
            message = usernameAdmin + (ban ? " banned" : " unbanned") + " IP " + ip + (argument != null ? argument : "");
            LoggerManager.getLogger("admin").write((String)message, "IMPORTANT");
            ServerWorldDatabase.instance.banIp(ip, "", argument, ban);
        } else {
            DebugLog.Multiplayer.println("Server is in Steam mode");
        }
        return message;
    }

    public static String BanUserByIP(String username, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        String message = "";
        if (!SteamUtils.isSteamModeEnabled()) {
            if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
                return "You don't have capability to ban/unban users.";
            }
            String usernameAdmin = adminConnection != null ? adminConnection.getUserName() : "System";
            if (ban) {
                IsoPlayer pl = GameServer.getPlayerByUserName(username);
                if (pl != null) {
                    UdpConnection c = GameServer.getConnectionFromPlayer(pl);
                    if (c != null) {
                        LoggerManager.getLogger("admin").write(usernameAdmin + " banned IP " + c.getIP() + "(" + c.getUserName() + ")" + (argument != null ? argument : ""), "IMPORTANT");
                        ServerWorldDatabase.instance.banIp(c.getIP(), username, argument, true);
                        if (pl.getRole() != Roles.getDefaultForBanned()) {
                            message = BanSystem.BanUser(username, adminConnection, argument, true);
                        }
                    } else {
                        DebugLog.Multiplayer.println("Connection not found");
                    }
                } else {
                    DebugLog.Multiplayer.println("Player not found");
                }
            } else {
                LoggerManager.getLogger("admin").write(usernameAdmin + " unbanned IP (" + username + ")" + (argument != null ? argument : ""), "IMPORTANT");
                String ip = ServerWorldDatabase.instance.getFirstBannedIPForUser(username);
                ServerWorldDatabase.instance.banIp(ip, username, argument, false);
                message = BanSystem.BanUser(username, adminConnection, argument, false);
            }
        }
        return message;
    }
}

