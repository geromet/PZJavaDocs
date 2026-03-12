/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.inventory.InventoryItem;

public interface ILuaGameCharacterAttachedItems {
    public AttachedItems getAttachedItems();

    public void setAttachedItems(AttachedItems var1);

    public InventoryItem getAttachedItem(String var1);

    public void setAttachedItem(String var1, InventoryItem var2);

    public void removeAttachedItem(InventoryItem var1);

    public void clearAttachedItems();

    public AttachedLocationGroup getAttachedLocationGroup();
}

