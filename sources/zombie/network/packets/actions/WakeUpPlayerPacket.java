/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.actions;

import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class WakeUpPlayerPacket
extends PlayerID
implements INetworkPacket {
    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0]);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.getPlayer().setAsleep(false);
        this.getPlayer().setAsleepTime(0.0f);
        this.sendToClients(PacketTypes.PacketType.WakeUpPlayer, connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        SleepingEvent.instance.wakeUp(this.getPlayer(), true);
    }
}

