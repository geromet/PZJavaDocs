/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.network;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.entity.network.PacketGroup;

public enum EntityPacketType {
    UpdateUsingPlayer(PacketGroup.GameEntity),
    SyncGameEntity(PacketGroup.GameEntity),
    RequestSyncGameEntity(PacketGroup.GameEntity),
    CraftLogicSync(PacketGroup.CraftLogic),
    CraftLogicSyncFull(PacketGroup.CraftLogic),
    CraftLogicStartRequest(PacketGroup.CraftLogic),
    CraftLogicStopRequest(PacketGroup.CraftLogic),
    MashingLogicSync(PacketGroup.MashingLogic),
    MashingLogicSyncFull(PacketGroup.MashingLogic),
    MashingLogicStartRequest(PacketGroup.MashingLogic),
    MashingLogicStopRequest(PacketGroup.MashingLogic),
    ResourcesSync(PacketGroup.Resources);

    private static final Map<Short, EntityPacketType> entityPacketMap;
    private short id;
    private final PacketGroup group;

    private EntityPacketType() {
        this.group = PacketGroup.Generic;
    }

    private EntityPacketType(PacketGroup group) {
        this.group = group;
    }

    private EntityPacketType(short id, PacketGroup group) {
        this.group = group;
        this.id = id;
    }

    public PacketGroup getGroup() {
        return this.group;
    }

    public boolean isEntityPacket() {
        return this.group == PacketGroup.GameEntity;
    }

    public boolean isComponentPacket() {
        return this.group != PacketGroup.GameEntity;
    }

    public void saveToByteBuffer(ByteBufferWriter bb) {
        bb.putShort(this.id);
    }

    public void saveToByteBuffer(ByteBuffer bb) {
        bb.putShort(this.id);
    }

    public static EntityPacketType FromByteBuffer(ByteBufferReader bb) {
        short id = bb.getShort();
        return entityPacketMap.get(id);
    }

    static {
        entityPacketMap = new HashMap<Short, EntityPacketType>();
        int nextID = 1000;
        for (EntityPacketType entityPacketType : EntityPacketType.values()) {
            if (entityPacketType.id == 0) {
                int n = nextID;
                nextID = (short)(nextID + 1);
                entityPacketType.id = (short)n;
            }
            entityPacketMap.put(entityPacketType.id, entityPacketType);
        }
    }
}

