/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import java.util.ArrayList;
import zombie.core.utils.WrappedBuffer;

public final class DirectBufferAllocator {
    private static final Object LOCK = "DirectBufferAllocator.LOCK";
    private static final ArrayList<WrappedBuffer> ALL = new ArrayList();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static WrappedBuffer allocate(int size) {
        Object object = LOCK;
        synchronized (object) {
            DirectBufferAllocator.destroyDisposed();
            WrappedBuffer wrapped = new WrappedBuffer(size);
            ALL.add(wrapped);
            return wrapped;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void destroyDisposed() {
        Object object = LOCK;
        synchronized (object) {
            for (int i = ALL.size() - 1; i >= 0; --i) {
                WrappedBuffer wrapped = ALL.get(i);
                if (!wrapped.isDisposed()) continue;
                ALL.remove(i);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static long getBytesAllocated() {
        Object object = LOCK;
        synchronized (object) {
            DirectBufferAllocator.destroyDisposed();
            long total = 0L;
            for (int i = 0; i < ALL.size(); ++i) {
                WrappedBuffer wrappedBuffer = ALL.get(i);
                if (wrappedBuffer.isDisposed()) continue;
                total += (long)wrappedBuffer.capacity();
            }
            return total;
        }
    }
}

