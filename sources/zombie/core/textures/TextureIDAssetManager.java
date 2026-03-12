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
import zombie.asset.FileTask_LoadPackImage;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.ImageData;
import zombie.core.textures.TextureID;
import zombie.core.utils.DirectBufferAllocator;
import zombie.fileSystem.FileSystem;

public final class TextureIDAssetManager
extends AssetManager {
    public static final TextureIDAssetManager instance = new TextureIDAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        TextureID textureID = (TextureID)asset;
        FileSystem fs = this.getOwner().getFileSystem();
        if (textureID.assetParams != null && textureID.assetParams.subTexture != null) {
            FileSystem.SubTexture subTex = textureID.assetParams.subTexture;
            FileTask_LoadPackImage fileTask = new FileTask_LoadPackImage(subTex.packName, subTex.pageName, fs, result -> this.onFileTaskFinished(asset, result));
            fileTask.setPriority(7);
            AssetTask_RunFileTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
            this.setTask(asset, assetTask);
            ((AssetTask)assetTask).execute();
        } else {
            FileTask_LoadImageData fileTask = new FileTask_LoadImageData(asset.getPath().getPath(), fs, result -> this.onFileTaskFinished(asset, result));
            fileTask.setPriority(7);
            AssetTask_RunFileTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
            this.setTask(asset, assetTask);
            ((AssetTask)assetTask).execute();
        }
    }

    @Override
    protected void unloadData(Asset asset) {
        TextureID textureID = (TextureID)asset;
        if (textureID.isDestroyed()) {
            return;
        }
        RenderThread.invokeOnRenderContext(textureID::destroy);
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new TextureID(path, this, (TextureID.TextureIDAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    private void onFileTaskFinished(Asset asset, Object result) {
        TextureID textureID = (TextureID)asset;
        if (result instanceof ImageData) {
            ImageData imageData = (ImageData)result;
            textureID.setImageData(imageData);
            this.onLoadingSucceeded(asset);
        } else {
            this.onLoadingFailed(asset);
        }
    }

    public void waitFileTask() {
        while (DirectBufferAllocator.getBytesAllocated() > 0x3200000L) {
            try {
                Thread.sleep(20L);
            }
            catch (InterruptedException interruptedException) {}
        }
    }
}

