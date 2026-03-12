/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.raknet;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.lwjglx.BufferUtils;
import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.core.znet.ZNetStatistics;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class RakNetPeerInterface {
    private static Thread mainThread;
    public static final int ID_NEW_INCOMING_CONNECTION = 19;
    public static final int ID_DISCONNECTION_NOTIFICATION = 21;
    public static final int ID_INCOMPATIBLE_PROTOCOL_VERSION = 25;
    public static final int ID_CONNECTED_PING = 0;
    public static final int ID_UNCONNECTED_PING = 1;
    public static final int ID_CONNECTION_LOST = 22;
    public static final int ID_ALREADY_CONNECTED = 18;
    public static final int ID_REMOTE_DISCONNECTION_NOTIFICATION = 31;
    public static final int ID_REMOTE_CONNECTION_LOST = 32;
    public static final int ID_REMOTE_NEW_INCOMING_CONNECTION = 33;
    public static final int ID_CONNECTION_BANNED = 23;
    public static final int ID_CONNECTION_ATTEMPT_FAILED = 17;
    public static final int ID_NO_FREE_INCOMING_CONNECTIONS = 20;
    public static final int ID_CONNECTION_REQUEST_ACCEPTED = 16;
    public static final int ID_INVALID_PASSWORD = 24;
    public static final int ID_PING = 28;
    public static final int ID_RAKVOICE_OPEN_CHANNEL_REQUEST = 44;
    public static final int ID_RAKVOICE_OPEN_CHANNEL_REPLY = 45;
    public static final int ID_RAKVOICE_CLOSE_CHANNEL = 46;
    public static final int ID_USER_PACKET_ENUM = 134;
    public static final int PacketPriority_IMMEDIATE = 0;
    public static final int PacketPriority_HIGH = 1;
    public static final int PacketPriority_MEDIUM = 2;
    public static final int PacketPriority_LOW = 3;
    public static final int PacketReliability_UNRELIABLE = 0;
    public static final int PacketReliability_UNRELIABLE_SEQUENCED = 1;
    public static final int PacketReliability_RELIABLE = 2;
    public static final int PacketReliability_RELIABLE_ORDERED = 3;
    public static final int PacketReliability_RELIABLE_SEQUENCED = 4;
    public static final int PacketReliability_UNRELIABLE_WITH_ACK_RECEIPT = 5;
    public static final int PacketReliability_RELIABLE_WITH_ACK_RECEIPT = 6;
    public static final int PacketReliability_RELIABLE_ORDERED_WITH_ACK_RECEIPT = 7;
    public static final byte ConnectionType_Disconnected = 0;
    public static final byte ConnectionType_UDPRakNet = 1;
    public static final byte ConnectionType_Steam = 2;
    ByteBuffer receiveBuf = BufferUtils.createByteBuffer(1000000);
    ByteBuffer sendBuf = BufferUtils.createByteBuffer(1000000);
    Lock sendLock = new ReentrantLock();

    public static void init() {
        mainThread = Thread.currentThread();
    }

    public native void Init(boolean var1);

    private native int Startup(int var1, String var2, boolean var3);

    public int Startup(int maxConnections) {
        return this.Startup(maxConnections, Core.getInstance().getVersionNumber(), GameServer.server);
    }

    public native void Shutdown();

    public native void SetServerIP(String var1);

    public native void SetServerPort(int var1, int var2);

    public native void SetClientPort(int var1);

    public native int Connect(String var1, int var2, String var3, boolean var4);

    public native int ConnectToSteamServer(long var1, String var3, boolean var4);

    public native String GetServerIP();

    public native long GetClientSteamID(long var1);

    public native long GetClientOwnerSteamID(long var1);

    public native void SetIncomingPassword(String var1);

    public native void SetTimeoutTime(int var1);

    public native void SetMaximumIncomingConnections(int var1);

    public native void SetOccasionalPing(boolean var1);

    public native void SetUnreliableTimeout(int var1);

    private native boolean TryReceive();

    private native int nativeGetData(ByteBuffer var1);

    public boolean Receive(ByteBuffer buffer) {
        if (this.TryReceive()) {
            try {
                buffer.clear();
                this.receiveBuf.clear();
                int n = this.nativeGetData(this.receiveBuf);
                buffer.put(this.receiveBuf);
                buffer.flip();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    public int Send(ByteBuffer data, int packetPriority, int packetReliability, byte orderingChannel, long guid, boolean broadcast) {
        this.sendLock.lock();
        this.sendBuf.clear();
        if (data.remaining() > this.sendBuf.remaining()) {
            System.out.println("Packet data too big.");
            this.sendLock.unlock();
            return 0;
        }
        try {
            this.sendBuf.put(data);
            this.sendBuf.flip();
            int i = this.sendNative(this.sendBuf, this.sendBuf.remaining(), packetPriority, packetReliability, orderingChannel, guid, broadcast);
            this.sendLock.unlock();
            return i;
        }
        catch (Exception ex) {
            System.out.println("Other weird packet data error.");
            ex.printStackTrace();
            this.sendLock.unlock();
            return 0;
        }
    }

    public int SendRaw(ByteBuffer data, int packetPriority, int packetReliability, byte orderingChannel, long guid, boolean broadcast) {
        try {
            return this.sendNative(data, data.remaining(), packetPriority, packetReliability, orderingChannel, guid, broadcast);
        }
        catch (Exception ex) {
            System.out.println("Other weird packet data error.");
            ex.printStackTrace();
            return 0;
        }
    }

    private native int sendNative(ByteBuffer var1, int var2, int var3, int var4, byte var5, long var6, boolean var8);

    public native long getGuidFromIndex(int var1);

    public native long getGuidOfPacket();

    public native String getIPFromGUID(long var1);

    public native void disconnect(long var1, String var3);

    private void connectionStateChangedCallback(String string, String message) {
        Thread thread2 = Thread.currentThread();
        if (thread2 == mainThread) {
            LuaEventManager.triggerEvent("OnConnectionStateChanged", string, message);
        } else {
            DebugLog.Multiplayer.debugln("state=\"%s\", message=\"%s\", thread=%s", string, message, thread2);
        }
        if (GameClient.client && "Connected".equals(string)) {
            GameClient.instance.udpEngine.connected();
        }
    }

    public native ZNetStatistics GetNetStatistics(long var1);

    public native int GetAveragePing(long var1);

    public native int GetLastPing(long var1);

    public native int GetLowestPing(long var1);

    public native int GetMTUSize(long var1);

    public native int GetConnectionsNumber();

    public native byte GetConnectionType(long var1);
}

