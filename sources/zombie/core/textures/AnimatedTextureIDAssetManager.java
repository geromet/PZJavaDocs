/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.asset.FileTask_LoadImageData;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.AnimatedTextureID;
import zombie.core.textures.ImageData;
import zombie.fileSystem.FileSystem;

public final class AnimatedTextureIDAssetManager
extends AssetManager {
    public static final AnimatedTextureIDAssetManager instance = new AnimatedTextureIDAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        AnimatedTextureID textureID = (AnimatedTextureID)asset;
        FileSystem fs = this.getOwner().getFileSystem();
        FileTask_LoadImageData fileTask = new FileTask_LoadImageData(asset.getPath().getPath(), fs, result -> this.onFileTaskFinished(asset, result));
        fileTask.setPriority(7);
        AssetTask_RunFileTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        ((AssetTask)assetTask).execute();
    }

    @Override
    protected void unloadData(Asset asset) {
        AnimatedTextureID textureID = (AnimatedTextureID)asset;
        if (textureID.isDestroyed()) {
            return;
        }
        RenderThread.invokeOnRenderContext(textureID::destroy);
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new AnimatedTextureID(path, this, (AnimatedTextureID.AnimatedTextureIDAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    private void onFileTaskFinished(Asset asset, Object result) {
        AnimatedTextureID textureID = (AnimatedTextureID)asset;
        if (result instanceof ImageData) {
            ImageData imageData = (ImageData)result;
            textureID.setImageData(imageData);
            this.onLoadingSucceeded(asset);
        } else {
            this.onLoadingFailed(asset);
        }
    }
}

