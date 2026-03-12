/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;

public class AnimNodeAsset
extends Asset {
    public static final AssetType ASSET_TYPE = new AssetType("AnimNode");
    public AnimNode animNode;

    protected AnimNodeAsset(AssetPath path, AssetManager manager) {
        super(path, manager);
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }
}

