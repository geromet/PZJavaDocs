/*
 * Decompiled with CFR 0.152.
 */
package zombie.fileSystem;

import java.io.InputStream;
import zombie.fileSystem.FileSeekMode;
import zombie.fileSystem.IFileDevice;

public interface IFile {
    public boolean open(String var1, int var2);

    public void close();

    public boolean read(byte[] var1, long var2);

    public boolean write(byte[] var1, long var2);

    public byte[] getBuffer();

    public long size();

    public boolean seek(FileSeekMode var1, long var2);

    public long pos();

    public InputStream getInputStream();

    public IFileDevice getDevice();

    public void release();
}

