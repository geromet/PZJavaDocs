/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BufferUtils {
    private static boolean trackDirectMemory;
    private static final ReferenceQueue<Buffer> removeCollected;
    private static final ConcurrentHashMap<BufferInfo, BufferInfo> trackedBuffers;
    static ClearReferences cleanupThread;
    private static final AtomicBoolean loadedMethods;
    private static Method cleanerMethod;
    private static final Method cleanMethod;
    private static Method viewedBufferMethod;
    private static Method freeMethod;

    public static void setTrackDirectMemoryEnabled(boolean enabled) {
        trackDirectMemory = enabled;
    }

    private static void onBufferAllocated(Buffer buffer) {
        if (trackDirectMemory) {
            if (cleanupThread == null) {
                cleanupThread = new ClearReferences();
                cleanupThread.start();
            }
            if (buffer instanceof ByteBuffer) {
                BufferInfo info = new BufferInfo(ByteBuffer.class, buffer.capacity(), buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof FloatBuffer) {
                BufferInfo info = new BufferInfo(FloatBuffer.class, buffer.capacity() * 4, buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof IntBuffer) {
                BufferInfo info = new BufferInfo(IntBuffer.class, buffer.capacity() * 4, buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof ShortBuffer) {
                BufferInfo info = new BufferInfo(ShortBuffer.class, buffer.capacity() * 2, buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof DoubleBuffer) {
                BufferInfo info = new BufferInfo(DoubleBuffer.class, buffer.capacity() * 8, buffer, removeCollected);
                trackedBuffers.put(info, info);
            }
        }
    }

    public static void printCurrentDirectMemory(StringBuilder store) {
        boolean printStout;
        long totalHeld = 0L;
        long heapMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        boolean bl = printStout = store == null;
        if (store == null) {
            store = new StringBuilder();
        }
        if (trackDirectMemory) {
            int fBufs = 0;
            int bBufs = 0;
            int iBufs = 0;
            int sBufs = 0;
            int dBufs = 0;
            int fBufsM = 0;
            int bBufsM = 0;
            int iBufsM = 0;
            int sBufsM = 0;
            int dBufsM = 0;
            for (BufferInfo b : trackedBuffers.values()) {
                if (b.type == ByteBuffer.class) {
                    totalHeld += (long)b.size;
                    bBufsM += b.size;
                    ++bBufs;
                    continue;
                }
                if (b.type == FloatBuffer.class) {
                    totalHeld += (long)b.size;
                    fBufsM += b.size;
                    ++fBufs;
                    continue;
                }
                if (b.type == IntBuffer.class) {
                    totalHeld += (long)b.size;
                    iBufsM += b.size;
                    ++iBufs;
                    continue;
                }
                if (b.type == ShortBuffer.class) {
                    totalHeld += (long)b.size;
                    sBufsM += b.size;
                    ++sBufs;
                    continue;
                }
                if (b.type != DoubleBuffer.class) continue;
                totalHeld += (long)b.size;
                dBufsM += b.size;
                ++dBufs;
            }
            store.append("Existing buffers: ").append(trackedBuffers.size()).append("\n");
            store.append("(b: ").append(bBufs).append("  f: ").append(fBufs).append("  i: ").append(iBufs).append("  s: ").append(sBufs).append("  d: ").append(dBufs).append(")").append("\n");
            store.append("Total   heap memory held: ").append(heapMem / 1024L).append("kb\n");
            store.append("Total direct memory held: ").append(totalHeld / 1024L).append("kb\n");
            store.append("(b: ").append(bBufsM / 1024).append("kb  f: ").append(fBufsM / 1024).append("kb  i: ").append(iBufsM / 1024).append("kb  s: ").append(sBufsM / 1024).append("kb  d: ").append(dBufsM / 1024).append("kb)").append("\n");
        } else {
            store.append("Total   heap memory held: ").append(heapMem / 1024L).append("kb\n");
            store.append("Only heap memory available, if you want to monitor direct memory use BufferUtils.setTrackDirectMemoryEnabled(true) during initialization.").append("\n");
        }
        if (printStout) {
            System.out.println(store);
        }
    }

    private static Method loadMethod(String className, String methodName) {
        try {
            Method method = Class.forName(className).getMethod(methodName, new Class[0]);
            method.setAccessible(true);
            return method;
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    public static ByteBuffer createByteBuffer(int size) {
        ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        buf.clear();
        BufferUtils.onBufferAllocated(buf);
        return buf;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void loadCleanerMethods() {
        if (loadedMethods.getAndSet(true)) {
            return;
        }
        AtomicBoolean atomicBoolean = loadedMethods;
        synchronized (atomicBoolean) {
            cleanerMethod = BufferUtils.loadMethod("sun.nio.ch.DirectBuffer", "cleaner");
            viewedBufferMethod = BufferUtils.loadMethod("sun.nio.ch.DirectBuffer", "viewedBuffer");
            if (viewedBufferMethod == null) {
                viewedBufferMethod = BufferUtils.loadMethod("sun.nio.ch.DirectBuffer", "attachment");
            }
            ByteBuffer bb = BufferUtils.createByteBuffer(1);
            Class<?> clazz = bb.getClass();
            try {
                freeMethod = clazz.getMethod("free", new Class[0]);
            }
            catch (NoSuchMethodException | SecurityException exception) {
                // empty catch block
            }
        }
    }

    public static void destroyDirectBuffer(Buffer toBeDestroyed) {
        if (!toBeDestroyed.isDirect()) {
            return;
        }
        BufferUtils.loadCleanerMethods();
        try {
            if (freeMethod != null) {
                freeMethod.invoke(toBeDestroyed, new Object[0]);
            } else {
                Object cleaner = cleanerMethod.invoke(toBeDestroyed, new Object[0]);
                if (cleaner == null) {
                    Object viewedBuffer = viewedBufferMethod.invoke(toBeDestroyed, new Object[0]);
                    if (viewedBuffer != null) {
                        BufferUtils.destroyDirectBuffer((Buffer)viewedBuffer);
                    } else {
                        Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "Buffer cannot be destroyed: {0}", toBeDestroyed);
                    }
                }
            }
        }
        catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException ex) {
            Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "{0}", ex);
        }
    }

    static {
        removeCollected = new ReferenceQueue();
        trackedBuffers = new ConcurrentHashMap();
        loadedMethods = new AtomicBoolean(false);
        cleanMethod = null;
    }

    private static class ClearReferences
    extends Thread {
        ClearReferences() {
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Reference<Buffer> toClean = removeCollected.remove();
                    trackedBuffers.remove(toClean);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private static class BufferInfo
    extends PhantomReference<Buffer> {
        private final Class<?> type;
        private final int size;

        public BufferInfo(Class<?> type, int size, Buffer referent, ReferenceQueue<? super Buffer> q) {
            super(referent, q);
            this.type = type;
            this.size = size;
        }
    }
}

