/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ByteBufferBackedInputStream
extends InputStream {
    final ByteBuffer buf;

    public ByteBufferBackedInputStream(ByteBuffer buf) {
        Objects.requireNonNull(buf);
        this.buf = buf;
    }

    @Override
    public int read() throws IOException {
        if (!this.buf.hasRemaining()) {
            return -1;
        }
        return this.buf.get() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!this.buf.hasRemaining()) {
            return -1;
        }
        len = Math.min(len, this.buf.remaining());
        this.buf.get(bytes, off, len);
        return len;
    }
}

