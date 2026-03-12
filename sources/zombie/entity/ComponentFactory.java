/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.reflect.ClassReflection;
import zombie.entity.util.reflect.Constructor;
import zombie.entity.util.reflect.ReflectionException;

public class ComponentFactory {
    private final ObjectMap<Class<?>, ComponentPool> pools = new ObjectMap();
    private final int initialSize;
    private final int maxSize;

    public ComponentFactory() {
        this(1024, Integer.MAX_VALUE);
    }

    public ComponentFactory(int initialSize, int maxSize) {
        this.initialSize = initialSize;
        this.maxSize = maxSize;
    }

    public <T extends Component> T alloc(Class<T> componentClass) {
        ComponentPool<T> pool = this.pools.get(componentClass);
        if (pool == null) {
            pool = new ComponentPool<T>(componentClass, this.initialSize, this.maxSize);
            this.pools.put(componentClass, pool);
        }
        return pool.obtain();
    }

    public <T extends Component> void release(T component) {
        if (component == null) {
            throw new IllegalArgumentException("component cannot be null.");
        }
        ComponentPool pool = this.pools.get(component.getClass());
        if (pool == null) {
            return;
        }
        assert (!Core.debug || !pool.pool.contains(component)) : "Object already in pool.";
        if (component.owner != null) {
            DebugLog.General.error("Owner not removed?");
            if (Core.debug) {
                throw new RuntimeException("Owner not removed");
            }
            component.owner.removeComponent(component);
        }
        pool.free(component);
    }

    private static class ComponentPool<T extends Component> {
        protected final ConcurrentLinkedDeque<T> pool;
        private final Constructor constructor;
        private final int max;
        private int peak;
        private final AtomicInteger atomicSize = new AtomicInteger();

        public ComponentPool(Class<T> type) {
            this(type, 16, Integer.MAX_VALUE);
        }

        public ComponentPool(Class<T> type, int initialCapacity) {
            this(type, initialCapacity, Integer.MAX_VALUE);
        }

        public ComponentPool(Class<T> type, int initialCapacity, int max) {
            this.max = max;
            this.pool = new ConcurrentLinkedDeque();
            this.constructor = this.findConstructor(type);
            if (this.constructor == null) {
                throw new RuntimeException("Class cannot be created (missing no-arg constructor): " + type.getName());
            }
        }

        private @Nullable Constructor findConstructor(Class<T> type) {
            try {
                return ClassReflection.getConstructor(type, new Class[0]);
            }
            catch (Exception ex1) {
                try {
                    Constructor constructor = ClassReflection.getDeclaredConstructor(type, new Class[0]);
                    constructor.setAccessible(true);
                    return constructor;
                }
                catch (ReflectionException ex2) {
                    return null;
                }
            }
        }

        public T obtain() {
            Component o = (Component)this.pool.poll();
            if (o == null) {
                o = this.newObject();
            } else {
                this.atomicSize.decrementAndGet();
            }
            return (T)o;
        }

        public void free(T o) {
            if (o == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }
            if (this.atomicSize.get() < this.max) {
                ((Component)o).reset();
                this.pool.add(o);
                this.peak = Math.max(this.peak, this.atomicSize.incrementAndGet());
            } else {
                ((Component)o).reset();
            }
        }

        protected T newObject() {
            try {
                return (T)((Component)this.constructor.newInstance(null));
            }
            catch (Exception ex) {
                throw new RuntimeException("Unable to create new instance: " + this.constructor.getDeclaringClass().getName(), ex);
            }
        }
    }
}

