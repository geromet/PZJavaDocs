/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.ArrayDeque;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.secure.PZcrypt;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.ServerWorldDatabase;

public class ConnectionManager {
    private static final ConnectionManager instance = new ConnectionManager();
    final ArrayDeque<Request> connectionRequests = new ArrayDeque();

    public static ConnectionManager getInstance() {
        return instance;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void ping(String username, String pwd, String ip, String port, boolean doHash) {
        ArrayDeque<Request> arrayDeque = this.connectionRequests;
        synchronized (arrayDeque) {
            this.connectionRequests.add(new Request(RequestType.askPing, username, pwd, ip, "", port, "", "", false, doHash, 1, ""));
        }
        ConnectionManager.getInstance().process();
    }

    public void stopPing() {
        GameClient.askPing = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void getCustomizationData(String username, String pwd, String ip, String port, String serverPassword, String serverName, boolean doHash) {
        ArrayDeque<Request> arrayDeque = this.connectionRequests;
        synchronized (arrayDeque) {
            for (Request r : this.connectionRequests) {
                if (!r.server.equals(ip)) continue;
                return;
            }
            this.connectionRequests.push(new Request(RequestType.askCustomizationData, username, pwd, ip, "", port, serverPassword, serverName, false, doHash, 1, ""));
        }
        ConnectionManager.getInstance().process();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendSecretKey(String username, String pwd, String ip, int port, String serverPassword, boolean doHash, int authType, String secretKey) {
        ArrayDeque<Request> arrayDeque = this.connectionRequests;
        synchronized (arrayDeque) {
            this.connectionRequests.removeIf(r -> r.type == RequestType.askCustomizationData);
            this.connectionRequests.push(new Request(RequestType.sendQR, username, pwd, ip, "", String.valueOf(port), serverPassword, "", false, doHash, authType, secretKey));
        }
        ConnectionManager.getInstance().process();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void serverConnect(String username, String pwd, String server, String localIP, String port, String serverPassword, String serverName, boolean useSteamRelay, boolean doHash, int authType, String secretKey) {
        ArrayDeque<Request> arrayDeque = this.connectionRequests;
        synchronized (arrayDeque) {
            this.connectionRequests.removeIf(r -> r.type == RequestType.askCustomizationData);
            this.connectionRequests.push(new Request(RequestType.connect, username, pwd, server, localIP, port, serverPassword, serverName, useSteamRelay, doHash, authType, secretKey));
        }
        ConnectionManager.getInstance().process();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void serverConnectCoop(String serverSteamID) {
        ArrayDeque<Request> arrayDeque = this.connectionRequests;
        synchronized (arrayDeque) {
            this.connectionRequests.removeIf(r -> r.type == RequestType.askCustomizationData);
            this.connectionRequests.push(new Request(RequestType.connectCoop, "", "", serverSteamID, "", "", "", "", true, false, 1, ""));
        }
        ConnectionManager.getInstance().process();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void clearQueue() {
        ArrayDeque<Request> arrayDeque = this.connectionRequests;
        synchronized (arrayDeque) {
            this.connectionRequests.clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void process() {
        Request r;
        if (GameClient.connection != null) {
            return;
        }
        ArrayDeque<Request> arrayDeque = this.connectionRequests;
        synchronized (arrayDeque) {
            if (this.connectionRequests.isEmpty()) {
                return;
            }
            r = this.connectionRequests.poll();
        }
        switch (r.type.ordinal()) {
            case 0: {
                GameClient.askPing = false;
                GameClient.askCustomizationData = false;
                GameClient.sendQR = false;
                ConnectionManager.doServerConnect(r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey);
                break;
            }
            case 1: {
                GameClient.askPing = false;
                GameClient.askCustomizationData = false;
                GameClient.sendQR = false;
                ConnectionManager.doServerConnectCoop(r.server);
                break;
            }
            case 2: {
                GameClient.askPing = true;
                GameClient.askCustomizationData = false;
                GameClient.sendQR = false;
                ConnectionManager.doServerConnect(r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey);
                break;
            }
            case 3: {
                GameClient.askPing = false;
                GameClient.askCustomizationData = true;
                GameClient.sendQR = false;
                ConnectionManager.doServerConnect(r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey);
                break;
            }
            case 4: {
                GameClient.askPing = false;
                GameClient.askCustomizationData = false;
                GameClient.sendQR = true;
                ConnectionManager.doServerConnect(r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey);
            }
        }
    }

    public static void log(String event, String message, IConnection connection) {
        DebugLog.Multiplayer.println("connection: %s [%s] \"%s\"", connection, event, message);
    }

    public static void doServerConnect(String user, String pass, String server, String localIP, String port, String serverPassword, String serverName, boolean useSteamRelay, boolean doHash, int authtype, String secretKey) {
        Core.getInstance().setGameMode("Multiplayer");
        if (GameClient.connection != null) {
            GameClient.connection.forceDisconnect("lua-connect");
        }
        if (!GameClient.askCustomizationData) {
            GameClient.instance.resetDisconnectTimer();
        }
        GameClient.client = true;
        GameClient.clientSave = true;
        GameClient.coopInvite = false;
        ZomboidFileSystem.instance.cleanMultiplayerSaves();
        if (doHash) {
            GameClient.instance.doConnect(user, PZcrypt.hash(ServerWorldDatabase.encrypt(pass)), server, localIP, port, serverPassword, serverName, useSteamRelay, authtype, secretKey);
        } else {
            GameClient.instance.doConnect(user, pass, server, localIP, port, serverPassword, serverName, useSteamRelay, authtype, secretKey);
        }
    }

    public static void doServerConnectCoop(String serverSteamID) {
        Core.getInstance().setGameMode("Multiplayer");
        if (GameClient.connection != null) {
            GameClient.connection.forceDisconnect("lua-connect-coop");
        }
        GameClient.client = true;
        GameClient.clientSave = true;
        GameClient.coopInvite = true;
        GameClient.instance.doConnectCoop(serverSteamID);
    }

    private static class Request {
        RequestType type;
        String user;
        String pass;
        String server;
        String localIp;
        String port;
        String serverPassword;
        String serverName;
        boolean useSteamRelay;
        boolean doHash;
        int authtype;
        String secretKey;

        public Request(RequestType type, String user, String pass, String server, String localIp, String port, String serverPassword, String serverName, boolean useSteamRelay, boolean doHash, int authtype, String secretKey) {
            this.type = type;
            this.user = user;
            this.pass = pass;
            this.server = server;
            this.localIp = localIp;
            this.port = port;
            this.serverPassword = serverPassword;
            this.serverName = serverName;
            this.useSteamRelay = useSteamRelay;
            this.doHash = doHash;
            this.authtype = authtype;
            this.secretKey = secretKey;
        }
    }

    private static enum RequestType {
        connect,
        connectCoop,
        askPing,
        askCustomizationData,
        sendQR;

    }
}

