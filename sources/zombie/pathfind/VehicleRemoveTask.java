/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.pathfind.IVehicleTask;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Vehicle;
import zombie.vehicles.BaseVehicle;

final class VehicleRemoveTask
implements IVehicleTask {
    PolygonalMap2 map;
    BaseVehicle vehicle;
    static final ArrayDeque<VehicleRemoveTask> pool = new ArrayDeque();

    VehicleRemoveTask() {
    }

    @Override
    public void init(PolygonalMap2 map, BaseVehicle vehicle) {
        this.map = map;
        this.vehicle = vehicle;
    }

    @Override
    public void execute() {
        Vehicle vehicle = this.map.vehicleMap.remove(this.vehicle);
        if (vehicle != null) {
            this.map.vehicles.remove(vehicle);
            vehicle.release();
        }
        this.vehicle = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static VehicleRemoveTask alloc() {
        ArrayDeque<VehicleRemoveTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            return pool.isEmpty() ? new VehicleRemoveTask() : pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void release() {
        ArrayDeque<VehicleRemoveTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }
}

