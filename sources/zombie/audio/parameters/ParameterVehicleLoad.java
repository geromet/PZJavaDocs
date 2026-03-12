/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleLoad
extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleLoad(BaseVehicle vehicle) {
        super("VehicleLoad");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.getController().isGasPedalPressed() ? 1.0f : 0.0f;
    }
}

