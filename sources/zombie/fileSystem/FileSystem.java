/*
 * Decompiled with CFR 0.152.
 */
package zombie.fileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import zombie.core.textures.TexturePackPage;
import zombie.fileSystem.DeviceList;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFile;
import zombie.fileSystem.IFileDevice;
import zombie.fileSystem.IFileTask2Callback;

public abstract class FileSystem {
    public static final int INVALID_ASYNC = -1;

    public abstract boolean mount(IFileDevice var1);

    public abstract boolean unMount(IFileDevice var1);

    public abstract IFile open(DeviceList var1, String var2, int var3);

    public abstract void close(IFile var1);

    public abstract int openAsync(DeviceList var1, String var2, int var3, IFileTask2Callback var4);

    public abstract void closeAsync(IFile var1, IFileTask2Callback var2);

    public abstract void cancelAsync(int var1);

    public abstract InputStream openStream(DeviceList var1, String var2) throws IOException;

    public abstract void closeStream(InputStream var1);

    public abstract int runAsync(FileTask var1);

    public abstract void updateAsyncTransactions();

    public abstract boolean hasWork();

    public abstract DeviceList getDefaultDevice();

    public abstract void mountTexturePack(String var1, TexturePackTextures var2, int var3);

    public abstract DeviceList getTexturePackDevice(String var1);

    public abstract int getTexturePackFlags(String var1);

    public abstract boolean getTexturePackAlpha(String var1, String var2);

    public static final class TexturePackTextures
    extends HashMap<String, SubTexture> {
    }

    public static final class SubTexture {
        public String packName;
        public String pageName;
        public TexturePackPage.SubTextureInfo info;

        public SubTexture(String packName, String pageName, TexturePackPage.SubTextureInfo info) {
            this.packName = packName;
            this.pageName = pageName;
            this.info = info;
        }
    }
}

