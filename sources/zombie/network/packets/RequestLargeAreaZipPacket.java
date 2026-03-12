/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunkMap;
import zombie.iso.Vector3;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.ZombiePopulationManager;

@PacketSetting(ordering=4, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class RequestLargeAreaZipPacket
implements INetworkPacket {
    @JSONField
    private int wx;
    @JSONField
    private int wy;
    @JSONField
    private int chunkMapWidth;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 2 && values2[0] instanceof Integer && values2[1] instanceof Integer) {
            this.set((Integer)values2[0], (Integer)values2[1]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(int wx, int wy) {
        this.wx = wx;
        this.wy = wy;
        this.chunkMapWidth = IsoChunkMap.chunkGridWidth;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.wx);
        b.putInt(this.wy);
        b.putInt(this.chunkMapWidth);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.wx = b.getInt();
        this.wy = b.getInt();
        this.chunkMapWidth = b.getInt();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.wasInLoadingQueue()) {
            GameServer.kick(connection, "UI_Policy_Kick", "The server received an invalid request");
        }
        if (connection.getPlayerDownloadServer() != null) {
            connection.connectArea[0] = new Vector3(this.wx, this.wy, this.chunkMapWidth);
            connection.setChunkGridWidth(this.chunkMapWidth);
            ZombiePopulationManager.instance.updateLoadedAreas();
        }
    }
}

