/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import org.joml.Vector3f;
import zombie.audio.FMODLocalParameter;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleHitLocation
extends FMODLocalParameter {
    private HitLocation location = HitLocation.Front;

    public ParameterVehicleHitLocation() {
        super("VehicleHitLocation");
    }

    @Override
    public float calculateCurrentValue() {
        return this.location.label;
    }

    public static HitLocation calculateLocation(BaseVehicle vehicle, float x, float y, float z) {
        VehicleScript script = vehicle.getScript();
        if (script == null) {
            return HitLocation.Front;
        }
        Vector3f chrPos = vehicle.getLocalPos(x, y, z, (Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc());
        Vector3f ext = script.getExtents();
        Vector3f com = script.getCenterOfMassOffset();
        float yMin = com.z - ext.z / 2.0f;
        float yMax = com.z + ext.z / 2.0f;
        HitLocation position = chrPos.z >= (yMin *= 0.9f) && chrPos.z <= (yMax *= 0.9f) ? HitLocation.Side : (chrPos.z > 0.0f ? HitLocation.Front : HitLocation.Rear);
        BaseVehicle.TL_vector3f_pool.get().release(chrPos);
        return position;
    }

    public void setLocation(HitLocation location) {
        this.location = location;
    }

    public static enum HitLocation {
        Front(0),
        Rear(1),
        Side(2);

        final int label;

        private HitLocation(int label) {
            this.label = label;
        }

        public int getValue() {
            return this.label;
        }
    }
}

