/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.stash.StashSystem;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class ReadAnnotedMapPacket
implements INetworkPacket {
    @JSONField
    String stashName;

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.stashName);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.stashName = b.getUTF();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        StashSystem.prepareBuildingStash(this.stashName);
    }

    @Override
    public void setData(Object ... values2) {
        this.stashName = (String)values2[0];
    }
}

