/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunkMap;
import zombie.network.ClientServerMap;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=4, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=6)
public class ServerMapPacket
implements INetworkPacket {
    @JSONField
    ClientServerMap map;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 1 && values2[0] instanceof ClientServerMap) {
            this.set((ClientServerMap)values2[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(ClientServerMap map) {
        this.map = map;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.map.playerIndex);
        b.putInt(this.map.centerX);
        b.putInt(this.map.centerY);
        for (int y = 0; y < this.map.width; ++y) {
            for (int x = 0; x < this.map.width; ++x) {
                b.putBoolean(this.map.loaded[x + y * this.map.width]);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        byte playerIndex = b.getByte();
        int centerX = b.getInt();
        int centerY = b.getInt();
        this.map = GameClient.loadedCells[playerIndex];
        if (this.map == null) {
            this.map = GameClient.loadedCells[playerIndex] = new ClientServerMap(playerIndex, centerX, centerY, IsoChunkMap.chunkGridWidth);
        }
        this.map.centerX = centerX;
        this.map.centerY = centerY;
        for (int y = 0; y < this.map.width; ++y) {
            for (int x = 0; x < this.map.width; ++x) {
                this.map.loaded[x + y * this.map.width] = b.getBoolean();
            }
        }
    }
}

