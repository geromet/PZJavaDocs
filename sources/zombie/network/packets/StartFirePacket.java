/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatFire;
import zombie.network.anticheats.AntiCheatSmoke;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2, anticheats={AntiCheat.Fire, AntiCheat.Smoke})
public class StartFirePacket
implements INetworkPacket,
AntiCheatFire.IAntiCheat,
AntiCheatSmoke.IAntiCheat {
    @JSONField
    protected final Square square = new Square();
    @JSONField
    protected int fireEnergy;
    @JSONField
    protected boolean ignite;
    @JSONField
    protected int life;
    @JSONField
    protected boolean smoke;
    @JSONField
    protected int spreadDelay;
    @JSONField
    protected int numParticles;

    @Override
    public void setData(Object ... values2) {
        this.square.set((IsoGridSquare)values2[0]);
        this.ignite = (Boolean)values2[1];
        this.fireEnergy = (Integer)values2[2];
        this.life = (Integer)values2[3];
        this.smoke = (Boolean)values2[4];
        this.spreadDelay = 0;
        this.numParticles = 0;
    }

    public void set(IsoGridSquare gridSquare, boolean ignite, int fireEnergy, int life, boolean smoke) {
        this.square.set(gridSquare);
        this.fireEnergy = fireEnergy;
        this.ignite = ignite;
        this.life = life;
        this.smoke = smoke;
        this.spreadDelay = 0;
        this.numParticles = 0;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.square.parse(b, connection);
        this.fireEnergy = b.getInt();
        this.ignite = b.getBoolean();
        this.life = b.getInt();
        this.smoke = b.getBoolean();
        if (GameClient.client) {
            this.spreadDelay = b.getInt();
            this.numParticles = b.getInt();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.square.write(b);
        b.putInt(this.fireEnergy);
        b.putBoolean(this.ignite);
        b.putInt(this.life);
        b.putBoolean(this.smoke);
        if (GameServer.server) {
            b.putInt(this.spreadDelay);
            b.putInt(this.numParticles);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoFire newFire = this.smoke ? new IsoFire(IsoWorld.instance.currentCell, this.square.getSquare(), this.ignite, this.fireEnergy, this.life, true) : new IsoFire(IsoWorld.instance.currentCell, this.square.getSquare(), this.ignite, this.fireEnergy, this.life);
        newFire.spreadDelay = this.spreadDelay;
        newFire.numFlameParticles = this.numParticles;
        IsoFireManager.Add(newFire);
        this.square.getSquare().getObjects().add(newFire);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoFire newFire = this.smoke ? new IsoFire(this.square.getSquare().getCell(), this.square.getSquare(), this.ignite, this.fireEnergy, this.life, true) : new IsoFire(this.square.getSquare().getCell(), this.square.getSquare(), this.ignite, this.fireEnergy, this.life);
        IsoFireManager.Add(newFire);
        this.spreadDelay = newFire.getSpreadDelay();
        this.numParticles = newFire.numFlameParticles;
        this.square.getSquare().getObjects().add(newFire);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.square.getSquare() != null && this.life <= 700;
    }

    @Override
    public boolean getSmoke() {
        return this.smoke;
    }

    @Override
    public boolean getIgnition() {
        return this.ignite;
    }

    @Override
    public Square getSquare() {
        return this.square;
    }
}

