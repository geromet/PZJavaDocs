/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.InventoryItem;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.ContainerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=2, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class SyncItemModDataPacket
implements INetworkPacket {
    @JSONField
    ContainerID containerId = new ContainerID();
    @JSONField
    int itemId;
    @JSONField
    KahluaTable table;

    @Override
    public void setData(Object ... values2) {
        InventoryItem item = (InventoryItem)values2[0];
        this.containerId.set(item.getContainer());
        this.itemId = item.getID();
        this.table = item.getModData();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putInt(this.itemId);
        try {
            this.table.save(b.bb);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.itemId = b.getInt();
        InventoryItem item = this.containerId.getContainer().getItemWithID(this.itemId);
        if (item == null) {
            return;
        }
        this.table = item.getModData();
        try {
            this.table.load(b.bb, 244);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

