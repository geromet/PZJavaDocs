/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import zombie.core.math.PZMath;

public final class ObjectCache<E> {
    final AtomicInteger size = new AtomicInteger(0);
    final ConcurrentLinkedQueue<ObjectCacheList> queue = new ConcurrentLinkedQueue();
    final ConcurrentLinkedQueue<ObjectCacheList> pool = new ConcurrentLinkedQueue();
    final ThreadLocal<ObjectCacheList> tl = new ThreadLocal();

    public int size() {
        return this.size.get();
    }

    public ObjectCacheList popList() {
        ObjectCacheList list = this.pool.poll();
        if (list == null) {
            list = new ObjectCacheList(this);
        }
        list.clear();
        return list;
    }

    public void push(E object) {
        ObjectCacheList list = this.queue.poll();
        if (list == null) {
            list = this.pool.poll();
        }
        if (list == null) {
            list = new ObjectCacheList(this);
        }
        list.add(object);
        this.size.getAndAdd(1);
        this.queue.add(list);
    }

    public void push(List<E> objects) {
        for (int i = 0; i < objects.size(); i += 128) {
            ObjectCacheList list = this.pool.poll();
            if (list == null) {
                list = new ObjectCacheList(this);
            }
            list.clear();
            int max = PZMath.min(128, objects.size() - i);
            for (int j = 0; j < max; ++j) {
                list.add(objects.get(i + j));
            }
            this.size.getAndAdd(list.size());
            this.queue.add(list);
        }
    }

    public void push(ObjectCacheList list) {
        if (list.isEmpty()) {
            this.pool.add(list);
            return;
        }
        this.size.getAndAdd(list.size());
        this.queue.add(list);
    }

    public E pop() {
        ObjectCacheList list = this.tl.get();
        if (list == null) {
            list = this.queue.poll();
        }
        if (list == null) {
            return null;
        }
        Object e = list.remove(list.size() - 1);
        this.size.getAndDecrement();
        if (list.isEmpty()) {
            this.tl.set(null);
            this.pool.add(list);
        } else {
            this.tl.set(list);
        }
        return e;
    }

    public final class ObjectCacheList
    extends ArrayList<E> {
        final /* synthetic */ ObjectCache this$0;

        public ObjectCacheList(ObjectCache this$0) {
            ObjectCache objectCache = this$0;
            Objects.requireNonNull(objectCache);
            this.this$0 = objectCache;
        }
    }
}

