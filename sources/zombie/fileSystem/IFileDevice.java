/*
 * Decompiled with CFR 0.152.
 */
package zombie.fileSystem;

import java.io.IOException;
import java.io.InputStream;
import zombie.fileSystem.IFile;

public interface IFileDevice {
    public IFile createFile(IFile var1);

    public void destroyFile(IFile var1);

    public InputStream createStream(String var1, InputStream var2) throws IOException;

    public void destroyStream(InputStream var1);

    public String name();
}

