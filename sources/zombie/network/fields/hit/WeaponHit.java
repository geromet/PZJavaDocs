/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.hit;

import zombie.CombatManager;
import zombie.Lua.LuaEventManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.hit.Hit;

public class WeaponHit
extends Hit
implements INetworkPacketField {
    @JSONField
    protected float range;
    @JSONField
    protected boolean hitHead;
    @JSONField
    protected boolean removeKnife;

    public void set(float damage, float range, float hitForce, float hitDirectionX, float hitDirectionY, boolean hitHead, boolean removeKnife) {
        this.set(damage, hitForce, hitDirectionX, hitDirectionY);
        this.range = range;
        this.hitHead = hitHead;
        this.removeKnife = removeKnife;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.range = b.getFloat();
        this.hitHead = b.getBoolean();
        this.removeKnife = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putFloat(this.range);
        b.putBoolean(this.hitHead);
        b.putBoolean(this.removeKnife);
    }

    public void process(IsoGameCharacter wielder, IsoGameCharacter target, HandWeapon weapon, boolean ignore) {
        if (this.removeKnife && wielder instanceof IsoPlayer) {
            IsoPlayer owner = (IsoPlayer)wielder;
            if (target instanceof IsoZombie) {
                IsoZombie zombie = (IsoZombie)target;
                InventoryItem item = owner.getPrimaryHandItem();
                owner.getInventory().Remove(item);
                owner.removeFromHands(item);
                zombie.setAttachedItem("JawStab", item);
                zombie.setJawStabAttach(true);
                zombie.setKnifeDeath(true);
            }
        }
        target.Hit(weapon, wielder, this.damage, ignore, this.range, true);
        this.process(wielder, target);
        if (wielder.isAimAtFloor() && !weapon.isRanged() && wielder.isNPC()) {
            CombatManager.getInstance().splash(target, weapon, wielder);
        }
        if (this.hitHead) {
            CombatManager.getInstance().splash(target, weapon, wielder);
            CombatManager.getInstance().splash(target, weapon, wielder);
            target.addBlood(BloodBodyPartType.Head, true, true, true);
            target.addBlood(BloodBodyPartType.Torso_Upper, true, false, false);
            target.addBlood(BloodBodyPartType.UpperArm_L, true, false, false);
            target.addBlood(BloodBodyPartType.UpperArm_R, true, false, false);
        }
        if ((!((IsoLivingCharacter)wielder).isDoShove() || wielder.isAimAtFloor()) && wielder.DistToSquared(target) < 2.0f && Math.abs(wielder.getZ() - target.getZ()) < 0.5f) {
            wielder.addBlood(null, false, false, false);
        }
        if (!(target.isDead() || target instanceof IsoPlayer || ((IsoLivingCharacter)wielder).isDoShove() && !wielder.isAimAtFloor())) {
            CombatManager.getInstance().splash(target, weapon, wielder);
        }
        if (GameServer.server) {
            LuaEventManager.triggerEvent("OnWeaponHitXp", wielder, weapon, target, Float.valueOf(this.damage), 1);
            CombatManager.getInstance().processMaintenanceCheck(wielder, weapon, target);
            GameServer.helmetFall(target, this.hitHead);
        }
    }
}

