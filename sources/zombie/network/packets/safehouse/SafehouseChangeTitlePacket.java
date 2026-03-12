/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.SafehouseID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class SafehouseChangeTitlePacket
extends SafehouseID
implements INetworkPacket {
    @JSONField
    public String title;

    @Override
    public void setData(Object ... values2) {
        this.set((SafeHouse)values2[0]);
        this.title = (String)values2[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.title);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.title = b.getUTF();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        }
        if (StringUtils.isNullOrEmpty(this.title)) {
            DebugLog.Multiplayer.error("title is not set");
            return false;
        }
        if (!connection.getRole().hasCapability(Capability.CanSetupSafehouses) && !connection.hasPlayer(this.getSafehouse().getOwner())) {
            DebugLog.Multiplayer.error("player is not owner");
            return false;
        }
        if (!connection.hasPlayer(this.getSafehouse().getOwner())) {
            DebugLog.Multiplayer.error("player renaming safehouse is not the owner");
            return false;
        }
        return true;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.getSafehouse().setTitle(this.title);
        for (UdpConnection c : GameServer.udpEngine.connections) {
            if (!c.isFullyConnected()) continue;
            INetworkPacket.send(c, PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), false);
        }
    }
}

