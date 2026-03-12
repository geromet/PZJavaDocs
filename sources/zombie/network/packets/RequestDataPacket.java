/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.PersistentOutfits;
import zombie.SharedDescriptors;
import zombie.characters.Capability;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.ConnectionDetails;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.RequestDataManager;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.RecordedMedia;
import zombie.worldMap.network.WorldMapClient;
import zombie.worldMap.network.WorldMapServer;

@PacketSetting(ordering=4, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=7)
public class RequestDataPacket
implements INetworkPacket {
    RequestType type;
    RequestID id;
    ByteBuffer buffer;
    int dataSize;
    int dataSent;
    int partSize;
    public static ByteBuffer largeFileBb;
    public static ByteBufferReader largeFileBbReader;
    public static ByteBufferWriter largeFileBbWriter;

    public void allocateLargeFileBB() {
        if (largeFileBb == null) {
            largeFileBb = ByteBuffer.allocate(0x3200000);
            largeFileBbReader = new ByteBufferReader(largeFileBb);
            largeFileBbWriter = new ByteBufferWriter(largeFileBb);
        }
    }

    public void setRequest() {
        this.type = RequestType.Request;
        this.id = RequestID.ZombieOutfitDescriptors;
    }

    public void setRequest(RequestID id) {
        this.type = RequestType.Request;
        this.id = id;
    }

    public void setPartData(RequestID id, ByteBufferReader bb) {
        this.type = RequestType.PartData;
        this.buffer = bb.bb;
        this.id = id;
        this.dataSize = bb.limit();
    }

    public void setPartDataParameters(int bytesSent, int partSize) {
        this.dataSent = bytesSent;
        this.partSize = partSize;
    }

    public void setACK(RequestID id) {
        this.type = RequestType.PartDataACK;
        this.id = id;
    }

    public void sendConnectingDetails(UdpConnection connection, ServerWorldDatabase.LogonResult logonResult) {
        if (!GameServer.server) {
            return;
        }
        this.id = RequestID.ConnectionDetails;
        this.allocateLargeFileBB();
        largeFileBb.clear();
        ConnectionDetails.write(connection, logonResult, largeFileBbWriter);
        this.doSendRequest(connection);
        DebugLog.Multiplayer.debugln("%s %db", this.id.name(), largeFileBb.position());
        ConnectionManager.log("send-packet", "connection-details", connection);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        try {
            this.type = b.getEnum(RequestType.class);
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, "RequestData packet parse failed", LogSeverity.Error);
            this.type = RequestType.None;
        }
        this.id = b.getEnum(RequestID.class);
        if (GameClient.client) {
            if (this.type == RequestType.FullData) {
                int size = b.limit() - b.position();
                this.allocateLargeFileBB();
                largeFileBb.clear();
                largeFileBb.limit(size);
                largeFileBb.put(b.array(), b.position(), size);
                this.buffer = largeFileBb;
            } else if (this.type == RequestType.PartData) {
                this.dataSize = b.getInt();
                this.dataSent = b.getInt();
                this.partSize = b.getInt();
                this.allocateLargeFileBB();
                largeFileBb.clear();
                largeFileBb.limit(this.partSize);
                largeFileBb.put(b.array(), b.position(), this.partSize);
                this.buffer = largeFileBb;
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.type);
        b.putEnum(this.id);
        if (GameServer.server) {
            if (this.type == RequestType.FullData) {
                b.put(this.buffer.array(), 0, this.buffer.position());
            } else if (this.type == RequestType.PartData) {
                b.putInt(this.dataSize);
                b.putInt(this.dataSent);
                b.putInt(this.partSize);
                b.put(this.buffer.array(), this.dataSent, this.partSize);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.wasInLoadingQueue() && this.id != RequestID.ConnectionDetails) {
            GameServer.kick(connection, "UI_Policy_Kick", "The server received an invalid request");
        }
        if (this.type == RequestType.Request) {
            this.doProcessRequest(connection);
        } else if (this.type == RequestType.PartDataACK) {
            RequestDataManager.getInstance().ACKWasReceived(this.id, connection, this.dataSent);
        }
    }

    private void doSendRequest(UdpConnection connection) {
        this.allocateLargeFileBB();
        if (largeFileBb.position() < 1024) {
            this.type = RequestType.FullData;
            this.buffer = largeFileBb;
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(b);
            this.write(b);
            PacketTypes.PacketType.RequestData.send(connection);
        } else {
            RequestDataManager.getInstance().putDataForTransmit(this.id, connection, largeFileBb);
        }
    }

    private void doProcessRequest(UdpConnection connection) {
        if (this.id == RequestID.ZombieOutfitDescriptors) {
            try {
                largeFileBb.clear();
                PersistentOutfits.instance.save(largeFileBb);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            this.doSendRequest(connection);
        }
        if (this.id == RequestID.PlayerZombieDescriptors) {
            SharedDescriptors.Descriptor[] descs = SharedDescriptors.getPlayerZombieDescriptors();
            int count = 0;
            for (int i = 0; i < descs.length; ++i) {
                if (descs[i] == null) continue;
                ++count;
            }
            if (count * 2 * 1024 > largeFileBb.capacity()) {
                largeFileBb = ByteBuffer.allocate(count * 2 * 1024);
                largeFileBbReader = new ByteBufferReader(largeFileBb);
                largeFileBbWriter = new ByteBufferWriter(largeFileBb);
            }
            try {
                largeFileBb.clear();
                largeFileBb.putShort((short)count);
                for (SharedDescriptors.Descriptor desc : descs) {
                    if (desc == null) continue;
                    desc.save(largeFileBbWriter);
                }
                this.doSendRequest(connection);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (this.id == RequestID.RadioData) {
            largeFileBb.clear();
            ZomboidRadio.getInstance().getRecordedMedia().sendRequestData(largeFileBb);
            this.doSendRequest(connection);
        }
        if (this.id == RequestID.WorldMap) {
            try {
                largeFileBb.clear();
                WorldMapServer.instance.sendRequestData(largeFileBb);
                this.doSendRequest(connection);
            }
            catch (IOException ex) {
                DebugLog.Multiplayer.printException(ex, "shared map symbols could not be saved", LogSeverity.Error);
                GameServer.kick(connection, "UI_Policy_KickReason", "shared map symbols could not be saved.");
                connection.forceDisconnect("save-shared-map-symbols");
                GameServer.addDisconnect(connection);
            }
        }
        DebugLog.Multiplayer.debugln("%s %db", this.id.name(), largeFileBb.position());
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.type == RequestType.FullData) {
            largeFileBb.position(0);
            this.doProcessData(largeFileBbReader);
        } else if (this.type == RequestType.PartData) {
            largeFileBb.position(0);
            this.doProcessPart(largeFileBbReader);
        }
    }

    private void doProcessPart(ByteBufferReader bb) {
        ByteBufferReader bb2 = RequestDataManager.getInstance().receiveClientData(this.id, bb.bb, this.dataSize, this.dataSent);
        if (bb2 != null) {
            this.doProcessData(bb2);
        }
    }

    private void doProcessData(ByteBufferReader bb) {
        if (this.id == RequestID.ConnectionDetails) {
            ConnectionDetails.parse(bb);
        }
        if (this.id == RequestID.ZombieOutfitDescriptors) {
            try {
                DebugLog.Multiplayer.debugln("received zombie descriptors");
                PersistentOutfits.instance.load(bb.bb);
            }
            catch (IOException ex) {
                DebugLog.Multiplayer.printException(ex, "PersistentOutfits loading IO error", LogSeverity.Error);
                ExceptionLogger.logException(ex);
            }
            catch (Exception ex) {
                DebugLog.Multiplayer.printException(ex, "PersistentOutfits loading error", LogSeverity.Error);
            }
        }
        if (this.id == RequestID.PlayerZombieDescriptors) {
            try {
                this.receivePlayerZombieDescriptors(bb);
            }
            catch (Exception ex) {
                DebugLog.Multiplayer.printException(ex, "Player zombie descriptors loading error", LogSeverity.Error);
                ExceptionLogger.logException(ex);
            }
        }
        if (this.id == RequestID.RadioData) {
            try {
                RecordedMedia.receiveRequestData(bb);
            }
            catch (Exception ex) {
                DebugLog.Multiplayer.printException(ex, "Radio data loading error", LogSeverity.Error);
                ExceptionLogger.logException(ex);
            }
        }
        if (this.id == RequestID.WorldMap) {
            try {
                WorldMapClient.instance.receiveRequestData(bb);
            }
            catch (IOException ex) {
                DebugLog.Multiplayer.printException(ex, "Shared map symbols loading error", LogSeverity.Error);
                ExceptionLogger.logException(ex);
            }
        }
        this.sendNextRequest(this.id);
    }

    private void sendNextRequest(RequestID id) {
        switch (id.ordinal()) {
            case 1: {
                this.setRequest(RequestID.PlayerZombieDescriptors);
                break;
            }
            case 2: {
                this.setRequest(RequestID.RadioData);
                break;
            }
            case 3: {
                this.setRequest(RequestID.WorldMap);
                break;
            }
            case 4: {
                GameClient.instance.setRequest(GameClient.RequestState.Complete);
            }
        }
        if (id != RequestID.WorldMap) {
            ByteBufferWriter bbw = GameClient.connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(bbw);
            this.write(bbw);
            PacketTypes.PacketType.RequestData.send(GameClient.connection);
        }
    }

    private void receivePlayerZombieDescriptors(ByteBufferReader bb) throws IOException {
        short count = bb.getShort();
        DebugLog.Multiplayer.debugln("received " + count + " player-zombie descriptors");
        for (short i = 0; i < count; i = (short)(i + 1)) {
            SharedDescriptors.Descriptor sharedDesc = new SharedDescriptors.Descriptor();
            sharedDesc.load(bb, 244);
            SharedDescriptors.registerPlayerZombieDescriptor(sharedDesc);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.type != RequestType.None;
    }

    @Override
    public String getDescription() {
        int i;
        Object s = "\n\tRequestDataPacket [";
        s = (String)s + "type=" + this.type.name() + " | ";
        if (this.type == RequestType.Request || this.type == RequestType.PartDataACK) {
            s = (String)s + "id=" + this.id.name() + "] ";
        }
        if (this.type == RequestType.FullData) {
            s = (String)s + "id=" + this.id.name() + " | ";
            s = (String)s + "data=(size:" + this.buffer.limit() + ", data=";
            this.buffer.position(0);
            for (i = 0; i < Math.min(15, this.buffer.limit()); ++i) {
                s = (String)s + " 0x" + Integer.toHexString(this.buffer.get() & 0xFF);
            }
            s = (String)s + ".. ] ";
        }
        if (this.type == RequestType.PartData) {
            s = (String)s + "id=" + this.id.name() + " | ";
            s = (String)s + "dataSize=" + this.dataSize + " | ";
            s = (String)s + "dataSent=" + this.dataSent + " | ";
            s = (String)s + "partSize=" + this.partSize + " | ";
            s = (String)s + "data=(size:" + this.buffer.limit() + ", data=";
            if (this.buffer.limit() >= this.dataSize) {
                this.buffer.position(this.dataSent);
            } else {
                this.buffer.position(0);
            }
            for (i = 0; i < Math.min(15, this.buffer.limit() - this.buffer.position()); ++i) {
                s = (String)s + " " + Integer.toHexString(this.buffer.get() & 0xFF);
            }
            s = (String)s + ".. ] ";
        }
        return s;
    }

    static enum RequestType {
        None,
        Request,
        FullData,
        PartData,
        PartDataACK;

    }

    public static enum RequestID {
        ConnectionDetails,
        ZombieOutfitDescriptors,
        PlayerZombieDescriptors,
        RadioData,
        WorldMap;


        public String getDescriptor() {
            return Translator.getText("IGUI_RequestID_" + this.name());
        }
    }
}

