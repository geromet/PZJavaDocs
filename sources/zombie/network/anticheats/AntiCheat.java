/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.BanSystem;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.anticheats.AntiCheatCapability;
import zombie.network.anticheats.AntiCheatChecksum;
import zombie.network.anticheats.AntiCheatChecksumUpdate;
import zombie.network.anticheats.AntiCheatFire;
import zombie.network.anticheats.AntiCheatHitDamage;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.anticheats.AntiCheatHitWeaponRange;
import zombie.network.anticheats.AntiCheatHitWeaponRate;
import zombie.network.anticheats.AntiCheatNoClip;
import zombie.network.anticheats.AntiCheatNone;
import zombie.network.anticheats.AntiCheatPacketException;
import zombie.network.anticheats.AntiCheatPacketRakNet;
import zombie.network.anticheats.AntiCheatPacketType;
import zombie.network.anticheats.AntiCheatPlayer;
import zombie.network.anticheats.AntiCheatPlayerUpdate;
import zombie.network.anticheats.AntiCheatPower;
import zombie.network.anticheats.AntiCheatRecipe;
import zombie.network.anticheats.AntiCheatRecipeUpdate;
import zombie.network.anticheats.AntiCheatSafeHousePlayer;
import zombie.network.anticheats.AntiCheatSafeHouseSurvivor;
import zombie.network.anticheats.AntiCheatSafety;
import zombie.network.anticheats.AntiCheatServerCustomizationDDOS;
import zombie.network.anticheats.AntiCheatSmoke;
import zombie.network.anticheats.AntiCheatSpeed;
import zombie.network.anticheats.AntiCheatTarget;
import zombie.network.anticheats.AntiCheatTransaction;
import zombie.network.anticheats.AntiCheatXP;
import zombie.network.anticheats.AntiCheatXPPlayer;
import zombie.network.anticheats.AntiCheatXPUpdate;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.network.server.EventManager;

public enum AntiCheat {
    Transaction(AntiCheatTransaction.class, ServerOptions.instance.antiCheatItem, 6),
    Safety(AntiCheatSafety.class, ServerOptions.instance.antiCheatSafety, 1),
    HitDamage(AntiCheatHitDamage.class, ServerOptions.instance.antiCheatHit, 1),
    HitLongDistance(AntiCheatHitLongDistance.class, ServerOptions.instance.antiCheatHit, 2),
    HitShortDistance(AntiCheatHitShortDistance.class, ServerOptions.instance.antiCheatHit, 2),
    HitWeaponAmmo(AntiCheatHitWeaponAmmo.class, ServerOptions.instance.antiCheatHit, 1),
    HitWeaponRate(AntiCheatHitWeaponRate.class, ServerOptions.instance.antiCheatHit, 1),
    HitWeaponRange(AntiCheatHitWeaponRange.class, ServerOptions.instance.antiCheatHit, 1),
    Target(AntiCheatTarget.class, ServerOptions.instance.antiCheatHit, 1),
    PacketRakNet(AntiCheatPacketRakNet.class, ServerOptions.instance.antiCheatPacket, 1),
    PacketException(AntiCheatPacketException.class, ServerOptions.instance.antiCheatPacket, 1),
    PacketType(AntiCheatPacketType.class, ServerOptions.instance.antiCheatPacket, 1),
    XP(AntiCheatXP.class, ServerOptions.instance.antiCheatXp, 2),
    XPUpdate(AntiCheatXPUpdate.class, ServerOptions.instance.antiCheatXp, 2),
    XPPlayer(AntiCheatXPPlayer.class, ServerOptions.instance.antiCheatXp, 1),
    Recipe(AntiCheatRecipe.class, ServerOptions.instance.antiCheatRecipe, 4),
    RecipeUpdate(AntiCheatRecipeUpdate.class, ServerOptions.instance.antiCheatRecipe, 4),
    Player(AntiCheatPlayer.class, ServerOptions.instance.antiCheatPlayer, 4),
    PlayerUpdate(AntiCheatPlayerUpdate.class, ServerOptions.instance.antiCheatPlayer, 4),
    Power(AntiCheatPower.class, ServerOptions.instance.antiCheatPermission, 1),
    Capability(AntiCheatCapability.class, ServerOptions.instance.antiCheatPermission, 1),
    Fire(AntiCheatFire.class, ServerOptions.instance.antiCheatFire, 1),
    Smoke(AntiCheatSmoke.class, ServerOptions.instance.antiCheatFire, 1),
    SafeHousePlayer(AntiCheatSafeHousePlayer.class, ServerOptions.instance.antiCheatSafeHouse, 1),
    SafeHouseSurviving(AntiCheatSafeHouseSurvivor.class, ServerOptions.instance.antiCheatSafeHouse, 1),
    Speed(AntiCheatSpeed.class, ServerOptions.instance.antiCheatMovement, 8),
    NoClip(AntiCheatNoClip.class, ServerOptions.instance.antiCheatMovement, 4),
    Checksum(AntiCheatChecksum.class, ServerOptions.instance.antiCheatChecksum, 1),
    ChecksumUpdate(AntiCheatChecksumUpdate.class, ServerOptions.instance.antiCheatChecksum, 1),
    ServerCustomizationDDOS(AntiCheatServerCustomizationDDOS.class, ServerOptions.instance.antiCheatServerCustomization, 4),
    None(AntiCheatNone.class, null, 0);

    private static final long USER_LOG_INTERVAL_MS = 1000L;
    public final AbstractAntiCheat anticheat;
    public final ServerOptions.EnumServerOption option;
    public final int maxSuspiciousCounter;

    private AntiCheat(Class<? extends AbstractAntiCheat> antiCheat, ServerOptions.EnumServerOption option, int maxSuspiciousCounter) {
        AbstractAntiCheat abstractAntiCheat;
        try {
            abstractAntiCheat = antiCheat.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            abstractAntiCheat.setAntiCheat(this);
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            abstractAntiCheat = null;
        }
        this.anticheat = abstractAntiCheat;
        this.option = option;
        this.maxSuspiciousCounter = maxSuspiciousCounter;
    }

    public boolean isEnabled() {
        return !GameServer.coop && this.option != null && this.option.getValue() != 4;
    }

    public boolean isValid(UdpConnection connection, INetworkPacket packet) {
        String result;
        if (this.isEnabled() && (result = this.anticheat.validate(connection, packet)) != null) {
            this.anticheat.react(connection, packet);
            if ("".equals(result)) {
                String message = String.format("Anti-Cheat %s skipped: %s", this.anticheat.getClass().getSimpleName(), result);
                DebugLog.Multiplayer.warn(message);
                EventManager.instance().report("[" + connection.getUserName() + "] " + message);
                AntiCheat.log(connection, this, connection.getValidator().getCounters().getOrDefault((Object)this, 0), "'skip'");
            } else {
                String message = String.format("Anti-Cheat %s triggered: %s", this.anticheat.getClass().getSimpleName(), result);
                DebugLog.Multiplayer.warn(message);
                EventManager.instance().report("[" + connection.getUserName() + "] " + message);
                this.act(connection, "'" + result + "'");
            }
            return false;
        }
        return true;
    }

    public static void update(UdpConnection connection) {
        for (AntiCheat antiCheat : AntiCheat.values()) {
            if (antiCheat.anticheat.update(connection) || GameServer.isDelayedDisconnect(connection)) continue;
            antiCheat.act(connection, antiCheat.name());
        }
    }

    public static void preUpdate(UdpConnection connection) {
        for (AntiCheat antiCheat : AntiCheat.values()) {
            if (antiCheat.anticheat.preUpdate(connection)) continue;
            DebugLog.Multiplayer.warn("AntiCheat %s pre-update check failed", antiCheat.name());
        }
    }

    public static void log(UdpConnection connection, AntiCheat antiCheat, int counter, String text) {
        if (antiCheat.isEnabled()) {
            try {
                String message = String.format("Warning: player=\"%s\" option=%s anti-cheat=%s reason=%s counter=%d/%d action=%s", connection.getUserName(), antiCheat.option.getName(), antiCheat.name(), text, counter, antiCheat.maxSuspiciousCounter, Policy.name(antiCheat.option.getValue()));
                LoggerManager.getLogger("user").write(message);
                ChatServer.getInstance().sendMessageToAdminChat(message);
            }
            catch (Exception e) {
                DebugLog.Multiplayer.printException(e, "Log anti-cheat error", LogSeverity.Error);
            }
        }
    }

    public void act(UdpConnection connection, String text) {
        if (this.isEnabled() && connection.isFullyConnected() && !connection.getRole().hasCapability(zombie.characters.Capability.CantBeKickedByAnticheat)) {
            int counter = connection.getValidator().report(this);
            AntiCheat.log(connection, this, counter, text);
            if (!Core.debug) {
                AntiCheat.doLogUser(connection, this.option.getName(), text);
                if (counter >= this.maxSuspiciousCounter) {
                    switch (this.option.getValue()) {
                        case 1: {
                            AntiCheat.doBanUser(connection, this.option.getName(), text);
                            break;
                        }
                        case 2: {
                            AntiCheat.doKickUser(connection, this.option.getName(), text);
                        }
                    }
                }
            }
        }
    }

    public static void doLogUser(UdpConnection connection, String issuedBy, String text) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > connection.lastUnauthorizedPacket) {
            connection.lastUnauthorizedPacket = currentTime + 1000L;
            ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.SuspiciousActivity, text, issuedBy, 1);
        }
    }

    public static void doKickUser(UdpConnection connection, String issuedBy, String text) {
        if (!connection.getRole().hasCapability(zombie.characters.Capability.CantBeKickedByAnticheat)) {
            ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.Kicked, text, issuedBy, 1);
            GameServer.kick(connection, "UI_Policy_Kick", "UI_ValidationFailed");
            connection.forceDisconnect(issuedBy);
        }
    }

    public static void doBanUser(UdpConnection connection, String issuedBy, String text) {
        if (!connection.getRole().hasCapability(zombie.characters.Capability.CantBeBannedByAnticheat)) {
            ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.Banned, text, issuedBy, 1);
            try {
                BanSystem.BanUser(connection.getUserName(), null, issuedBy, true);
                if (SteamUtils.isSteamModeEnabled()) {
                    String steamID = SteamUtils.convertSteamIDToString(connection.getSteamId());
                    BanSystem.BanUserBySteamID(steamID, null, issuedBy, true);
                }
            }
            catch (SQLException e) {
                DebugLog.Multiplayer.printException(e, "User ban error", LogSeverity.Error);
            }
            GameServer.kick(connection, "UI_Policy_Ban", "UI_ValidationFailed");
            connection.forceDisconnect(issuedBy);
        }
    }

    public static class Policy {
        public static final int Ban = 1;
        public static final int Kick = 2;
        public static final int Log = 3;
        public static final int Disabled = 4;

        public static String name(int value) {
            return switch (value) {
                case 1 -> "Ban";
                case 2 -> "Kick";
                case 3 -> "Log";
                default -> "Unknown";
            };
        }
    }
}

