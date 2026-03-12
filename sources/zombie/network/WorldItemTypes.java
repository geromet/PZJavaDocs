/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import zombie.core.network.ByteBufferReader;
import zombie.iso.IsoObject;

public class WorldItemTypes {
    public static IsoObject createFromBuffer(ByteBufferReader bb) {
        return IsoObject.factoryFromFileInput(null, bb.bb);
    }
}

