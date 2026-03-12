/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.bucket;

import zombie.core.bucket.Bucket;

public final class BucketManager {
    static final Bucket SharedBucket = new Bucket();

    public static Bucket Active() {
        return SharedBucket;
    }

    public static Bucket Shared() {
        return SharedBucket;
    }
}

