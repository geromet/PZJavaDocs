/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayDeque;
import zombie.pathfind.IVehicleTask;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Vehicle;
import zombie.pathfind.VehiclePoly;
import zombie.vehicles.BaseVehicle;

final class VehicleAddTask
implements IVehicleTask {
    PolygonalMap2 map;
    BaseVehicle vehicle;
    final VehiclePoly poly = new VehiclePoly();
    final VehiclePoly polyPlusRadius = new VehiclePoly();
    final TFloatArrayList crawlOffsets = new TFloatArrayList();
    float upVectorDot;
    static final ArrayDeque<VehicleAddTask> pool = new ArrayDeque();

    VehicleAddTask() {
    }

    @Override
    public void init(PolygonalMap2 map, BaseVehicle vehicle) {
        this.map = map;
        this.vehicle = vehicle;
        this.poly.init(vehicle.getPoly());
        this.poly.z += 32.0f;
        this.polyPlusRadius.init(vehicle.getPolyPlusRadius());
        this.polyPlusRadius.z += 32.0f;
        this.crawlOffsets.resetQuick();
        this.crawlOffsets.addAll(vehicle.getScript().getCrawlOffsets());
        this.upVectorDot = vehicle.getUpVectorDot();
    }

    @Override
    public void execute() {
        Vehicle vehicle = Vehicle.alloc();
        vehicle.poly.init(this.poly);
        vehicle.polyPlusRadius.init(this.polyPlusRadius);
        vehicle.crawlOffsets.resetQuick();
        vehicle.crawlOffsets.addAll(this.crawlOffsets);
        vehicle.upVectorDot = this.upVectorDot;
        this.map.vehicles.add(vehicle);
        this.map.vehicleMap.put(this.vehicle, vehicle);
        this.vehicle = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static VehicleAddTask alloc() {
        ArrayDeque<VehicleAddTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            return pool.isEmpty() ? new VehicleAddTask() : pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void release() {
        ArrayDeque<VehicleAddTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }
}

