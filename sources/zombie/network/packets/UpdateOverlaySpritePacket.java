/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoObject;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class UpdateOverlaySpritePacket
implements INetworkPacket {
    @JSONField
    NetObject netObject = new NetObject();
    @JSONField
    String spriteName;
    @JSONField
    float colorR;
    @JSONField
    float colorG;
    @JSONField
    float colorB;
    @JSONField
    float colorA;

    @Override
    public void setData(Object ... values2) {
        this.netObject.setObject((IsoObject)values2[0]);
        this.spriteName = (String)values2[1];
        this.colorR = ((Float)values2[2]).floatValue();
        this.colorG = ((Float)values2[3]).floatValue();
        this.colorB = ((Float)values2[4]).floatValue();
        this.colorA = ((Float)values2[5]).floatValue();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.netObject.write(b);
        b.putUTF(this.spriteName);
        b.putFloat(this.colorR);
        b.putFloat(this.colorG);
        b.putFloat(this.colorB);
        b.putFloat(this.colorA);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.netObject.parse(b, connection);
        this.spriteName = b.getUTF();
        this.colorR = b.getFloat();
        this.colorG = b.getFloat();
        this.colorB = b.getFloat();
        this.colorA = b.getFloat();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.netObject.getObject() != null) {
            this.netObject.getObject().setOverlaySprite(this.spriteName, this.colorR, this.colorG, this.colorB, this.colorA, false);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.netObject.getObject() != null && this.netObject.getObject().setOverlaySprite(this.spriteName, this.colorR, this.colorG, this.colorB, this.colorA, false)) {
            GameServer.updateOverlayForClients(this.netObject.getObject(), this.spriteName, this.colorR, this.colorG, this.colorB, this.colorA, connection);
        }
    }
}

