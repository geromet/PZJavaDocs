/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.util.ReferencedObject;

public class ReferencedObjectPool<T extends ReferencedObject> {
    private final ConcurrentLinkedQueue<T> released = new ConcurrentLinkedQueue();
    private final Supplier<T> allocator;

    public ReferencedObjectPool(Supplier<T> allocator) {
        this.allocator = allocator;
    }

    public T alloc() {
        ReferencedObject obj = (ReferencedObject)this.released.poll();
        if (obj == null) {
            return this.create();
        }
        if (obj.getReferenceCount() == 0) {
            obj.retain();
            return (T)obj;
        }
        if (Core.debug) {
            DebugLog.General.printStackTrace("Object is referenced " + obj.getReferenceCount() + " times");
        }
        return this.create();
    }

    public void release(T obj) {
        if (((ReferencedObject)obj).getReferenceCount() == 1) {
            ((ReferencedObject)obj).release();
            this.released.offer(obj);
        } else if (Core.debug) {
            DebugLog.General.printStackTrace("Object is referenced " + ((ReferencedObject)obj).getReferenceCount() + " times");
        }
    }

    int size() {
        return this.released.size();
    }

    private T create() {
        ReferencedObject obj = (ReferencedObject)this.allocator.get();
        obj.retain();
        return (T)obj;
    }
}

