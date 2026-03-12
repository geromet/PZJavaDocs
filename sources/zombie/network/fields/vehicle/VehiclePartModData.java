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
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class VehiclePartModData
extends VehicleField
implements INetworkPacketField {
    public VehiclePartModData(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            BaseVehicle vehicle = this.getVehicle();
            int numParts = bb.getByte() & 0xFF;
            int totalPartBytes = bb.getShort() & 0xFFFF;
            int positionOfPartsStart = bb.bb.position();
            int expectedPositionOfPartsEnd = positionOfPartsStart + totalPartBytes;
            for (int i = 0; i < numParts; ++i) {
                int partIndex = bb.getByte() & 0xFF;
                int lengthOfModData = bb.getShort() & 0xFFFF;
                if (lengthOfModData == 0) continue;
                int positionModDataStart = bb.bb.position();
                int positionModDataEnd = positionModDataStart + lengthOfModData;
                VehiclePart part = vehicle.getPartByIndex(partIndex);
                if (part == null) {
                    bb.bb.position(positionModDataEnd);
                    DebugLog.Multiplayer.warn("%s: could not resolve VehiclePart index: %d. Skipping forward %d bytes.", this.getClass().getSimpleName(), partIndex, lengthOfModData);
                    continue;
                }
                part.getModData().load(bb.bb, 244);
                if (part.isContainer()) {
                    part.setContainerContentAmount(part.getContainerContentAmount());
                }
                if (bb.bb.position() != positionModDataEnd) {
                    DebugLog.Multiplayer.warn("%s: unexpected buffer position for part %d. The ModData.load hasn't loaded the same number of bytes (%d) that was written (%d).", this.getClass().getSimpleName(), partIndex, bb.bb.position() - positionModDataStart, positionModDataEnd - positionModDataStart);
                }
                bb.bb.position(positionModDataEnd);
            }
            int positionOfPartsEnd = bb.bb.position();
            if (positionOfPartsEnd != expectedPositionOfPartsEnd) {
                throw new IOException("Unexpected buffer position at parts end " + positionOfPartsEnd + ". Expected: " + expectedPositionOfPartsEnd);
            }
            int endByte = bb.getByte() & 0xFF;
            if (endByte != 255) {
                throw new IOException("Unexpected value at ModData end: " + endByte + ". Expected 0xFF");
            }
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, LogSeverity.Error, "%s: failed", this.getClass().getSimpleName());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            int positionOfNumParts = b.bb.position();
            b.putByte(0);
            int positionOfTotalPartBytes = b.bb.position();
            b.putShort(0);
            int positionOfPartsStart = b.bb.position();
            int numPartsWritten = 0;
            for (int j = 0; j < this.getVehicle().getPartCount(); ++j) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (!part.getFlag((short)16)) continue;
                b.putByte(j);
                int positionOfLength = b.position();
                b.putShort(0);
                int positionModDataStart = b.bb.position();
                part.getModData().save(b.bb);
                int positionModDataEnd = b.bb.position();
                int modDataBytes = positionModDataEnd - positionModDataStart;
                b.bb.position(positionOfLength);
                b.putShort((short)modDataBytes);
                b.bb.position(positionModDataEnd);
                ++numPartsWritten;
            }
            int positionOfPartsEnd = b.bb.position();
            int totalPartBytes = positionOfPartsEnd - positionOfPartsStart;
            b.putByte(255);
            int positionOfEnd = b.bb.position();
            b.bb.position(positionOfNumParts);
            b.putByte(numPartsWritten);
            b.bb.position(positionOfTotalPartBytes);
            b.putShort(totalPartBytes);
            b.bb.position(positionOfEnd);
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}

