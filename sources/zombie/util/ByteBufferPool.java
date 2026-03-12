/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import zombie.debug.DebugType;
import zombie.util.ByteBufferPooledObject;
import zombie.util.Pool;

public class ByteBufferPool {
    private static final Pool<ByteBufferPooledObject> byteBufferPool = new Pool<ByteBufferPooledObject>(ByteBufferPooledObject::new);
    private static final int MEMORY_BLOCK_SIZE = 0xA00000;
    private MemoryBlock memoryPool = new MemoryBlock(0xA00000);
    private int memoryPoolIndex;

    public ByteBufferPooledObject allocate(int size) {
        int freeMemory = this.memoryPool.capacity() - this.memoryPoolIndex;
        if (size > freeMemory) {
            MemoryBlock oldPool = this.memoryPool;
            int additionalRequiredMemoryBlockCount = (size - freeMemory) / 0xA00000 + 1;
            this.memoryPool = new MemoryBlock(this.memoryPool.capacity() + 0xA00000 * additionalRequiredMemoryBlockCount);
            this.memoryPool.next = oldPool;
            this.memoryPoolIndex = 0;
            DebugType.General.debugln("ByteBufferPool: Allocating larger pool - new size: %d", this.memoryPool.capacity());
        }
        ByteBufferPooledObject newInstance = byteBufferPool.alloc();
        newInstance.buffer = this.memoryPool.bb;
        newInstance.startIndex = this.memoryPoolIndex;
        newInstance.capacity = size;
        this.memoryPoolIndex += size;
        return newInstance;
    }

    public void resetBuffer() {
        this.memoryPool.next = null;
        for (MemoryBlock block = this.memoryPool.next; block != null; block = block.release()) {
        }
        this.memoryPoolIndex = 0;
    }

    private static final class MemoryBlock {
        final ByteBuffer bb;
        MemoryBlock next;

        MemoryBlock(int size) {
            this.bb = MemoryUtil.memAlloc(size);
        }

        int capacity() {
            return this.bb.capacity();
        }

        MemoryBlock release() {
            MemoryUtil.memFree(this.bb);
            return this.next;
        }
    }
}

