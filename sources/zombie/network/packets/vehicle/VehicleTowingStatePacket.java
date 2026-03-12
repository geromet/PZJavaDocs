/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.vehicle;

import gnu.trove.iterator.TShortShortIterator;
import gnu.trove.map.hash.TShortShortHashMap;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.VehicleManager;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class VehicleTowingStatePacket
implements INetworkPacket {
    @JSONField
    protected final TShortShortHashMap towedVehicleMap = new TShortShortHashMap();

    @Override
    public void setData(Object ... values2) {
        this.towedVehicleMap.putAll((TShortShortHashMap)values2[0]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        int size = b.getInt();
        for (int i = 0; i < size; ++i) {
            VehicleManager.instance.towedVehicleMap.put(b.getShort(), b.getShort());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.towedVehicleMap.size());
        TShortShortIterator iterator2 = this.towedVehicleMap.iterator();
        while (iterator2.hasNext()) {
            iterator2.advance();
            b.putShort(iterator2.key());
            b.putShort(iterator2.value());
        }
    }

    @Override
    public void parseClient(ByteBufferReader b, UdpConnection connection) {
        VehicleManager.instance.towedVehicleMap.putAll(this.towedVehicleMap);
    }
}

