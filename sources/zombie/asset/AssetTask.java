/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import zombie.asset.Asset;

public abstract class AssetTask {
    public Asset asset;

    public AssetTask(Asset asset) {
        this.asset = asset;
    }

    public abstract void execute();

    public abstract void cancel();
}

