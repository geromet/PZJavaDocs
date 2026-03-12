/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.Translator;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.GameLoadingState;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.RequestDataPacket;

public class RequestDataManager {
    public static final int smallFileSize = 1024;
    public static final int maxLargeFileSize = 0x3200000;
    public static final int packSize = 204800;
    private final ArrayList<RequestData> requests = new ArrayList();
    private static RequestDataManager instance;

    private RequestDataManager() {
    }

    public static RequestDataManager getInstance() {
        if (instance == null) {
            instance = new RequestDataManager();
        }
        return instance;
    }

    public void ACKWasReceived(RequestDataPacket.RequestID id, UdpConnection connection, int bytesTransmitted) {
        RequestData data = null;
        for (int i = 0; i <= this.requests.size(); ++i) {
            if (this.requests.get((int)i).connectionGuid != connection.getConnectedGUID()) continue;
            data = this.requests.get(i);
            break;
        }
        if (data == null || data.id != id) {
            return;
        }
        this.sendData(data);
    }

    public void putDataForTransmit(RequestDataPacket.RequestID id, UdpConnection connection, ByteBuffer bb) {
        RequestData data = new RequestData(id, bb, connection.getConnectedGUID());
        this.requests.add(data);
        this.sendData(data);
    }

    public void disconnect(UdpConnection connection) {
        long currentTime = System.currentTimeMillis();
        this.requests.removeIf(requestData -> currentTime - requestData.creationTime > 60000L || requestData.connectionGuid == connection.getConnectedGUID());
    }

    public void clear() {
        this.requests.clear();
    }

    private void sendData(RequestData data) {
        int toSend;
        data.creationTime = System.currentTimeMillis();
        int fileSize = data.bbr.limit();
        data.realTransmittedFromLastAck = 0;
        UdpConnection connection = GameServer.udpEngine.getActiveConnection(data.connectionGuid);
        RequestDataPacket packet = new RequestDataPacket();
        packet.setPartData(data.id, data.bbr);
        while (data.realTransmittedFromLastAck < 204800 && (toSend = Math.min(1024, fileSize - data.realTransmitted)) != 0) {
            packet.setPartDataParameters(data.realTransmitted, toSend);
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.RequestData.send(connection);
            data.realTransmittedFromLastAck += toSend;
            data.realTransmitted += toSend;
        }
        if (data.realTransmitted == fileSize) {
            this.requests.remove(data);
        }
    }

    public ByteBufferReader receiveClientData(RequestDataPacket.RequestID id, ByteBuffer bb, int fileSize, int bytesTransmitted) {
        RequestData data = null;
        for (int i = 0; i < this.requests.size(); ++i) {
            if (this.requests.get((int)i).id != id) continue;
            data = this.requests.get(i);
            break;
        }
        if (data == null) {
            data = new RequestData(id, fileSize, 0L);
            this.requests.add(data);
        }
        data.bbr.position(bytesTransmitted);
        data.bbr.put(bb.array(), 0, bb.limit());
        data.realTransmitted += bb.limit();
        data.realTransmittedFromLastAck += bb.limit();
        if (data.realTransmittedFromLastAck >= 204800) {
            data.realTransmittedFromLastAck = 0;
            RequestDataPacket packet = new RequestDataPacket();
            packet.setACK(data.id);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.RequestData.send(GameClient.connection);
        }
        GameLoadingState.gameLoadingString = Translator.getText("IGUI_MP_DownloadedLargeFile", data.realTransmitted * 100 / fileSize, data.id.getDescriptor());
        if (data.realTransmitted == fileSize) {
            this.requests.remove(data);
            data.bbr.position(0);
            return data.bbr;
        }
        return null;
    }

    static class RequestData {
        private final RequestDataPacket.RequestID id;
        private final ByteBufferReader bbr;
        private final long connectionGuid;
        private long creationTime = System.currentTimeMillis();
        private int realTransmitted;
        private int realTransmittedFromLastAck;

        public RequestData(RequestDataPacket.RequestID id, ByteBuffer bb, long connectionGuid) {
            this(id, bb.position(), connectionGuid);
            this.bbr.put(bb.array(), 0, this.bbr.limit());
        }

        public RequestData(RequestDataPacket.RequestID id, int bufferSize, long connectionGuid) {
            this.id = id;
            this.bbr = new ByteBufferReader(ByteBuffer.allocate(bufferSize));
            this.bbr.clear();
            this.connectionGuid = connectionGuid;
            this.realTransmitted = 0;
            this.realTransmittedFromLastAck = 0;
        }
    }
}

