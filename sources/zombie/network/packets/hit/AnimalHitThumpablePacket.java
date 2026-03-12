/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.hit;

import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.fields.hit.Thumpable;
import zombie.network.packets.hit.AnimalHit;

@PacketSetting(ordering=0, priority=0, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3, anticheats={AntiCheat.HitShortDistance})
public class AnimalHitThumpablePacket
extends AnimalHit
implements AntiCheatHitShortDistance.IAntiCheat {
    @JSONField
    protected final Thumpable thumpable = new Thumpable();

    /*
     * Enabled aggressive block sorting
     */
    @Override
    public void setData(Object ... values2) {
        Object object;
        if (values2.length == 2 && (object = values2[0]) instanceof IsoAnimal) {
            IsoAnimal animal = (IsoAnimal)object;
            object = values2[1];
            if (object instanceof IsoObject) {
                IsoObject object2 = (IsoObject)object;
                this.set(animal, object2);
                return;
            }
        }
        DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
    }

    public void set(IsoAnimal wielder, IsoObject thumpable) {
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
        return super.isConsistent(connection) && this.thumpable.isConsistent(connection);
    }

    @Override
    public void process() {
        this.thumpable.process(this.wielder.getAnimal());
    }

    @Override
    public void postProcess() {
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.thumpable.getX(), this.thumpable.getY(), this.wielder.getX(), this.wielder.getY());
    }
}

