/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.vehicle;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class VehicleTowingDetachPacket
implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleA = new VehicleID();
    @JSONField
    protected final VehicleID vehicleB = new VehicleID();

    public void set(BaseVehicle vehicleA, BaseVehicle vehicleB) {
        if (vehicleA == null) {
            this.vehicleA.setID((short)-1);
        } else {
            this.vehicleA.set(vehicleA);
        }
        if (vehicleB == null) {
            this.vehicleB.setID((short)-1);
        } else {
            this.vehicleB.set(vehicleB);
        }
    }

    @Override
    public void setData(Object ... values2) {
        this.set((BaseVehicle)values2[0], (BaseVehicle)values2[1]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.vehicleA.parse(b, connection);
        this.vehicleB.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleA.write(b);
        this.vehicleB.write(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (VehicleManager.instance.towedVehicleMap.containsKey(this.vehicleA.getID())) {
            VehicleManager.instance.towedVehicleMap.remove(this.vehicleA.getID());
        }
        if (this.vehicleA.getVehicle() != null) {
            this.vehicleA.getVehicle().breakConstraint(true, true);
        }
        if (VehicleManager.instance.towedVehicleMap.containsKey(this.vehicleB.getID())) {
            VehicleManager.instance.towedVehicleMap.remove(this.vehicleB.getID());
        }
        if (this.vehicleB.getVehicle() != null) {
            this.vehicleB.getVehicle().breakConstraint(true, true);
        }
    }
}

