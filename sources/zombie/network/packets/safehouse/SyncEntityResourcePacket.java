/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.safehouse;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.entity.components.resources.Resource;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.ResourceID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=2, anticheats={})
public class SyncEntityResourcePacket
implements INetworkPacket {
    private final ResourceID resourceID = new ResourceID();

    @Override
    public void setData(Object ... values2) {
        this.resourceID.set((Resource)values2[0]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.resourceID.parse(b, connection);
        try {
            this.resourceID.getResource().load(b.bb, 244);
        }
        catch (IOException e) {
            ExceptionLogger.logException(e);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.resourceID.write(b);
        try {
            this.resourceID.getResource().save(b.bb);
        }
        catch (IOException e) {
            ExceptionLogger.logException(e);
        }
    }
}

