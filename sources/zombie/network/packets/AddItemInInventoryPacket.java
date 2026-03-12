/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Collection;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class AddItemInInventoryPacket
implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    ArrayList<InventoryItem> items = new ArrayList();

    @Override
    public void setData(Object ... values2) {
        this.player.set((IsoPlayer)values2[0]);
        this.items.clear();
        this.items.addAll((Collection)values2[1]);
    }

    public void set(IsoPlayer player, ArrayList<InventoryItem> items) {
        this.items.clear();
        this.items.addAll(items);
        this.player.set(player);
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (InventoryItem item : this.items) {
            this.player.getPlayer().getInventory().addItem(item);
            ActionManager.getInstance().replaceObjectInQueuedActions(this.player.getPlayer(), null, item);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.items.clear();
        this.player.parse(b, connection);
        int size = b.getShort();
        for (int i = 0; i < size; ++i) {
            short id = b.getShort();
            b.getByte();
            try {
                InventoryItem item = InventoryItemFactory.CreateItem(id);
                if (item != null) {
                    item.load(b.bb, 244);
                }
                this.items.add(item);
                continue;
            }
            catch (IOException | BufferUnderflowException e) {
                DebugLog.Multiplayer.printException(e, "Item load error", LogSeverity.Error);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putShort(this.items.size());
        for (int i = 0; i < this.items.size(); ++i) {
            try {
                this.items.get(i).save(b.bb, true);
                continue;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

