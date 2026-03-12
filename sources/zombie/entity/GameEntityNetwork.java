/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.GameEntityType;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.entity.network.PacketGroup;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

@PacketSetting(ordering=0, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class GameEntityNetwork
implements INetworkPacket {
    @JSONField
    EntityPacketData data;
    @JSONField
    GameEntity entity;
    @JSONField
    Component component;

    public static EntityPacketData createPacketData(EntityPacketType packetType) {
        return EntityPacketData.alloc(packetType);
    }

    public static void sendPacketDataTo(IsoPlayer player, EntityPacketData data, GameEntity entity, Component component) {
        if (!GameServer.server) {
            throw new RuntimeException("Can only call on server.");
        }
        if (player == null) {
            return;
        }
        UdpConnection connection = GameServer.getConnectionFromPlayer(player);
        if (connection != null) {
            GameEntityNetwork.sendPacketData(data, entity, component, connection, false);
        }
    }

    public static void sendPacketData(EntityPacketData data, GameEntity entity, Component component, IConnection connection, boolean isIgnoreConnection) {
        if (entity == null) {
            DebugLog.General.warn("Packet should have entity set.");
            return;
        }
        if (entity.getGameEntityType() == GameEntityType.Template) {
            return;
        }
        if (!GameClient.client && !GameServer.server) {
            return;
        }
        if (data.getEntityPacketType().isEntityPacket() && component != null) {
            DebugLog.General.warn("Entity Packet should not have component set.");
            return;
        }
        if (data.getEntityPacketType().isComponentPacket() && component == null) {
            DebugLog.General.warn("Component Packet requires to have component set.");
            return;
        }
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.GameEntity, data, entity, component);
        } else if (GameServer.server) {
            if (isIgnoreConnection) {
                INetworkPacket.sendToAll(PacketTypes.PacketType.GameEntity, connection, data, entity, component);
            } else {
                INetworkPacket.send(connection, PacketTypes.PacketType.GameEntity, data, entity, component);
            }
        }
        EntityPacketData.release(data);
    }

    @Override
    public void write(ByteBufferWriter b) {
        long entityNetID = this.entity.getEntityNetID();
        if (entityNetID < 0L) {
            throw new RuntimeException("Invalid EntityNetID");
        }
        b.putLong(entityNetID);
        InventoryItem item = Type.tryCastTo(this.entity, InventoryItem.class);
        if (item != null && item.getContainer() != null && item.getContainer().getParent() instanceof IsoPlayer) {
            b.putShort(((IsoPlayer)item.getContainer().getParent()).getOnlineID());
        } else {
            b.putShort(-1);
        }
        if (this.component != null) {
            b.putShort(this.component.getComponentType().GetID());
        } else {
            b.putShort(-1);
        }
        this.data.bb.limit(this.data.bb.position());
        this.data.bb.position(0);
        b.put(this.data.bb);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (!GameClient.client && !GameServer.server) {
            return;
        }
        IConnection senderConnection = GameClient.client ? null : connection;
        long entityNetId = b.getLong();
        short onlineID = b.getShort();
        GameEntity gameEntity = GameEntityManager.GetEntity(entityNetId);
        if (gameEntity == null) {
            InventoryItem item;
            DebugLog.Objects.debugln("Cannot find registered game entity for id = %d", entityNetId);
            IsoPlayer player = GameClient.IDToPlayerMap.get(onlineID);
            if (player != null && (item = player.getInventory().getItemWithID((int)entityNetId)) != null) {
                gameEntity = Type.tryCastTo(item, GameEntity.class);
            }
            if (gameEntity == null) {
                DebugLog.Objects.debugln("Cannot find inventory item for id = %d", entityNetId);
                return;
            }
        }
        short componentID = b.getShort();
        EntityPacketType packetType = EntityPacketType.FromByteBuffer(b);
        try {
            if (packetType.getGroup() != PacketGroup.GameEntity) {
                boolean success;
                Component component = gameEntity.getComponentFromID(componentID);
                if (component != null && !(success = component.onReceivePacket(b, packetType, senderConnection)) && Core.debug) {
                    DebugLog.Entity.error("ReadPacketContents returned failure. Component = " + String.valueOf((Object)component.getComponentType()) + ", packetType = " + String.valueOf((Object)packetType));
                }
            } else {
                boolean success = gameEntity.onReceiveEntityPacket(b, packetType, senderConnection);
                if (!success && Core.debug) {
                    DebugLog.Entity.error("ReadPacketContents returned failure. PacketType = " + String.valueOf((Object)packetType));
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setData(Object ... values2) {
        this.data = (EntityPacketData)values2[0];
        this.entity = (GameEntity)values2[1];
        this.component = (Component)values2[2];
    }
}

