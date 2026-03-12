/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects.interfaces;

import se.krka.kahlua.vm.KahluaTable;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;

public interface IClothingWasherDryerLogic {
    public void update();

    public void saveChange(IsoObjectChange var1, KahluaTable var2, ByteBufferWriter var3);

    public void loadChange(IsoObjectChange var1, ByteBufferReader var2);

    public ItemContainer getContainer();

    public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2);

    public boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2);

    public boolean isActivated();

    public void setActivated(boolean var1);

    public void switchModeOn();

    public void switchModeOff();
}

