/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoMannequin;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.fields.ContainerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class AddInventoryItemToContainerPacket
implements INetworkPacket {
    @JSONField
    private final ContainerID containerId = new ContainerID();
    @JSONField
    private final ArrayList<InventoryItem> items = new ArrayList();

    @Override
    public void setData(Object ... values2) {
        this.containerId.set((ItemContainer)values2[0]);
        this.items.clear();
        if (values2[1] instanceof InventoryItem) {
            this.items.add((InventoryItem)values2[1]);
        } else {
            this.items.addAll((ArrayList)values2[1]);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        try {
            CompressIdenticalItems.save(b.bb, this.items, null);
        }
        catch (IOException e) {
            DebugLog.Multiplayer.printException(e, "Items save fail", LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.items.clear();
        try {
            this.items.addAll(CompressIdenticalItems.load(b.bb, 244, null, null));
        }
        catch (IOException e) {
            DebugLog.Multiplayer.printException(e, "Items load fail", LogSeverity.Error);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        HashSet<String> logItemType = new HashSet<String>();
        ItemContainer container = this.containerId.getContainer();
        IsoObject object = this.containerId.getObject();
        if (container != null) {
            try {
                for (int i = 0; i < this.items.size(); ++i) {
                    InventoryItem item = this.items.get(i);
                    if (item == null) continue;
                    if (container.containsID(item.id)) {
                        System.out.println("Error: Dupe item ID for " + connection.getUserName());
                        AddInventoryItemToContainerPacket.logDupeItem(connection, item.getDisplayName());
                        continue;
                    }
                    container.addItem(item);
                    container.setExplored(true);
                    logItemType.add(item.getFullType());
                    if (!(object instanceof IsoMannequin)) continue;
                    IsoMannequin isoMannequin = (IsoMannequin)object;
                    isoMannequin.wearItem(item, null);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            if (object != null) {
                LuaManager.updateOverlaySprite(object);
                if ("campfire".equals(container.getType())) {
                    object.sendObjectChange(IsoObjectChange.CONTAINER_CUSTOM_TEMPERATURE);
                }
            }
        } else {
            DebugLog.log("ERROR AddInventoryItemToContainerPacket container is null");
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || !c.isRelevantTo(this.containerId.x, this.containerId.y)) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.AddInventoryItemToContainer.doPacket(b2);
            this.write(b2);
            PacketTypes.PacketType.AddInventoryItemToContainer.send(c);
        }
        LoggerManager.getLogger("item").write(connection.getIDStr() + " \"" + connection.getUserName() + "\" container +" + this.items.size() + " " + this.containerId.x + "," + this.containerId.y + "," + this.containerId.z + " " + String.valueOf(logItemType));
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (IsoWorld.instance.currentCell == null) {
            return;
        }
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            InventoryItem item;
            if (container.getType().equals("floor") && !container.getItems().isEmpty() && (item = container.getItems().get(0)) instanceof InventoryContainer) {
                InventoryContainer inventoryContainer = (InventoryContainer)item;
                container = inventoryContainer.getItemContainer();
            }
            try {
                for (int i = 0; i < this.items.size(); ++i) {
                    InventoryItem item2 = this.items.get(i);
                    if (item2 == null) continue;
                    if (container.containsID(item2.id)) {
                        if (this.containerId.containerType == ContainerID.ContainerType.DeadBody) continue;
                        System.out.println("Error: Dupe item ID. id = " + item2.id);
                        continue;
                    }
                    container.addItem(item2);
                    container.setExplored(true);
                    if (container.getParent() instanceof IsoMannequin) {
                        ((IsoMannequin)container.getParent()).wearItem(item2, null);
                    }
                    for (int j = 0; j < IsoPlayer.numPlayers; ++j) {
                        IsoPlayer isoPlayer = IsoPlayer.players[j];
                        if (!(isoPlayer instanceof IsoPlayer)) continue;
                        IsoPlayer player = isoPlayer;
                        ActionManager.getInstance().replaceObjectInQueuedActions(player, null, item2);
                    }
                }
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
            if (this.containerId.getPart() != null) {
                this.containerId.getPart().setContainerContentAmount(container.getCapacityWeight());
            }
        }
    }

    public int getX() {
        return this.containerId.x;
    }

    public int getY() {
        return this.containerId.y;
    }

    private static void logDupeItem(UdpConnection connection, String text) {
        IsoPlayer player = null;
        for (int k = 0; k < GameServer.Players.size(); ++k) {
            if (!connection.getUserName().equals(GameServer.Players.get((int)k).username)) continue;
            player = GameServer.Players.get(k);
            break;
        }
        if (player != null) {
            String coordinate = LoggerManager.getPlayerCoords(player);
            LoggerManager.getLogger("user").write("Error: Dupe item ID for " + player.getDisplayName() + " " + coordinate);
        }
        ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.DupeItem, text, GameServer.class.getSimpleName(), 1);
    }
}

