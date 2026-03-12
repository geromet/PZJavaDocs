/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import zombie.worldMap.styles.WorldMapStyleLayer;

public abstract class IWorldMapStyleListener {
    abstract void onAdd(WorldMapStyleLayer var1);

    abstract void onBeforeRemove(WorldMapStyleLayer var1);

    abstract void onAfterRemove(WorldMapStyleLayer var1);

    abstract void onMoveLayer(int var1, int var2);

    abstract void onBeforeClear();

    abstract void onAfterClear();
}

