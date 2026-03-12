/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.actions;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.Position;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=3, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class AddBloodPacket
extends Position
implements INetworkPacket {
    @JSONField
    private final Square square = new Square();
    @JSONField
    private int type;

    @Override
    public void setData(Object ... values2) {
        this.set(((Float)values2[0]).floatValue(), ((Float)values2[1]).floatValue(), ((Float)values2[2]).floatValue());
        this.type = (Integer)values2[3];
        this.square.set((IsoGridSquare)values2[4]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.type = b.getInt();
        this.square.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.type);
        this.square.write(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.square.getSquare().getChunk().addBloodSplat(this.x, this.y, this.z, this.type);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.square.isConsistent(connection);
    }
}

