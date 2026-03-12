/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.CustomBuckets;
import zombie.entity.Engine;
import zombie.entity.EntityDebugger;
import zombie.entity.EntitySimulation;
import zombie.entity.GameEntity;
import zombie.entity.InventoryItemSystem;
import zombie.entity.MetaEntity;
import zombie.entity.MetaEntitySystem;
import zombie.entity.UsingPlayerUpdateSystem;
import zombie.entity.components.crafting.CraftLogicSystem;
import zombie.entity.components.crafting.FurnaceLogicSystem;
import zombie.entity.components.crafting.MashingLogicSystem;
import zombie.entity.components.fluids.FluidContainerUpdateSystem;
import zombie.entity.components.resources.LogisticsSystem;
import zombie.entity.components.resources.ResourceUpdateSystem;
import zombie.entity.meta.MetaTagComponent;
import zombie.entity.system.RenderLastSystem;
import zombie.entity.util.LongMap;
import zombie.iso.IsoObject;
import zombie.iso.SaveBufferMap;
import zombie.network.GameClient;
import zombie.util.ByteBufferPooledObject;

public class GameEntityManager {
    private static final boolean VERBOSE = true;
    public static boolean debugMode;
    private static final boolean DEBUG_DISABLE_SAVE = false;
    private static final String saveFile = "entity_data.bin";
    private static File cacheFile;
    private static ByteBuffer cacheByteBuffer;
    private static final LongMap<GameEntity> idToEntityMap;
    private static Engine engine;
    private static EntityDebugger debugger;
    private static final ArrayDeque<MetaEntity> delayedReleaseMetaEntities;
    private static boolean initialized;
    private static boolean wasClient;
    public static final int bbBlockSize = 0x100000;

    protected GameEntityManager() {
    }

    public static void Init(int worldVersion) {
        if (engine != null) {
            DebugLog.General.warn("Previous engine not disposed!");
            engine = null;
        }
        cacheFile = ZomboidFileSystem.instance.getFileInCurrentSave(saveFile);
        engine = new Engine();
        debugMode = Core.debug;
        if (Core.debug) {
            debugger = new EntityDebugger(engine);
        }
        int order = 0;
        int updateUsingplayer = order++;
        int updateInventoryitem = order++;
        int updateFluidcontainers = order++;
        int updateLogistics = order++;
        int updateCraftlogic = order++;
        int updateFurnacelogic = order++;
        int updateMashinglogic = order++;
        int updateMetaentities = order++;
        int updateResources = order++;
        order = 0;
        int renderRenderlast = order++;
        engine.setEntityListener(new EngineEntityListener());
        CustomBuckets.initializeCustomBuckets(engine);
        engine.addSystem(new CraftLogicSystem(updateCraftlogic));
        engine.addSystem(new UsingPlayerUpdateSystem(updateUsingplayer));
        engine.addSystem(new InventoryItemSystem(updateInventoryitem));
        engine.addSystem(new MetaEntitySystem(updateMetaentities));
        engine.addSystem(new LogisticsSystem(updateLogistics));
        engine.addSystem(new ResourceUpdateSystem(updateResources));
        engine.addSystem(new MashingLogicSystem(updateMashinglogic));
        engine.addSystem(new FurnaceLogicSystem(updateFurnacelogic));
        engine.addSystem(new FluidContainerUpdateSystem(updateFluidcontainers));
        engine.addSystem(new RenderLastSystem(renderRenderlast));
        if (Core.debug) {
            // empty if block
        }
        wasClient = GameClient.client;
        initialized = true;
        GameEntityManager.load(worldVersion);
    }

    public static void Update() {
        int simulationTicks;
        if (debugger != null) {
            debugger.beginUpdate();
        }
        engine.update();
        EntitySimulation.update();
        int n = simulationTicks = GameClient.client ? 0 : EntitySimulation.getSimulationTicksThisFrame();
        if (simulationTicks > 0) {
            for (int i = 0; i < simulationTicks; ++i) {
                engine.updateSimulation();
            }
        }
        if (!delayedReleaseMetaEntities.isEmpty()) {
            MetaEntity m;
            while ((m = delayedReleaseMetaEntities.poll()) != null) {
                MetaEntity.release(m);
            }
        }
        if (debugger != null) {
            debugger.endUpdate();
        }
    }

    public static boolean isEngineProcessing() {
        return engine != null && engine.isProcessing();
    }

    public static void RenderLast() {
        engine.renderLast();
    }

    public static void Reset() {
        initialized = false;
        engine = null;
        debugger = null;
        idToEntityMap.clear();
        EntitySimulation.reset();
        if (cacheByteBuffer != null) {
            cacheByteBuffer.clear();
        }
        cacheFile = null;
        delayedReleaseMetaEntities.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static GameEntity GetEntity(long gameEntityNetID) {
        LongMap<GameEntity> longMap = idToEntityMap;
        synchronized (longMap) {
            return idToEntityMap.get(gameEntityNetID);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    static void RegisterEntity(GameEntity gameEntity) {
        IsoObject isoObject;
        GameEntity stored;
        if (gameEntity == null || !gameEntity.hasComponents()) {
            return;
        }
        if (gameEntity.componentSize() == 1 && gameEntity.hasComponent(ComponentType.Script)) {
            return;
        }
        if (GameClient.client) {
            long entityNetID = gameEntity.getEntityNetID();
            if (entityNetID == -1L) {
                throw new RuntimeException("getEntityNetID returned -1");
            }
            LongMap<GameEntity> longMap = idToEntityMap;
            synchronized (longMap) {
                idToEntityMap.put(entityNetID, gameEntity);
            }
            gameEntity.addedToEntityManager = true;
            gameEntity.addedToEngine = true;
            gameEntity.sendRequestSyncGameEntity();
            return;
        }
        DebugLog.Entity.noise("Registering entity id = " + gameEntity.getEntityNetID() + " [type:" + String.valueOf(gameEntity.getGameEntityType()) + ", name:" + gameEntity.getEntityFullTypeDebug() + ", comps: " + gameEntity.componentSize() + "]");
        boolean loadMeta = false;
        long storedID = gameEntity.getEntityNetID();
        if (gameEntity instanceof IsoObject && gameEntity.hasComponent(ComponentType.MetaTag)) {
            MetaTagComponent metaTag = (MetaTagComponent)gameEntity.removeComponent(ComponentType.MetaTag);
            storedID = metaTag.getStoredID();
            loadMeta = true;
        }
        LongMap<GameEntity> longMap = idToEntityMap;
        synchronized (longMap) {
            stored = idToEntityMap.get(storedID);
        }
        if (loadMeta) {
            if (!(stored instanceof MetaEntity)) return;
            MetaEntity source2 = (MetaEntity)stored;
            DebugLog.Entity.println("IsoObject Entity respawn - " + gameEntity.getEntityNetID() + " loading from MetaEntity...");
            for (int i = source2.componentSize() - 1; i >= 0; --i) {
                Component component = source2.getComponentForIndex(i);
                source2.removeComponent(component);
                if (gameEntity.hasComponent(component.getComponentType())) {
                    gameEntity.releaseComponent(component.getComponentType());
                }
                gameEntity.addComponent(component);
            }
            gameEntity.connectComponents();
            GameEntityManager.UnregisterEntity(stored);
        } else if (stored != null) {
            return;
        }
        engine.addEntity(gameEntity);
        long entityNetID = gameEntity.getEntityNetID();
        LongMap<GameEntity> i = idToEntityMap;
        synchronized (i) {
            idToEntityMap.put(entityNetID, gameEntity);
        }
        gameEntity.addedToEntityManager = true;
        if (gameEntity instanceof IsoObject) {
            isoObject = (IsoObject)gameEntity;
            if (gameEntity.hasComponent(ComponentType.FluidContainer)) {
                isoObject.sync();
            }
        }
        if (!loadMeta || !(gameEntity instanceof IsoObject)) return;
        isoObject = (IsoObject)gameEntity;
        isoObject.getChunk().requiresHotSave = true;
    }

    static void UnregisterEntity(GameEntity gameEntity) {
        GameEntityManager.UnregisterEntity(gameEntity, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void UnregisterEntity(GameEntity gameEntity, boolean offloadToMeta) {
        GameEntity stored;
        if (gameEntity == null || !gameEntity.addedToEntityManager) {
            return;
        }
        if (GameClient.client || wasClient) {
            long entityNetID = gameEntity.getEntityNetID();
            LongMap<GameEntity> longMap = idToEntityMap;
            synchronized (longMap) {
                idToEntityMap.remove(entityNetID);
            }
            gameEntity.addedToEntityManager = false;
            gameEntity.addedToEngine = false;
            return;
        }
        DebugLog.Entity.noise("Unregistering entity id = " + gameEntity.getEntityNetID() + " [type:" + String.valueOf(gameEntity.getGameEntityType()) + ", name:" + gameEntity.getEntityFullTypeDebug() + ", tryOffloadToMeta:" + offloadToMeta + "]");
        long entityNetID = gameEntity.getEntityNetID();
        LongMap<GameEntity> longMap = idToEntityMap;
        synchronized (longMap) {
            stored = idToEntityMap.remove(entityNetID);
        }
        if (stored == null) {
            return;
        }
        if (stored != gameEntity) {
            throw new RuntimeException("Stored entity mismatch");
        }
        engine.removeEntity(gameEntity);
        if (offloadToMeta && gameEntity instanceof IsoObject && ComponentType.bitsRunInMeta.intersects(gameEntity.getComponentBits())) {
            Component component;
            DebugLog.Entity.println("IsoObject Entity despawn - " + gameEntity.getEntityNetID() + " saving to MetaEntity...");
            boolean shouldStoreMeta = false;
            for (int i = gameEntity.componentSize() - 1; i >= 0; --i) {
                component = gameEntity.getComponentForIndex(i);
                if (!component.getComponentType().isRunInMeta() || !component.isQualifiesForMetaStorage()) continue;
                shouldStoreMeta = true;
                break;
            }
            if (!shouldStoreMeta) {
                DebugLog.Entity.println("IsoObject Entity despawn - ignoring meta storage for entity: " + gameEntity.getEntityNetID() + ", no components actually require meta updating right now...");
            } else {
                MetaEntity metaEntity = MetaEntity.alloc(gameEntity);
                for (int i = gameEntity.componentSize() - 1; i >= 0; --i) {
                    component = gameEntity.getComponentForIndex(i);
                    gameEntity.removeComponent(component);
                    metaEntity.addComponent(component);
                }
                metaEntity.connectComponents();
                GameEntityManager.RegisterEntity(metaEntity);
                MetaTagComponent metaTag = (MetaTagComponent)ComponentType.MetaTag.CreateComponent();
                metaTag.setStoredID(metaEntity.getEntityNetID());
                gameEntity.addComponent(metaTag);
            }
        }
        gameEntity.addedToEntityManager = false;
        if (gameEntity instanceof MetaEntity) {
            MetaEntity metaEntity = (MetaEntity)gameEntity;
            if (!(gameEntity.addedToEngine || gameEntity.scheduledForEngineRemoval || gameEntity.removingFromEngine)) {
                MetaEntity.release(metaEntity);
            } else {
                delayedReleaseMetaEntities.add(metaEntity);
            }
        }
    }

    static void onEntityAddedToEngine(GameEntity entity) {
    }

    static void onEntityRemovedFromEngine(GameEntity entity) {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void checkEntityIDChange(GameEntity entity, long oldID, long newID) {
        if (entity == null) {
            return;
        }
        if (oldID == -1L) {
            return;
        }
        if (oldID == newID) {
            return;
        }
        LongMap<GameEntity> longMap = idToEntityMap;
        synchronized (longMap) {
            GameEntity storedOld = idToEntityMap.get(oldID);
            GameEntity storedNew = idToEntityMap.get(newID);
            if (storedOld == entity) {
                idToEntityMap.remove(oldID);
            } else {
                DebugLog.Entity.error("idToEntityMap(%ld)=%s, expected %s", oldID, storedOld, entity);
            }
            if (storedNew instanceof IsoObject) {
                IsoObject newObject = (IsoObject)storedNew;
                newObject.getEntityNetID();
            }
            if (idToEntityMap.get(newID) == null) {
                idToEntityMap.put(newID, entity);
            } else {
                DebugLog.Entity.error("idToEntityMap(%ld)=%s, expected null", newID, idToEntityMap.get(newID), entity);
            }
        }
    }

    public static ByteBuffer ensureCapacity(ByteBuffer bb, int requiredSize) {
        requiredSize = PZMath.max(requiredSize, 0x100000);
        if (bb == null) {
            return ByteBuffer.allocate(requiredSize + 0x100000);
        }
        if (bb.capacity() < requiredSize + 0x100000) {
            ByteBuffer old = bb;
            bb = ByteBuffer.allocate(requiredSize + 0x100000);
            bb.put(old.array(), 0, old.position());
        }
        return bb;
    }

    public static void Save() {
        if (!initialized || engine == null) {
            throw new UnsupportedOperationException("Can not save when manager not initialized.");
        }
        if (!Core.getInstance().isNoSave()) {
            if (Core.debug) {
                // empty if block
            }
        } else {
            return;
        }
        try {
            DebugLog.Entity.println("Saving GameEntityManager...");
            if (debugger != null) {
                debugger.beginSave(cacheFile);
            }
            ByteBuffer bb = GameEntityManager.ensureCapacity(cacheByteBuffer, 0x100000);
            bb.clear();
            MetaEntitySystem system = engine.getSystem(MetaEntitySystem.class);
            bb = system.saveMetaEntities(bb);
            try (FileOutputStream output = new FileOutputStream(cacheFile);){
                output.getChannel().truncate(0L);
                output.write(bb.array(), 0, bb.position());
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                return;
            }
            cacheByteBuffer = bb;
            if (debugger != null) {
                debugger.endSave(cacheByteBuffer);
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    public static void saveToBufferMap(SaveBufferMap bufferMap) {
        if (!initialized || engine == null) {
            throw new UnsupportedOperationException("Can not save when manager not initialized.");
        }
        if (!Core.getInstance().isNoSave()) {
            if (Core.debug) {
                // empty if block
            }
        } else {
            return;
        }
        try {
            DebugLog.Entity.println("Saving GameEntityManager...");
            if (debugger != null) {
                debugger.beginSave(cacheFile);
            }
            ByteBuffer bb = GameEntityManager.ensureCapacity(cacheByteBuffer, 0x100000);
            bb.clear();
            MetaEntitySystem system = engine.getSystem(MetaEntitySystem.class);
            bb = system.saveMetaEntities(bb);
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave(saveFile);
            ByteBufferPooledObject buffer = bufferMap.allocate(bb.position());
            buffer.put(bb.array(), 0, bb.position());
            bufferMap.put(fileName, buffer);
            cacheByteBuffer = bb;
            if (debugger != null) {
                debugger.endSave(cacheByteBuffer);
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    private static void load(int worldVersion) {
        if (!initialized || engine == null) {
            throw new UnsupportedOperationException("Can not load when manager not initialized.");
        }
        if (!Core.getInstance().isNoSave()) {
            if (Core.debug) {
                // empty if block
            }
        } else {
            return;
        }
        try {
            ByteBuffer bb;
            DebugLog.Entity.println("Loading GameEntityManager...");
            if (!cacheFile.exists()) {
                DebugLog.Entity.println("- skipping entity loading, no file -");
                return;
            }
            if (debugger != null) {
                debugger.beginLoad(cacheFile);
            }
            try (FileInputStream inStream = new FileInputStream(cacheFile);){
                bb = GameEntityManager.ensureCapacity(cacheByteBuffer, (int)cacheFile.length());
                bb.clear();
                int len = inStream.read(bb.array());
                bb.limit(PZMath.max(len, 0));
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                return;
            }
            MetaEntitySystem system = engine.getSystem(MetaEntitySystem.class);
            system.loadMetaEntities(bb, worldVersion);
            cacheByteBuffer = bb;
            if (debugger != null) {
                debugger.endLoad(cacheByteBuffer);
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    public static ArrayList<GameEntity> getIsoEntitiesDebug() {
        if (debugger != null) {
            return debugger.getIsoEntitiesDebug();
        }
        return null;
    }

    public static void reloadDebug() throws Exception {
        if (debugger != null) {
            debugger.reloadDebug();
        }
    }

    public static void reloadDebugEntity(GameEntity gameEntity) throws Exception {
        if (debugger != null) {
            debugger.reloadDebugEntity(gameEntity);
        }
    }

    public static void reloadEntityFromScriptDebug(GameEntity gameEntity) throws Exception {
        if (debugger != null) {
            debugger.reloadEntityFromScriptDebug(gameEntity);
        }
    }

    static {
        idToEntityMap = new LongMap();
        delayedReleaseMetaEntities = new ArrayDeque();
    }

    private static class EngineEntityListener
    implements Engine.EntityListener {
        private EngineEntityListener() {
        }

        @Override
        public void onEntityAddedToEngine(GameEntity entity) {
            GameEntityManager.onEntityAddedToEngine(entity);
        }

        @Override
        public void onEntityRemovedFromEngine(GameEntity entity) {
            GameEntityManager.onEntityRemovedFromEngine(entity);
        }
    }
}

