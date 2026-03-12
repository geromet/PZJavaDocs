/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.network.ZomboidNetData;

public class ZomboidNetDataPool {
    public static final ZomboidNetDataPool instance = new ZomboidNetDataPool();
    final ConcurrentLinkedQueue<ZomboidNetData> pool = new ConcurrentLinkedQueue();

    public ZomboidNetData get() {
        ZomboidNetData data = this.pool.poll();
        if (data == null) {
            return new ZomboidNetData();
        }
        return data;
    }

    public void discard(ZomboidNetData data) {
        data.reset();
        if (data.buffer.capacity() == 2048) {
            this.pool.add(data);
        }
    }

    public ZomboidNetData getLong(int len) {
        return new ZomboidNetData(len);
    }
}

