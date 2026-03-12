/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayDeque;
import zombie.pathfind.VehiclePoly;

final class Vehicle {
    final VehiclePoly poly = new VehiclePoly();
    final VehiclePoly polyPlusRadius = new VehiclePoly();
    final TFloatArrayList crawlOffsets = new TFloatArrayList();
    float upVectorDot;
    static final ArrayDeque<Vehicle> pool = new ArrayDeque();

    Vehicle() {
    }

    static Vehicle alloc() {
        return pool.isEmpty() ? new Vehicle() : pool.pop();
    }

    void release() {
        assert (!pool.contains(this));
        pool.push(this);
    }
}

