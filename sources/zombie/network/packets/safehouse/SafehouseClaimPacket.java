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
import zombie.iso.IsoGridSquare;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseSurvivor;
import zombie.network.fields.Square;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1, anticheats={AntiCheat.SafeHouseSurviving})
public class SafehouseClaimPacket
extends PlayerID
implements INetworkPacket,
AntiCheatSafeHouseSurvivor.IAntiCheat {
    @JSONField
    private final Square square = new Square();
    @JSONField
    private String title;

    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[1]);
        this.square.set((IsoGridSquare)values2[0]);
        this.title = (String)values2[2];
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.square.parse(b, connection);
        this.title = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.square.write(b);
        b.putUTF(this.title);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        boolean capability = connection.getRole().hasCapability(Capability.CanSetupSafehouses);
        if (!(ServerOptions.instance.playerSafehouse.getValue() || ServerOptions.instance.adminSafehouse.getValue() && capability)) {
            DebugLog.Multiplayer.error("safehouse options are disabled");
            return false;
        }
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("player is not found");
            return false;
        }
        if (StringUtils.isNullOrEmpty(this.title)) {
            DebugLog.Multiplayer.error("title is not set");
            return false;
        }
        if (!this.square.isConsistent(connection)) {
            DebugLog.Multiplayer.error("square is not found");
            return false;
        }
        if (SafeHouse.getSafeHouse(this.square.getSquare()) != null) {
            DebugLog.Multiplayer.error("safehouse is already claimed");
            return false;
        }
        String reason = SafeHouse.canBeSafehouse(this.square.getSquare(), this.getPlayer());
        if (!"".equals(reason)) {
            DebugLog.Multiplayer.error("can't be safehouse");
            return false;
        }
        return true;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        SafeHouse safehouse = SafeHouse.addSafeHouse(this.square.getSquare(), this.getPlayer());
        if (safehouse != null) {
            safehouse.setTitle(this.title);
            for (UdpConnection c : GameServer.udpEngine.connections) {
                if (!c.isFullyConnected()) continue;
                INetworkPacket.send(c, PacketTypes.PacketType.SafehouseSync, safehouse, false);
            }
        }
    }

    @Override
    public IsoPlayer getSurvivor() {
        return this.getPlayer();
    }
}

