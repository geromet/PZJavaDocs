/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class ClothingWetnessPacket
implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private final List<ItemWetness> itemWetness = new ArrayList<ItemWetness>();

    @Override
    public void setData(Object ... values2) {
        this.playerId.set((IsoPlayer)values2[0]);
        this.itemWetness.clear();
        List items = (List)values2[1];
        for (InventoryItem item : items) {
            this.itemWetness.add(new ItemWetness(item.getID(), item.getWetness()));
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        this.itemWetness.clear();
        int count = b.getByte() & 0xFF;
        for (int i = 0; i < count; ++i) {
            int itemID = b.getInt();
            float wetness = b.getFloat();
            this.itemWetness.add(new ItemWetness(itemID, wetness));
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putByte(this.itemWetness.size());
        for (ItemWetness itemWetness : this.itemWetness) {
            b.putInt(itemWetness.id);
            b.putFloat(itemWetness.wetness);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        ItemContainer inventory = this.playerId.getPlayer().getInventory();
        for (ItemWetness itemWetness : this.itemWetness) {
            InventoryItem inventoryItem = inventory.getItemWithID(itemWetness.id);
            if (!(inventoryItem instanceof Clothing)) continue;
            Clothing clothing = (Clothing)inventoryItem;
            clothing.setWetness(itemWetness.wetness);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerId.getPlayer() != null;
    }

    private record ItemWetness(int id, float wetness) {
    }
}

