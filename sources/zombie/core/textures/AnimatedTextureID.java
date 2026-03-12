/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import java.util.ArrayList;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.textures.AnimatedTextureIDFrame;
import zombie.core.textures.ImageData;
import zombie.core.textures.ImageDataFrame;
import zombie.core.textures.TextureID;

public class AnimatedTextureID
extends Asset {
    public static final AssetType ASSET_TYPE = new AssetType("AnimatedTextureID");
    private int width;
    private int height;
    public final ArrayList<AnimatedTextureIDFrame> frames = new ArrayList();
    public AnimatedTextureIDAssetParams assetParams;

    protected AnimatedTextureID(AssetPath path, AssetManager manager, AnimatedTextureIDAssetParams params) {
        super(path, manager);
        this.assetParams = params;
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    public void setImageData(ImageData data) {
        this.width = data.getWidth();
        this.height = data.getHeight();
        for (int i = 0; i < data.frames.size(); ++i) {
            ImageDataFrame frame = data.frames.get(i);
            ImageData imageData = new ImageData(frame);
            frame.data = null;
            TextureID textureID1 = new TextureID(imageData);
            AnimatedTextureIDFrame frame1 = new AnimatedTextureIDFrame();
            frame1.textureId = textureID1;
            frame1.apngFrame = frame.apngFrame;
            this.frames.add(frame1);
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getFrameCount() {
        return this.frames.size();
    }

    public AnimatedTextureIDFrame getFrame(int index) {
        if (index < 0 || index >= this.frames.size()) {
            return null;
        }
        return this.frames.get(index);
    }

    public boolean isDestroyed() {
        return false;
    }

    public void destroy() {
    }

    public static final class AnimatedTextureIDAssetParams
    extends AssetManager.AssetParams {
        int flags = 0;
    }
}

