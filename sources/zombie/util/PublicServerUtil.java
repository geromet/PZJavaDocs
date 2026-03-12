/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.util.Lambda;

public final class PublicServerUtil {
    public static String webSite = "https://www.projectzomboid.com/server_browser/";
    private static long timestampForUpdate;
    private static long timestampForPlayerUpdate;
    private static long updateTick;
    private static long updatePlayerTick;
    private static int sentPlayerCount;
    private static boolean isEnabled;

    public static void init() {
        isEnabled = false;
        if (!DebugOptions.instance.network.publicServerUtil.enabled.getValue()) {
            return;
        }
        try {
            String[] split;
            String line;
            if (GameServer.server) {
                ServerOptions.instance.changeOption("PublicName", PublicServerUtil.checkHacking(ServerOptions.instance.getOption("PublicName")));
                ServerOptions.instance.changeOption("PublicDescription", PublicServerUtil.checkHacking(ServerOptions.instance.getOption("PublicDescription")));
            }
            if (GameServer.server && !PublicServerUtil.isPublic()) {
                return;
            }
            DebugLog.log("connecting to public server list");
            URL url = new URL(webSite + "serverVar.php");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(3000);
            try {
                conn.connect();
            }
            catch (Exception e) {
                DebugLog.Network.error(Translator.getText("UI_OnConnectFailed_UnknownHost"));
                return;
            }
            InputStreamReader ipsr = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(ipsr);
            StringBuilder buffer = new StringBuilder();
            while ((line = br.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            br.close();
            for (String info : split = buffer.toString().split("<br>")) {
                if (info.contains("allowed") && info.contains("true")) {
                    isEnabled = true;
                }
                if (info.contains("updateTick")) {
                    updateTick = Long.parseLong(info.split("=")[1].trim());
                }
                if (info.contains("updatePlayerTick")) {
                    updatePlayerTick = Long.parseLong(info.split("=")[1].trim());
                }
                if (!info.contains("ip") || !(GameServer.ip = info.split("=")[1].trim()).contains(":")) continue;
                DebugLog.DetailedInfo.trace("The IP address (" + GameServer.ip + ") looks like the IPv6 address. Please make sure IPv4 server address is set to the " + ServerOptions.getInstance().serverBrowserAnnouncedIp.getName() + " server option.");
            }
        }
        catch (SocketTimeoutException e) {
            isEnabled = false;
            DebugLog.log("timeout trying to connect to public server list");
            DebugType.General.printException(e, "timeout trying to connect to public server list", LogSeverity.General);
        }
        catch (Exception e) {
            isEnabled = false;
            DebugType.General.printException(e, "Exception thrown during PublicServerUtil.init()", LogSeverity.Error);
        }
    }

    private static String checkHacking(String check2) {
        if (check2 == null) {
            return "";
        }
        return check2.replaceAll("--", "").replaceAll("->", "").replaceAll("(?i)select union", "").replaceAll("(?i)select join", "").replaceAll("1=1", "").replaceAll("(?i)delete from", "");
    }

    public static void insertOrUpdate() {
        if (!isEnabled) {
            return;
        }
        if (PublicServerUtil.isPublic()) {
            try {
                PublicServerUtil.insertDatas();
            }
            catch (Exception e) {
                System.out.println("Can't reach PZ.com");
            }
        }
    }

    private static boolean isPublic() {
        String publicName = PublicServerUtil.checkHacking(ServerOptions.instance.publicName.getValue());
        return ServerOptions.instance.isPublic.getValue() && !publicName.isEmpty();
    }

    public static void update() {
        if (System.currentTimeMillis() - timestampForUpdate > updateTick) {
            timestampForUpdate = System.currentTimeMillis();
            PublicServerUtil.init();
            if (!isEnabled) {
                return;
            }
            if (PublicServerUtil.isPublic()) {
                try {
                    PublicServerUtil.insertDatas();
                }
                catch (Exception e) {
                    System.out.println("Can't reach PZ.com");
                }
            }
        }
    }

    private static void insertDatas() throws Exception {
        if (!isEnabled) {
            return;
        }
        Object desc = "";
        if (!ServerOptions.instance.publicDescription.getValue().isEmpty()) {
            desc = "&desc=" + ServerOptions.instance.publicDescription.getValue().replaceAll(" ", "%20");
        }
        Object mods = "";
        for (String mod : GameServer.ServerMods) {
            mods = (String)mods + mod + ",";
        }
        if (!"".equals(mods)) {
            mods = ((String)mods).substring(0, ((String)mods).length() - 1);
            mods = "&mods=" + ((String)mods).replaceAll(" ", "%20");
        }
        String ip = GameServer.ip;
        if (!ServerOptions.instance.serverBrowserAnnouncedIp.getValue().isEmpty()) {
            ip = ServerOptions.instance.serverBrowserAnnouncedIp.getValue();
        }
        timestampForUpdate = System.currentTimeMillis();
        int playerCount = GameServer.getPlayerCount();
        PublicServerUtil.callUrl(webSite + "write.php?name=" + ServerOptions.instance.publicName.getValue().replaceAll(" ", "%20") + (String)desc + "&port=" + ServerOptions.instance.defaultPort.getValue() + "&UDPPort=" + ServerOptions.instance.udpPort.getValue() + "&players=" + playerCount + "&ip=" + ip + "&open=" + (ServerOptions.instance.open.getValue() ? "1" : "0") + "&password=" + ("".equals(ServerOptions.instance.password.getValue()) ? "0" : "1") + "&maxPlayers=" + ServerOptions.getInstance().getMaxPlayers() + "&version=" + Core.getInstance().getVersionNumber().replaceAll(" ", "%20") + (String)mods + "&mac=" + PublicServerUtil.getMacAddress());
        sentPlayerCount = playerCount;
    }

    public static void updatePlayers() {
        if (System.currentTimeMillis() - timestampForPlayerUpdate > updatePlayerTick) {
            timestampForPlayerUpdate = System.currentTimeMillis();
            if (!isEnabled) {
                return;
            }
            try {
                String ip = GameServer.ip;
                if (!ServerOptions.instance.serverBrowserAnnouncedIp.getValue().isEmpty()) {
                    ip = ServerOptions.instance.serverBrowserAnnouncedIp.getValue();
                }
                int playerCount = GameServer.getPlayerCount();
                PublicServerUtil.callUrl(webSite + "updatePlayers.php?port=" + ServerOptions.instance.defaultPort.getValue() + "&players=" + playerCount + "&ip=" + ip);
                sentPlayerCount = GameServer.getPlayerCount();
            }
            catch (Exception e) {
                System.out.println("Can't reach PZ.com");
            }
        }
    }

    public static void updatePlayerCountIfChanged() {
        if (isEnabled && sentPlayerCount != GameServer.getPlayerCount()) {
            PublicServerUtil.updatePlayers();
        }
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    private static String getMacAddress() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network != null) {
                byte[] mac = network.getHardwareAddress();
                if (mac == null) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; ++i) {
                    sb.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? "-" : ""));
                }
                return sb.toString();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void callUrl(String url) {
        new Thread(ThreadGroups.Workers, Lambda.invoker(url, url2 -> {
            try {
                URL yurl = new URL((String)url2);
                URLConnection conn = yurl.openConnection();
                conn.getInputStream();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }), "openUrl").start();
    }

    public static void callPostJson(String url, String json) {
        new Thread(ThreadGroups.Workers, Lambda.invoker(url, url2 -> {
            try {
                URL yurl = new URL((String)url2);
                URLConnection conn = yurl.openConnection();
                HttpURLConnection http = (HttpURLConnection)conn;
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                byte[] out = json.getBytes(StandardCharsets.UTF_8);
                int length = out.length;
                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                http.connect();
                try (OutputStream os = http.getOutputStream();){
                    os.write(out);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }), "callPostJson").start();
    }

    static {
        updateTick = 600000L;
        updatePlayerTick = 300000L;
    }
}

