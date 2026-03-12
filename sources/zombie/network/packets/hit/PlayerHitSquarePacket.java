/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.hit;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoUtils;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.fields.Square;
import zombie.network.packets.hit.PlayerHit;

@PacketSetting(ordering=0, priority=0, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3, anticheats={AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo})
public class PlayerHitSquarePacket
extends PlayerHit
implements AntiCheatHitLongDistance.IAntiCheat,
AntiCheatHitWeaponAmmo.IAntiCheat {
    @JSONField
    protected final Square square = new Square();

    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0], (HandWeapon)values2[1], (Boolean)values2[2], (Boolean)values2[3]);
    }

    @Override
    public void set(IsoPlayer wielder, HandWeapon weapon, boolean isIgnoreDamage, boolean isCriticalHit) {
        super.set(wielder, weapon, isIgnoreDamage, isCriticalHit);
        this.square.set(wielder);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.square.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.square.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.square.isConsistent(connection);
    }

    @Override
    public void process() {
        this.square.process(this.wielder.getCharacter());
    }

    @Override
    public IsoPlayer getWielder() {
        return (IsoPlayer)this.wielder.getCharacter();
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.square.getX(), this.square.getY(), this.wielder.getX(), this.wielder.getY());
    }
}

