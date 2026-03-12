/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoWorld;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.ContainerID;
import zombie.network.fields.PlayerItem;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class ReplaceInventoryItemInContainerPacket
implements INetworkPacket {
    @JSONField
    ContainerID containerId = new ContainerID();
    @JSONField
    int oldItemId;
    @JSONField
    PlayerItem item = new PlayerItem();

    @Override
    public void setData(Object ... values2) {
        this.containerId.set((ItemContainer)values2[0]);
        this.oldItemId = ((InventoryItem)values2[1]).id;
        this.item.set((InventoryItem)values2[2]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putInt(this.oldItemId);
        this.item.write(b);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.oldItemId = b.getInt();
        this.item.parse(b, connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (IsoWorld.instance.currentCell == null) {
            return;
        }
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            InventoryItem item;
            if (container.getType().equals("floor") && !container.getItems().isEmpty() && (item = container.getItems().get(0)) instanceof InventoryContainer) {
                InventoryContainer inventoryContainer = (InventoryContainer)item;
                container = inventoryContainer.getItemContainer();
            }
            InventoryItem oldItem = container.getItemWithID(this.oldItemId);
            if (this.containerId.getPart() != null) {
                this.containerId.getPart().setContainerContentAmount(container.getCapacityWeight());
            }
            container.removeItemWithID(this.oldItemId);
            container.addItem(this.item.getItem());
            container.setExplored(true);
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer isoPlayer = IsoPlayer.players[i];
                if (!(isoPlayer instanceof IsoPlayer)) continue;
                IsoPlayer player = isoPlayer;
                ActionManager.getInstance().replaceObjectInQueuedActions(player, oldItem, this.item.getItem());
            }
        }
    }

    public int getX() {
        return this.containerId.x;
    }

    public int getY() {
        return this.containerId.y;
    }
}

