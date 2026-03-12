/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.vehicles.VehiclePart;

public final class ItemUser {
    private static final ArrayList<InventoryItem> tempItems = new ArrayList();

    public static void UseItem(InventoryItem item) {
        ItemUser.UseItem(item, false, false, 1, false, false);
    }

    public static int UseItem(InventoryItem item, boolean bCrafting, boolean bInContainer, int consumes, boolean keep, boolean destroy) {
        DrainableComboItem drainableComboItem;
        if (!(item.isDisappearOnUse() || bCrafting || destroy)) {
            return 0;
        }
        int consumed = Math.min(item.getCurrentUses(), consumes);
        if (!keep) {
            item.setCurrentUses(item.getCurrentUses() - consumed);
        }
        if (!(item.replaceOnUse == null || bInContainer || bCrafting || destroy)) {
            Object fullType = item.replaceOnUse;
            if (!((String)fullType).contains(".")) {
                fullType = item.module + "." + (String)fullType;
            }
            ItemUser.CreateItem((String)fullType, tempItems);
            for (int i = 0; i < tempItems.size(); ++i) {
                InventoryItem newItem = tempItems.get(i);
                ItemUser.AddItem(item, newItem);
                newItem.copyConditionStatesFrom(item);
            }
        }
        if (item instanceof DrainableComboItem && !StringUtils.isNullOrEmpty((drainableComboItem = (DrainableComboItem)item).getReplaceOnDeplete()) && item.getCurrentUses() <= 0) {
            Object fullType = drainableComboItem.getReplaceOnDeplete();
            if (!((String)fullType).contains(".")) {
                fullType = item.module + "." + (String)fullType;
            }
            ItemUser.CreateItem((String)fullType, tempItems);
            for (int i = 0; i < tempItems.size(); ++i) {
                InventoryItem newItem = tempItems.get(i);
                ItemUser.AddItem(item, newItem);
                newItem.copyConditionStatesFrom(item);
            }
        }
        if (destroy) {
            ItemUser.RemoveItem(item);
        } else if (item.getCurrentUses() <= 0) {
            if (!item.isKeepOnDeplete()) {
                ItemUser.RemoveItem(item);
            }
        } else if (GameServer.server) {
            GameServer.sendItemStats(item);
        }
        return consumed;
    }

    public static void CreateItem(String fullType, ArrayList<InventoryItem> result) {
        result.clear();
        Item scriptItem = ScriptManager.instance.FindItem(fullType);
        if (scriptItem == null) {
            DebugLog.General.warn("ERROR: ItemUses.CreateItem: can't find " + fullType);
            return;
        }
        int count = scriptItem.getCount();
        for (int i = 0; i < count; ++i) {
            Object item = InventoryItemFactory.CreateItem(fullType);
            if (item == null) {
                return;
            }
            result.add((InventoryItem)item);
        }
    }

    public static void AddItem(InventoryItem existingItem, InventoryItem newItem) {
        IsoWorldInventoryObject worldObj = existingItem.getWorldItem();
        if (worldObj != null && worldObj.getWorldObjectIndex() == -1) {
            worldObj = null;
        }
        if (worldObj != null) {
            worldObj.getSquare().AddWorldInventoryItem(newItem, 0.0f, 0.0f, 0.0f, true);
            return;
        }
        if (existingItem.container != null) {
            VehiclePart vehiclePart = existingItem.container.vehiclePart;
            if (GameServer.server) {
                GameServer.sendAddItemToContainer(existingItem.container, newItem);
            }
            existingItem.container.AddItem(newItem);
            if (vehiclePart != null) {
                vehiclePart.setContainerContentAmount(vehiclePart.getItemContainer().getCapacityWeight());
            }
        }
    }

    public static void RemoveItem(InventoryItem item) {
        IsoWorldInventoryObject worldObj = item.getWorldItem();
        if (worldObj != null && worldObj.getWorldObjectIndex() == -1) {
            worldObj = null;
        }
        if (worldObj != null) {
            worldObj.getSquare().transmitRemoveItemFromSquare(worldObj);
            if (item.container != null) {
                item.container.items.remove(item);
                item.container.setDirty(true);
                item.container.setDrawDirty(true);
                item.container = null;
            }
            return;
        }
        if (item.container != null) {
            IsoObject parent = item.container.parent;
            VehiclePart vehiclePart = item.container.vehiclePart;
            if (parent instanceof IsoGameCharacter) {
                IsoGameCharacter chr = (IsoGameCharacter)parent;
                if (item instanceof Clothing) {
                    Clothing clothing = (Clothing)item;
                    if (item.isWorn()) {
                        clothing.Unwear();
                    }
                }
                chr.removeFromHands(item);
                if (chr.getClothingItem_Back() == item) {
                    chr.setClothingItem_Back(null);
                }
            }
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(item.container, item);
            }
            if (GameClient.client) {
                GameClient.sendRemoveItemFromContainer(item.container, item);
            }
            if (item.container != null) {
                item.container.items.remove(item);
                item.container.setDirty(true);
                item.container.setDrawDirty(true);
                item.container = null;
            }
            if (parent instanceof IsoDeadBody) {
                IsoDeadBody isoDeadBody = (IsoDeadBody)parent;
                isoDeadBody.checkClothing(item);
            }
            if (parent instanceof IsoMannequin) {
                IsoMannequin isoMannequin = (IsoMannequin)parent;
                isoMannequin.checkClothing(item);
            }
            if (vehiclePart != null) {
                vehiclePart.setContainerContentAmount(vehiclePart.getItemContainer().getCapacityWeight());
            }
        }
    }
}

