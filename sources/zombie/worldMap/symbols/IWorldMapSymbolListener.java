/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import zombie.worldMap.symbols.WorldMapBaseSymbol;

public interface IWorldMapSymbolListener {
    public void onAdd(WorldMapBaseSymbol var1);

    public void onBeforeRemove(WorldMapBaseSymbol var1);

    public void onAfterRemove(WorldMapBaseSymbol var1);

    public void onBeforeClear();

    public void onAfterClear();
}

