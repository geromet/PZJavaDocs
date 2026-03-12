/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.debug.DebugLog;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;

public class ItemTags {
    private static final ArrayList<Item> emptyList = new ArrayList();
    private static final Map<ItemTag, ArrayList<Item>> tagItemMap = new HashMap<ItemTag, ArrayList<Item>>();

    public static void Init(ArrayList<Item> allItems) {
        tagItemMap.clear();
        for (Item item : allItems) {
            for (ItemTag itemTag : item.getTags()) {
                ItemTags.registerItemTag(itemTag, item);
            }
        }
    }

    private static void registerItemTag(ItemTag itemTag, Item item) {
        if (!tagItemMap.containsKey(itemTag)) {
            tagItemMap.put(itemTag, new ArrayList());
        }
        if (!tagItemMap.get(itemTag).contains(item)) {
            tagItemMap.get(itemTag).add(item);
        }
    }

    public static ArrayList<Item> getItemsForTag(ItemTag itemTag) {
        if (tagItemMap.containsKey(itemTag)) {
            return tagItemMap.get(itemTag);
        }
        return emptyList;
    }

    private static void printDebug() {
        DebugLog.log("==== ITEM TAGS ====");
        for (Map.Entry<ItemTag, ArrayList<Item>> entry : tagItemMap.entrySet()) {
            DebugLog.log("[tag: " + String.valueOf(entry.getKey()) + "]");
            for (Item item : entry.getValue()) {
                DebugLog.log("  - " + item.getFullName());
            }
        }
        DebugLog.log("===/ ITEM TAGS /===");
    }
}

