/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.SurvivorDesc;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.WornItems;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.ItemBodyLocation;

public interface ILuaGameCharacterClothing {
    public void dressInNamedOutfit(String var1);

    public void dressInPersistentOutfit(String var1);

    public void dressInPersistentOutfitID(int var1);

    public String getOutfitName();

    public WornItems getWornItems();

    public void setWornItems(WornItems var1);

    public InventoryItem getWornItem(ItemBodyLocation var1);

    public void setWornItem(ItemBodyLocation var1, InventoryItem var2);

    public void removeWornItem(InventoryItem var1);

    public void removeWornItem(InventoryItem var1, boolean var2);

    public void clearWornItems();

    public BodyLocationGroup getBodyLocationGroup();

    public void setClothingItem_Head(InventoryItem var1);

    public void setClothingItem_Torso(InventoryItem var1);

    public void setClothingItem_Back(InventoryItem var1);

    public void setClothingItem_Hands(InventoryItem var1);

    public void setClothingItem_Legs(InventoryItem var1);

    public void setClothingItem_Feet(InventoryItem var1);

    public void Dressup(SurvivorDesc var1);
}

