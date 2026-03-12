/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.packets.IDescriptor;

public interface INetworkPacketField
extends IDescriptor {
    public void parse(ByteBufferReader var1, IConnection var2);

    public void write(ByteBufferWriter var1);

    default public int getPacketSizeBytes() {
        return 0;
    }

    default public boolean isConsistent(IConnection connection) {
        return true;
    }
}

