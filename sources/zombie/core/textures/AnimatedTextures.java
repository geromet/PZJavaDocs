/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.core.textures.AnimatedTexture;
import zombie.core.textures.AnimatedTextureID;
import zombie.core.textures.AnimatedTextureIDAssetManager;

public final class AnimatedTextures {
    private static final HashMap<String, AnimatedTexture> textures = new HashMap();

    public static AnimatedTexture getTexture(String relativePath) {
        String path = ZomboidFileSystem.instance.getString(relativePath);
        if (textures.containsKey(path)) {
            return textures.get(path);
        }
        AssetManager.AssetParams params = null;
        AnimatedTextureID asset = (AnimatedTextureID)AnimatedTextureIDAssetManager.instance.load(new AssetPath(path), params);
        AnimatedTexture animatedTexture = new AnimatedTexture(asset);
        textures.put(path, animatedTexture);
        return animatedTexture;
    }
}

