/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.HashMap;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public abstract class PacketsCache {
    private final HashMap<PacketTypes.PacketType, INetworkPacket> packets = new HashMap();

    protected PacketsCache() {
        for (PacketTypes.PacketType packetType : PacketTypes.PacketType.values()) {
            if (packetType.handler == null) {
                DebugType.Packet.warn("No packet handler for type: \"%s\"", packetType.name());
                continue;
            }
            try {
                this.packets.put(packetType, packetType.handler.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]));
            }
            catch (Exception e) {
                DebugType.Packet.printException(e, LogSeverity.Warning, "Error creating packet type: \"%s\"", packetType.name());
            }
        }
    }

    public INetworkPacket getPacket(PacketTypes.PacketType packetType) {
        return this.packets.get((Object)packetType);
    }
}

