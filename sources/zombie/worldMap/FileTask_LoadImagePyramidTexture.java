/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import zombie.core.textures.ImageData;
import zombie.core.utils.DirectBufferAllocator;
import zombie.debug.DebugOptions;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;
import zombie.worldMap.ImagePyramid;

class FileTask_LoadImagePyramidTexture
extends FileTask {
    ImagePyramid pyramid;
    String key;
    Path path;
    ImageData imageData;
    int asyncOp = -1;
    boolean cancelled;

    public FileTask_LoadImagePyramidTexture(ImagePyramid pyramid, Path path, String key, FileSystem fs, IFileTaskCallback cb) {
        super(fs, cb);
        this.setPriority(10);
        this.pyramid = pyramid;
        this.path = path;
        this.key = key;
    }

    @Override
    public String getErrorMessage() {
        return "???";
    }

    @Override
    public void done() {
        if (this.cancelled) {
            this.pyramid.onFileTaskCancelled(this);
        } else {
            this.pyramid.onFileTaskFinished(this);
        }
    }

    @Override
    public Object call() throws Exception {
        DirectBufferAllocator.getBytesAllocated();
        if (this.cancelled) {
            return this;
        }
        if (DebugOptions.instance.asset.slowLoad.getValue()) {
            Thread.sleep(250L);
        }
        try (InputStream is = Files.newInputStream(this.path, new OpenOption[0]);){
            this.imageData = new ImageData(is, false);
            this.pyramid.onFileTaskCalled(this);
        }
        catch (NoSuchFileException noSuchFileException) {
            // empty catch block
        }
        return this;
    }
}

