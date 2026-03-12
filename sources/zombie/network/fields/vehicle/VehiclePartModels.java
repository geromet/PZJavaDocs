/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.vehicle;

import java.util.ArrayList;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.vehicle.VehicleField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class VehiclePartModels
extends VehicleField
implements INetworkPacketField {
    private static final ArrayList<BaseVehicle.ModelInfo> oldModels = new ArrayList();
    private static final ArrayList<BaseVehicle.ModelInfo> curModels = new ArrayList();

    public VehiclePartModels(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            int i;
            oldModels.clear();
            oldModels.addAll(this.getVehicle().models);
            curModels.clear();
            int numModels = bb.getByte();
            for (i = 0; i < numModels; ++i) {
                byte partIndex = bb.getByte();
                byte modelIndex = bb.getByte();
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                VehicleScript.Model model = part.getScriptPart().models.get(modelIndex);
                BaseVehicle.ModelInfo modelInfo = this.getVehicle().setModelVisible(part, model, true);
                curModels.add(modelInfo);
            }
            for (i = 0; i < oldModels.size(); ++i) {
                BaseVehicle.ModelInfo modelInfo = oldModels.get(i);
                if (curModels.contains(modelInfo)) continue;
                this.getVehicle().setModelVisible(modelInfo.part, modelInfo.scriptModel, false);
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
            b.putByte(this.getVehicle().models.size());
            for (int j = 0; j < this.getVehicle().models.size(); ++j) {
                BaseVehicle.ModelInfo modelInfo = this.getVehicle().models.get(j);
                b.putByte(modelInfo.part.getIndex());
                b.putByte(modelInfo.part.getScriptPart().models.indexOf(modelInfo.scriptModel));
            }
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}

