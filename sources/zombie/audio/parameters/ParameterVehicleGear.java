/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleGear
extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleGear(BaseVehicle vehicle) {
        super("VehicleGear");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.getTransmissionNumber() + 1;
    }
}

