/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.hit;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;

public class Bite
implements INetworkPacketField {
    protected short flags;
    protected float hitDirection;
    protected String hitReaction;

    public void set(IsoZombie wielder, boolean didDamage, String hitReaction) {
        this.flags = 0;
        this.flags = (short)(this.flags | (short)(wielder.getEatBodyTarget() != null ? 1 : 0));
        this.flags = (short)(this.flags | (short)(didDamage ? 2 : 0));
        this.flags = (short)(this.flags | (short)("BiteDefended".equals(wielder.getHitReaction()) ? 4 : 0));
        this.flags = (short)(this.flags | (short)(wielder.scratch ? 8 : 0));
        this.flags = (short)(this.flags | (short)(wielder.laceration ? 16 : 0));
        this.hitDirection = wielder.getHitDir().getDirection();
        this.hitReaction = hitReaction;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.flags = b.getShort();
        this.hitDirection = b.getFloat();
        this.hitReaction = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.flags);
        b.putFloat(this.hitDirection);
        b.putUTF(this.hitReaction);
    }

    @Override
    public String getDescription() {
        return "\n\tBite [ eatBodyTarget=" + ((this.flags & 1) != 0) + " | attackDidDamage=" + ((this.flags & 2) != 0) + " | biteDefended=" + ((this.flags & 4) != 0) + " | scratch=" + ((this.flags & 8) != 0) + " | laceration=" + ((this.flags & 0x10) != 0) + " | hitDirection=" + this.hitDirection + " ]";
    }

    public void process(IsoZombie wielder, IsoGameCharacter target) {
        if ((this.flags & 4) == 0) {
            target.setAttackedBy(wielder);
            if ((this.flags & 1) != 0 || target.isDead()) {
                wielder.setEatBodyTarget(target, true);
                wielder.setTarget(null);
            }
            if (target.isAsleep()) {
                if (GameServer.server) {
                    target.sendObjectChange(IsoObjectChange.WAKE_UP);
                } else {
                    target.forceAwake();
                }
            }
            if ((this.flags & 2) != 0) {
                target.reportEvent("washit");
                target.setVariable("hitpvp", false);
                target.setHitReaction(this.hitReaction);
            }
            wielder.scratch = (this.flags & 8) != 0;
            boolean bl = wielder.laceration = (this.flags & 8) != 0;
            if (GameServer.server && target.getBodyDamage() != null) {
                target.getBodyDamage().AddRandomDamageFromZombie(wielder, this.hitReaction);
                if (target.isDeathDragDown()) {
                    target.Kill(wielder);
                }
            }
        }
        wielder.getHitDir().setLengthAndDirection(this.hitDirection, 1.0f);
    }
}

