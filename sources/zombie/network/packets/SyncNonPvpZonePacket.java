/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoWorld;
import zombie.iso.areas.NonPvpZone;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.CanSetupNonPVPZone, handlingType=3)
public class SyncNonPvpZonePacket
implements INetworkPacket {
    public final NonPvpZone zone = new NonPvpZone();
    public boolean doRemove;

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            GameServer.sendNonPvpZone(this.zone, this.doRemove, connection);
            this.processClient(connection);
            DebugLog.Multiplayer.debugln("ReceiveSyncNonPvpZone: %s", this.getDescription());
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.zone.load(b.bb, IsoWorld.getWorldVersion());
        this.doRemove = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.zone.save(b.bb);
        b.putBoolean(this.doRemove);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return !StringUtils.isNullOrEmpty(this.zone.getTitle());
    }

    @Override
    public String getDescription() {
        return String.format("\"%s\" remove=%b size=%d (%d;%d) (%d;%d)", this.zone.getTitle(), this.doRemove, this.zone.getSize(), this.zone.getX(), this.zone.getY(), this.zone.getX2(), this.zone.getY2());
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.doRemove) {
            NonPvpZone.getAllZones().removeIf(z -> z.getTitle().equals(this.zone.getTitle()));
        } else if (NonPvpZone.getZoneByTitle(this.zone.getTitle()) == null) {
            NonPvpZone.getAllZones().add(this.zone);
        }
    }
}

