/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import zombie.util.ByteBufferPool;
import zombie.util.ByteBufferPooledObject;
import zombie.util.Pool;

public class SaveBufferMap {
    private final ByteBufferPool saveBufferPool = new ByteBufferPool();
    private final HashMap<String, Buffer> saveBufferMap = new HashMap();

    public ByteBufferPooledObject allocate(int size) {
        return this.saveBufferPool.allocate(size);
    }

    public void put(String key, ByteBufferPooledObject value, IWriter writer) {
        this.saveBufferMap.put(key, new Buffer(value, writer));
    }

    public void put(String key, ByteBufferPooledObject value) {
        this.saveBufferMap.put(key, new Buffer(value, null));
    }

    public ByteBufferPooledObject get(String key) {
        return this.saveBufferMap.get(key).buffer();
    }

    public void save(IWriter writer) throws IOException {
        if (this.saveBufferMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Buffer> entry : this.saveBufferMap.entrySet()) {
            String fileName = entry.getKey();
            Buffer buffer = entry.getValue();
            if (buffer.writer() == null) {
                writer.accept(fileName, buffer.buffer());
                continue;
            }
            buffer.writer().accept(fileName, buffer.buffer());
        }
    }

    public void clear() {
        for (Buffer buffer : this.saveBufferMap.values()) {
            Pool.tryRelease(buffer.buffer());
        }
        this.saveBufferPool.resetBuffer();
        this.saveBufferMap.clear();
    }

    public Set<String> keySet() {
        return this.saveBufferMap.keySet();
    }

    private record Buffer(ByteBufferPooledObject buffer, IWriter writer) {
    }

    public static interface IWriter {
        public void accept(String var1, ByteBufferPooledObject var2) throws IOException;
    }
}

