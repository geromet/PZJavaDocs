/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.util.Comparator;
import java.util.Objects;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.SingleThreadPool;

public final class SystemManager {
    private final SystemUpdateComparator systemUpdateComparator = new SystemUpdateComparator();
    private final SystemUpdateSimulationComparator systemUpdateSimulationComparator = new SystemUpdateSimulationComparator();
    private final SystemRenderComparator systemRenderComparator = new SystemRenderComparator();
    private final Array<EngineSystem> systems = new Array(false, 16);
    private final Array<EngineSystem> updaterSystems = new Array(true, 16);
    private final Array<EngineSystem> simulationUpdaterSystems = new Array(true, 16);
    private final Array<EngineSystem> rendererSystems = new Array(true, 16);
    private final ImmutableArray<EngineSystem> immutableSystems = new ImmutableArray<EngineSystem>(this.systems);
    private final ImmutableArray<EngineSystem> immutableUpdaterSystems = new ImmutableArray<EngineSystem>(this.updaterSystems);
    private final ImmutableArray<EngineSystem> immutableSimulationUpdaterSystems = new ImmutableArray<EngineSystem>(this.simulationUpdaterSystems);
    private final ImmutableArray<EngineSystem> immutableRendererSystems = new ImmutableArray<EngineSystem>(this.rendererSystems);
    private final ObjectMap<Class<?>, EngineSystem> systemsByClass = new ObjectMap();
    private final Engine engine;
    private final Array<SystemOperation> pendingOperations = new Array(false, 16);
    private final SystemOperationPool systemOperationPool = new SystemOperationPool();
    private final SystemMembershipListener systemMembershipListener = new SystemMembershipListener(this);
    private final boolean enableDynamicSystems;

    protected SystemManager(Engine engine, boolean enableDynamicSystems) {
        this.engine = engine;
        this.enableDynamicSystems = enableDynamicSystems;
    }

    void addSystem(EngineSystem system) {
        if (this.engine.isProcessing()) {
            if (!this.enableDynamicSystems) {
                throw new UnsupportedOperationException("Cannot modify systems while the Engine is processing.");
            }
            this.addSystemDelayed(system);
        } else {
            this.addSystemInternal(system);
        }
    }

    void addSystemDelayed(EngineSystem system) {
        for (int i = 0; i < this.pendingOperations.size; ++i) {
            SystemOperation operation = this.pendingOperations.get(i);
            if (operation.system != system) continue;
            operation.valid = false;
        }
        SystemOperation operation = (SystemOperation)this.systemOperationPool.obtain();
        operation.system = system;
        operation.type = SystemOperation.Type.Add;
        this.pendingOperations.add(operation);
    }

    void addSystemInternal(EngineSystem system) {
        Class<?> systemType = system.getClass();
        Object oldSytem = this.getSystem(systemType);
        if (oldSytem == system) {
            this.updateSystemMembership(system);
            return;
        }
        if (oldSytem != null) {
            this.removeSystem((EngineSystem)oldSytem);
        }
        system.membershipListener = this.systemMembershipListener;
        this.systems.add(system);
        this.systemsByClass.put(systemType, system);
        this.updateSystemMembership(system);
        system.addedToEngineInternal(this.engine);
    }

    void removeSystem(EngineSystem system) {
        if (this.engine.isProcessing()) {
            if (!this.enableDynamicSystems) {
                throw new UnsupportedOperationException("Cannot modify systems while the Engine is processing.");
            }
            this.removeSystemDelayed(system);
        } else {
            this.removeSystemInternal(system);
        }
    }

    void removeSystemDelayed(EngineSystem system) {
        for (int i = 0; i < this.pendingOperations.size; ++i) {
            SystemOperation operation = this.pendingOperations.get(i);
            if (operation.system != system) continue;
            operation.valid = false;
        }
        SystemOperation operation = (SystemOperation)this.systemOperationPool.obtain();
        operation.system = system;
        operation.type = SystemOperation.Type.Remove;
        this.pendingOperations.add(operation);
    }

    void removeSystemInternal(EngineSystem system) {
        if (this.systems.removeValue(system, true)) {
            system.membershipListener = null;
            this.systemsByClass.remove(system.getClass());
            this.updaterSystems.removeValue(system, true);
            this.simulationUpdaterSystems.removeValue(system, true);
            this.rendererSystems.removeValue(system, true);
            system.removedFromEngineInternal(this.engine);
        }
    }

    void removeAllSystems() {
        while (this.systems.size > 0) {
            this.removeSystem(this.systems.first());
        }
    }

    <T extends EngineSystem> T getSystem(Class<T> systemType) {
        return (T)this.systemsByClass.get(systemType);
    }

    ImmutableArray<EngineSystem> getSystems() {
        return this.immutableSystems;
    }

    ImmutableArray<EngineSystem> getUpdaterSystems() {
        return this.immutableUpdaterSystems;
    }

    ImmutableArray<EngineSystem> getSimulationUpdaterSystems() {
        return this.immutableSimulationUpdaterSystems;
    }

    ImmutableArray<EngineSystem> getRendererSystems() {
        return this.immutableRendererSystems;
    }

    boolean hasPendingOperations() {
        return this.pendingOperations.size > 0;
    }

    void processPendingOperations() {
        for (int i = 0; i < this.pendingOperations.size; ++i) {
            SystemOperation operation = this.pendingOperations.get(i);
            if (!operation.valid) {
                this.systemOperationPool.free(operation);
                continue;
            }
            switch (operation.type.ordinal()) {
                case 0: {
                    this.addSystemInternal(operation.system);
                    break;
                }
                case 1: {
                    this.removeSystemInternal(operation.system);
                    break;
                }
                case 2: {
                    this.updateSystemMembership(operation.system);
                    break;
                }
                default: {
                    throw new AssertionError((Object)"Unexpected SystemOperation type");
                }
            }
            this.systemOperationPool.free(operation);
        }
        this.pendingOperations.clear();
    }

    private void updateSystemMembership(EngineSystem system) {
        boolean enabled = system.isEnabled();
        boolean updater = system.isUpdater();
        if (enabled && updater && !this.updaterSystems.contains(system, true)) {
            this.updaterSystems.add(system);
        } else if (!(enabled && updater || !this.updaterSystems.contains(system, true))) {
            this.updaterSystems.removeValue(system, true);
        }
        boolean simulationUpdater = system.isSimulationUpdater();
        if (enabled && simulationUpdater && !this.simulationUpdaterSystems.contains(system, true)) {
            this.simulationUpdaterSystems.add(system);
        } else if (!(enabled && simulationUpdater || !this.simulationUpdaterSystems.contains(system, true))) {
            this.simulationUpdaterSystems.removeValue(system, true);
        }
        boolean renderer = system.isRenderer();
        if (enabled && renderer && !this.rendererSystems.contains(system, true)) {
            this.rendererSystems.add(system);
        } else if (!(enabled && renderer || !this.rendererSystems.contains(system, true))) {
            this.rendererSystems.removeValue(system, true);
        }
        this.updaterSystems.sort(this.systemUpdateComparator);
        this.simulationUpdaterSystems.sort(this.systemUpdateSimulationComparator);
        this.rendererSystems.sort(this.systemRenderComparator);
    }

    private static class SystemUpdateComparator
    implements Comparator<EngineSystem> {
        private SystemUpdateComparator() {
        }

        @Override
        public int compare(EngineSystem a, EngineSystem b) {
            return Integer.compare(a.getUpdatePriority(), b.getUpdatePriority());
        }
    }

    private static class SystemUpdateSimulationComparator
    implements Comparator<EngineSystem> {
        private SystemUpdateSimulationComparator() {
        }

        @Override
        public int compare(EngineSystem a, EngineSystem b) {
            return Integer.compare(a.getUpdateSimulationPriority(), b.getUpdateSimulationPriority());
        }
    }

    private static class SystemRenderComparator
    implements Comparator<EngineSystem> {
        private SystemRenderComparator() {
        }

        @Override
        public int compare(EngineSystem a, EngineSystem b) {
            return Integer.compare(a.getRenderLastPriority(), b.getRenderLastPriority());
        }
    }

    private static class SystemOperationPool
    extends SingleThreadPool<SystemOperation> {
        private SystemOperationPool() {
        }

        @Override
        protected SystemOperation newObject() {
            return new SystemOperation();
        }
    }

    private class SystemMembershipListener
    implements EngineSystem.MembershipListener {
        final /* synthetic */ SystemManager this$0;

        private SystemMembershipListener(SystemManager systemManager) {
            SystemManager systemManager2 = systemManager;
            Objects.requireNonNull(systemManager2);
            this.this$0 = systemManager2;
        }

        @Override
        public void onMembershipPropertyChanged(EngineSystem system) {
            for (int i = 0; i < this.this$0.pendingOperations.size; ++i) {
                SystemOperation operation = this.this$0.pendingOperations.get(i);
                if (operation.system != system || !operation.valid) continue;
                return;
            }
            SystemOperation operation = (SystemOperation)this.this$0.systemOperationPool.obtain();
            operation.system = system;
            operation.type = SystemOperation.Type.UpdateMembership;
            this.this$0.pendingOperations.add(operation);
        }
    }

    private static class SystemOperation
    implements SingleThreadPool.Poolable {
        Type type;
        EngineSystem system;
        boolean valid = true;

        private SystemOperation() {
        }

        @Override
        public void reset() {
            this.system = null;
            this.valid = true;
        }

        public static enum Type {
            Add,
            Remove,
            UpdateMembership;

        }
    }
}

