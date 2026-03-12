/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.safehouse;

import java.util.ArrayList;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2, anticheats={})
public class SafehouseSyncPacket
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
    ArrayList<String> members = new ArrayList();
    @JSONField
    ArrayList<String> membersRespawn = new ArrayList();
    @JSONField
    public boolean remove;
    @JSONField
    String title = "";
    @JSONField
    int hitPoints;
    public SafeHouse safehouse;
    public boolean shouldCreateChat;

    @Override
    public void setData(Object ... values2) {
        this.set((SafeHouse)values2[0], (Boolean)values2[1]);
    }

    public void set(SafeHouse safehouse, boolean doRemove) {
        this.x = safehouse.getX();
        this.y = safehouse.getY();
        this.w = (short)safehouse.getW();
        this.h = (short)safehouse.getH();
        this.ownerUsername = safehouse.getOwner();
        this.members.clear();
        this.members.addAll(safehouse.getPlayers());
        this.membersRespawn.clear();
        this.membersRespawn.addAll(safehouse.getPlayersRespawn());
        this.remove = doRemove;
        this.title = safehouse.getTitle();
        this.hitPoints = safehouse.getHitPoints();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.w = b.getShort();
        this.h = b.getShort();
        this.ownerUsername = b.getUTF();
        this.safehouse = SafeHouse.getSafeHouse(this.x, this.y, this.w, this.h);
        if (GameClient.client && this.safehouse == null) {
            SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.ownerUsername);
            this.safehouse = SafeHouse.getSafeHouse(this.x, this.y, this.w, this.h);
        }
        int playersSize = b.getShort();
        this.members.clear();
        for (int i = 0; i < playersSize; ++i) {
            this.members.add(b.getUTF());
        }
        int playersRespawnSize = b.getShort();
        for (int i = 0; i < playersRespawnSize; ++i) {
            this.membersRespawn.add(b.getUTF());
        }
        this.remove = b.getBoolean();
        this.title = b.getUTF();
        this.hitPoints = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putShort(this.w);
        b.putShort(this.h);
        b.putUTF(this.ownerUsername);
        b.putShort(this.members.size());
        for (String member : this.members) {
            b.putUTF(member);
        }
        b.putShort(this.membersRespawn.size());
        for (String member : this.membersRespawn) {
            b.putUTF(member);
        }
        b.putBoolean(this.remove);
        b.putUTF(this.title);
        b.putInt(this.hitPoints);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.shouldCreateChat = false;
        if (this.safehouse == null) {
            this.safehouse = SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.ownerUsername);
            this.shouldCreateChat = true;
        }
        this.safehouse.getPlayers().clear();
        this.safehouse.getPlayers().addAll(this.members);
        this.safehouse.getPlayersRespawn().clear();
        this.safehouse.getPlayersRespawn().addAll(this.membersRespawn);
        this.safehouse.setTitle(this.title);
        this.safehouse.setOwner(this.ownerUsername);
        this.safehouse.setHitPoints(this.hitPoints);
        if (this.remove) {
            SafeHouse.removeSafeHouse(this.safehouse);
        }
        if (GameClient.client) {
            LuaEventManager.triggerEvent("OnSafehousesChanged");
        }
    }
}

