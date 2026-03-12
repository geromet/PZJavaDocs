/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.vehicle;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.vehicle.VehicleField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.BaseVehicle;

public class VehicleEngine
extends VehicleField
implements INetworkPacketField {
    public VehicleEngine(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            switch (bb.getEnum(BaseVehicle.engineStateTypes.class)) {
                case Idle: {
                    this.getVehicle().engineDoIdle();
                    break;
                }
                case RetryingStarting: {
                    this.getVehicle().engineDoRetryingStarting();
                    break;
                }
                case StartingSuccess: {
                    this.getVehicle().engineDoStartingSuccess();
                    break;
                }
                case StartingFailed: {
                    this.getVehicle().engineDoStartingFailed();
                    break;
                }
                case StartingFailedNoPower: {
                    this.getVehicle().engineDoStartingFailedNoPower();
                    break;
                }
                case Running: {
                    this.getVehicle().engineDoRunning();
                    break;
                }
                case Stalling: {
                    this.getVehicle().engineDoStalling();
                    break;
                }
                case ShutingDown: {
                    this.getVehicle().engineDoShuttingDown();
                }
            }
            this.getVehicle().setEngineFeature(bb.getInt(), bb.getInt(), bb.getInt());
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.putEnum(this.getVehicle().engineState);
            b.putInt(this.getVehicle().getEngineQuality());
            b.putInt(this.getVehicle().getEngineLoudness());
            b.putInt(this.getVehicle().getEnginePower());
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}

