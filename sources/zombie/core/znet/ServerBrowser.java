/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.core.znet.GameServerDetails;
import zombie.core.znet.IServerBrowserCallback;
import zombie.core.znet.SteamUtils;
import zombie.network.GameServer;
import zombie.network.Server;

public class ServerBrowser {
    private static boolean suppressLuaCallbacks;
    private static IServerBrowserCallback callbackInterface;

    public static boolean init() {
        boolean result = false;
        if (SteamUtils.isSteamModeEnabled()) {
            result = ServerBrowser.n_Init();
        }
        return result;
    }

    public static void shutdown() {
        if (SteamUtils.isSteamModeEnabled()) {
            ServerBrowser.n_Shutdown();
        }
    }

    public static void RefreshInternetServers() {
        if (SteamUtils.isSteamModeEnabled()) {
            ServerBrowser.n_RefreshInternetServers();
        }
    }

    public static int GetServerCount() {
        int result = 0;
        if (SteamUtils.isSteamModeEnabled()) {
            result = ServerBrowser.n_GetServerCount();
        }
        return result;
    }

    public static GameServerDetails GetServerDetails(int serverIndex) {
        GameServerDetails result = null;
        if (SteamUtils.isSteamModeEnabled()) {
            result = ServerBrowser.n_GetServerDetails(serverIndex);
        }
        return result;
    }

    public static void Release() {
        if (SteamUtils.isSteamModeEnabled()) {
            ServerBrowser.n_Release();
        }
    }

    public static boolean IsRefreshing() {
        boolean result = false;
        if (SteamUtils.isSteamModeEnabled()) {
            result = ServerBrowser.n_IsRefreshing();
        }
        return result;
    }

    public static boolean QueryServer(String host, int port) {
        boolean result = false;
        if (SteamUtils.isSteamModeEnabled()) {
            result = ServerBrowser.n_QueryServer(host, port);
        }
        return result;
    }

    public static GameServerDetails GetServerDetails(String host, int port) {
        GameServerDetails result = null;
        if (SteamUtils.isSteamModeEnabled()) {
            result = ServerBrowser.n_GetServerDetails(host, port);
        }
        return result;
    }

    public static void ReleaseServerQuery(String host, int port) {
        if (SteamUtils.isSteamModeEnabled()) {
            ServerBrowser.n_ReleaseServerQuery(host, port);
        }
    }

    public static List<GameServerDetails> GetServerList() {
        ArrayList<GameServerDetails> result = new ArrayList<GameServerDetails>();
        if (SteamUtils.isSteamModeEnabled()) {
            try {
                while (ServerBrowser.IsRefreshing()) {
                    Thread.sleep(100L);
                    SteamUtils.runLoop();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < ServerBrowser.GetServerCount(); ++i) {
                GameServerDetails details = ServerBrowser.GetServerDetails(i);
                if (details.steamId == 0L) continue;
                result.add(details);
            }
        }
        return result;
    }

    public static GameServerDetails GetServerDetailsSync(String host, int port) {
        GameServerDetails result = null;
        if (SteamUtils.isSteamModeEnabled() && (result = ServerBrowser.GetServerDetails(host, port)) == null) {
            ServerBrowser.QueryServer(host, port);
            try {
                while (result == null) {
                    Thread.sleep(100L);
                    SteamUtils.runLoop();
                    result = ServerBrowser.GetServerDetails(host, port);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static boolean RequestServerRules(String host, int port) {
        return ServerBrowser.n_RequestServerRules(host, port);
    }

    public static void setSuppressLuaCallbacks(boolean bSupress) {
        suppressLuaCallbacks = bSupress;
    }

    public static void setCallbackInterface(IServerBrowserCallback callbackInterface) {
        ServerBrowser.callbackInterface = callbackInterface;
    }

    private static native boolean n_Init();

    private static native void n_Shutdown();

    private static native void n_RefreshInternetServers();

    private static native int n_GetServerCount();

    private static native GameServerDetails n_GetServerDetails(int var0);

    private static native void n_Release();

    private static native boolean n_IsRefreshing();

    private static native boolean n_QueryServer(String var0, int var1);

    private static native GameServerDetails n_GetServerDetails(String var0, int var1);

    private static native void n_ReleaseServerQuery(String var0, int var1);

    private static native boolean n_RequestServerRules(String var0, int var1);

    private static void onServerRespondedCallback(int serverIndex) {
        if (callbackInterface != null) {
            callbackInterface.OnServerResponded(serverIndex);
        }
        if (suppressLuaCallbacks) {
            return;
        }
        LuaEventManager.triggerEvent("OnSteamServerResponded", serverIndex);
    }

    private static void onServerFailedToRespondCallback(int serverIndex) {
        if (callbackInterface != null) {
            callbackInterface.OnServerFailedToRespond(serverIndex);
        }
    }

    private static void onRefreshCompleteCallback() {
        if (callbackInterface != null) {
            callbackInterface.OnRefreshComplete();
        }
        if (suppressLuaCallbacks) {
            return;
        }
        LuaEventManager.triggerEvent("OnSteamRefreshInternetServers");
    }

    private static void onServerRespondedCallback(String host, int port) {
        GameServerDetails details;
        if (callbackInterface != null) {
            callbackInterface.OnServerResponded(host, port);
        }
        if ((details = ServerBrowser.GetServerDetails(host, port)) == null) {
            return;
        }
        Server newServer = GameServer.steamGetInternetServerDetails(details);
        ServerBrowser.ReleaseServerQuery(host, port);
        if (suppressLuaCallbacks) {
            return;
        }
        LuaEventManager.triggerEvent("OnSteamServerResponded2", host, port, newServer);
    }

    private static void onServerFailedToRespondCallback(String host, int port) {
        if (callbackInterface != null) {
            callbackInterface.OnServerFailedToRespond(host, port);
        }
        if (suppressLuaCallbacks) {
            return;
        }
        LuaEventManager.triggerEvent("OnSteamServerFailedToRespond2", host, port);
    }

    private static void onRulesRefreshComplete(String host, int port, String[] rulesArray) {
        if (callbackInterface != null) {
            callbackInterface.OnSteamRulesRefreshComplete(host, port);
        }
        KahluaTable rulesTable = LuaManager.platform.newTable();
        for (int i = 0; i < rulesArray.length; i += 2) {
            rulesTable.rawset(rulesArray[i], (Object)rulesArray[i + 1]);
        }
        if (suppressLuaCallbacks) {
            return;
        }
        LuaEventManager.triggerEvent("OnSteamRulesRefreshComplete", host, port, rulesTable);
    }
}

