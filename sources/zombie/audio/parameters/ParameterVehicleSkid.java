/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.network.GameClient;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleSkid
extends FMODLocalParameter {
    private final BaseVehicle vehicle;
    private final BaseVehicle.WheelInfo[] wheelInfo;

    public ParameterVehicleSkid(BaseVehicle vehicle) {
        super("VehicleSkid");
        this.vehicle = vehicle;
        this.wheelInfo = vehicle.wheelInfo;
    }

    @Override
    public float calculateCurrentValue() {
        float value = 1.0f;
        if (GameClient.client && !this.vehicle.isLocalPhysicSim()) {
            return value;
        }
        VehicleScript script = this.vehicle.getScript();
        if (script == null) {
            return value;
        }
        int count = script.getWheelCount();
        for (int i = 0; i < count; ++i) {
            value = PZMath.min(value, this.wheelInfo[i].skidInfo);
        }
        return (float)((int)(100.0f - PZMath.clamp(value, 0.0f, 1.0f) * 100.0f)) / 100.0f;
    }
}

