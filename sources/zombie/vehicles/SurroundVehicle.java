/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.VirtualZombieManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.VehiclePoly;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.VehicleScript;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class SurroundVehicle {
    private static final ObjectPool<Position> s_positionPool = new ObjectPool<Position>(Position::new);
    private static final Vector3f s_tempVector3f = new Vector3f();
    private final BaseVehicle vehicle;
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public float x3;
    public float y3;
    public float x4;
    public float y4;
    private float x1p;
    private float y1p;
    private float x2p;
    private float y2p;
    private float x3p;
    private float y3p;
    private float x4p;
    private float y4p;
    private boolean moved;
    private final ArrayList<Position> positions = new ArrayList();
    private long updateMs;

    public SurroundVehicle(BaseVehicle vehicle) {
        Objects.requireNonNull(vehicle);
        this.vehicle = vehicle;
    }

    private void calcPositionsLocal() {
        s_positionPool.release((List<Position>)this.positions);
        this.positions.clear();
        VehicleScript script = this.vehicle.getScript();
        if (script == null) {
            return;
        }
        Vector3f ext = script.getExtents();
        Vector3f com = script.getCenterOfMassOffset();
        float width = ext.x;
        float length = ext.z;
        float canStandAtFudge = 0.005f;
        float radius = 0.155f;
        float minX = com.x - width / 2.0f - 0.155f;
        float minY = com.z - length / 2.0f - 0.155f;
        float maxX = com.x + width / 2.0f + 0.155f;
        float maxY = com.z + length / 2.0f + 0.155f;
        this.addPositions(minX, com.z - length / 2.0f, minX, com.z + length / 2.0f, PositionSide.Right);
        this.addPositions(maxX, com.z - length / 2.0f, maxX, com.z + length / 2.0f, PositionSide.Left);
        this.addPositions(minX, minY, maxX, minY, PositionSide.Rear);
        this.addPositions(minX, maxY, maxX, maxY, PositionSide.Front);
    }

    private void addPositions(float x1, float y1, float x2, float y2, PositionSide side) {
        Vector3f passengerPos = this.vehicle.getPassengerLocalPos(0, s_tempVector3f);
        if (passengerPos == null) {
            return;
        }
        float radius = 0.3f;
        if (side == PositionSide.Left || side == PositionSide.Right) {
            float targetY;
            float y;
            float targetX = x1;
            for (y = targetY = passengerPos.z; y >= y1 + 0.3f; y -= 0.6f) {
                this.addPosition(targetX, y, side);
            }
            for (y = targetY + 0.6f; y < y2 - 0.3f; y += 0.6f) {
                this.addPosition(targetX, y, side);
            }
        } else {
            float x;
            float targetX = 0.0f;
            float targetY = y1;
            for (x = 0.0f; x >= x1 + 0.3f; x -= 0.6f) {
                this.addPosition(x, targetY, side);
            }
            for (x = 0.6f; x < x2 - 0.3f; x += 0.6f) {
                this.addPosition(x, targetY, side);
            }
        }
    }

    private Position addPosition(float localX, float localY, PositionSide side) {
        Position position = s_positionPool.alloc();
        position.posLocal.set(localX, localY);
        position.side = side;
        this.positions.add(position);
        return position;
    }

    private void calcPositionsWorld() {
        block4: for (int i = 0; i < this.positions.size(); ++i) {
            Position position = this.positions.get(i);
            this.vehicle.getWorldPos(position.posLocal.x, 0.0f, position.posLocal.y, position.posWorld);
            switch (position.side.ordinal()) {
                case 0: 
                case 1: {
                    this.vehicle.getWorldPos(position.posLocal.x, 0.0f, 0.0f, position.posAxis);
                    continue block4;
                }
                case 2: 
                case 3: {
                    this.vehicle.getWorldPos(0.0f, 0.0f, position.posLocal.y, position.posAxis);
                }
            }
        }
        VehiclePoly poly = this.vehicle.getPoly();
        this.x1p = poly.x1;
        this.x2p = poly.x2;
        this.x3p = poly.x3;
        this.x4p = poly.x4;
        this.y1p = poly.y1;
        this.y2p = poly.y2;
        this.y3p = poly.y3;
        this.y4p = poly.y4;
    }

    private Position getClosestPositionFor(IsoZombie zombie) {
        if (zombie == null || zombie.getTarget() == null) {
            return null;
        }
        float closestDist = Float.MAX_VALUE;
        Position closestPosition = null;
        for (int i = 0; i < this.positions.size(); ++i) {
            float dist;
            float occupierDist;
            Position position = this.positions.get(i);
            if (position.blocked) continue;
            float moverDist = IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), position.posWorld.x, position.posWorld.y);
            if (position.isOccupied() && (occupierDist = IsoUtils.DistanceToSquared(position.zombie.getX(), position.zombie.getY(), position.posWorld.x, position.posWorld.y)) < moverDist || !((dist = IsoUtils.DistanceToSquared(zombie.getTarget().getX(), zombie.getTarget().getY(), position.posWorld.x, position.posWorld.y)) < closestDist)) continue;
            closestDist = dist;
            closestPosition = position;
        }
        return closestPosition;
    }

    public Vector2f getPositionForZombie(IsoZombie zombie, Vector2f out) {
        if (zombie.isOnFloor() && !zombie.isCanWalk() || PZMath.fastfloor(zombie.getZ()) != PZMath.fastfloor(this.vehicle.getZ())) {
            return out.set(this.vehicle.getX(), this.vehicle.getY());
        }
        float distToVehicle = IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), this.vehicle.getX(), this.vehicle.getY());
        if (distToVehicle > 100.0f) {
            return out.set(this.vehicle.getX(), this.vehicle.getY());
        }
        if (this.checkPosition()) {
            this.moved = true;
        }
        for (int i = 0; i < this.positions.size(); ++i) {
            Position position = this.positions.get(i);
            if (position.blocked) {
                position.zombie = null;
            }
            if (position.zombie != zombie) continue;
            return out.set(position.posWorld.x, position.posWorld.y);
        }
        Position position = this.getClosestPositionFor(zombie);
        if (position == null) {
            return null;
        }
        position.zombie = zombie;
        position.targetX = zombie.getTarget().getX();
        position.targetY = zombie.getTarget().getY();
        return out.set(position.posWorld.x, position.posWorld.y);
    }

    private boolean checkPosition() {
        if (this.vehicle.getScript() == null) {
            return false;
        }
        if (this.positions.isEmpty()) {
            this.calcPositionsLocal();
            this.x1 = -1.0f;
        }
        VehiclePoly poly = this.vehicle.getPoly();
        if (this.x1 != poly.x1 || this.x2 != poly.x2 || this.x3 != poly.x3 || this.x4 != poly.x4 || this.y1 != poly.y1 || this.y2 != poly.y2 || this.y3 != poly.y3 || this.y4 != poly.y4) {
            this.x1 = poly.x1;
            this.x2 = poly.x2;
            this.x3 = poly.x3;
            this.x4 = poly.x4;
            this.y1 = poly.y1;
            this.y2 = poly.y2;
            this.y3 = poly.y3;
            this.y4 = poly.y4;
            this.calcPositionsWorld();
            return true;
        }
        return false;
    }

    private boolean movedSincePositionsWereCalculated() {
        VehiclePoly poly = this.vehicle.getPoly();
        return this.x1p != poly.x1 || this.x2p != poly.x2 || this.x3p != poly.x3 || this.x4p != poly.x4 || this.y1p != poly.y1 || this.y2p != poly.y2 || this.y3p != poly.y3 || this.y4p != poly.y4;
    }

    private boolean hasOccupiedPositions() {
        for (int i = 0; i < this.positions.size(); ++i) {
            Position position = this.positions.get(i);
            if (position.zombie == null) continue;
            return true;
        }
        return false;
    }

    public void update() {
        long now;
        if (this.hasOccupiedPositions() && this.checkPosition()) {
            this.moved = true;
        }
        if ((now = System.currentTimeMillis()) - this.updateMs < 1000L) {
            return;
        }
        this.updateMs = now;
        if (this.moved) {
            this.moved = false;
            for (int i = 0; i < this.positions.size(); ++i) {
                Position position = this.positions.get(i);
                position.zombie = null;
            }
        }
        boolean bMovedSincePositionWereCalculated = this.movedSincePositionsWereCalculated();
        for (int i = 0; i < this.positions.size(); ++i) {
            Position position = this.positions.get(i);
            if (!bMovedSincePositionWereCalculated) {
                position.checkBlocked(this.vehicle);
            }
            if (position.zombie == null) continue;
            float distToVehicle = IsoUtils.DistanceToSquared(position.zombie.getX(), position.zombie.getY(), this.vehicle.getX(), this.vehicle.getY());
            if (distToVehicle > 100.0f) {
                position.zombie = null;
                continue;
            }
            IsoGameCharacter target = Type.tryCastTo(position.zombie.getTarget(), IsoGameCharacter.class);
            if (position.zombie.isDead() || VirtualZombieManager.instance.isReused(position.zombie) || position.zombie.isOnFloor() || target == null || this.vehicle.getSeat(target) == -1) {
                position.zombie = null;
                continue;
            }
            if (!(IsoUtils.DistanceToSquared(position.targetX, position.targetY, target.getX(), target.getY()) > 0.1f)) continue;
            position.zombie = null;
        }
    }

    public void render() {
        if (!this.hasOccupiedPositions()) {
            return;
        }
        for (int i = 0; i < this.positions.size(); ++i) {
            Position position = this.positions.get(i);
            Vector3f v = position.posWorld;
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            if (position.isOccupied()) {
                b = 0.0f;
                r = 0.0f;
            } else if (position.blocked) {
                b = 0.0f;
                g = 0.0f;
            }
            this.vehicle.getController().drawCircle(v.x, v.y, 0.3f, r, g, b, 1.0f);
        }
    }

    public void reset() {
        s_positionPool.release((List<Position>)this.positions);
        this.positions.clear();
    }

    private static enum PositionSide {
        Front,
        Rear,
        Left,
        Right;

    }

    private static final class Position {
        final Vector2f posLocal = new Vector2f();
        final Vector3f posWorld = new Vector3f();
        final Vector3f posAxis = new Vector3f();
        PositionSide side;
        IsoZombie zombie;
        float targetX;
        float targetY;
        boolean blocked;

        private Position() {
        }

        boolean isOccupied() {
            return this.zombie != null;
        }

        void checkBlocked(BaseVehicle vehicle) {
            this.blocked = PolygonalMap2.instance.lineClearCollide(this.posWorld.x, this.posWorld.y, this.posAxis.x, this.posAxis.y, PZMath.fastfloor(vehicle.getZ()), vehicle);
            if (!this.blocked) {
                this.blocked = !PolygonalMap2.instance.canStandAt(this.posWorld.x, this.posWorld.y, PZMath.fastfloor(vehicle.getZ()), vehicle, false, false);
            }
        }
    }
}

