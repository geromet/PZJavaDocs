/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import zombie.asset.Asset;
import zombie.asset.AssetTask;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.IFile;
import zombie.fileSystem.IFileTask2Callback;

final class AssetTask_LoadFromFileAsync
extends AssetTask
implements IFileTask2Callback {
    int asyncOp = -1;
    boolean stream;

    AssetTask_LoadFromFileAsync(Asset asset, boolean stream) {
        super(asset);
        this.stream = stream;
    }

    @Override
    public void execute() {
        FileSystem fs = this.asset.getAssetManager().getOwner().getFileSystem();
        int mode = 4 | (this.stream ? 16 : 1);
        this.asyncOp = fs.openAsync(fs.getDefaultDevice(), this.asset.getPath().path, mode, this);
    }

    @Override
    public void cancel() {
        FileSystem fs = this.asset.getAssetManager().getOwner().getFileSystem();
        fs.cancelAsync(this.asyncOp);
        this.asyncOp = -1;
    }

    @Override
    public void onFileTaskFinished(IFile file, Object result) {
        this.asyncOp = -1;
        if (this.asset.priv.desiredState != Asset.State.READY) {
            return;
        }
        if (result != Boolean.TRUE) {
            this.asset.priv.onLoadingFailed();
            return;
        }
        if (!this.asset.getAssetManager().loadDataFromFile(this.asset, file)) {
            this.asset.priv.onLoadingFailed();
            return;
        }
        this.asset.priv.onLoadingSucceeded();
    }
}

