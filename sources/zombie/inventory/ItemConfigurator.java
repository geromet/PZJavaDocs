/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.core.properties.IsoPropertyType;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemPickInfo;
import zombie.iso.BuildingDef;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.scripting.ScriptManager;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;

public class ItemConfigurator {
    private static final String[] vehicle_containers = new String[]{"TruckBed", "TruckBedOpen", "SeatFrontLeft", "SeatFrontRight", "SeatMiddleLeft", "SeatMiddleRight", "SeatRearLeft", "SeatRearRight", "GloveBox"};
    private static final boolean verbose = false;
    private static final boolean verbose_tiles = false;
    private static int nextId;
    private static final HashMap<String, IntegerStore> STRING_INTEGER_HASH_MAP;
    private static final HashMap<String, IntegerStore> TILE_INTEGER_HASH_MAP;

    private static boolean registerString(String s) {
        if (s != null && !STRING_INTEGER_HASH_MAP.containsKey(s)) {
            STRING_INTEGER_HASH_MAP.put(s, new IntegerStore(nextId++));
            return true;
        }
        return false;
    }

    public static boolean registerZone(String s) {
        return ItemConfigurator.registerString(s);
    }

    public static int GetIdForString(String s) {
        IntegerStore store = STRING_INTEGER_HASH_MAP.get(s);
        if (store != null) {
            return store.get();
        }
        return -1;
    }

    public static int GetIdForSprite(String s) {
        IntegerStore store = TILE_INTEGER_HASH_MAP.get(s);
        if (store != null) {
            return store.get();
        }
        return -1;
    }

    /*
     * WARNING - void declaration
     */
    public static void Preprocess() {
        void var5_14;
        STRING_INTEGER_HASH_MAP.clear();
        TILE_INTEGER_HASH_MAP.clear();
        ArrayList<Zone> zones = IsoWorld.instance.metaGrid.zones;
        for (Zone zone : zones) {
            if (StringUtils.isNullOrWhitespace(zone.name) || ItemConfigurator.registerString(zone.name)) {
                // empty if block
            }
            if (!StringUtils.isNullOrWhitespace(zone.type) && !ItemConfigurator.registerString(zone.type)) continue;
        }
        ArrayList<BuildingDef> buildings = IsoWorld.instance.metaGrid.buildings;
        for (BuildingDef buildingDef : buildings) {
            for (RoomDef roomDef : buildingDef.rooms) {
                if (!ItemConfigurator.registerString(roomDef.getName())) continue;
            }
        }
        ArrayList<Item> arrayList = ScriptManager.instance.getAllItems();
        for (Item item : arrayList) {
            if (item.isItemType(ItemType.CONTAINER) && !ItemConfigurator.registerString(item.getName())) continue;
        }
        String[] stringArray = vehicle_containers;
        int item = stringArray.length;
        boolean bl = false;
        while (var5_14 < item) {
            String vehicleContainer = stringArray[var5_14];
            if (ItemConfigurator.registerString(vehicleContainer)) {
                // empty if block
            }
            ++var5_14;
        }
        ArrayList<VehicleScript> arrayList2 = ScriptManager.instance.getAllVehicleScripts();
        for (VehicleScript vehicleScript : arrayList2) {
            if (vehicleScript.getName() != null && !ItemConfigurator.registerString(vehicleScript.getName())) continue;
        }
        ItemConfigurator.registerString("freezer");
        for (Map.Entry entry : IsoSpriteManager.instance.namedMap.entrySet()) {
            if (((IsoSprite)entry.getValue()).getProperties() != null && ((IsoSprite)entry.getValue()).getProperties().has(IsoFlagType.container) && !ItemConfigurator.registerString(((IsoSprite)entry.getValue()).getProperties().get(IsoPropertyType.CONTAINER))) continue;
        }
        for (IsoSprite isoSprite : IsoSpriteManager.instance.intMap.valueCollection()) {
            if (isoSprite == null || isoSprite.getID() < 0 || isoSprite.getName() == null) continue;
            TILE_INTEGER_HASH_MAP.put(isoSprite.getName(), new IntegerStore(isoSprite.getID()));
        }
        ArrayList<ItemConfig> itemConfigs = ScriptManager.instance.getAllItemConfigs();
        for (ItemConfig itemConfig : itemConfigs) {
            itemConfig.BuildBuckets();
        }
    }

    public static void ConfigureItem(InventoryItem item, ItemPickInfo pickInfo, boolean isJunk, float zombieDensity) {
        if (item == null || pickInfo == null || item.getScriptItem() == null || item.getScriptItem().getItemConfig() == null) {
            return;
        }
        pickInfo.setJunk(isJunk);
        Item itemScript = item.getScriptItem();
        ItemConfig itemConfig = itemScript.getItemConfig();
        if (itemConfig == null) {
            return;
        }
        try {
            itemConfig.ConfigureEntitySpawned(item, pickInfo);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ConfigureItemOnCreate(InventoryItem item) {
        if (item == null || item.getScriptItem() == null || item.getScriptItem().getItemConfig() == null) {
            return;
        }
        Item itemScript = item.getScriptItem();
        ItemConfig itemConfig = itemScript.getItemConfig();
        if (itemConfig == null) {
            return;
        }
        try {
            itemConfig.ConfigureEntityOnCreate(item);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static {
        STRING_INTEGER_HASH_MAP = new HashMap();
        TILE_INTEGER_HASH_MAP = new HashMap();
    }

    public static class IntegerStore {
        private final int id;

        public IntegerStore(int id) {
            this.id = id;
        }

        public int get() {
            return this.id;
        }
    }
}

