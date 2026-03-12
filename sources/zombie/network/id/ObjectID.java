/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.id;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.id.IIdentifiable;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;

public abstract class ObjectID
implements INetworkPacketField {
    @JSONField
    protected long id;
    @JSONField
    protected ObjectIDType type;

    ObjectID(ObjectIDType type) {
        this.type = type;
        this.reset();
    }

    public long getObjectID() {
        return this.id;
    }

    ObjectIDType getType() {
        return this.type;
    }

    public IIdentifiable getObject() {
        return ObjectIDManager.get(this);
    }

    void set(long id, ObjectIDType type) {
        this.id = id;
        this.type = type;
    }

    public void set(ObjectID other) {
        this.set(other.id, other.type);
    }

    public void reset() {
        this.id = -1L;
    }

    public void load(ByteBuffer input) {
        this.type = ObjectIDType.valueOf(input.get());
    }

    public void save(ByteBuffer output) {
        output.put(this.type.index);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.load(b.bb);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.save(b.bb);
    }

    public String toString() {
        return this.type.name() + "-" + this.id;
    }

    public int hashCode() {
        return (int)(this.id * 10L + (long)this.type.index);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ObjectID objectID = (ObjectID)o;
        return this.id == objectID.id && this.type == objectID.type;
    }

    static class ObjectIDShort
    extends ObjectID {
        ObjectIDShort(ObjectIDType type) {
            super(type);
        }

        @Override
        public void load(ByteBuffer input) {
            this.id = input.getShort();
            super.load(input);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putShort((short)this.id);
            super.save(output);
        }

        @Override
        public int getPacketSizeBytes() {
            return 3;
        }
    }

    static class ObjectIDInteger
    extends ObjectID {
        ObjectIDInteger(ObjectIDType type) {
            super(type);
        }

        @Override
        public void load(ByteBuffer input) {
            this.id = input.getInt();
            super.load(input);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putInt((int)this.id);
            super.save(output);
        }

        @Override
        public int getPacketSizeBytes() {
            return 5;
        }
    }
}

