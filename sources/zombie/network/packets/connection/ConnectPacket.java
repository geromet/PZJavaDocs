/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.connection;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunkMap;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class ConnectPacket
implements INetworkPacket {
    @JSONField
    protected byte index;
    @JSONField
    protected byte range;
    @JSONField
    byte extraInfoFlags;
    protected IsoPlayer player;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 1 && values2[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values2[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(IsoPlayer player) {
        this.player = player;
        this.index = (byte)player.playerIndex;
        this.range = (byte)IsoChunkMap.chunkGridWidth;
        this.extraInfoFlags = player.getExtraInfoFlags();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        GameServer.receivePlayerConnect(b, connection, connection.getUserName());
        GameServer.sendInitialWorldState(connection);
        INetworkPacket.send(connection, PacketTypes.PacketType.MetaData, connection.getUserName());
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.index);
        b.putByte(this.range);
        b.putByte(this.extraInfoFlags);
    }
}

