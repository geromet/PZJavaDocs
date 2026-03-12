/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.hit;

import zombie.characters.IsoLivingCharacter;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.IDInteger;
import zombie.network.fields.INetworkPacketField;
import zombie.util.Type;

public class Weapon
extends IDInteger
implements INetworkPacketField {
    @JSONField
    protected HandWeapon weapon;

    public void set(HandWeapon weapon) {
        this.setID(weapon.getID());
        this.weapon = weapon;
    }

    public void parse(ByteBufferReader b, IConnection connection, IsoLivingCharacter character) {
        super.parse(b, connection);
        if (character == null) {
            this.weapon = null;
            return;
        }
        if (character.bareHands.getID() == this.getID()) {
            this.weapon = character.bareHands;
        } else if (GameServer.server) {
            this.weapon = Type.tryCastTo(character.getInventory().getItemWithID(this.id), HandWeapon.class);
        } else if (GameClient.client) {
            if (character.getPrimaryHandItem() != null) {
                if (character.getPrimaryHandItem().getID() == this.getID()) {
                    this.weapon = Type.tryCastTo(character.getPrimaryHandItem(), HandWeapon.class);
                }
            } else {
                this.weapon = character.bareHands;
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        DebugLog.Multiplayer.error("Weapon.parse is not implemented");
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    public HandWeapon getWeapon() {
        return this.weapon;
    }
}

