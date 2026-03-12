/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.entity.GameEntity;
import zombie.entity.IBooleanInformer;
import zombie.entity.IBucketInformer;
import zombie.entity.util.Array;
import zombie.entity.util.SingleThreadPool;

public final class ComponentOperationHandler {
    private final OperationListener operationListener;
    private final IBooleanInformer delayed;
    private final IBucketInformer bucketsUpdating;
    private final ComponentOperationPool operationPool = new ComponentOperationPool();
    private final Array<ComponentOperation> operations = new Array();

    protected ComponentOperationHandler(IBooleanInformer delayed, IBucketInformer bucketsUpdating, OperationListener listener) {
        this.delayed = delayed;
        this.bucketsUpdating = bucketsUpdating;
        this.operationListener = listener;
    }

    void add(GameEntity entity) {
        if (this.bucketsUpdating.value()) {
            throw new IllegalStateException("Cannot perform component operation when buckets are updating.");
        }
        if (this.delayed.value()) {
            if (entity.scheduledForBucketUpdate) {
                return;
            }
            entity.scheduledForBucketUpdate = true;
            ComponentOperation operation = (ComponentOperation)this.operationPool.obtain();
            operation.make(entity);
            this.operations.add(operation);
        } else {
            this.operationListener.componentsChanged(entity);
        }
    }

    void remove(GameEntity entity) {
        if (this.bucketsUpdating.value()) {
            throw new IllegalStateException("Cannot perform component operation when buckets are updating.");
        }
        if (this.delayed.value()) {
            if (entity.scheduledForBucketUpdate) {
                return;
            }
            entity.scheduledForBucketUpdate = true;
            ComponentOperation operation = (ComponentOperation)this.operationPool.obtain();
            operation.make(entity);
            this.operations.add(operation);
        } else {
            this.operationListener.componentsChanged(entity);
        }
    }

    boolean hasOperationsToProcess() {
        return this.operations.size > 0;
    }

    void processOperations() {
        for (int i = 0; i < this.operations.size; ++i) {
            ComponentOperation operation = this.operations.get(i);
            this.operationListener.componentsChanged(operation.entity);
            operation.entity.scheduledForBucketUpdate = false;
            this.operationPool.free(operation);
        }
        this.operations.clear();
    }

    private static class ComponentOperationPool
    extends SingleThreadPool<ComponentOperation> {
        private ComponentOperationPool() {
        }

        @Override
        protected ComponentOperation newObject() {
            return new ComponentOperation();
        }
    }

    static interface OperationListener {
        public void componentsChanged(GameEntity var1);
    }

    private static class ComponentOperation
    implements SingleThreadPool.Poolable {
        public GameEntity entity;

        private ComponentOperation() {
        }

        public void make(GameEntity entity) {
            this.entity = entity;
        }

        @Override
        public void reset() {
            this.entity = null;
        }
    }
}

