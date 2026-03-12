/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.fields.IDInteger;
import zombie.network.fields.INetworkPacketField;

public class SafehouseID
extends IDInteger
implements INetworkPacketField {
    private SafeHouse safeHouse;

    public void set(SafeHouse safeHouse) {
        this.setID(safeHouse.getOnlineID());
        this.safeHouse = safeHouse;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.safeHouse = SafeHouse.getSafeHouse(this.getID());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.getSafehouse() != null;
    }

    public String toString() {
        return String.valueOf(this.getID());
    }

    public SafeHouse getSafehouse() {
        return this.safeHouse;
    }
}

