/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoThumpable;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SyncThumpablePacket
implements INetworkPacket {
    @JSONField
    NetObject netObject = new NetObject();
    @JSONField
    int lockedByCode;
    @JSONField
    boolean lockedByPadlock;
    @JSONField
    int keyId;

    @Override
    public void setData(Object ... values2) {
        IsoThumpable obj = (IsoThumpable)values2[0];
        this.netObject.setObject(obj);
        this.lockedByCode = obj.getLockedByCode();
        this.lockedByPadlock = obj.isLockedByPadlock();
        this.keyId = obj.getKeyId();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.netObject.write(b);
        b.putInt(this.lockedByCode);
        b.putBoolean(this.lockedByPadlock);
        b.putInt(this.keyId);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.netObject.parse(b, connection);
        this.lockedByCode = b.getInt();
        this.lockedByPadlock = b.getBoolean();
        this.keyId = b.getInt();
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoThumpable obj = (IsoThumpable)this.netObject.getObject();
        obj.lockedByCode = this.lockedByCode;
        obj.lockedByPadlock = this.lockedByPadlock;
        obj.keyId = this.keyId;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoThumpable obj = (IsoThumpable)this.netObject.getObject();
        obj.lockedByCode = this.lockedByCode;
        obj.lockedByPadlock = this.lockedByPadlock;
        obj.keyId = this.keyId;
        this.sendToRelativeClients(PacketTypes.PacketType.SyncThumpable, connection, obj.square.getX(), obj.square.getY());
    }
}

