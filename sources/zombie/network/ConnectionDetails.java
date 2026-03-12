/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionMain;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.ConnectToServerState;
import zombie.gameStates.MainScreenState;
import zombie.globalObjects.SGlobalObjects;
import zombie.network.ConnectionManager;
import zombie.network.CoopSlave;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.world.WorldDictionary;

public class ConnectionDetails {
    public static void write(UdpConnection connection, ServerWorldDatabase.LogonResult logonResult, ByteBufferWriter bb) {
        try {
            ConnectionDetails.writeServerDetails(bb, connection, logonResult);
            ConnectionDetails.writeGameMap(bb);
            if (SteamUtils.isSteamModeEnabled()) {
                ConnectionDetails.writeWorkshopItems(bb);
            }
            ConnectionDetails.writeMods(bb);
            ConnectionDetails.writeStartLocation(bb);
            ConnectionDetails.writeServerOptions(bb);
            ConnectionDetails.writeSandboxOptions(bb);
            ConnectionDetails.writeGameTime(bb);
            ConnectionDetails.writeErosionMain(bb);
            ConnectionDetails.writeGlobalObjects(bb);
            ConnectionDetails.writeResetID(bb);
            ConnectionDetails.writeBerries(bb);
            ConnectionDetails.writeWorldDictionary(bb);
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void parse(ByteBufferReader b) {
        ConnectionManager.log("receive-packet", "connection-details", GameClient.connection);
        Calendar endAuth = Calendar.getInstance();
        ConnectToServerState ctss = new ConnectToServerState(b);
        ctss.enter();
        MainScreenState.getInstance().setConnectToServerState(ctss);
        DebugLog.General.println("LOGGED INTO : %d millisecond", endAuth.getTimeInMillis() - GameClient.startAuth.getTimeInMillis());
    }

    private static void writeServerDetails(ByteBufferWriter b, UdpConnection connection, ServerWorldDatabase.LogonResult logonResult) {
        b.putBoolean(connection.isCoopHost);
        b.putInt(ServerOptions.getInstance().getMaxPlayers());
        if (b.putBoolean(SteamUtils.isSteamModeEnabled() && CoopSlave.instance != null && !connection.isCoopHost)) {
            b.putLong(CoopSlave.instance.hostSteamId);
            b.putUTF(GameServer.serverName);
        }
        b.putByte(connection.playerIds[0] / 4);
        logonResult.role.send(b);
    }

    private static void writeGameMap(ByteBufferWriter b) {
        b.putUTF(GameServer.gameMap);
    }

    private static void writeWorkshopItems(ByteBufferWriter b) {
        b.putShort(GameServer.WorkshopItems.size());
        for (int i = 0; i < GameServer.WorkshopItems.size(); ++i) {
            b.putLong(GameServer.WorkshopItems.get(i));
            b.putLong(GameServer.workshopTimeStamps[i]);
        }
    }

    private static void writeMods(ByteBufferWriter b) {
        ArrayList<ChooseGameInfo.Mod> mods = new ArrayList<ChooseGameInfo.Mod>();
        ArrayList<String> missingMods = new ArrayList<String>();
        for (String string : GameServer.ServerMods) {
            ChooseGameInfo.Mod mod;
            String modDir = ZomboidFileSystem.instance.getModDir(string);
            if (modDir != null) {
                try {
                    mod = ChooseGameInfo.readModInfo(modDir);
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                    missingMods.add(string);
                    mod = null;
                }
            } else {
                missingMods.add(string);
                mod = null;
            }
            if (mod == null) continue;
            mods.add(mod);
        }
        b.putInt(mods.size() + missingMods.size());
        for (ChooseGameInfo.Mod mod : mods) {
            b.putUTF(mod.getId());
            b.putUTF(mod.getWorkshopID());
            b.putUTF(mod.getName());
        }
        for (String string : missingMods) {
            b.putUTF(string);
            b.putUTF("");
            b.putUTF(string);
        }
    }

    private static void writeStartLocation(ByteBufferWriter b) {
        Object r = null;
        b.putInt(10745);
        b.putInt(9412);
        b.putInt(0);
    }

    private static void writeServerOptions(ByteBufferWriter b) {
        b.putInt(ServerOptions.instance.getPublicOptions().size());
        for (String key : ServerOptions.instance.getPublicOptions()) {
            b.putUTF(key);
            b.putUTF(ServerOptions.instance.getOption(key));
        }
    }

    private static void writeSandboxOptions(ByteBufferWriter b) throws IOException {
        SandboxOptions.instance.save(b.bb);
    }

    private static void writeGameTime(ByteBufferWriter b) throws IOException {
        GameTime.getInstance().saveToPacket(b);
    }

    private static void writeErosionMain(ByteBufferWriter b) {
        ErosionMain.getInstance().getConfig().save(b);
    }

    private static void writeGlobalObjects(ByteBufferWriter b) throws IOException {
        SGlobalObjects.saveInitialStateForClient(b);
    }

    private static void writeResetID(ByteBufferWriter b) {
        b.putInt(GameServer.resetId);
    }

    private static void writeBerries(ByteBufferWriter b) {
        b.putUTF(Core.getInstance().getPoisonousBerry());
        b.putUTF(Core.getInstance().getPoisonousMushroom());
    }

    private static void writeWorldDictionary(ByteBufferWriter b) throws IOException {
        WorldDictionary.saveDataForClient(b);
    }
}

