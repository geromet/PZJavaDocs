/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig;

import zombie.scripting.itemConfig.SelectorBucket;
import zombie.scripting.itemConfig.enums.RootType;

public class BucketRoot {
    private final RootType type;
    private final String id;
    private SelectorBucket bucketSpawn;
    private SelectorBucket bucketOnCreate;

    public BucketRoot(RootType type, String id) {
        this.type = type;
        this.id = id;
    }

    public RootType getType() {
        return this.type;
    }

    protected String getId() {
        return this.id;
    }

    protected void setBucketSpawn(SelectorBucket bucket) {
        this.bucketSpawn = bucket;
    }

    public SelectorBucket getBucketSpawn() {
        return this.bucketSpawn;
    }

    protected void setBucketOnCreate(SelectorBucket bucket) {
        this.bucketOnCreate = bucket;
    }

    public SelectorBucket getBucketOnCreate() {
        return this.bucketOnCreate;
    }
}

