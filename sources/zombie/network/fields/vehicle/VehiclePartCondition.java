/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.vehicle;

import java.io.IOException;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.vehicle.VehicleField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.VehiclePart;

public class VehiclePartCondition
extends VehicleField
implements INetworkPacketField {
    public VehiclePartCondition(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            int numParts = bb.getByte();
            short totalPartBytes = bb.getShort();
            int positionOfPartsStart = bb.position();
            int expectedPositionOfPartsEnd = positionOfPartsStart + totalPartBytes;
            for (int i = 0; i < numParts; ++i) {
                int partIndex = bb.getByte() & 0xFF;
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                if (part != null) {
                    part.setFlag((short)2048);
                    part.setCondition(bb.getInt());
                    continue;
                }
                bb.position(bb.position() + 5);
            }
            int positionOfPartsEnd = bb.position();
            if (positionOfPartsEnd != expectedPositionOfPartsEnd) {
                throw new IOException(String.format("Unexpected number of bytes read: %d. Expected %d", positionOfPartsEnd - positionOfPartsStart, expectedPositionOfPartsEnd - positionOfPartsStart));
            }
            int endMarker = bb.getByte() & 0xFF;
            if (endMarker != 255) {
                throw new IOException(String.format("Unexpected endMarker byte: %d, Expected: %d", endMarker, 255));
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
            int positionOfNumParts = b.position();
            b.putByte(0);
            int positionOfTotalPartBytes = b.position();
            b.putShort(0);
            int numParts = 0;
            int positionOfPartsStart = b.position();
            for (int j = 0; j < this.getVehicle().getPartCount(); ++j) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (!part.getFlag((short)2048)) continue;
                b.putByte(j);
                b.putInt(part.getCondition());
                ++numParts;
            }
            int positionOfPartsEnd = b.position();
            int totalPartBytes = positionOfPartsEnd - positionOfPartsStart;
            b.putByte(255);
            int positionOfEnd = b.position();
            b.position(positionOfNumParts);
            b.putByte(numParts);
            b.position(positionOfTotalPartBytes);
            b.putShort(totalPartBytes);
            b.position(positionOfEnd);
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}

