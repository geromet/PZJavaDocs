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
import zombie.vehicles.VehiclePart;

public class VehiclePartWindow
extends VehicleField
implements INetworkPacketField {
    public VehiclePartWindow(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            int partIndex = bb.getByte() & 0xFF;
            while (partIndex != -1) {
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                part.getWindow().load(bb.bb, 244);
                partIndex = bb.getByte();
            }
            this.getVehicle().doDamageOverlay();
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            for (int j = 0; j < this.getVehicle().getPartCount(); ++j) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (!part.getFlag((short)256)) continue;
                b.putByte(j);
                part.getWindow().save(b.bb);
            }
            b.putByte(255);
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}

