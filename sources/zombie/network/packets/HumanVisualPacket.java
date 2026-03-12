/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class HumanVisualPacket
implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 1 && values2[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values2[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(IsoPlayer player) {
        this.player.set(player);
    }

    public void process(UdpConnection connection) {
        if (GameServer.server) {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                IsoPlayer p2;
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() == connection.getConnectedGUID() || (p2 = GameServer.getAnyPlayerFromConnection(c)) == null) continue;
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.HumanVisual.doPacket(b2);
                try {
                    this.write(b2);
                    PacketTypes.PacketType.HumanVisual.send(c);
                    continue;
                }
                catch (RuntimeException e) {
                    c.cancelPacket();
                    ExceptionLogger.logException(e);
                }
            }
        }
        if (GameClient.client) {
            this.player.getPlayer().resetModelNextFrame();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.process(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.process(connection);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.player.parse(b, connection);
        if (this.player.isConsistent(connection)) {
            try {
                this.player.getPlayer().getHumanVisual().load(b.bb, 244);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        try {
            this.player.getPlayer().getHumanVisual().save(b.bb);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

