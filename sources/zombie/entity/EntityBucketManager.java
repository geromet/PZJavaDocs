/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.util.Objects;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.IBucketInformer;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectMap;

public final class EntityBucketManager {
    private final ObjectMap<Family, EntityBucket> buckets = new ObjectMap();
    private final ObjectMap<String, EntityBucket> customBuckets = new ObjectMap();
    private final Array<EntityBucket> bucketsArray = new Array(false, 16);
    private final EntityBucket.RendererBucket rendererBucket;
    private EntityBucket.IsoObjectBucket isoObjectBucket;
    private EntityBucket.InventoryItemBucket inventoryItemBucket;
    private EntityBucket.VehiclePartBucket vehiclePartBucket;
    private int bucketIndex;
    private final ImmutableArray<GameEntity> entities;
    private boolean updating;
    private GameEntity currentUpdatingEntity;
    private final BucketsUpdatingInformer bucketsUpdatingInformer = new BucketsUpdatingInformer(this);

    protected EntityBucketManager(ImmutableArray<GameEntity> entities) {
        this.entities = entities;
        this.rendererBucket = new EntityBucket.RendererBucket(this.bucketIndex++);
        this.bucketsArray.add(this.rendererBucket);
    }

    BucketsUpdatingInformer getBucketsUpdatingInformer() {
        return this.bucketsUpdatingInformer;
    }

    EntityBucket getRendererBucket() {
        return this.rendererBucket;
    }

    EntityBucket getIsoObjectBucket() {
        if (this.isoObjectBucket == null) {
            this.isoObjectBucket = new EntityBucket.IsoObjectBucket(this.bucketIndex++);
            this.bucketsArray.add(this.isoObjectBucket);
            for (int i = 0; i < this.entities.size(); ++i) {
                this.isoObjectBucket.updateMembership(this.entities.get(i));
            }
        }
        return this.isoObjectBucket;
    }

    EntityBucket getInventoryItemBucket() {
        if (this.inventoryItemBucket == null) {
            this.inventoryItemBucket = new EntityBucket.InventoryItemBucket(this.bucketIndex++);
            this.bucketsArray.add(this.inventoryItemBucket);
            for (int i = 0; i < this.entities.size(); ++i) {
                this.inventoryItemBucket.updateMembership(this.entities.get(i));
            }
        }
        return this.inventoryItemBucket;
    }

    EntityBucket getVehiclePartBucket() {
        if (this.vehiclePartBucket == null) {
            this.vehiclePartBucket = new EntityBucket.VehiclePartBucket(this.bucketIndex++);
            this.bucketsArray.add(this.vehiclePartBucket);
            for (int i = 0; i < this.entities.size(); ++i) {
                this.vehiclePartBucket.updateMembership(this.entities.get(i));
            }
        }
        return this.vehiclePartBucket;
    }

    EntityBucket getBucket(Family family) {
        EntityBucket bucket = this.buckets.get(family);
        if (bucket == null) {
            bucket = new EntityBucket.FamilyBucket(this.bucketIndex++, family);
            this.buckets.put(family, bucket);
            this.bucketsArray.add(bucket);
            for (int i = 0; i < this.entities.size(); ++i) {
                bucket.updateMembership(this.entities.get(i));
            }
        }
        return bucket;
    }

    EntityBucket registerCustomBucket(String identifier, EntityBucket.EntityValidator validator) {
        if (this.customBuckets.get(identifier) != null) {
            throw new IllegalArgumentException("Bucket with identifier '" + identifier + "' already exists.");
        }
        EntityBucket.CustomBucket bucket = new EntityBucket.CustomBucket(this.bucketIndex++, validator);
        this.customBuckets.put(identifier, bucket);
        this.bucketsArray.add(bucket);
        for (int i = 0; i < this.entities.size(); ++i) {
            bucket.updateMembership(this.entities.get(i));
        }
        return bucket;
    }

    EntityBucket getCustomBucket(String identifier) {
        return this.customBuckets.get(identifier);
    }

    void updateBucketMembership(GameEntity entity) {
        this.updating = true;
        this.currentUpdatingEntity = entity;
        try {
            if (this.bucketsArray.size > 0) {
                for (int i = 0; i < this.bucketsArray.size; ++i) {
                    this.bucketsArray.get(i).updateMembership(entity);
                }
            }
        }
        finally {
            this.updating = false;
            this.currentUpdatingEntity = null;
        }
    }

    protected class BucketsUpdatingInformer
    implements IBucketInformer {
        final /* synthetic */ EntityBucketManager this$0;

        protected BucketsUpdatingInformer(EntityBucketManager this$0) {
            EntityBucketManager entityBucketManager = this$0;
            Objects.requireNonNull(entityBucketManager);
            this.this$0 = entityBucketManager;
        }

        @Override
        public boolean value() {
            return this.this$0.updating;
        }

        @Override
        public GameEntity updatingEntity() {
            return this.this$0.currentUpdatingEntity;
        }
    }
}

