/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.safehouse;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.SafehouseID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SafehouseInvitePacket
extends SafehouseID
implements INetworkPacket {
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    public short w;
    @JSONField
    public short h;
    @JSONField
    public String ownerUsername;
    @JSONField
    protected String owner;
    @JSONField
    protected String invited;

    @Override
    public void setData(Object ... values2) {
        this.set((SafeHouse)values2[0]);
        this.owner = ((IsoPlayer)values2[1]).getUsername();
        this.invited = (String)values2[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.getSafehouse().getX());
        b.putInt(this.getSafehouse().getY());
        b.putShort(this.getSafehouse().getW());
        b.putShort(this.getSafehouse().getH());
        b.putUTF(this.getSafehouse().getOwner());
        b.putUTF(this.owner);
        b.putUTF(this.invited);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.w = b.getShort();
        this.h = b.getShort();
        this.ownerUsername = b.getUTF();
        SafeHouse safehouse = SafeHouse.getSafeHouse(this.x, this.y, this.w, this.h);
        if (GameClient.client && safehouse == null) {
            SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.ownerUsername);
            safehouse = SafeHouse.getSafeHouse(this.x, this.y, this.w, this.h);
        }
        this.set(safehouse);
        this.owner = b.getUTF();
        this.invited = b.getUTF();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (GameServer.server && !super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        }
        if (StringUtils.isNullOrEmpty(this.owner)) {
            DebugLog.Multiplayer.error("owner is not set");
            return false;
        }
        if (StringUtils.isNullOrEmpty(this.invited)) {
            DebugLog.Multiplayer.error("member is not set");
            return false;
        }
        return true;
    }

    @Override
    public void processClient(UdpConnection connection) {
        LuaEventManager.triggerEvent("ReceiveSafehouseInvite", this.getSafehouse(), this.owner);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.CanSetupSafehouses) && !connection.hasPlayer(this.getSafehouse().getOwner())) {
            DebugLog.Multiplayer.error("player is not owner");
            return;
        }
        if (SafeHouse.hasSafehouse(this.invited) != null) {
            DebugLog.Multiplayer.error("player is already member or owner");
            return;
        }
        IsoPlayer player = GameServer.getPlayerByUserName(this.invited);
        if (player == null) {
            DebugLog.Multiplayer.error("player is not found");
            return;
        }
        UdpConnection c = GameServer.getConnectionFromPlayer(player);
        if (c == null) {
            DebugLog.Multiplayer.error("connection is not found");
            return;
        }
        this.getSafehouse().addInvite(this.invited);
        this.sendToClient(packetType, c);
    }
}

