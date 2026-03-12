/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import zombie.core.textures.ImageData;
import zombie.core.textures.TextureIDAssetManager;
import zombie.debug.DebugOptions;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

public final class FileTask_LoadImageData
extends FileTask {
    String imageName;
    boolean mask;

    public FileTask_LoadImageData(String imageName, FileSystem fs, IFileTaskCallback cb) {
        super(fs, cb);
        this.imageName = imageName;
    }

    @Override
    public String getErrorMessage() {
        return this.imageName;
    }

    @Override
    public void done() {
    }

    @Override
    public Object call() throws Exception {
        TextureIDAssetManager.instance.waitFileTask();
        if (DebugOptions.instance.asset.slowLoad.getValue()) {
            try {
                Thread.sleep(500L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
        try (FileInputStream input = new FileInputStream(this.imageName);){
            ImageData imageData;
            try (BufferedInputStream bis = new BufferedInputStream(input);){
                imageData = new ImageData(bis, this.mask);
            }
            return imageData;
        }
    }
}

