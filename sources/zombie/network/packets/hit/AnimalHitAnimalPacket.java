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
import zombie.iso.IsoUtils;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.hit.Damage;
import zombie.network.packets.hit.AnimalHit;

@PacketSetting(ordering=0, priority=0, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3, anticheats={AntiCheat.HitShortDistance})
public class AnimalHitAnimalPacket
extends AnimalHit
implements AntiCheatHitShortDistance.IAntiCheat {
    @JSONField
    protected final AnimalID target = new AnimalID();
    @JSONField
    protected final Damage damage = new Damage();

    /*
     * Enabled aggressive block sorting
     */
    @Override
    public void setData(Object ... values2) {
        Object object;
        if (values2.length == 4 && (object = values2[0]) instanceof IsoAnimal) {
            IsoAnimal animal = (IsoAnimal)object;
            object = values2[1];
            if (object instanceof IsoAnimal) {
                IsoAnimal animalTarget = (IsoAnimal)object;
                object = values2[2];
                if (object instanceof Float) {
                    Float damageValue = (Float)object;
                    object = values2[3];
                    if (object instanceof Boolean) {
                        Boolean ignore = (Boolean)object;
                        this.set(animal, animalTarget, ignore, damageValue.floatValue());
                        return;
                    }
                }
            }
        }
        DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
    }

    public void set(IsoAnimal wielder, IsoAnimal target, boolean ignore, float damage) {
        this.set(wielder);
        this.target.set(target);
        this.damage.set(ignore, damage);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.target.parse(b, connection);
        this.damage.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.target.write(b);
        this.damage.write(b);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.target.isRelevant(connection);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.target.isConsistent(connection);
    }

    @Override
    public void preProcess() {
    }

    @Override
    public void process() {
        this.damage.processAnimal(this.wielder.getAnimal(), this.target.getAnimal());
    }

    @Override
    public void postProcess() {
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.target.getX(), this.target.getY(), this.wielder.getX(), this.wielder.getY());
    }
}

