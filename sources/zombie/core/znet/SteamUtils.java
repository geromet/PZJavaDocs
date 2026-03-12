/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import zombie.Lua.LuaEventManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderThread;
import zombie.core.znet.IJoinRequestCallback;
import zombie.core.znet.ZNet;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.network.CoopSlave;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;

public class SteamUtils {
    private static boolean steamEnabled;
    private static boolean netEnabled;
    private static boolean floatingGamepadTextInputVisible;
    private static final BigInteger TWO_64;
    private static final BigInteger MAX_ULONG;
    private static List<IJoinRequestCallback> joinRequestCallbacks;
    public static final int k_EGamepadTextInputModeNormal = 0;
    public static final int k_EGamepadTextInputModePassword = 1;
    public static final int k_EGamepadTextInputLineModeSingleLine = 0;
    public static final int k_EGamepadTextInputLineModeMultipleLines = 1;
    public static final int k_EFloatingGamepadTextInputModeSingleLine = 0;
    public static final int k_EFloatingGamepadTextInputModeMultipleLines = 1;
    public static final int k_EFloatingGamepadTextInputModeEmail = 2;
    public static final int k_EFloatingGamepadTextInputModeNumeric = 3;

    private static void loadLibrary(String name) {
        DebugLog.log("Loading " + name + "...");
        System.loadLibrary(name);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void init() {
        block25: {
            steamEnabled = System.getProperty("zomboid.steam") != null && System.getProperty("zomboid.steam").equals("1");
            DebugLog.log("Loading networking libraries...");
            String libSuffix = "";
            if ("1".equals(System.getProperty("zomboid.debuglibs.znet"))) {
                DebugLog.log("***** Loading debug versions of libraries");
                libSuffix = "d";
            }
            try {
                if (System.getProperty("os.name").contains("OS X")) {
                    if (steamEnabled) {
                        SteamUtils.loadLibrary("steam_api");
                        SteamUtils.loadLibrary("RakNet");
                        SteamUtils.loadLibrary("ZNetJNI");
                    } else {
                        SteamUtils.loadLibrary("RakNet");
                        SteamUtils.loadLibrary("ZNetNoSteam");
                    }
                } else if (System.getProperty("os.name").startsWith("Win")) {
                    if (steamEnabled) {
                        SteamUtils.loadLibrary("steam_api64");
                        SteamUtils.loadLibrary("RakNet64" + libSuffix);
                        SteamUtils.loadLibrary("ZNetJNI64" + libSuffix);
                    } else {
                        SteamUtils.loadLibrary("RakNet64" + libSuffix);
                        SteamUtils.loadLibrary("ZNetNoSteam64" + libSuffix);
                    }
                } else if (steamEnabled) {
                    SteamUtils.loadLibrary("steam_api");
                    SteamUtils.loadLibrary("RakNet64");
                    SteamUtils.loadLibrary("ZNetJNI64");
                } else {
                    SteamUtils.loadLibrary("RakNet64");
                    SteamUtils.loadLibrary("ZNetNoSteam64");
                }
                netEnabled = true;
            }
            catch (UnsatisfiedLinkError ex) {
                steamEnabled = false;
                netEnabled = false;
                ExceptionLogger.logException(ex);
                if (!System.getProperty("os.name").startsWith("Win")) break block25;
                DebugLog.log("One of the game's DLLs could not be loaded.");
                DebugLog.log("  Your system may be missing a DLL needed by the game's DLL.");
                DebugLog.log("  You may need to install the Microsoft Visual C++ Redistributable 2013.");
                File file = new File("../_CommonRedist/vcredist/");
                if (!file.exists()) break block25;
                DebugLog.DetailedInfo.trace("  This file is provided in " + file.getAbsolutePath());
            }
        }
        String logLevelStr = System.getProperty("zomboid.znetlog");
        if (netEnabled && logLevelStr != null) {
            try {
                int logLevel = Integer.parseInt(logLevelStr);
                ZNet.SetLogLevel(logLevel);
            }
            catch (NumberFormatException ex) {
                ExceptionLogger.logException(ex);
            }
        }
        if (!netEnabled) {
            DebugLog.log("Failed to load networking libraries");
        } else {
            ZNet.init();
            ZNet.SetLogLevel(DebugLog.getLogLevel(DebugType.Network));
            Object object = RenderThread.m_contextLock;
            synchronized (object) {
                if (!steamEnabled) {
                    DebugLog.log("SteamUtils started without Steam");
                } else if (SteamUtils.n_Init(GameServer.server)) {
                    DebugLog.log("SteamUtils initialised successfully");
                } else {
                    DebugLog.log("Could not initialise SteamUtils");
                    steamEnabled = false;
                }
            }
        }
        joinRequestCallbacks = new ArrayList<IJoinRequestCallback>();
    }

    public static void shutdown() {
        if (steamEnabled) {
            SteamUtils.n_Shutdown();
        }
    }

    public static void runLoop() {
        if (steamEnabled) {
            SteamUtils.n_RunLoop();
        }
    }

    public static boolean isSteamModeEnabled() {
        return steamEnabled;
    }

    public static boolean isOverlayEnabled() {
        return steamEnabled && SteamUtils.n_IsOverlayEnabled();
    }

    public static String convertSteamIDToString(long steamID) {
        BigInteger b = BigInteger.valueOf(steamID);
        if (b.signum() < 0) {
            b.add(TWO_64);
        }
        return b.toString();
    }

    public static boolean isValidSteamID(String s) {
        try {
            BigInteger b = new BigInteger(s);
            if (b.signum() < 0 || b.compareTo(MAX_ULONG) > 0) {
                return false;
            }
        }
        catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }

    public static long convertStringToSteamID(String s) {
        try {
            BigInteger b = new BigInteger(s);
            if (b.signum() < 0 || b.compareTo(MAX_ULONG) > 0) {
                return -1L;
            }
            return b.longValue();
        }
        catch (NumberFormatException ex) {
            return -1L;
        }
    }

    public static void addJoinRequestCallback(IJoinRequestCallback callback) {
        joinRequestCallbacks.add(callback);
    }

    public static void removeJoinRequestCallback(IJoinRequestCallback callback) {
        joinRequestCallbacks.remove(callback);
    }

    public static boolean isRunningOnSteamDeck() {
        return SteamUtils.n_IsSteamRunningOnSteamDeck();
    }

    public static boolean showGamepadTextInput(boolean password, boolean multipleLines, String description, int maxChars, String existingText) {
        return SteamUtils.n_ShowGamepadTextInput(password ? 1 : 0, multipleLines ? 1 : 0, description, maxChars, existingText);
    }

    public static boolean showFloatingGamepadTextInput(boolean multipleLines, int x, int y, int width, int height) {
        if (floatingGamepadTextInputVisible) {
            return true;
        }
        floatingGamepadTextInputVisible = SteamUtils.n_ShowFloatingGamepadTextInput(multipleLines ? 1 : 0, x, y, width, height);
        return floatingGamepadTextInputVisible;
    }

    public static boolean isFloatingGamepadTextInputVisible() {
        return floatingGamepadTextInputVisible;
    }

    private static native boolean n_Init(boolean var0);

    private static native void n_Shutdown();

    private static native void n_RunLoop();

    private static native boolean n_IsOverlayEnabled();

    private static native boolean n_IsSteamRunningOnSteamDeck();

    private static native boolean n_ShowGamepadTextInput(int var0, int var1, String var2, int var3, String var4);

    private static native boolean n_ShowFloatingGamepadTextInput(int var0, int var1, int var2, int var3, int var4);

    private static void joinRequestCallback(long friendSteamID, String connectionString) {
        DebugLog.log("Got Join Request");
        for (IJoinRequestCallback callback : joinRequestCallbacks) {
            callback.onJoinRequest(friendSteamID, connectionString);
        }
        if (connectionString.contains("+connect ")) {
            String connect2 = connectionString.substring(9);
            System.setProperty("args.server.connect", connect2);
            LuaEventManager.triggerEvent("OnSteamGameJoin");
        }
    }

    private static int clientInitiateConnectionCallback(long steamID) {
        if (CoopSlave.instance != null) {
            return CoopSlave.instance.isHost(steamID) || CoopSlave.instance.isInvited(steamID) ? 0 : 2;
        }
        ServerWorldDatabase.LogonResult r = ServerWorldDatabase.instance.authClient(steamID);
        return r.authorized ? 0 : 1;
    }

    private static int validateOwnerCallback(long steamID, long ownerID) {
        if (CoopSlave.instance != null) {
            return 0;
        }
        ServerWorldDatabase.LogonResult r = ServerWorldDatabase.instance.authOwner(steamID, ownerID);
        return r.authorized ? 0 : 1;
    }

    private static void gamepadTextInputDismissedCallback(String text) {
        if (text == null) {
            DebugLog.log("null");
        } else {
            DebugLog.log(text);
        }
    }

    private static void floatingGamepadTextInputDismissedCallback() {
        floatingGamepadTextInputVisible = false;
    }

    static {
        TWO_64 = BigInteger.ONE.shiftLeft(64);
        MAX_ULONG = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    }
}

