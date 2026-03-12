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
public class SafehouseChangeOwnerPacket
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
        int daysSurvived;
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        }
        if (StringUtils.isNullOrEmpty(this.player)) {
            DebugLog.Multiplayer.error("player is not set");
            return false;
        }
        IsoPlayer isoPlayer = GameServer.getPlayerByUserName(this.player);
        if (isoPlayer == null) {
            DebugLog.Multiplayer.error("player is not found");
            return false;
        }
        if (!isoPlayer.getRole().hasCapability(Capability.CanSetupSafehouses) && (daysSurvived = ServerOptions.instance.safehouseDaySurvivedToClaim.getValue()) > 0 && isoPlayer.getHoursSurvived() < (double)(daysSurvived * 24)) {
            DebugLog.Multiplayer.error("player \"%s\" not survived enough", isoPlayer.getUsername());
            return false;
        }
        return true;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.getSafehouse().isOwner(this.player)) {
            DebugLog.Multiplayer.error("player is already owner");
            return;
        }
        SafeHouse newSafeHouse = SafeHouse.hasSafehouse(this.player);
        if (newSafeHouse != null && newSafeHouse != this.getSafehouse()) {
            DebugLog.Multiplayer.error("player is already member");
            return;
        }
        String member = this.getSafehouse().getOwner();
        this.getSafehouse().setOwner(this.player);
        this.getSafehouse().addPlayer(member);
        for (UdpConnection c : GameServer.udpEngine.connections) {
            if (!c.isFullyConnected()) continue;
            INetworkPacket.send(c, PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), false);
        }
    }

    @Override
    public String getPlayer() {
        return this.getSafehouse().getOwner();
    }
}

