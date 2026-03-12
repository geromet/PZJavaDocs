/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.actions;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=3, reliability=0, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SneezeCoughPacket
implements INetworkPacket {
    protected final PlayerID wielder = new PlayerID();
    protected byte value;

    public void set(IsoPlayer wielder, int sneezingCoughing, byte sneezeVar) {
        this.wielder.set(wielder);
        this.value = 0;
        if (sneezingCoughing % 2 == 0) {
            this.value = (byte)(this.value | 1);
        }
        if (sneezingCoughing > 2) {
            this.value = (byte)(this.value | 2);
        }
        if (sneezeVar > 1) {
            this.value = (byte)(this.value | 4);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
        b.putByte(this.value);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.wielder.parse(b, connection);
        this.value = b.getByte();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.wielder.getPlayer() != null) {
            boolean isSneeze = (this.value & 1) == 0;
            boolean isMuffled = (this.value & 2) != 0;
            int sneezeVar = (this.value & 4) == 0 ? 1 : 2;
            this.wielder.getPlayer().setVariable("Ext", (String)(isSneeze ? "Sneeze" + sneezeVar : "Cough"));
            this.wielder.getPlayer().Say(Translator.getText("IGUI_PlayerText_" + (isSneeze ? "Sneeze" : "Cough") + (isMuffled ? "Muffled" : "")));
            this.wielder.getPlayer().reportEvent("EventDoExt");
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.wielder.getPlayer() != null) {
            float x = this.wielder.getPlayer().getX();
            float y = this.wielder.getPlayer().getY();
            int size = GameServer.udpEngine.connections.size();
            for (int n = 0; n < size; ++n) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (connection.getConnectedGUID() == c.getConnectedGUID() || !c.isRelevantTo(x, y)) continue;
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.SneezeCough.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.SneezeCough.send(c);
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.wielder.isConsistent(connection);
    }
}

