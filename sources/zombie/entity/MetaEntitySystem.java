/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.debug.DebugLog;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.MetaEntity;
import zombie.entity.util.ImmutableArray;

public class MetaEntitySystem
extends EngineSystem {
    EntityBucket metaEntities;

    public MetaEntitySystem(int updatePriority) {
        super(false, false, Integer.MAX_VALUE, false, Integer.MAX_VALUE);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.metaEntities = engine.getCustomBucket("MetaEntities");
    }

    ByteBuffer saveMetaEntities(ByteBuffer output) throws IOException {
        int sizeEstimate = this.metaEntities.getEntities().size() * 1024;
        output = GameEntityManager.ensureCapacity(output, output.position() + sizeEstimate);
        ImmutableArray<GameEntity> entities = this.metaEntities.getEntities();
        output.putInt(entities.size());
        DebugLog.Entity.println("Saving meta entities, size = " + entities.size());
        for (int i = 0; i < entities.size(); ++i) {
            GameEntity entity = entities.get(i);
            if (!(entity instanceof MetaEntity)) {
                throw new IOException("Expected MetaEntity");
            }
            MetaEntity metaEntity = (MetaEntity)entity;
            metaEntity.saveMetaEntity(output);
            if (output.position() <= output.capacity() - 0x100000) continue;
            output = GameEntityManager.ensureCapacity(output, output.capacity() + 0x100000);
        }
        return output;
    }

    void loadMetaEntities(ByteBuffer input, int worldVersion) throws IOException {
        int storedSize = input.getInt();
        DebugLog.Entity.println("Loading meta entities, size = " + storedSize);
        for (int i = 0; i < storedSize; ++i) {
            MetaEntity metaEntity = MetaEntity.alloc();
            metaEntity.loadMetaEntity(input, worldVersion);
            GameEntityManager.RegisterEntity(metaEntity);
        }
    }
}

