/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.core.skinnedmodel.model.Model;

public final class ModelAssetManager
extends AssetManager {
    public static final ModelAssetManager instance = new ModelAssetManager();

    @Override
    protected void startLoading(Asset asset) {
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new Model(path, this, (Model.ModelAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}

