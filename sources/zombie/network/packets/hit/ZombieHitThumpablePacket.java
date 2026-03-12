/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.hit;

import zombie.characters.Capability;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.fields.hit.Thumpable;
import zombie.network.packets.hit.ZombieHit;

@PacketSetting(ordering=0, priority=0, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3, anticheats={AntiCheat.HitShortDistance})
public class ZombieHitThumpablePacket
extends ZombieHit
implements AntiCheatHitShortDistance.IAntiCheat {
    @JSONField
    protected final Thumpable thumpable = new Thumpable();

    public void set(IsoZombie wielder, IsoObject thumpable) {
        this.set(wielder);
        this.thumpable.set(thumpable);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.thumpable.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.thumpable.write(b);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.thumpable.isRelevant(connection);
    }

    @Override
    public void preProcess() {
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (GameClient.client) {
            return super.isConsistent(connection) && this.thumpable.isConsistent(connection) && this.wielder.isConsistent(connection);
        }
        return super.isConsistent(connection) && this.thumpable.isConsistent(connection) && this.wielder.getZombie().getOwner() == connection;
    }

    @Override
    public void process() {
        this.thumpable.process(this.wielder.getZombie());
        if (GameServer.server) {
            this.sendToClients(PacketTypes.PacketType.ZombieHitThumpable, null);
        }
    }

    @Override
    public void postProcess() {
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.thumpable.getX(), this.thumpable.getY(), this.wielder.getX(), this.wielder.getY());
    }
}

