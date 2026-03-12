/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.connection;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.LoginQueue;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=0, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class LoginQueueDonePacket
implements INetworkPacket {
    @JSONField
    long dt;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 1 && values2[0] instanceof Long) {
            this.set((Long)values2[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(long dt) {
        this.dt = dt;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putLong(this.dt);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.dt = b.getLong();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LoginQueue.receiveLoginQueueDone(this.dt, connection);
    }
}

