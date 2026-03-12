/*
 * Decompiled with CFR 0.152.
 */
package zombie.world;

import zombie.scripting.objects.Item;
import zombie.world.DictionaryInfo;

public class ItemInfo
extends DictionaryInfo<ItemInfo> {
    protected Item scriptItem;

    @Override
    public String getInfoType() {
        return "item";
    }

    public Item getScriptItem() {
        return this.scriptItem;
    }

    @Override
    public ItemInfo copy() {
        ItemInfo c = new ItemInfo();
        c.copyFrom(this);
        c.scriptItem = this.scriptItem;
        return c;
    }
}

