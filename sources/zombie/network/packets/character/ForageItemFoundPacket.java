/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class ForageItemFoundPacket
implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    String itemType;
    @JSONField
    float amount;

    @Override
    public void setData(Object ... values2) {
        this.player.set((IsoPlayer)values2[0]);
        this.itemType = values2[1].toString();
        this.amount = ((Float)values2[2]).floatValue();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putUTF(this.itemType);
        b.putFloat(this.amount);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.player.parse(b, connection);
        this.itemType = b.getUTF();
        this.amount = b.getFloat();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LuaEventManager.triggerEvent("OnItemFound", this.player.getPlayer(), this.itemType, Float.valueOf(this.amount));
    }
}

