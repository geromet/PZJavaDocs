/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.vehicle;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.vehicle.VehicleField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.BaseVehicle;

public class VehicleAuthorization
extends VehicleField
implements INetworkPacketField {
    @JSONField
    protected BaseVehicle.Authorization authorization = BaseVehicle.Authorization.Server;
    @JSONField
    protected short authorizationPlayer = (short)-1;

    public VehicleAuthorization(VehicleID vehicleID) {
        super(vehicleID);
    }

    public void set(BaseVehicle vehicle) {
        this.authorization = vehicle.netPlayerAuthorization;
        this.authorizationPlayer = vehicle.netPlayerId;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        try {
            this.authorization = BaseVehicle.Authorization.values()[b.getInt()];
            this.authorizationPlayer = b.getShort();
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.putInt(this.authorization.ordinal());
            b.putShort(this.authorizationPlayer);
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    public BaseVehicle.Authorization getAuthorization() {
        return this.authorization;
    }

    public short getAuthorizationPlayer() {
        return this.authorizationPlayer;
    }
}

