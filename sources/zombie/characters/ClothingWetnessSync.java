/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class ClothingWetnessSync {
    private static final List<InventoryItem> tempItems = new ArrayList<InventoryItem>();
    private static final List<InventoryItem> toSend = new ArrayList<InventoryItem>();
    private final IsoPlayer player;
    private final Map<InventoryItem, ItemWetness> itemMap = new HashMap<InventoryItem, ItemWetness>();
    private final UpdateLimit updateLimit = new UpdateLimit(3000L);

    public ClothingWetnessSync(IsoPlayer player) {
        this.player = player;
    }

    public void update() {
        if (!this.updateLimit.Check()) {
            return;
        }
        ArrayList<InventoryItem> items = this.player.getInventory().getItems();
        tempItems.clear();
        tempItems.addAll(this.itemMap.keySet());
        toSend.clear();
        for (InventoryItem item : items) {
            if (!(item instanceof Clothing)) continue;
            Clothing clothing = (Clothing)item;
            float wetness = clothing.getWetness();
            ItemWetness itemWetness = this.itemMap.computeIfAbsent(item, ItemWetness::new);
            if (PZMath.ceil(itemWetness.lastSentWetness) != PZMath.ceil(wetness)) {
                itemWetness.lastSentWetness = wetness;
                toSend.add(item);
            }
            tempItems.remove(item);
        }
        tempItems.forEach(this.itemMap::remove);
        if (toSend.isEmpty()) {
            return;
        }
        UdpConnection connection = GameServer.getConnectionFromPlayer(this.player);
        INetworkPacket.send(connection, PacketTypes.PacketType.ClothingWetness, this.player, toSend);
    }

    private static final class ItemWetness {
        final InventoryItem item;
        float lastSentWetness = Float.NaN;

        ItemWetness(InventoryItem item) {
            this.item = item;
        }
    }
}

