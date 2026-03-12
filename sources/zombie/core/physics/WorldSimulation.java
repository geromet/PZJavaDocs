/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.physics.Bullet;
import zombie.core.physics.PhysicsDebugRenderer;
import zombie.core.physics.Transform;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public final class WorldSimulation {
    public static WorldSimulation instance = new WorldSimulation();
    public HashMap<Integer, IsoMovingObject> physicsObjectMap = new HashMap();
    public boolean created;
    public float offsetX;
    public float offsetY;
    public long time;
    private final ArrayList<BaseVehicle> collideVehicles = new ArrayList(4);
    private final Vector3f tempVector3f = new Vector3f();
    private final Vector3f tempVector3f2 = new Vector3f();
    private final Transform tempTransform = new Transform();
    private final Quaternionf javaxQuat4f = new Quaternionf();
    private final float[] ff = new float[8192];
    private final float[] wheelSteer = new float[4];
    private final float[] wheelRotation = new float[4];
    private final float[] wheelSkidInfo = new float[4];
    private final float[] wheelSuspensionLength = new float[4];
    private float localTime;
    public float periodSec;
    private int bulletFrameNo = -1;

    public void create() {
        if (this.created) {
            return;
        }
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        this.offsetX = metaGrid.getMinX() * 256;
        this.offsetY = metaGrid.getMinY() * 256;
        this.time = GameTime.getServerTimeMills();
        IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[0];
        Bullet.initWorld(metaGrid.getMinX(), metaGrid.getMinY(), metaGrid.getMaxX(), metaGrid.getMaxY(), (int)this.offsetX, (int)this.offsetY, cm.getWorldXMin(), cm.getWorldYMin(), IsoChunkMap.chunkGridWidth);
        for (int i = 0; i < 4; ++i) {
            this.wheelSteer[i] = 0.0f;
            this.wheelRotation[i] = 0.0f;
            this.wheelSkidInfo[i] = 0.0f;
            this.wheelSuspensionLength[i] = 0.0f;
        }
        this.created = true;
    }

    public void destroy() {
        Bullet.destroyWorld();
    }

    private void updatePhysic() {
        float fixedTimeStep = 0.01f;
        int numSimulationSubSteps = 0;
        this.localTime += GameTime.instance.getPhysicsSecondsSinceLastUpdate();
        if (this.localTime >= 0.01f) {
            numSimulationSubSteps = (int)(this.localTime / 0.01f);
            this.localTime -= (float)numSimulationSubSteps * 0.01f;
            for (int i = 0; i < numSimulationSubSteps; ++i) {
                ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
                for (int k = 0; k < vehicles.size(); ++k) {
                    BaseVehicle vehicle = vehicles.get(k);
                    vehicle.applyAccumulatedImpulsesFromHitObjectsToPhysics();
                    vehicle.applyAllImpulsesFromProneCharacters();
                }
                Bullet.stepSimulation(0.01f, 0, 0.0f);
            }
            this.periodSec = (float)numSimulationSubSteps * 0.01f;
            ++this.bulletFrameNo;
        }
        this.time = Math.abs(this.time - GameTime.getServerTimeMills()) > 100L ? GameTime.getServerTimeMills() : (this.time += (long)(10.0f * (float)numSimulationSubSteps));
    }

    public void update() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.worldSimulationUpdate.profile();){
            this.updateInternal();
        }
    }

    private void updateInternal() {
        BaseVehicle vehicleDriver;
        IsoPlayer driver;
        float z;
        float y;
        float x;
        int numVehicles;
        if (!this.created) {
            return;
        }
        this.updatePhysic();
        if (GameClient.client) {
            try {
                VehicleManager.instance.clientUpdate();
            }
            catch (Exception ex) {
                DebugLog.Vehicle.printException(ex, "VehicleManager.clientUpdate was failed", LogSeverity.Error);
            }
        }
        this.collideVehicles.clear();
        int totalVehicles = Bullet.getVehicleCount();
        int offset = 0;
        while (offset < totalVehicles && (numVehicles = Bullet.getVehiclePhysics(offset, this.ff)) > 0) {
            offset += numVehicles;
            int fn = 0;
            for (int i = 0; i < numVehicles; ++i) {
                int id = (int)this.ff[fn++];
                x = this.ff[fn++];
                y = this.ff[fn++];
                z = this.ff[fn++];
                this.tempTransform.origin.set(x, y, z);
                float qx = this.ff[fn++];
                float qy = this.ff[fn++];
                float qz = this.ff[fn++];
                float qw = this.ff[fn++];
                this.javaxQuat4f.set(qx, qy, qz, qw);
                this.tempTransform.setRotation(this.javaxQuat4f);
                float vx = this.ff[fn++];
                float vy = this.ff[fn++];
                float vz = this.ff[fn++];
                this.tempVector3f.set(vx, vy, vz);
                float speed = this.ff[fn++];
                float isCollide = this.ff[fn++];
                int wheelCount = (int)this.ff[fn++];
                for (int n = 0; n < wheelCount; ++n) {
                    this.wheelSteer[n] = this.ff[fn++];
                    this.wheelRotation[n] = this.ff[fn++];
                    this.wheelSkidInfo[n] = this.ff[fn++];
                    this.wheelSuspensionLength[n] = this.ff[fn++];
                }
                int hash = (int)(x * 100.0f + y * 100.0f + z * 100.0f + qx * 100.0f + qy * 100.0f + qz * 100.0f + qw * 100.0f);
                BaseVehicle vehicle = VehicleManager.instance.getVehicleByID((short)id);
                if (vehicle == null || GameClient.client && vehicle != null && vehicle.timeSinceLastAuth <= 0.0f && (vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote) || vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide))) continue;
                if (vehicle.vehicleId == id && isCollide > 0.5f) {
                    this.collideVehicles.add(vehicle);
                    vehicle.authSimulationHash = hash;
                }
                if (GameClient.client && vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide)) {
                    if (vehicle.authSimulationHash != hash) {
                        vehicle.authSimulationTime = System.currentTimeMillis();
                        vehicle.authSimulationHash = hash;
                    }
                    if (System.currentTimeMillis() - vehicle.authSimulationTime > 1000L) {
                        INetworkPacket.send(PacketTypes.PacketType.VehicleCollide, vehicle, vehicle.getDriver(), false);
                        vehicle.authSimulationTime = 0L;
                    }
                }
                if (!vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote) || !vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide)) {
                    if (GameClient.client && vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
                        vehicle.setSpeedKmHour(0.0f);
                    } else {
                        vehicle.setSpeedKmHour(speed);
                    }
                }
                if (GameClient.client && vehicle != null && vehicle.timeSinceLastAuth <= 0.0f && vehicle.constraintChangedTime == -1L && (vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Server) || vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote) || vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide))) continue;
                if (this.compareTransform(this.tempTransform, vehicle.getPoly().t)) {
                    vehicle.polyDirty = true;
                }
                vehicle.jniTransform.set(this.tempTransform);
                vehicle.jniLinearVelocity.set(this.tempVector3f);
                vehicle.jniIsCollide = isCollide > 0.5f;
                for (int m = 0; m < wheelCount; ++m) {
                    vehicle.wheelInfo[m].steering = this.wheelSteer[m];
                    vehicle.wheelInfo[m].rotation = this.wheelRotation[m];
                    vehicle.wheelInfo[m].skidInfo = this.wheelSkidInfo[m];
                    vehicle.wheelInfo[m].suspensionLength = this.wheelSuspensionLength[m];
                }
            }
        }
        if (GameClient.client && (driver = IsoPlayer.players[IsoPlayer.getPlayerIndex()]) != null && (vehicleDriver = driver.getVehicle()) != null && vehicleDriver.isNetPlayerId(driver.getOnlineID()) && this.collideVehicles.contains(vehicleDriver)) {
            for (BaseVehicle vehicle : this.collideVehicles) {
                if (!(vehicle.DistTo(vehicleDriver) < 16.0f) || !vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) continue;
                INetworkPacket.send(PacketTypes.PacketType.VehicleCollide, vehicle, driver, true);
                vehicle.authorizationClientCollide(driver);
            }
        }
        int numObjects = Bullet.getObjectPhysics(this.ff);
        int fn = 0;
        for (int i = 0; i < numObjects; ++i) {
            int id = (int)this.ff[fn++];
            x = this.ff[fn++];
            y = this.ff[fn++];
            z = this.ff[fn++];
            x += this.offsetX;
            z += this.offsetY;
            IsoMovingObject obj = this.physicsObjectMap.get(id);
            if (obj == null) continue;
            obj.removeFromSquare();
            obj.setX(x + 0.18f);
            obj.setY(z);
            obj.setZ(Math.max(0.0f, y / 3.0f / 0.8164967f));
            obj.setCurrentSquareFromPosition();
        }
    }

    private boolean compareTransform(Transform t1, Transform t2) {
        if (Math.abs(t1.origin.x - t2.origin.x) > 0.01f || Math.abs(t1.origin.z - t2.origin.z) > 0.01f || (int)t1.origin.y != (int)t2.origin.y) {
            return true;
        }
        int forwardAxis = 2;
        t1.basis.getColumn(2, this.tempVector3f2);
        float t1x = this.tempVector3f2.x;
        float t1z = this.tempVector3f2.z;
        t2.basis.getColumn(2, this.tempVector3f2);
        float t2x = this.tempVector3f2.x;
        float t2z = this.tempVector3f2.z;
        return Math.abs(t1x - t2x) > 0.001f || Math.abs(t1z - t2z) > 0.001f;
    }

    public void activateChunkMap(int playerIndex) {
        this.create();
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
        if (GameServer.server) {
            return;
        }
        Bullet.activateChunkMap(playerIndex, chunkMap.getWorldXMin(), chunkMap.getWorldYMin(), IsoChunkMap.chunkGridWidth);
    }

    public void deactivateChunkMap(int playerIndex) {
        if (!this.created) {
            return;
        }
        Bullet.deactivateChunkMap(playerIndex);
    }

    public void scrollGroundLeft(int playerIndex) {
        if (!this.created) {
            return;
        }
        Bullet.scrollChunkMapLeft(playerIndex);
    }

    public void scrollGroundRight(int playerIndex) {
        if (!this.created) {
            return;
        }
        Bullet.scrollChunkMapRight(playerIndex);
    }

    public void scrollGroundUp(int playerIndex) {
        if (!this.created) {
            return;
        }
        Bullet.scrollChunkMapUp(playerIndex);
    }

    public void scrollGroundDown(int playerIndex) {
        if (!this.created) {
            return;
        }
        Bullet.scrollChunkMapDown(playerIndex);
    }

    public static TextureDraw.GenericDrawer getDrawer(int playerIndex) {
        PhysicsDebugRenderer drawer = PhysicsDebugRenderer.alloc();
        drawer.init(IsoPlayer.players[playerIndex]);
        IsoPlayer.players[playerIndex].physicsDebugRenderer = drawer;
        return drawer;
    }

    public int getBulletFrameNo() {
        return this.bulletFrameNo;
    }

    private static class s_performance {
        static final PerformanceProfileProbe worldSimulationUpdate = new PerformanceProfileProbe("WorldSimulation.update");

        private s_performance() {
        }
    }
}

