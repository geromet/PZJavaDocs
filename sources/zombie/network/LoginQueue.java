/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.network.ConnectionManager;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.connection.QueuePacket;

public class LoginQueue {
    private static final ArrayList<UdpConnection> LoginQueue = new ArrayList();
    private static final ArrayList<UdpConnection> PreferredLoginQueue = new ArrayList();
    private static final UpdateLimit UpdateLimit = new UpdateLimit(3050L);
    private static final UpdateLimit UpdateServerInformationLimit = new UpdateLimit(20000L);
    private static final UpdateLimit LoginQueueTimeout = new UpdateLimit(15000L);
    private static UdpConnection currentLoginQueue;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void receiveLoginQueueDone(long gameLoadingTime, UdpConnection connection) {
        LoggerManager.getLogger("user").write("player " + connection.getUserName() + " loading time was: " + gameLoadingTime + " ms");
        ArrayList<UdpConnection> arrayList = LoginQueue;
        synchronized (arrayList) {
            if (currentLoginQueue == connection) {
                currentLoginQueue = null;
            }
            zombie.network.LoginQueue.loadNextPlayer();
        }
        ConnectionManager.log("receive-packet", "login-queue-done", connection);
        connection.getValidator().checksumSend(true, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void receiveServerLoginQueueRequest(UdpConnection connection) {
        LoggerManager.getLogger("user").write(connection.getIDStr() + " \"" + connection.getUserName() + "\" attempting to join used " + (connection.getRole().hasCapability(Capability.PriorityLogin) ? "preferred " : "") + "queue");
        ArrayList<UdpConnection> arrayList = LoginQueue;
        synchronized (arrayList) {
            if (!ServerOptions.getInstance().loginQueueEnabled.getValue() || !connection.getRole().hasCapability(Capability.PriorityLogin) && currentLoginQueue == null && PreferredLoginQueue.isEmpty() && LoginQueue.isEmpty() && zombie.network.LoginQueue.getCountPlayers() < ServerOptions.getInstance().getMaxPlayers() || connection.getRole().hasCapability(Capability.PriorityLogin) && currentLoginQueue == null && PreferredLoginQueue.isEmpty()) {
                DebugLog.DetailedInfo.trace("ConnectionImmediate ip=%s", connection.getIP());
                currentLoginQueue = connection;
                currentLoginQueue.setWasInLoadingQueue(true);
                LoginQueueTimeout.Reset((long)ServerOptions.getInstance().loginQueueConnectTimeout.getValue() * 1000L);
                QueuePacket packet = new QueuePacket();
                packet.setConnectionImmediate();
                ByteBufferWriter b = connection.startPacket();
                PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.LoginQueueRequest.send(connection);
            } else {
                DebugLog.DetailedInfo.trace("PlaceInQueue ip=%s preferredInQueue=%b", connection.getIP(), connection.getRole().hasCapability(Capability.PriorityLogin));
                if (connection.getRole().hasCapability(Capability.PriorityLogin)) {
                    if (!PreferredLoginQueue.contains(connection)) {
                        PreferredLoginQueue.add(connection);
                    }
                } else if (!LoginQueue.contains(connection)) {
                    LoginQueue.add(connection);
                }
                zombie.network.LoginQueue.sendPlaceInTheQueue();
            }
        }
        ConnectionManager.log("receive-packet", "login-queue-request", connection);
    }

    private static void sendPlaceInTheQueue() {
        ByteBufferWriter b;
        QueuePacket packet = new QueuePacket();
        packet.setInformationFields();
        for (UdpConnection connection : PreferredLoginQueue) {
            packet.setPlaceInQueue((byte)(PreferredLoginQueue.indexOf(connection) + 1));
            b = connection.startPacket();
            PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.LoginQueueRequest.send(connection);
        }
        for (UdpConnection connection : LoginQueue) {
            packet.setPlaceInQueue((byte)(LoginQueue.indexOf(connection) + 1 + PreferredLoginQueue.size()));
            b = connection.startPacket();
            PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.LoginQueueRequest.send(connection);
        }
    }

    private static void sendConnectRequest(UdpConnection connection) {
        DebugLog.DetailedInfo.trace("SendApplyRequest ip=%s", connection.getIP());
        QueuePacket packet = new QueuePacket();
        packet.setConnectionImmediate();
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.LoginQueueRequest.send(connection);
        ConnectionManager.log("send-packet", "login-queue-request", connection);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void disconnect(UdpConnection connection) {
        DebugLog.DetailedInfo.trace("ip=%s", connection.getIP());
        ArrayList<UdpConnection> arrayList = LoginQueue;
        synchronized (arrayList) {
            if (connection == currentLoginQueue) {
                currentLoginQueue = null;
            } else {
                LoginQueue.remove(connection);
                PreferredLoginQueue.remove(connection);
            }
            zombie.network.LoginQueue.sendPlaceInTheQueue();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isInTheQueue(UdpConnection connection) {
        if (!ServerOptions.getInstance().loginQueueEnabled.getValue()) {
            return false;
        }
        ArrayList<UdpConnection> arrayList = LoginQueue;
        synchronized (arrayList) {
            return connection == currentLoginQueue || LoginQueue.contains(connection) || PreferredLoginQueue.contains(connection);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void update() {
        if (ServerOptions.getInstance().loginQueueEnabled.getValue() && UpdateLimit.Check()) {
            ArrayList<UdpConnection> arrayList = LoginQueue;
            synchronized (arrayList) {
                if (currentLoginQueue != null) {
                    if (currentLoginQueue.isFullyConnected()) {
                        DebugLog.DetailedInfo.trace("Connection isFullyConnected ip=%s", currentLoginQueue.getIP());
                        currentLoginQueue = null;
                    } else if (LoginQueueTimeout.Check()) {
                        DebugLog.DetailedInfo.trace("Connection timeout ip=%s", currentLoginQueue.getIP());
                        currentLoginQueue = null;
                    }
                }
                zombie.network.LoginQueue.loadNextPlayer();
            }
        }
        if (UpdateServerInformationLimit.Check()) {
            zombie.network.LoginQueue.sendPlaceInTheQueue();
        }
    }

    private static void loadNextPlayer() {
        if (!PreferredLoginQueue.isEmpty() && currentLoginQueue == null) {
            currentLoginQueue = PreferredLoginQueue.remove(0);
            currentLoginQueue.setWasInLoadingQueue(true);
            DebugLog.DetailedInfo.trace("Next player from the preferred queue to connect ip=%s", currentLoginQueue.getIP());
            LoginQueueTimeout.Reset((long)ServerOptions.getInstance().loginQueueConnectTimeout.getValue() * 1000L);
            zombie.network.LoginQueue.sendConnectRequest(currentLoginQueue);
            zombie.network.LoginQueue.sendPlaceInTheQueue();
        }
        if (!LoginQueue.isEmpty() && currentLoginQueue == null && zombie.network.LoginQueue.getCountPlayers() < ServerOptions.getInstance().getMaxPlayers()) {
            currentLoginQueue = LoginQueue.remove(0);
            currentLoginQueue.setWasInLoadingQueue(true);
            DebugLog.DetailedInfo.trace("Next player from queue to connect ip=%s", currentLoginQueue.getIP());
            LoginQueueTimeout.Reset((long)ServerOptions.getInstance().loginQueueConnectTimeout.getValue() * 1000L);
            zombie.network.LoginQueue.sendConnectRequest(currentLoginQueue);
            zombie.network.LoginQueue.sendPlaceInTheQueue();
        }
    }

    public static int getCountPlayers() {
        int countPlayers = 0;
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getRole() == null || c.getRole().hasCapability(Capability.HideFromSteamUserList) || !c.wasInLoadingQueue() || LoginQueue.contains(c) || PreferredLoginQueue.contains(c)) continue;
            ++countPlayers;
        }
        return countPlayers;
    }

    public static String getDescription() {
        return "queue=[" + LoginQueue.size() + "/" + PreferredLoginQueue.size() + "/\"" + String.valueOf(currentLoginQueue == null ? "" : Long.valueOf(currentLoginQueue.getConnectedGUID())) + "\"]";
    }
}

