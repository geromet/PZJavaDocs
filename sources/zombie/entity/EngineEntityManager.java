/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.util.Objects;
import zombie.core.Core;
import zombie.entity.ComponentOperationHandler;
import zombie.entity.Engine;
import zombie.entity.EntityBucketManager;
import zombie.entity.GameEntity;
import zombie.entity.IBooleanInformer;
import zombie.entity.IBucketInformer;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectSet;
import zombie.entity.util.SingleThreadPool;

public final class EngineEntityManager {
    private final EntityBucketManager bucketManager;
    private final Array<GameEntity> entities = new Array(false, 16);
    private final ObjectSet<GameEntity> entitySet = new ObjectSet();
    private final ImmutableArray<GameEntity> immutableEntities = new ImmutableArray<GameEntity>(this.entities);
    private final Array<EntityOperation> pendingOperations = new Array(false, 16);
    private final EntityOperationPool entityOperationPool = new EntityOperationPool();
    private final ComponentOperationHandler componentOperationHandler;
    private final IBooleanInformer delayed;
    private final IBucketInformer bucketsUpdating;
    private final Engine engine;

    protected EngineEntityManager(Engine engine, IBooleanInformer delayed) {
        this.engine = engine;
        this.bucketManager = new EntityBucketManager(this.immutableEntities);
        this.bucketsUpdating = this.bucketManager.getBucketsUpdatingInformer();
        this.delayed = delayed;
        this.componentOperationHandler = new ComponentOperationHandler(this.delayed, this.bucketsUpdating, new ComponentOperationListener(this));
    }

    EntityBucketManager getBucketManager() {
        return this.bucketManager;
    }

    void addEntity(GameEntity entity) {
        if (this.delayed.value() || this.bucketsUpdating.value()) {
            if (entity.scheduledForEngineRemoval || entity.removingFromEngine) {
                throw new IllegalArgumentException("Entity is scheduled for removal.");
            }
            if (entity.addedToEngine) {
                if (Core.debug) {
                    throw new IllegalArgumentException("Entity has already been added to Engine.");
                }
                return;
            }
            entity.addedToEngine = true;
            entity.scheduledDelayedAddToEngine = true;
            EntityOperation operation = (EntityOperation)this.entityOperationPool.obtain();
            operation.entity = entity;
            operation.type = EntityOperation.Type.Add;
            this.pendingOperations.add(operation);
        } else {
            this.addEntityInternal(entity);
        }
    }

    void removeEntity(GameEntity entity) {
        if (this.delayed.value() || this.bucketsUpdating.value()) {
            if (entity.scheduledForEngineRemoval) {
                return;
            }
            entity.scheduledForEngineRemoval = true;
            EntityOperation operation = (EntityOperation)this.entityOperationPool.obtain();
            operation.entity = entity;
            operation.type = EntityOperation.Type.Remove;
            this.pendingOperations.add(operation);
        } else {
            this.removeEntityInternal(entity);
        }
    }

    void removeAllEntities() {
        this.removeAllEntities(this.immutableEntities);
    }

    void removeAllEntities(ImmutableArray<GameEntity> entities) {
        if (this.delayed.value() || this.bucketsUpdating.value()) {
            for (GameEntity entity : entities) {
                entity.scheduledForEngineRemoval = true;
            }
            EntityOperation operation = (EntityOperation)this.entityOperationPool.obtain();
            operation.type = EntityOperation.Type.RemoveAll;
            operation.entities = entities;
            this.pendingOperations.add(operation);
        } else {
            while (entities.size() > 0) {
                this.removeEntityInternal(entities.first());
            }
        }
    }

    ImmutableArray<GameEntity> getEntities() {
        return this.immutableEntities;
    }

    boolean hasPendingOperations() {
        return this.pendingOperations.size > 0;
    }

    void processPendingOperations() {
        for (int i = 0; i < this.pendingOperations.size; ++i) {
            EntityOperation operation = this.pendingOperations.get(i);
            switch (operation.type.ordinal()) {
                case 0: {
                    this.addEntityInternal(operation.entity);
                    break;
                }
                case 1: {
                    this.removeEntityInternal(operation.entity);
                    break;
                }
                case 2: {
                    while (operation.entities.size() > 0) {
                        this.removeEntityInternal(operation.entities.first());
                    }
                    break;
                }
                default: {
                    throw new AssertionError((Object)"Unexpected EntityOperation type");
                }
            }
            this.entityOperationPool.free(operation);
        }
        this.pendingOperations.clear();
    }

    void updateOperations() {
        while (this.componentOperationHandler.hasOperationsToProcess() || this.hasPendingOperations()) {
            this.componentOperationHandler.processOperations();
            this.processPendingOperations();
        }
    }

    void addEntityInternal(GameEntity entity) {
        if (this.entitySet.contains(entity)) {
            throw new IllegalArgumentException("Entity is already registered " + String.valueOf(entity));
        }
        entity.scheduledDelayedAddToEngine = false;
        this.entities.add(entity);
        this.entitySet.add(entity);
        entity.setComponentOperationHandler(this.componentOperationHandler);
        entity.addedToEngine = true;
        this.bucketManager.updateBucketMembership(entity);
        this.engine.onEntityAdded(entity);
    }

    void removeEntityInternal(GameEntity entity) {
        boolean removed = this.entitySet.remove(entity);
        if (removed) {
            entity.scheduledForEngineRemoval = false;
            entity.removingFromEngine = true;
            this.entities.removeValue(entity, true);
            this.bucketManager.updateBucketMembership(entity);
            entity.setComponentOperationHandler(null);
            entity.removingFromEngine = false;
            entity.addedToEngine = false;
            this.engine.onEntityRemoved(entity);
        }
    }

    private static class EntityOperationPool
    extends SingleThreadPool<EntityOperation> {
        private EntityOperationPool() {
        }

        @Override
        protected EntityOperation newObject() {
            return new EntityOperation();
        }
    }

    private class ComponentOperationListener
    implements ComponentOperationHandler.OperationListener {
        final /* synthetic */ EngineEntityManager this$0;

        private ComponentOperationListener(EngineEntityManager engineEntityManager) {
            EngineEntityManager engineEntityManager2 = engineEntityManager;
            Objects.requireNonNull(engineEntityManager2);
            this.this$0 = engineEntityManager2;
        }

        @Override
        public void componentsChanged(GameEntity entity) {
            this.this$0.bucketManager.updateBucketMembership(entity);
        }
    }

    private static class EntityOperation
    implements SingleThreadPool.Poolable {
        Type type;
        GameEntity entity;
        ImmutableArray<GameEntity> entities;

        private EntityOperation() {
        }

        @Override
        public void reset() {
            this.entity = null;
        }

        public static enum Type {
            Add,
            Remove,
            RemoveAll;

        }
    }
}

