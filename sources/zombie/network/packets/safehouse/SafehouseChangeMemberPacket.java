/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHousePlayer;
import zombie.network.fields.SafehouseID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1, anticheats={AntiCheat.SafeHousePlayer})
public class SafehouseChangeMemberPacket
extends SafehouseID
implements INetworkPacket,
AntiCheatSafeHousePlayer.IAntiCheat {
    @JSONField
    public String player;

    @Override
    public void setData(Object ... values2) {
        this.set((SafeHouse)values2[0]);
        this.player = (String)values2[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.player);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.player = b.getUTF();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        }
        if (StringUtils.isNullOrEmpty(this.player)) {
            DebugLog.Multiplayer.error("player is not set");
            return false;
        }
        return true;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        UdpConnection c;
        IsoPlayer isoPlayer;
        if (!this.getSafehouse().getPlayers().contains(this.player)) {
            DebugLog.Multiplayer.error("player is not member");
            return;
        }
        if (this.getSafehouse().isOwner(this.player)) {
            DebugLog.Multiplayer.error("player is owner");
            return;
        }
        this.getSafehouse().removePlayer(this.player);
        if (!ServerOptions.instance.safehouseAllowTrepass.getValue() && (isoPlayer = GameServer.getPlayerByUserName(this.player)) != null && this.getSafehouse().containsLocation(isoPlayer.getX(), isoPlayer.getY()) && (c = GameServer.getConnectionFromPlayer(isoPlayer)) != null) {
            GameServer.sendTeleport(isoPlayer, (float)this.getSafehouse().getX() - 1.0f, (float)this.getSafehouse().getY() - 1.0f, 0.0f);
            if (isoPlayer.isAsleep()) {
                isoPlayer.setAsleep(false);
                isoPlayer.setAsleepTime(0.0f);
                INetworkPacket.sendToAll(PacketTypes.PacketType.WakeUpPlayer, isoPlayer);
            }
        }
        if ((isoPlayer = GameServer.getPlayerByUserName(this.player)) != null && GameServer.getConnectionFromPlayer(isoPlayer) != null && !isoPlayer.role.hasCapability(Capability.CanSetupSafehouses)) {
            INetworkPacket.send(GameServer.getConnectionFromPlayer(isoPlayer), PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), true);
        }
        for (UdpConnection c2 : GameServer.udpEngine.connections) {
            if (!c2.isFullyConnected()) continue;
            INetworkPacket.send(c2, PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), false);
        }
    }

    @Override
    public String getPlayer() {
        return this.player;
    }
}

