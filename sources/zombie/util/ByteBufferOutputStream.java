/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream
extends OutputStream {
    private ByteBuffer wrappedBuffer;
    private final boolean autoEnlarge;

    public ByteBufferOutputStream(ByteBuffer wrappedBuffer, boolean autoEnlarge) {
        this.wrappedBuffer = wrappedBuffer;
        this.autoEnlarge = autoEnlarge;
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer byteBuffer = this.wrappedBuffer.duplicate();
        byteBuffer.flip();
        return byteBuffer.asReadOnlyBuffer();
    }

    public ByteBuffer getWrappedBuffer() {
        return this.wrappedBuffer;
    }

    public void clear() {
        this.wrappedBuffer.clear();
    }

    public void flip() {
        this.wrappedBuffer.flip();
    }

    private void growTo(int minCapacity) {
        int oldCapacity = this.wrappedBuffer.capacity();
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity < 0) {
            if (minCapacity < 0) {
                throw new OutOfMemoryError();
            }
            newCapacity = Integer.MAX_VALUE;
        }
        ByteBuffer oldWrappedBuffer = this.wrappedBuffer;
        this.wrappedBuffer = this.wrappedBuffer.isDirect() ? ByteBuffer.allocateDirect(newCapacity) : ByteBuffer.allocate(newCapacity);
        oldWrappedBuffer.flip();
        this.wrappedBuffer.put(oldWrappedBuffer);
    }

    @Override
    public void write(int bty) {
        try {
            this.wrappedBuffer.put((byte)bty);
        }
        catch (BufferOverflowException ex) {
            if (this.autoEnlarge) {
                int newBufferSize = this.wrappedBuffer.capacity() * 2;
                this.growTo(newBufferSize);
                this.write(bty);
            }
            throw ex;
        }
    }

    @Override
    public void write(byte[] bytes) {
        int oldPosition = 0;
        try {
            oldPosition = this.wrappedBuffer.position();
            this.wrappedBuffer.put(bytes);
        }
        catch (BufferOverflowException ex) {
            if (this.autoEnlarge) {
                int newBufferSize = Math.max(this.wrappedBuffer.capacity() * 2, oldPosition + bytes.length);
                this.growTo(newBufferSize);
                this.write(bytes);
            }
            throw ex;
        }
    }

    @Override
    public void write(byte[] bytes, int off, int len) {
        int oldPosition = 0;
        try {
            oldPosition = this.wrappedBuffer.position();
            this.wrappedBuffer.put(bytes, off, len);
        }
        catch (BufferOverflowException ex) {
            if (this.autoEnlarge) {
                int newBufferSize = Math.max(this.wrappedBuffer.capacity() * 2, oldPosition + len);
                this.growTo(newBufferSize);
                this.write(bytes, off, len);
            }
            throw ex;
        }
    }
}

