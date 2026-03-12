/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.pathfind.IVehicleTask;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Vehicle;
import zombie.pathfind.VehiclePoly;
import zombie.vehicles.BaseVehicle;

final class VehicleUpdateTask
implements IVehicleTask {
    PolygonalMap2 map;
    BaseVehicle vehicle;
    final VehiclePoly poly = new VehiclePoly();
    final VehiclePoly polyPlusRadius = new VehiclePoly();
    float upVectorDot;
    static final ArrayDeque<VehicleUpdateTask> pool = new ArrayDeque();

    VehicleUpdateTask() {
    }

    @Override
    public void init(PolygonalMap2 map, BaseVehicle vehicle) {
        this.map = map;
        this.vehicle = vehicle;
        this.poly.init(vehicle.getPoly());
        this.poly.z += 32.0f;
        this.polyPlusRadius.init(vehicle.getPolyPlusRadius());
        this.polyPlusRadius.z += 32.0f;
        this.upVectorDot = vehicle.getUpVectorDot();
    }

    @Override
    public void execute() {
        Vehicle vehicle = this.map.vehicleMap.get(this.vehicle);
        vehicle.poly.init(this.poly);
        vehicle.polyPlusRadius.init(this.polyPlusRadius);
        vehicle.upVectorDot = this.upVectorDot;
        this.vehicle = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static VehicleUpdateTask alloc() {
        ArrayDeque<VehicleUpdateTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            return pool.isEmpty() ? new VehicleUpdateTask() : pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void release() {
        ArrayDeque<VehicleUpdateTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }
}

