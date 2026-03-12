/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleRPM
extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleRPM(BaseVehicle vehicle) {
        super("VehicleRPM");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        float rpm1 = PZMath.clamp((float)this.vehicle.getEngineSpeed(), 0.0f, 7000.0f);
        float rpmIdle = this.vehicle.getScript().getEngineIdleSpeed();
        float rpmIdleMax = rpmIdle * 1.1f;
        float fmodIdleMax = 800.0f;
        float fmodRpmMax = 7000.0f;
        float rpm2 = rpm1 < rpmIdleMax ? rpm1 / rpmIdleMax * 800.0f : 800.0f + (rpm1 - rpmIdleMax) / (7000.0f - rpmIdleMax) * 6200.0f;
        return (float)((int)((rpm2 + 50.0f - 1.0f) / 50.0f)) * 50.0f;
    }
}

