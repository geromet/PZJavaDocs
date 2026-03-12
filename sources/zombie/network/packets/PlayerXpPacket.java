/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.CanModifyPlayerStatsInThePlayerStatsUI, handlingType=3)
public class PlayerXpPacket
extends PlayerID
implements INetworkPacket {
    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        try {
            this.getPlayer().getXp().save(b.bb);
        }
        catch (IOException e) {
            DebugLog.Multiplayer.printException(e, "Player XP save error", LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection) && !this.getPlayer().isDead()) {
            try {
                this.getPlayer().getXp().load(b.bb, 244);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "Player XP load error", LogSeverity.Error);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToClients(packetType, connection);
    }
}

