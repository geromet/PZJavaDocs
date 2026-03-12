/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.core.textures.Texture;

public final class TextureAssetManager
extends AssetManager {
    public static final TextureAssetManager instance = new TextureAssetManager();

    @Override
    protected void startLoading(Asset asset) {
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new Texture(path, this, (Texture.TextureAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}

