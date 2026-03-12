/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleBrake
extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleBrake(BaseVehicle vehicle) {
        super("VehicleBrake");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.getController().isBrakePedalPressed() ? 1.0f : 0.0f;
    }
}

