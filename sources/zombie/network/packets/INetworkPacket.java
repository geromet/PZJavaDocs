/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.fields.INetworkPacketField;

public interface INetworkPacket
extends INetworkPacketField {
    default public void setData(Object ... values2) {
    }

    default public void parseClientLoading(ByteBufferReader b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default public void parseClient(ByteBufferReader b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default public void parseServer(ByteBufferReader b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default public void postpone() {
    }

    default public boolean isPostponed() {
        return false;
    }

    default public void processClientLoading(UdpConnection connection) {
    }

    default public void processClient(UdpConnection connection) {
    }

    default public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
    }

    default public void sync(PacketTypes.PacketType packetType, UdpConnection connection) {
    }

    private void sendToConnection(PacketTypes.PacketType packetType, IConnection connection) {
        if (connection != null) {
            ByteBufferWriter b = connection.startPacket();
            try {
                packetType.doPacket(b);
                this.write(b);
                packetType.send(connection);
            }
            catch (Exception e) {
                connection.cancelPacket();
                DebugLog.Multiplayer.printException(e, "Packet " + packetType.name() + " send error", LogSeverity.Error);
            }
        }
    }

    default public void sendToClient(PacketTypes.PacketType packetType, IConnection connection) {
        if (GameServer.server) {
            this.sendToConnection(packetType, connection);
        }
    }

    default public void sendToServer(PacketTypes.PacketType packetType) {
        if (GameClient.client) {
            this.sendToConnection(packetType, GameClient.connection);
        }
    }

    default public void sendToClients(PacketTypes.PacketType packetType, UdpConnection excluded) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (excluded != null && connection.getConnectedGUID() == excluded.getConnectedGUID() || !connection.isFullyConnected()) continue;
                this.sendToConnection(packetType, connection);
            }
        }
    }

    default public void sendToRelativeClients(PacketTypes.PacketType packetType, UdpConnection excluded, float x, float y) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (excluded != null && connection.getConnectedGUID() == excluded.getConnectedGUID() || !connection.isFullyConnected() || !connection.isRelevantTo(x, y)) continue;
                this.sendToConnection(packetType, connection);
            }
        }
    }

    public static void send(IConnection connection, PacketTypes.PacketType packetType, Object ... values2) {
        if (connection != null) {
            INetworkPacket packet = connection.getPacket(packetType);
            packet.setData(values2);
            packet.sendToConnection(packetType, connection);
        }
    }

    public static void send(PacketTypes.PacketType packetType, Object ... values2) {
        if (GameClient.client) {
            INetworkPacket.send(GameClient.connection, packetType, values2);
        }
    }

    public static void send(IsoPlayer player, PacketTypes.PacketType packetType, Object ... values2) {
        UdpConnection connection;
        if (GameServer.server && (connection = GameServer.getConnectionFromPlayer(player)) != null) {
            INetworkPacket.send(connection, packetType, values2);
        }
    }

    public static void sendToAll(PacketTypes.PacketType packetType, Object ... values2) {
        if (GameServer.server) {
            INetworkPacket.sendToAll(packetType, null, values2);
        }
    }

    public static void sendToAll(PacketTypes.PacketType packetType, IConnection excluded, Object ... values2) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (excluded != null && connection.getConnectedGUID() == excluded.getConnectedGUID() || !connection.isFullyConnected()) continue;
                INetworkPacket packet = connection.getPacket(packetType);
                packet.setData(values2);
                packet.sendToConnection(packetType, connection);
            }
        }
    }

    public static void sendToRelative(PacketTypes.PacketType packetType, float x, float y, Object ... values2) {
        if (GameServer.server) {
            INetworkPacket.sendToRelative(packetType, null, x, y, values2);
        }
    }

    public static void sendToRelative(PacketTypes.PacketType packetType, IConnection excluded, float x, float y, Object ... values2) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (excluded != null && connection.getConnectedGUID() == excluded.getConnectedGUID() || !connection.isFullyConnected() || !connection.isRelevantTo(x, y)) continue;
                INetworkPacket packet = connection.getPacket(packetType);
                packet.setData(values2);
                packet.sendToConnection(packetType, connection);
            }
        }
    }

    public static void sendByCapability(PacketTypes.PacketType packetType, Capability capability, Object ... values2) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (!connection.isFullyConnected() || connection.getRole() == null || !connection.getRole().hasCapability(capability)) continue;
                INetworkPacket packet = connection.getPacket(packetType);
                packet.setData(values2);
                packet.sendToConnection(packetType, connection);
            }
        }
    }
}

