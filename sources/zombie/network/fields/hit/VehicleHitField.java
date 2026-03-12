/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.hit;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.IMovable;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.hit.Hit;
import zombie.vehicles.BaseVehicle;

public class VehicleHitField
extends Hit
implements IMovable,
INetworkPacketField {
    @JSONField
    public int vehicleDamage;
    @JSONField
    public float vehicleSpeed;
    @JSONField
    public boolean isVehicleHitFromBehind;
    @JSONField
    public boolean isTargetHitFromBehind;
    @JSONField
    public boolean isStaggerBack;
    @JSONField
    public boolean isKnockedDown;

    public void set(boolean ignore, float damage, float hitForce, float hitDirectionX, float hitDirectionY, int vehicleDamage, float vehicleSpeed, boolean isVehicleHitFromBehind, boolean isTargetHitFromBehind, boolean isStaggerBack, boolean isKnockedDown) {
        this.set(damage, hitForce, hitDirectionX, hitDirectionY);
        this.vehicleDamage = vehicleDamage;
        this.vehicleSpeed = vehicleSpeed;
        this.isVehicleHitFromBehind = isVehicleHitFromBehind;
        this.isTargetHitFromBehind = isTargetHitFromBehind;
        this.isStaggerBack = isStaggerBack;
        this.isKnockedDown = isKnockedDown;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.vehicleDamage = b.getInt();
        this.vehicleSpeed = b.getFloat();
        this.isVehicleHitFromBehind = b.getBoolean();
        this.isTargetHitFromBehind = b.getBoolean();
        this.isStaggerBack = b.getBoolean();
        this.isKnockedDown = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.vehicleDamage);
        b.putFloat(this.vehicleSpeed);
        b.putBoolean(this.isVehicleHitFromBehind);
        b.putBoolean(this.isTargetHitFromBehind);
        b.putBoolean(this.isStaggerBack);
        b.putBoolean(this.isKnockedDown);
    }

    public void process(IsoGameCharacter wielder, IsoGameCharacter target, BaseVehicle vehicle) {
        this.process(wielder, target);
        if (GameServer.server) {
            if (this.vehicleDamage != 0) {
                if (this.isVehicleHitFromBehind) {
                    vehicle.addDamageFrontHitAChr(this.vehicleDamage);
                } else {
                    vehicle.addDamageRearHitAChr(this.vehicleDamage);
                }
                vehicle.transmitBlood();
            }
            if (target instanceof IsoAnimal) {
                IsoAnimal isoAnimal = (IsoAnimal)target;
                isoAnimal.setHealth(0.0f);
            } else if (target instanceof IsoZombie) {
                IsoZombie isoZombie = (IsoZombie)target;
                isoZombie.applyDamageFromVehicleHit(this.vehicleSpeed, this.damage);
                isoZombie.setKnockedDown(this.isKnockedDown);
                isoZombie.setStaggerBack(this.isStaggerBack);
            } else if (target instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)target;
                isoPlayer.applyDamageFromVehicleHit(this.vehicleSpeed, this.damage);
                isoPlayer.setKnockedDown(this.isKnockedDown);
            }
        } else if (GameClient.client && target instanceof IsoPlayer) {
            target.getActionContext().reportEvent("washit");
            target.setVariable("hitpvp", false);
        }
    }

    @Override
    public float getSpeed() {
        return this.vehicleSpeed;
    }

    @Override
    public boolean isVehicle() {
        return true;
    }
}

