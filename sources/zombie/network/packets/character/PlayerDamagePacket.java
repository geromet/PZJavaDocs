/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=4, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class PlayerDamagePacket
extends PlayerID
implements INetworkPacket {
    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        try {
            b.putInt(this.getPlayer().getMaxWeight());
            b.putFloat(this.getPlayer().getCorpseSicknessRate());
            this.getPlayer().getBodyDamage().save(b.bb);
        }
        catch (IOException e) {
            DebugLog.Multiplayer.printException(e, "PlayerDamagePacket: failed", LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            try {
                this.getPlayer().setMaxWeight(b.getInt());
                this.getPlayer().setCorpseSicknessRate(b.getFloat());
                this.getPlayer().getBodyDamage().load(b.bb, IsoWorld.getWorldVersion());
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "PlayerDamagePacket: failed", LogSeverity.Error);
            }
        }
    }
}

