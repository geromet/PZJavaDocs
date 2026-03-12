/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.serverCommands.SetAccessLevelCommand;
import zombie.core.Color;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.RolesEditPacket;

public class Roles {
    public static final Role animal = new Role("animal");
    private static final ArrayList<Role> roles = new ArrayList();
    private static Role defaultForBanned;
    private static Role defaultForNewUser;
    private static Role defaultForUser;
    private static Role defaultForPriorityUser;
    private static Role defaultForObserver;
    private static Role defaultForGM;
    private static Role defaultForOverseer;
    private static Role defaultForModerator;
    private static Role defaultForAdmin;
    private static boolean initialised;

    public static void init() {
        if (initialised) {
            return;
        }
        Roles.addStatic();
        roles.removeIf(r -> !r.isReadOnly());
        ServerWorldDatabase.instance.loadRoles(roles);
        if (roles.get(0).getId() == -1) {
            Roles.save();
            ServerWorldDatabase.instance.loadRoles(roles);
        }
        Collections.sort(roles, (r1, r2) -> Integer.valueOf(r1.getPosition()).compareTo(r2.getPosition()));
        defaultForBanned = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForBanned"));
        defaultForNewUser = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForNewUser"));
        defaultForUser = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForUser"));
        defaultForPriorityUser = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForPriorityUser"));
        defaultForObserver = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForObserver"));
        defaultForGM = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForGM"));
        defaultForOverseer = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForOverseer"));
        defaultForModerator = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForModerator"));
        defaultForAdmin = Roles.getRoleById(ServerWorldDatabase.instance.getDefaultRoleId("defaultForAdmin"));
        initialised = true;
    }

    private static void updatePositions() {
        int position = 0;
        for (Role r : roles) {
            if (r.isReadOnly()) {
                position = r.getPosition() + 1;
                continue;
            }
            r.setPosition(position);
            ++position;
        }
    }

    public static void save() {
        Roles.updatePositions();
        for (Role r : roles) {
            ServerWorldDatabase.instance.saveRole(r);
        }
        ServerWorldDatabase.instance.saveDefaultRole(defaultForBanned, "defaultForBanned");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForNewUser, "defaultForNewUser");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForUser, "defaultForUser");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForPriorityUser, "defaultForPriorityUser");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForObserver, "defaultForObserver");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForGM, "defaultForGM");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForOverseer, "defaultForOverseer");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForModerator, "defaultForModerator");
        ServerWorldDatabase.instance.saveDefaultRole(defaultForAdmin, "defaultForAdmin");
    }

    public static ArrayList<Role> getRoles() {
        return roles;
    }

    public static Role getDefaultForBanned() {
        return defaultForBanned;
    }

    public static Role getDefaultForNewUser() {
        return defaultForNewUser;
    }

    public static Role getDefaultForUser() {
        return defaultForUser;
    }

    public static Role getDefaultForPriorityUser() {
        return defaultForPriorityUser;
    }

    public static Role getDefaultForObserver() {
        return defaultForObserver;
    }

    public static Role getDefaultForGM() {
        return defaultForGM;
    }

    public static Role getDefaultForOverseer() {
        return defaultForOverseer;
    }

    public static Role getDefaultForModerator() {
        return defaultForModerator;
    }

    public static Role getDefaultForAdmin() {
        return defaultForAdmin;
    }

    public static void addRole(String name) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.RolesEdit, new Object[]{RolesEditPacket.Command.AddRole, name});
            return;
        }
        Role r = Roles.getRole(name);
        if (r != null || roles.size() >= 255) {
            return;
        }
        r = new Role(name);
        r.addCapability(Capability.LoginOnServer);
        roles.add(r);
        Collections.sort(roles, (r1, r2) -> Integer.valueOf(r1.getPosition()).compareTo(r2.getPosition()));
        ServerWorldDatabase.instance.saveRole(r);
        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, new Object[0]);
        }
    }

    public static void deleteRole(String name, String adminName) {
        Role r = Roles.getRole(name);
        if (r.isReadOnly()) {
            return;
        }
        if (GameServer.server) {
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getRole() != r) continue;
                try {
                    SetAccessLevelCommand.update(adminName, null, player.getUsername(), defaultForUser.getName());
                    continue;
                }
                catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            ServerWorldDatabase.instance.removeRole(r, defaultForUser);
        }
        if (r != null) {
            if (defaultForBanned == r) {
                defaultForBanned = Roles.getRole("banned");
            }
            if (defaultForNewUser == r) {
                defaultForNewUser = Roles.getRole("user");
            }
            if (defaultForUser == r) {
                defaultForUser = Roles.getRole("user");
            }
            if (defaultForPriorityUser == r) {
                defaultForPriorityUser = Roles.getRole("priority");
            }
            if (defaultForObserver == r) {
                defaultForObserver = Roles.getRole("observer");
            }
            if (defaultForGM == r) {
                defaultForGM = Roles.getRole("gm");
            }
            if (defaultForOverseer == r) {
                defaultForOverseer = Roles.getRole("gm");
            }
            if (defaultForModerator == r) {
                defaultForModerator = Roles.getRole("moderator");
            }
            roles.remove(r);
        }
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.RolesEdit, new Object[]{RolesEditPacket.Command.DeleteRole, name});
            return;
        }
        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, new Object[0]);
        }
    }

    public static void moveRole(byte dir, String name) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.RolesEdit, new Object[]{RolesEditPacket.Command.MoveRole, dir, name});
            return;
        }
        Role r = Roles.getRole(name);
        int rIndex = roles.indexOf(r);
        if (dir == -1 && rIndex > 0) {
            Collections.swap(roles, rIndex, rIndex - 1);
        } else if (rIndex < roles.size() - 1) {
            Collections.swap(roles, rIndex, rIndex + 1);
        }
        Roles.save();
        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, new Object[0]);
        }
    }

    public static void setDefaultRoleFor(String defaultId, String name) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.RolesEdit, new Object[]{RolesEditPacket.Command.SetDefaultRole, defaultId, name});
            return;
        }
        Role r = Roles.getRole(name);
        if (r != null) {
            if ("defaultForBanned".equals(defaultId)) {
                defaultForBanned = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForBanned.getName());
            }
            if ("defaultForNewUser".equals(defaultId)) {
                defaultForNewUser = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForNewUser.getName());
            }
            if ("defaultForUser".equals(defaultId)) {
                defaultForUser = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForUser.getName());
            }
            if ("defaultForPriorityUser".equals(defaultId)) {
                defaultForPriorityUser = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForPriorityUser.getName());
            }
            if ("defaultForObserver".equals(defaultId)) {
                defaultForObserver = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForObserver.getName());
            }
            if ("defaultForGM".equals(defaultId)) {
                defaultForGM = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForGM.getName());
            }
            if ("defaultForOverseer".equals(defaultId)) {
                defaultForOverseer = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForOverseer.getName());
            }
            if ("defaultForModerator".equals(defaultId)) {
                defaultForModerator = r;
                ServerWorldDatabase.instance.saveDefaultRole(r, defaultForModerator.getName());
            }
        }
        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, new Object[0]);
        }
    }

    public static void setupRole(String name, String description, Color color, ArrayList<Capability> capabilities) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.RolesEdit, new Object[]{RolesEditPacket.Command.SetupRole, name, description, color, capabilities});
            return;
        }
        Role r = Roles.getRole(name);
        if (r != null) {
            r.setColor(color);
            r.setDescription(description);
            r.cleanCapability();
            for (Capability c : capabilities) {
                r.addCapability(c);
            }
        }
        Roles.save();
        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, new Object[0]);
        }
    }

    public static Role getRole(String name) {
        for (Role r : roles) {
            if (!r.getName().equals(name)) continue;
            return r;
        }
        return null;
    }

    public static Role getOrDefault(String name) {
        for (Role r : roles) {
            if (!r.getName().equals(name)) continue;
            return r;
        }
        return Roles.getDefaultForNewUser();
    }

    public static void setRoles(ArrayList<Role> roles, Role defaultForBanned, Role defaultForNewUser, Role defaultForUser, Role defaultForPriorityUser, Role defaultForObserver, Role defaultForGM, Role defaultForOverseer, Role defaultForModerator, Role defaultForAdmin) {
        Roles.roles.clear();
        Roles.roles.addAll(roles);
        Roles.defaultForBanned = defaultForBanned;
        Roles.defaultForNewUser = defaultForNewUser;
        Roles.defaultForUser = defaultForUser;
        Roles.defaultForPriorityUser = defaultForPriorityUser;
        Roles.defaultForObserver = defaultForObserver;
        Roles.defaultForGM = defaultForGM;
        Roles.defaultForOverseer = defaultForOverseer;
        Roles.defaultForModerator = defaultForModerator;
        Roles.defaultForAdmin = defaultForAdmin;
    }

    public static Role getRoleById(int id) {
        for (Role r : roles) {
            if (r.getId() != id) continue;
            return r;
        }
        return null;
    }

    public static void addStatic() {
        Role banned = new Role("banned");
        banned.setColor(new Color(0.5f, 0.5f, 0.5f));
        banned.setDescription("Can't login on server.");
        banned.setReadOnly();
        banned.setPosition(1000);
        roles.add(banned);
        Role user = new Role("user");
        user.addCapability(Capability.LoginOnServer);
        user.setColor(new Color(0.9f, 0.9f, 0.9f));
        user.setDescription("Have no capabilities.");
        user.setReadOnly();
        user.setPosition(2000);
        roles.add(user);
        Role priorityUser = new Role("priority");
        priorityUser.addCapability(Capability.LoginOnServer);
        priorityUser.addCapability(Capability.PriorityLogin);
        priorityUser.addCapability(Capability.CantBeKickedIfTooLaggy);
        priorityUser.setColor(new Color(0.9f, 0.9f, 0.9f));
        priorityUser.setDescription("Have login priority");
        priorityUser.setReadOnly();
        priorityUser.setPosition(3000);
        roles.add(priorityUser);
        Role observer = new Role("observer");
        observer.addCapability(Capability.LoginOnServer);
        observer.addCapability(Capability.PriorityLogin);
        observer.addCapability(Capability.CantBeKickedIfTooLaggy);
        observer.addCapability(Capability.ToggleGodModHimself);
        observer.addCapability(Capability.ToggleInvisibleHimself);
        observer.addCapability(Capability.ToggleNoclipHimself);
        observer.addCapability(Capability.SeePlayersConnected);
        observer.addCapability(Capability.TeleportToPlayer);
        observer.addCapability(Capability.TeleportToCoordinates);
        observer.addCapability(Capability.SeePublicServerOptions);
        observer.addCapability(Capability.CanOpenLockedDoors);
        observer.addCapability(Capability.CanGoInsideSafehouses);
        observer.addCapability(Capability.CanAlwaysJoinServer);
        observer.addCapability(Capability.SeesInvisiblePlayers);
        observer.addCapability(Capability.CanSeePlayersStats);
        observer.addCapability(Capability.CanSeeMessageForAdmin);
        observer.addCapability(Capability.PVPLogTool);
        observer.addCapability(Capability.CantBeKickedByAnticheat);
        observer.addCapability(Capability.CantBeBannedByAnticheat);
        observer.addCapability(Capability.SeeWorldMap);
        observer.addCapability(Capability.UIManagerProcessCommands);
        observer.addCapability(Capability.UseDebugContextMenu);
        observer.setColor(new Color(0.0f, 0.6f, 1.0f));
        observer.setDescription("Can use teleport, god mode, go inside safehouse. But he can't add xp, items and make another change.");
        observer.setReadOnly();
        observer.setPosition(4000);
        roles.add(observer);
        Role gm = new Role("gm");
        gm.addCapability(Capability.LoginOnServer);
        gm.addCapability(Capability.PriorityLogin);
        gm.addCapability(Capability.CantBeKickedIfTooLaggy);
        gm.addCapability(Capability.ToggleGodModHimself);
        gm.addCapability(Capability.ToggleGodModEveryone);
        gm.addCapability(Capability.ToggleInvisibleHimself);
        gm.addCapability(Capability.ToggleInvisibleEveryone);
        gm.addCapability(Capability.ToggleNoclipHimself);
        gm.addCapability(Capability.ToggleNoclipEveryone);
        gm.addCapability(Capability.SeePlayersConnected);
        gm.addCapability(Capability.TeleportToPlayer);
        gm.addCapability(Capability.TeleportToCoordinates);
        gm.addCapability(Capability.TeleportPlayerToAnotherPlayer);
        gm.addCapability(Capability.SeePublicServerOptions);
        gm.addCapability(Capability.CanOpenLockedDoors);
        gm.addCapability(Capability.CanGoInsideSafehouses);
        gm.addCapability(Capability.CanAlwaysJoinServer);
        gm.addCapability(Capability.SeesInvisiblePlayers);
        gm.addCapability(Capability.CanSeePlayersStats);
        gm.addCapability(Capability.CanSeeMessageForAdmin);
        gm.addCapability(Capability.PVPLogTool);
        gm.addCapability(Capability.CantBeKickedByAnticheat);
        gm.addCapability(Capability.CantBeBannedByAnticheat);
        gm.addCapability(Capability.SeeWorldMap);
        gm.addCapability(Capability.UIManagerProcessCommands);
        gm.addCapability(Capability.MakeEventsAlarmGunshot);
        gm.addCapability(Capability.StartStopRain);
        gm.addCapability(Capability.AddItem);
        gm.addCapability(Capability.AddXP);
        gm.addCapability(Capability.SeeNetworkUsers);
        gm.addCapability(Capability.CreateStory);
        gm.addCapability(Capability.UseLootTool);
        gm.addCapability(Capability.UseDebugContextMenu);
        gm.setColor(new Color(1.0f, 0.6f, 0.0f));
        gm.setDescription("Can use teleport, god mode, add xp, items and make another change.");
        gm.setReadOnly();
        gm.setPosition(5000);
        roles.add(gm);
        Role moderator = new Role("moderator");
        for (Capability c : Capability.values()) {
            moderator.addCapability(c);
        }
        moderator.removeCapability(Capability.UseMovablesCheat);
        moderator.removeCapability(Capability.SaveWorld);
        moderator.removeCapability(Capability.QuitWorld);
        moderator.removeCapability(Capability.ChangeAndReloadServerOptions);
        moderator.removeCapability(Capability.ReloadLuaFiles);
        moderator.removeCapability(Capability.BypassLuaChecksum);
        moderator.removeCapability(Capability.RolesWrite);
        moderator.removeCapability(Capability.ConnectWithDebug);
        moderator.setColor(new Color(0.2f, 1.0f, 0.2f));
        moderator.setDescription("Can make all except edit roles, reload lua files, change server options.");
        moderator.setReadOnly();
        moderator.setPosition(6000);
        roles.add(moderator);
        Role admin = new Role("admin");
        for (Capability c : Capability.values()) {
            admin.addCapability(c);
        }
        admin.setColor(new Color(1.0f, 0.2f, 0.2f));
        admin.setDescription("Have all capabilities.");
        admin.setReadOnly();
        admin.setPosition(7000);
        roles.add(admin);
        defaultForBanned = banned;
        defaultForNewUser = user;
        defaultForUser = user;
        defaultForPriorityUser = priorityUser;
        defaultForObserver = observer;
        defaultForGM = gm;
        defaultForOverseer = gm;
        defaultForModerator = moderator;
        defaultForAdmin = admin;
    }
}

