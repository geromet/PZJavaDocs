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
import zombie.iso.IsoWorld;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseSurvivor;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.CanSetupSafehouses, handlingType=1, anticheats={AntiCheat.SafeHouseSurviving})
public class SafezoneClaimPacket
extends PlayerID
implements INetworkPacket,
AntiCheatSafeHouseSurvivor.IAntiCheat {
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    public int w;
    @JSONField
    public int h;
    @JSONField
    private String title;

    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0]);
        this.x = (Integer)values2[1];
        this.y = (Integer)values2[2];
        this.w = (Integer)values2[3];
        this.h = (Integer)values2[4];
        this.title = (String)values2[5];
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.x = b.getInt();
        this.y = b.getInt();
        this.w = b.getInt();
        this.h = b.getInt();
        this.title = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.w);
        b.putInt(this.h);
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
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
        if (square == null) {
            DebugLog.Multiplayer.error("square is not found");
            return false;
        }
        int maxSize = ServerOptions.getInstance().maxSafezoneSize.getValue();
        if (maxSize > 0 && this.h * this.w > maxSize) {
            DebugLog.Multiplayer.error("safezone is too big");
            return false;
        }
        int onlineID = SafeHouse.getOnlineID(this.x, this.y);
        if (SafeHouse.getSafeHouse(onlineID) != null) {
            DebugLog.Multiplayer.error("safehouse is already claimed");
            return false;
        }
        boolean intersects = SafeHouse.intersects(this.x, this.y, this.x + this.w, this.y + this.h);
        if (intersects) {
            DebugLog.Multiplayer.error("can't be safezone");
            return false;
        }
        return true;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        SafeHouse safehouse = SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.getPlayer().getUsername());
        safehouse.setTitle(this.title);
        INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, safehouse, false);
    }

    @Override
    public IsoPlayer getSurvivor() {
        return this.getPlayer();
    }
}

