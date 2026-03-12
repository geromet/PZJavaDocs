/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.vehicles.BaseVehicle;

public class RagdollStateData {
    public float simulationTimeout;
    public boolean isSimulating;
    public boolean isSimulationMovement;
    public boolean isCalculated;
    public boolean isContactingVehicle;
    public float simulationRenderedAngle;
    public float simulationCharacterForwardAngle;
    public final Vector2 simulationDirection = new Vector2();
    public final Vector3 pelvisDirection = new Vector3();
    public BaseVehicle lastCollidedVehicle;

    public RagdollStateData() {
        this.reset();
    }

    public void reset() {
        this.simulationTimeout = 1.5f;
        this.isSimulating = false;
        this.isSimulationMovement = false;
        this.isCalculated = false;
        this.isContactingVehicle = false;
        this.simulationRenderedAngle = 0.0f;
        this.simulationCharacterForwardAngle = 0.0f;
        this.simulationDirection.set(0.0f, 0.0f);
        this.pelvisDirection.set(0.0f, 0.0f, 0.0f);
        this.lastCollidedVehicle = null;
    }
}

