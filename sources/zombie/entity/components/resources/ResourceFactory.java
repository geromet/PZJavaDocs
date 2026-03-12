/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.util.ObjectMap;

public class ResourceFactory {
    private static final ObjectMap<ResourceType, ResourcePool> pools = new ObjectMap();
    private static final int initialSize = 512;
    private static final int maxSize = Integer.MAX_VALUE;

    static Resource createResource(String blueprintSerial) {
        try {
            ResourceBlueprint blueprint = ResourceBlueprint.Deserialize(blueprintSerial);
            return ResourceFactory.createResource(blueprint);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Resource createResource(ResourceBlueprint blueprint) {
        Object resource = ResourceFactory.alloc(blueprint.getType());
        ((Resource)resource).loadBlueprint(blueprint);
        return resource;
    }

    static Resource createBlancResource(ResourceType resourceType) {
        return ResourceFactory.alloc(resourceType);
    }

    public static void releaseResource(Resource resource) {
        ResourceFactory.release(resource);
    }

    private static <T extends Resource> T alloc(ResourceType type) {
        ResourcePool pool = pools.get(type);
        if (pool == null) {
            DebugLog.General.error("ResourceFactory.alloc can't found pool for '" + String.valueOf((Object)type) + "'");
            pool = new ResourcePool<ResourceItem>(512, Integer.MAX_VALUE){

                @Override
                protected ResourceItem newObject() {
                    return new ResourceItem();
                }
            };
            pools.put(type, pool);
        }
        return pool.obtain();
    }

    private static <T extends Resource> void release(T resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource cannot be null.");
        }
        ResourcePool pool = pools.get(resource.getType());
        if (pool == null) {
            return;
        }
        assert (!Core.debug || !pool.pool.contains(resource)) : "Object already in pool.";
        if (resource.getResourcesComponent() != null) {
            DebugLog.General.error("Resource not removed from Resources Component");
            if (Core.debug) {
                // empty if block
            }
            resource.getResourcesComponent().removeResource(resource);
        } else {
            pool.free(resource);
        }
    }

    static {
        pools.put(ResourceType.Item, new ResourcePool<ResourceItem>(512, Integer.MAX_VALUE){

            @Override
            protected ResourceItem newObject() {
                return new ResourceItem();
            }
        });
        pools.put(ResourceType.Fluid, new ResourcePool<ResourceFluid>(512, Integer.MAX_VALUE){

            @Override
            protected ResourceFluid newObject() {
                return new ResourceFluid();
            }
        });
        pools.put(ResourceType.Energy, new ResourcePool<ResourceEnergy>(512, Integer.MAX_VALUE){

            @Override
            protected ResourceEnergy newObject() {
                return new ResourceEnergy();
            }
        });
    }

    private static abstract class ResourcePool<T extends Resource> {
        protected final ConcurrentLinkedDeque<T> pool;
        private final int max;
        private int peak;

        public ResourcePool() {
            this(16, Integer.MAX_VALUE);
        }

        public ResourcePool(int initialCapacity) {
            this(initialCapacity, Integer.MAX_VALUE);
        }

        public ResourcePool(int initialCapacity, int max) {
            this.max = max;
            this.pool = new ConcurrentLinkedDeque();
        }

        public T obtain() {
            Resource o = (Resource)this.pool.poll();
            if (o == null) {
                o = this.newObject();
            }
            return (T)o;
        }

        public void free(T o) {
            if (o == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }
            if (this.pool.size() < this.max) {
                ((Resource)o).reset();
                this.pool.add(o);
                this.peak = Math.max(this.peak, this.pool.size());
            } else {
                ((Resource)o).reset();
            }
        }

        protected abstract T newObject();
    }
}

