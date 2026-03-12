/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=4, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class PlayerHealthPacket
extends PlayerID
implements INetworkPacket {
    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        for (BodyPartType bodyPartType : BodyPartType.values()) {
            if (bodyPartType == BodyPartType.MAX) continue;
            b.putFloat(this.getPlayer().getBodyDamage().getBodyPart(bodyPartType).getHealth());
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            for (BodyPartType bodyPartType : BodyPartType.values()) {
                if (bodyPartType == BodyPartType.MAX) continue;
                this.getPlayer().getBodyDamage().getBodyPart(bodyPartType).SetHealth(b.getFloat());
            }
            this.getPlayer().getBodyDamage().calculateOverallHealth();
        }
    }
}

