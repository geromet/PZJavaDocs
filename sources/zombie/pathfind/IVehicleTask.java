/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import zombie.pathfind.PolygonalMap2;
import zombie.vehicles.BaseVehicle;

interface IVehicleTask {
    public void init(PolygonalMap2 var1, BaseVehicle var2);

    public void execute();

    public void release();
}

