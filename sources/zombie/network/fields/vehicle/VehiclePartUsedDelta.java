/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.vehicle;

import java.io.IOException;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.vehicle.VehicleField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class VehiclePartUsedDelta
extends VehicleField
implements INetworkPacketField {
    public VehiclePartUsedDelta(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            int numParts = bb.getByte() & 0xFF;
            int totalPartBytes = bb.getShort() & 0xFFFF;
            int posOfPartsStart = bb.position();
            int expectedPosOfPartsEnd = posOfPartsStart + totalPartBytes;
            for (int i = 0; i < numParts; ++i) {
                int partIndex = bb.getByte() & 0xFF;
                float usedDelta = bb.getFloat();
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                if (part == null) {
                    DebugLog.Multiplayer.warn("Part %d not found!", partIndex);
                    continue;
                }
                Object item = part.getInventoryItem();
                if (!(item instanceof DrainableComboItem)) continue;
                ((InventoryItem)item).setCurrentUses((int)((float)((InventoryItem)item).getMaxUses() * usedDelta));
            }
            int posOfPartsEnd = bb.position();
            if (posOfPartsEnd != expectedPosOfPartsEnd) {
                throw new IOException(String.format("Data-length mismatch. Expected to read bytes: %d. Instead, read bytes: %d", expectedPosOfPartsEnd - posOfPartsStart, posOfPartsEnd - posOfPartsStart));
            }
            int endMarker = bb.getByte() & 0xFF;
            if (endMarker != 255) {
                throw new IOException(String.format("Unexpected endMarker byte: %d. Expected: %d", endMarker, 255));
            }
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            int posOfNumParts = b.position();
            b.putByte(0);
            int posOfTotalPartBytes = b.position();
            b.putShort(0);
            int numParts = 0;
            int posOfPartsStart = b.position();
            BaseVehicle vehicle = this.getVehicle();
            for (int j = 0; j < vehicle.getPartCount(); ++j) {
                Object item;
                VehiclePart part = vehicle.getPartByIndex(j);
                if (!part.getFlag((short)32) || !((item = part.getInventoryItem()) instanceof DrainableComboItem)) continue;
                b.putByte(j);
                b.putFloat(((InventoryItem)item).getCurrentUsesFloat());
                ++numParts;
            }
            int posOfPartsEnd = b.position();
            int totalPartBytes = posOfPartsEnd - posOfPartsStart;
            b.position(posOfNumParts);
            b.putByte(numParts);
            b.position(posOfTotalPartBytes);
            b.putShort(totalPartBytes);
            b.position(posOfPartsEnd);
            b.putByte(255);
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}

