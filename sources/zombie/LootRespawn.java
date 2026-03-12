/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;

public final class LootRespawn {
    private static int lastRespawnHour = -1;
    private static final ArrayList<InventoryItem> existingItems = new ArrayList();
    private static final ArrayList<InventoryItem> newItems = new ArrayList();

    public static void update() {
        if (GameClient.client) {
            return;
        }
        int respawnEveryHours = LootRespawn.getRespawnInterval();
        if (respawnEveryHours <= 0) {
            return;
        }
        int lastRespawnHour = 7 + (int)(GameTime.getInstance().getWorldAgeHours() / (double)respawnEveryHours) * respawnEveryHours;
        if (LootRespawn.lastRespawnHour >= lastRespawnHour) {
            return;
        }
        LootRespawn.lastRespawnHour = lastRespawnHour;
        if (GameServer.server) {
            for (int i = 0; i < ServerMap.instance.loadedCells.size(); ++i) {
                ServerMap.ServerCell cell = ServerMap.instance.loadedCells.get(i);
                if (!cell.isLoaded) continue;
                for (int y = 0; y < 8; ++y) {
                    for (int x = 0; x < 8; ++x) {
                        IsoChunk chunk = cell.chunks[x][y];
                        LootRespawn.checkChunk(chunk);
                    }
                }
            }
        } else {
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[pn];
                if (chunkMap.ignore) continue;
                for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
                    for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                        IsoChunk chunk = chunkMap.getChunk(x, y);
                        LootRespawn.checkChunk(chunk);
                    }
                }
            }
        }
    }

    public static void Reset() {
        lastRespawnHour = -1;
    }

    public static void chunkLoaded(IsoChunk chunk) {
        if (GameClient.client) {
            return;
        }
        LootRespawn.checkChunk(chunk);
    }

    private static void checkChunk(IsoChunk chunk) {
        if (chunk == null) {
            return;
        }
        int respawnEveryHours = LootRespawn.getRespawnInterval();
        if (respawnEveryHours <= 0) {
            return;
        }
        if (GameTime.getInstance().getWorldAgeHours() < (double)respawnEveryHours) {
            return;
        }
        int lastRespawnHour = 7 + (int)(GameTime.getInstance().getWorldAgeHours() / (double)respawnEveryHours) * respawnEveryHours;
        if (chunk.lootRespawnHour > lastRespawnHour) {
            chunk.lootRespawnHour = lastRespawnHour;
        }
        if (chunk.lootRespawnHour >= lastRespawnHour) {
            return;
        }
        chunk.lootRespawnHour = lastRespawnHour;
        LootRespawn.respawnInChunk(chunk);
    }

    private static int getRespawnInterval() {
        return SandboxOptions.instance.hoursForLootRespawn.getValue();
    }

    private static void respawnInChunk(IsoChunk chunk) {
        boolean constructionPreventsRespawn = SandboxOptions.instance.constructionPreventsLootRespawn.getValue();
        boolean safehousePreventsRespawn = GameServer.server && ServerOptions.instance.safehousePreventsLootRespawn.getValue();
        int seenHoursPreventRespawn = SandboxOptions.instance.seenHoursPreventLootRespawn.getValue();
        double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                BuildingDef buildingDef;
                Zone zone;
                IsoGridSquare sq = chunk.getGridSquare(x, y, 0);
                Zone zone2 = zone = sq == null ? null : sq.getZone();
                if (zone == null || !"TownZone".equals(zone.getType()) && !"TownZones".equals(zone.getType()) && !"TrailerPark".equals(zone.getType()) || constructionPreventsRespawn && zone.haveConstruction || seenHoursPreventRespawn > 0 && zone.getHoursSinceLastSeen() <= (float)seenHoursPreventRespawn || safehousePreventsRespawn && SafeHouse.getSafeHouse(sq) != null) continue;
                if (sq.getBuilding() != null && (buildingDef = sq.getBuilding().getDef()) != null) {
                    if ((double)buildingDef.lootRespawnHour > worldAgeHours) {
                        buildingDef.lootRespawnHour = 0;
                    }
                    if (buildingDef.lootRespawnHour < chunk.lootRespawnHour) {
                        buildingDef.setKeySpawned(0);
                        buildingDef.lootRespawnHour = chunk.lootRespawnHour;
                    }
                }
                for (int z = chunk.getMinLevel(); z <= chunk.getMaxLevel(); ++z) {
                    sq = chunk.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    int numObjects = sq.getObjects().size();
                    IsoObject[] objectArray = sq.getObjects().getElements();
                    for (int i = 0; i < numObjects; ++i) {
                        IsoObject obj = objectArray[i];
                        if (obj instanceof IsoDeadBody || obj instanceof IsoThumpable || obj instanceof IsoCompost) continue;
                        for (int containerIndex = 0; containerIndex < obj.getContainerCount(); ++containerIndex) {
                            ItemContainer container = obj.getContainerByIndex(containerIndex);
                            if (!container.explored || !container.isHasBeenLooted()) continue;
                            LootRespawn.respawnInContainer(obj, container);
                        }
                    }
                }
            }
        }
    }

    private static void respawnInContainer(IsoObject obj, ItemContainer container) {
        int maxItem;
        if (container == null || container.getItems() == null) {
            return;
        }
        int count = container.getItems().size();
        if (count >= (maxItem = SandboxOptions.instance.maxItemsForLootRespawn.getValue())) {
            return;
        }
        existingItems.clear();
        existingItems.addAll(container.getItems());
        ItemPickerJava.fillContainer(container, null);
        ArrayList<InventoryItem> items = container.getItems();
        if (items == null || count == items.size()) {
            return;
        }
        container.setHasBeenLooted(false);
        newItems.clear();
        for (int j = 0; j < items.size(); ++j) {
            InventoryItem item = items.get(j);
            if (existingItems.contains(item)) continue;
            newItems.add(item);
            item.setAge(0.0f);
        }
        ItemPickerJava.updateOverlaySprite(obj);
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, obj.square.x, (float)obj.square.y, container, newItems);
        }
    }
}

