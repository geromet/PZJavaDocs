/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.globalObjects.CGlobalObjectNetwork;
import zombie.globalObjects.SGlobalObjectNetwork;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class GlobalObjectsPacket
implements INetworkPacket {
    ByteBuffer buffer;

    public void set(ByteBuffer bb) {
        this.buffer = bb;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.buffer.flip();
        b.put(this.buffer);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (GameServer.server) {
            byte playerIndex = b.getByte();
            IsoPlayer player = GameServer.getPlayerFromConnection(connection, playerIndex);
            if (playerIndex == -1) {
                player = GameServer.getAnyPlayerFromConnection(connection);
            }
            if (player == null) {
                DebugLog.log("receiveGlobalObjects: player is null");
                return;
            }
            SGlobalObjectNetwork.receive(b, player);
        } else {
            try {
                CGlobalObjectNetwork.receive(b);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

