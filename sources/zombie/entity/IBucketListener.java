/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.entity.EntityBucket;
import zombie.entity.GameEntity;

public interface IBucketListener {
    public void onBucketEntityAdded(EntityBucket var1, GameEntity var2);

    public void onBucketEntityRemoved(EntityBucket var1, GameEntity var2);
}

