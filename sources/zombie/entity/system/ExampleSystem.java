/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.system;

import java.util.Objects;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.IBucketListener;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.util.ImmutableArray;

public class ExampleSystem
extends EngineSystem {
    EntityBucket craftEntities;
    EntityBucket nonMetaEntities;
    int exampleCounter;

    public ExampleSystem(int updatePriority) {
        super(false, true, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.craftEntities = engine.getBucket(Family.all(ComponentType.CraftLogic).get());
        this.nonMetaEntities = engine.getCustomBucket("NonMetaEntities");
        this.nonMetaEntities.addListener(0, new CraftEntityBucketListener(this));
    }

    @Override
    public void update() {
        ImmutableArray<GameEntity> entities = this.nonMetaEntities.getEntities();
        for (int i = 0; i < entities.size(); ++i) {
            GameEntity gameEntity = entities.get(i);
        }
    }

    @Override
    public void updateSimulation() {
        ImmutableArray<GameEntity> entities = this.craftEntities.getEntities();
        for (int i = 0; i < entities.size(); ++i) {
            GameEntity entity = entities.get(i);
            CraftLogic craftLogic = (CraftLogic)entity.getComponent(ComponentType.CraftLogic);
        }
    }

    private class CraftEntityBucketListener
    implements IBucketListener {
        final /* synthetic */ ExampleSystem this$0;

        private CraftEntityBucketListener(ExampleSystem exampleSystem) {
            ExampleSystem exampleSystem2 = exampleSystem;
            Objects.requireNonNull(exampleSystem2);
            this.this$0 = exampleSystem2;
        }

        @Override
        public void onBucketEntityAdded(EntityBucket bucket, GameEntity entity) {
            this.this$0.exampleCounter += 100;
        }

        @Override
        public void onBucketEntityRemoved(EntityBucket bucket, GameEntity entity) {
            this.this$0.exampleCounter -= 100;
        }
    }
}

