/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.Vector3;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.packets.INetworkPacket;

public class Prediction
implements INetworkPacket {
    @JSONField
    public byte type = 0;
    @JSONField
    public float x;
    @JSONField
    public float y;
    @JSONField
    public byte z;
    @JSONField
    public float direction;
    @JSONField
    public byte distance;
    public final Vector3 position = new Vector3();

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.type = b.getByte();
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getByte();
        this.direction = b.getFloat();
        this.distance = b.getByte();
        this.position.set((float)this.distance * (float)Math.cos(this.direction) + this.x, (float)this.distance * (float)Math.sin(this.direction) + this.y, this.z);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.put(this.type);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.put(this.z);
        b.putFloat(this.direction);
        b.put(this.distance);
    }

    public void copy(Prediction other) {
        this.type = other.type;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.direction = other.direction;
        this.distance = other.distance;
    }
}

