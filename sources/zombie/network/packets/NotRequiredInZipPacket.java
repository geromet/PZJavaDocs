/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.WorldStreamer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=4, priority=0, reliability=0, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class NotRequiredInZipPacket
implements INetworkPacket {
    boolean networkFileDebug = DebugType.NetworkFileDebug.isEnabled();
    private ArrayList<WorldStreamer.ChunkRequest> requests;

    public void set(ArrayList<WorldStreamer.ChunkRequest> tempRequests) {
        this.requests = tempRequests;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.requests.size());
        for (int i = 0; i < this.requests.size(); ++i) {
            WorldStreamer.ChunkRequest request = this.requests.get(i);
            if (this.networkFileDebug) {
                DebugLog.NetworkFileDebug.debugln("cancelled " + request.chunk.wx + "," + request.chunk.wy);
            }
            b.putInt(request.requestNumber);
            request.flagsMain |= 2;
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (connection.getPlayerDownloadServer() == null) {
            return;
        }
        int numRequests = b.getInt();
        for (int i = 0; i < numRequests; ++i) {
            int requestNumber = b.getInt();
            connection.getPlayerDownloadServer().workerThread.cancelQ.add(requestNumber);
        }
    }
}

