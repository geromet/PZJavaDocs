/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=1)
public final class PlayerDropHeldItemsPacket
extends PlayerID
implements INetworkPacket {
    @JSONField
    boolean heavy;
    @JSONField
    boolean isThrow;
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    int z;

    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0]);
        this.x = (Integer)values2[1];
        this.y = (Integer)values2[2];
        this.z = (Integer)values2[3];
        this.heavy = (Boolean)values2[4];
        this.isThrow = values2.length > 5 ? (Boolean)values2[5] : false;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getByte();
        this.heavy = b.getBoolean();
        this.isThrow = b.getBoolean();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.getPlayer().dropHeldItems(this.x, this.y, this.z, this.heavy, this.isThrow);
        INetworkPacket.sendToRelative(PacketTypes.PacketType.Equip, this.x, (float)this.y, this.getPlayer());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        b.putBoolean(this.heavy);
        b.putBoolean(this.isThrow);
    }
}

