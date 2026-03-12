/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.fields.MovingObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class GameCharacterAttachedItemPacket
implements INetworkPacket {
    @JSONField
    final MovingObject movingObject = new MovingObject();
    @JSONField
    protected String location = "";
    @JSONField
    protected ContainerID containerId = new ContainerID();
    @JSONField
    protected int itemId;
    @JSONField
    protected InventoryItem item;

    @Override
    public void setData(Object ... values2) {
        this.movingObject.set((IsoGameCharacter)values2[0]);
        this.location = (String)values2[1];
        this.item = (InventoryItem)values2[2];
        if (this.item != null) {
            this.containerId.set(this.item.getContainer());
            this.itemId = this.item.getID();
        } else {
            this.containerId.set(null);
            this.itemId = -1;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.movingObject.write(b);
        b.putUTF(this.location);
        if (GameClient.client) {
            this.containerId.write(b);
            b.putInt(this.itemId);
        } else if (b.putBoolean(this.item != null)) {
            try {
                this.item.saveWithSize(b.bb, false);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "itemWriteError", LogSeverity.Error);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.movingObject.parse(b, connection);
        this.location = b.getUTF();
        if (GameServer.server) {
            this.containerId.parse(b, connection);
            this.itemId = b.getInt();
        } else {
            this.item = null;
            boolean hasItem = b.getBoolean();
            if (hasItem) {
                try {
                    IsoPlayer player;
                    this.item = InventoryItem.loadItem(b.bb, 244);
                    IsoPlayer isoPlayer = player = this.movingObject.getObject() instanceof IsoPlayer ? (IsoPlayer)this.movingObject.getObject() : null;
                    if (player != null && player.isLocalPlayer()) {
                        this.item = player.getInventory().getItemWithID(this.item.getID());
                    }
                }
                catch (Exception ex) {
                    DebugLog.General.printException(ex, "", LogSeverity.Error);
                }
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.movingObject.isConsistent(connection) && this.containerId.isConsistent(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoPlayer isoPlayer;
        IsoObject obj = this.movingObject.getObject();
        if (obj instanceof IsoPlayer && (isoPlayer = (IsoPlayer)obj).isLocalPlayer()) {
            return;
        }
        if (obj instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)obj;
            isoGameCharacter.setAttachedItem(this.location, this.item);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoObject obj;
        this.item = null;
        if (this.containerId.containerType != ContainerID.ContainerType.Undefined) {
            this.item = this.containerId.getContainer().getItemWithID(this.itemId);
            if (this.item == null) {
                DebugLog.Multiplayer.println("PlayerAttachedItemPacket.processServer item can't be found container:" + this.containerId.getDescription());
                return;
            }
        }
        if (!((obj = this.movingObject.getObject()) instanceof IsoGameCharacter)) {
            return;
        }
        IsoGameCharacter isoGameCharacter = (IsoGameCharacter)obj;
        isoGameCharacter.setAttachedItem(this.location, this.item);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            IsoPlayer p2;
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || (p2 = GameServer.getAnyPlayerFromConnection(connection)) == null) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.GameCharacterAttachedItem.doPacket(b2);
            this.write(b2);
            PacketTypes.PacketType.GameCharacterAttachedItem.send(c);
        }
    }
}

