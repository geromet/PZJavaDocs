/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class GetModDataPacket
implements INetworkPacket {
    @Override
    public void write(ByteBufferWriter b) {
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LuaEventManager.triggerEvent("SendCustomModData");
    }
}

