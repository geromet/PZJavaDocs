/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

public final class LimitSizeFileOutputStream
extends FilterOutputStream {
    private final File file;
    private final int maxKilobytes;
    private long bytesWritten;

    public LimitSizeFileOutputStream(File file, int maxKilobytes) throws FileNotFoundException {
        super(new FileOutputStream(file, false));
        this.file = file;
        this.maxKilobytes = maxKilobytes;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(int b) throws IOException {
        LimitSizeFileOutputStream limitSizeFileOutputStream = this;
        synchronized (limitSizeFileOutputStream) {
            this.ensureCapacity(1);
            this.out.write(b);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(byte[] b) throws IOException {
        LimitSizeFileOutputStream limitSizeFileOutputStream = this;
        synchronized (limitSizeFileOutputStream) {
            this.ensureCapacity(b.length);
            this.out.write(b);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        LimitSizeFileOutputStream limitSizeFileOutputStream = this;
        synchronized (limitSizeFileOutputStream) {
            this.ensureCapacity(len);
            this.out.write(b, off, len);
        }
    }

    private void ensureCapacity(int len) throws IOException {
        long newBytesWritten = this.bytesWritten + (long)len;
        if (newBytesWritten > (long)this.maxKilobytes * 1024L) {
            this.out.flush();
            this.out.close();
            this.out = new FileOutputStream(this.file, false);
            newBytesWritten = len;
        }
        this.bytesWritten = newBytesWritten;
    }
}

