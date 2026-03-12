/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.connection;

import java.util.ArrayList;
import java.util.LinkedList;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.None, handlingType=6)
public class MetaDataPacket
implements INetworkPacket {
    @JSONField
    String username = "";

    @Override
    public void setData(Object ... values2) {
        this.username = (String)values2[0];
    }

    @Override
    public void write(ByteBufferWriter b) {
        int n;
        LinkedList<SafeHouse> saveHousesForSend = new LinkedList<SafeHouse>();
        for (int n2 = 0; n2 < SafeHouse.getSafehouseList().size(); ++n2) {
            SafeHouse sh = SafeHouse.getSafehouseList().get(n2);
            if (GameServer.getPlayerByUserName(this.username) == null) continue;
            saveHousesForSend.add(sh);
        }
        b.putInt(saveHousesForSend.size());
        for (SafeHouse sh : saveHousesForSend) {
            sh.save(b.bb);
        }
        b.putInt(NonPvpZone.getAllZones().size());
        for (n = 0; n < NonPvpZone.getAllZones().size(); ++n) {
            NonPvpZone.getAllZones().get(n).save(b.bb);
        }
        b.putInt(Faction.getFactions().size());
        for (n = 0; n < Faction.getFactions().size(); ++n) {
            Faction.getFactions().get(n).save(b.bb);
        }
        b.putInt(DesignationZone.allZones.size());
        for (n = 0; n < DesignationZone.allZones.size(); ++n) {
            DesignationZone.allZones.get(n).save(b.bb);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        SafeHouse.clearSafehouseList();
        int nSafehouse = b.getInt();
        for (int n = 0; n < nSafehouse; ++n) {
            SafeHouse.load(b.bb, 244);
        }
        NonPvpZone.nonPvpZoneList.clear();
        int nZone = b.getInt();
        for (int n = 0; n < nZone; ++n) {
            NonPvpZone zone = new NonPvpZone();
            zone.load(b.bb, 244);
            NonPvpZone.getAllZones().add(zone);
        }
        Faction.factions = new ArrayList();
        int nFaction = b.getInt();
        for (int n = 0; n < nFaction; ++n) {
            Faction faction = new Faction();
            faction.load(b.bb, 244);
            Faction.getFactions().add(faction);
        }
        int nDZone = b.getInt();
        for (int n = 0; n < nDZone; ++n) {
            DesignationZone.load(b.bb, 244);
        }
    }
}

