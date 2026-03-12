/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.hit;

import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.hit.Player;
import zombie.network.fields.hit.Weapon;
import zombie.network.packets.hit.HitCharacter;

public abstract class PlayerHit
implements HitCharacter {
    @JSONField
    protected final Player wielder = new Player();
    @JSONField
    protected final Weapon weapon = new Weapon();
    @JSONField
    protected boolean isIgnoreDamage;
    @JSONField
    protected byte shotID;

    public void set(IsoPlayer wielder, HandWeapon weapon, boolean isIgnoreDamage, boolean isCriticalHit) {
        this.wielder.set(wielder, isCriticalHit);
        this.weapon.set(weapon);
        this.isIgnoreDamage = isIgnoreDamage;
        this.shotID = wielder.networkAi.getShotID();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.wielder.parse(b, connection);
        this.weapon.parse(b, connection, (IsoLivingCharacter)this.wielder.getCharacter());
        this.shotID = b.getByte();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
        this.weapon.write(b);
        b.putByte(this.shotID);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.wielder.isRelevant(connection);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.wielder.isConsistent(connection) && this.weapon.isConsistent(connection);
    }

    @Override
    public void preProcess() {
        this.wielder.process();
    }

    @Override
    public void postProcess() {
        this.wielder.process();
    }

    @Override
    public void attack() {
        this.wielder.attack(this.weapon.getWeapon(), false, this.shotID);
    }

    public boolean isIgnoreDamage() {
        return this.isIgnoreDamage;
    }

    public HandWeapon getHandWeapon() {
        return this.weapon.getWeapon();
    }

    @Override
    public void sync(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.wielder.isConsistent(connection)) {
            this.attack();
        }
    }
}

