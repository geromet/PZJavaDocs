/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleSteer
extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleSteer(BaseVehicle vehicle) {
        super("VehicleSteer");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        float value = 0.0f;
        if (!this.vehicle.isEngineRunning()) {
            return value;
        }
        VehicleScript script = this.vehicle.getScript();
        if (script == null) {
            return value;
        }
        BaseVehicle.WheelInfo[] wheelInfo = this.vehicle.wheelInfo;
        int count = script.getWheelCount();
        for (int i = 0; i < count; ++i) {
            value = PZMath.max(value, Math.abs(wheelInfo[i].steering));
        }
        return (float)((int)(PZMath.clamp(value, 0.0f, 1.0f) * 100.0f)) / 100.0f;
    }
}

