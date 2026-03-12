/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.streets;

import zombie.worldMap.streets.WorldMapStreet;

public interface IWorldMapStreetListener {
    public void onAdd(WorldMapStreet var1);

    public void onBeforeRemove(WorldMapStreet var1);

    public void onAfterRemove(WorldMapStreet var1);

    public void onBeforeModifyStreet(WorldMapStreet var1);

    public void onAfterModifyStreet(WorldMapStreet var1);
}

