/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.vehicle;

import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.BaseVehicle;

public abstract class VehicleField
implements INetworkPacketField {
    protected final VehicleID vehicleID;

    protected VehicleField(VehicleID vehicleID) {
        this.vehicleID = vehicleID;
    }

    protected BaseVehicle getVehicle() {
        return this.vehicleID.getVehicle();
    }
}

