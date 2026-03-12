/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joml.Vector2f;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.VehiclePoly;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;

public class CollideWithObstaclesPoly {
    private static final float RADIUS = 0.3f;
    private final ArrayList<CCObstacle> obstacles = new ArrayList();
    private final ArrayList<CCNode> nodes = new ArrayList();
    private final ImmutableRectF moveBounds = new ImmutableRectF();
    private final ImmutableRectF vehicleBounds = new ImmutableRectF();
    private static final Vector2 move = new Vector2();
    private static final Vector2 nodeNormal = new Vector2();
    private static final Vector2 edgeVec = new Vector2();
    private final ArrayList<BaseVehicle> vehicles = new ArrayList();
    private Clipper clipper;
    private final ByteBuffer xyBuffer = ByteBuffer.allocateDirect(8192);
    private final ClosestPointOnEdge closestPointOnEdge = new ClosestPointOnEdge();

    private void getVehiclesInRect(float x1, float y1, float x2, float y2, int z) {
        this.vehicles.clear();
        int chunkX1 = (int)(x1 / 8.0f);
        int chunkY1 = (int)(y1 / 8.0f);
        int chunkX2 = (int)Math.ceil(x2 / 8.0f);
        int chunkY2 = (int)Math.ceil(y2 / 8.0f);
        for (int y = chunkY1; y < chunkY2; ++y) {
            for (int x = chunkX1; x < chunkX2; ++x) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunkForGridSquare(x * 8, y * 8, 0);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (vehicle.getScript() == null || vehicle.getZi() != z) continue;
                    this.vehicles.add(vehicle);
                }
            }
        }
    }

    void getObstaclesInRect(float x1, float y1, float x2, float y2, int chrX, int chrY, int z, boolean bWithVehicles) {
        if (this.clipper == null) {
            this.clipper = new Clipper();
        }
        this.clipper.clear();
        this.moveBounds.init(x1 - 2.0f, y1 - 2.0f, x2 - x1 + 4.0f, y2 - y1 + 4.0f);
        int chunkX1 = (int)(this.moveBounds.x / 8.0f);
        int chunkY1 = (int)(this.moveBounds.y / 8.0f);
        int chunkX2 = (int)Math.ceil(this.moveBounds.right() / 8.0f);
        int chunkY2 = (int)Math.ceil(this.moveBounds.bottom() / 8.0f);
        if (Math.abs(x2 - x1) < 2.0f && Math.abs(y2 - y1) < 2.0f) {
            chunkX1 = chrX / 8;
            chunkY1 = chrY / 8;
            chunkX2 = chunkX1 + 1;
            chunkY2 = chunkY1 + 1;
        }
        for (int y = chunkY1; y < chunkY2; ++y) {
            for (int x = chunkX1; x < chunkX2; ++x) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunk(x, y);
                if (chunk == null) continue;
                ChunkDataZ chunkDataZ = chunk.collision.init(chunk, z, this);
                ArrayList<CCObstacle> obstacles = bWithVehicles ? chunkDataZ.worldVehicleUnion : chunkDataZ.worldVehicleSeparate;
                for (int i = 0; i < obstacles.size(); ++i) {
                    CCObstacle obstacle = obstacles.get(i);
                    if (!obstacle.bounds.intersects(this.moveBounds)) continue;
                    this.obstacles.add(obstacle);
                }
                this.nodes.addAll(chunkDataZ.nodes);
            }
        }
    }

    public Vector2f resolveCollision(IsoGameCharacter chr, float nx, float ny, Vector2f finalPos) {
        float dot;
        finalPos.set(nx, ny);
        boolean render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        float x1 = chr.getX();
        float y1 = chr.getY();
        float x2 = nx;
        float y2 = ny;
        if (render) {
            LineDrawer.addLine(x1, y1, PZMath.fastfloor(chr.getZ()), x2, y2, PZMath.fastfloor(chr.getZ()), 1.0f, 1.0f, 1.0f, null, true);
        }
        if (x1 == x2 && y1 == y2) {
            return finalPos;
        }
        move.set(nx - chr.getX(), ny - chr.getY());
        move.normalize();
        this.nodes.clear();
        this.obstacles.clear();
        this.getObstaclesInRect(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), PZMath.fastfloor(chr.getX()), PZMath.fastfloor(chr.getY()), PZMath.fastfloor(chr.getZ()), true);
        this.closestPointOnEdge.edge = null;
        this.closestPointOnEdge.node = null;
        this.closestPointOnEdge.distSq = Double.MAX_VALUE;
        for (int i = 0; i < this.obstacles.size(); ++i) {
            CCObstacle obstacle = this.obstacles.get(i);
            boolean flags = false;
            if (!obstacle.isPointInside(chr.getX(), chr.getY(), 0)) continue;
            obstacle.getClosestPointOnEdge(chr.getX(), chr.getY(), this.closestPointOnEdge);
        }
        CCEdge closestEdge = this.closestPointOnEdge.edge;
        CCNode nodeAtXY = this.closestPointOnEdge.node;
        if (closestEdge != null && (dot = closestEdge.normal.dot(move)) >= 0.01f) {
            closestEdge = null;
        }
        if (nodeAtXY != null && nodeAtXY.getNormalAndEdgeVectors(nodeNormal, edgeVec) && nodeNormal.dot(move) + 0.05f >= nodeNormal.dot(edgeVec)) {
            nodeAtXY = null;
            closestEdge = null;
        }
        if (closestEdge == null) {
            this.closestPointOnEdge.edge = null;
            this.closestPointOnEdge.node = null;
            this.closestPointOnEdge.distSq = Double.MAX_VALUE;
            for (int i = 0; i < this.obstacles.size(); ++i) {
                CCObstacle obstacle1 = this.obstacles.get(i);
                obstacle1.lineSegmentIntersect(x1, y1, x2, y2, this.closestPointOnEdge, render);
            }
            closestEdge = this.closestPointOnEdge.edge;
            nodeAtXY = this.closestPointOnEdge.node;
        }
        if (nodeAtXY != null) {
            move.set(nx - chr.getX(), ny - chr.getY());
            move.normalize();
            CCEdge edge1 = closestEdge;
            CCEdge edge2 = null;
            for (int i = 0; i < nodeAtXY.edges.size(); ++i) {
                CCEdge edge = nodeAtXY.edges.get(i);
                if (edge == closestEdge || edge1.node1.x == edge.node1.x && edge1.node1.y == edge.node1.y && edge1.node2.x == edge.node2.x && edge1.node2.y == edge.node2.y || edge1.node1.x == edge.node2.x && edge1.node1.y == edge.node2.y && edge1.node2.x == edge.node1.x && edge1.node2.y == edge.node1.y || edge1.hasNode(edge.node1) && edge1.hasNode(edge.node2)) continue;
                edge2 = edge;
            }
            if (edge1 != null && edge2 != null) {
                if (closestEdge == edge1) {
                    CCNode nodeOther = nodeAtXY == edge2.node1 ? edge2.node2 : edge2.node1;
                    edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                    edgeVec.normalize();
                    if (move.dot(edgeVec) >= 0.0f) {
                        closestEdge = edge2;
                    }
                } else if (closestEdge == edge2) {
                    CCNode nodeOther = nodeAtXY == edge1.node1 ? edge1.node2 : edge1.node1;
                    edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                    edgeVec.normalize();
                    if (move.dot(edgeVec) >= 0.0f) {
                        closestEdge = edge1;
                    }
                }
            }
        }
        if (closestEdge != null) {
            if (render) {
                float lineX1 = closestEdge.node1.x;
                float lineY1 = closestEdge.node1.y;
                float lineX2 = closestEdge.node2.x;
                float lineY2 = closestEdge.node2.y;
                LineDrawer.addLine(lineX1, lineY1, closestEdge.node1.z, lineX2, lineY2, closestEdge.node1.z, 0.0f, 1.0f, 1.0f, null, true);
            }
            this.closestPointOnEdge.distSq = Double.MAX_VALUE;
            closestEdge.getClosestPointOnEdge(nx, ny, this.closestPointOnEdge);
            finalPos.set(this.closestPointOnEdge.point.x, this.closestPointOnEdge.point.y);
        }
        return finalPos;
    }

    public boolean canStandAt(float x, float y, float z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        float x1 = x - 0.3f;
        float y1 = y - 0.3f;
        float x2 = x + 0.3f;
        float y2 = y + 0.3f;
        this.nodes.clear();
        this.obstacles.clear();
        this.getObstaclesInRect(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z), ignoreVehicle == null);
        for (int i = 0; i < this.obstacles.size(); ++i) {
            CCObstacle obstacle = this.obstacles.get(i);
            if (ignoreVehicle != null && obstacle.vehicle == ignoreVehicle || !obstacle.isPointInside(x, y, flags)) continue;
            return false;
        }
        return true;
    }

    public boolean isNotClear(float x0, float y0, float x1, float y1, int z, boolean render, BaseVehicle ignoreVehicle, boolean ignoreDoors, boolean closeToWalls) {
        int yInc;
        double error;
        int xInc;
        float sx = x0;
        float sy = y0;
        float ex = x1;
        float ey = y1;
        double dx = Math.abs((x1 /= 8.0f) - (x0 /= 8.0f));
        double dy = Math.abs((y1 /= 8.0f) - (y0 /= 8.0f));
        int x = PZMath.fastfloor(x0);
        int y = PZMath.fastfloor(y0);
        int n = 1;
        if (dx == 0.0) {
            xInc = 0;
            error = Double.POSITIVE_INFINITY;
        } else if (x1 > x0) {
            xInc = 1;
            n += PZMath.fastfloor(x1) - x;
            error = (double)((float)PZMath.fastfloor(x0) + 1.0f - x0) * dy;
        } else {
            xInc = -1;
            n += x - PZMath.fastfloor(x1);
            error = (double)(x0 - (float)PZMath.fastfloor(x0)) * dy;
        }
        if (dy == 0.0) {
            yInc = 0;
            error -= Double.POSITIVE_INFINITY;
        } else if (y1 > y0) {
            yInc = 1;
            n += PZMath.fastfloor(y1) - y;
            error -= (double)((float)PZMath.fastfloor(y0) + 1.0f - y0) * dx;
        } else {
            yInc = -1;
            n += y - PZMath.fastfloor(y1);
            error -= (double)(y0 - (float)PZMath.fastfloor(y0)) * dx;
        }
        while (n > 0) {
            IsoChunk chunk;
            IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunk(x, y);
            if (chunk != null) {
                if (render) {
                    int chunksPerWidth = 8;
                    LineDrawer.addRect(x * 8, y * 8, z, 8.0f, 8.0f, 1.0f, 1.0f, 1.0f);
                }
                ChunkDataZ chunkDataZ = chunk.collision.init(chunk, z, this);
                ArrayList<CCObstacle> obstacles = ignoreVehicle == null ? chunkDataZ.worldVehicleUnion : chunkDataZ.worldVehicleSeparate;
                for (int i = 0; i < obstacles.size(); ++i) {
                    CCObstacle obstacle = obstacles.get(i);
                    if (ignoreVehicle != null && obstacle.vehicle == ignoreVehicle || !obstacle.lineSegmentIntersects(sx, sy, ex, ey, render)) continue;
                    return true;
                }
            }
            if (error > 0.0) {
                y += yInc;
                error -= dx;
            } else {
                x += xInc;
                error += dy;
            }
            --n;
        }
        return false;
    }

    private void vehicleMoved(VehiclePoly poly) {
        int pad = 2;
        int minX = (int)Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
        int minY = (int)Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
        int maxX = (int)Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
        int maxY = (int)Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
        int z = PZMath.fastfloor(poly.z);
        int chunkMinX = (minX - 2) / 8;
        int chunkMinY = (minY - 2) / 8;
        int chunkMaxX = (int)Math.ceil(((float)(maxX + 2) - 1.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil(((float)(maxY + 2) - 1.0f) / 8.0f);
        for (int y = chunkMinY; y <= chunkMaxY; ++y) {
            for (int x = chunkMinX; x <= chunkMaxX; ++x) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunk(x, y);
                if (chunk == null || chunk.collision.data[z] == null) continue;
                ChunkDataZ chunkDataZ = chunk.collision.data[z];
                chunk.collision.data[z] = null;
                chunkDataZ.clear();
                ChunkDataZ.pool.release(chunkDataZ);
            }
        }
    }

    public void vehicleMoved(VehiclePoly oldPolyPlusRadius, VehiclePoly newPolyPlusRadius) {
        this.vehicleMoved(oldPolyPlusRadius);
        this.vehicleMoved(newPolyPlusRadius);
    }

    public void render() {
        boolean render;
        boolean bl = render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        if (render) {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                return;
            }
            this.nodes.clear();
            this.obstacles.clear();
            this.getObstaclesInRect(player.getX(), player.getY(), player.getX(), player.getY(), PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), PZMath.fastfloor(player.getZ()), true);
            if (DebugOptions.instance.collideWithObstacles.render.normals.getValue()) {
                for (CCNode node : this.nodes) {
                    if (!node.getNormalAndEdgeVectors(nodeNormal, edgeVec)) continue;
                    LineDrawer.addLine(node.x, node.y, node.z, node.x + CollideWithObstaclesPoly.nodeNormal.x, node.y + CollideWithObstaclesPoly.nodeNormal.y, node.z, 0.0f, 0.0f, 1.0f, null, true);
                }
            }
            for (CCObstacle obstacle : this.obstacles) {
                obstacle.render();
            }
        }
    }

    private static final class ImmutableRectF {
        private float x;
        private float y;
        private float w;
        private float h;
        private static final ArrayDeque<ImmutableRectF> pool = new ArrayDeque();

        private ImmutableRectF() {
        }

        private ImmutableRectF init(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        private float left() {
            return this.x;
        }

        private float top() {
            return this.y;
        }

        private float right() {
            return this.x + this.w;
        }

        private float bottom() {
            return this.y + this.h;
        }

        private float width() {
            return this.w;
        }

        private float height() {
            return this.h;
        }

        private boolean containsPoint(float x, float y) {
            return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
        }

        private boolean intersects(ImmutableRectF other) {
            return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
        }

        private static ImmutableRectF alloc() {
            return pool.isEmpty() ? new ImmutableRectF() : pool.pop();
        }

        private void release() {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }

    private static final class ClosestPointOnEdge {
        private CCEdge edge;
        private CCNode node;
        private final Vector2f point = new Vector2f();
        private double distSq;

        private ClosestPointOnEdge() {
        }
    }

    public static final class ChunkData {
        private final ChunkDataZ[] data = new ChunkDataZ[8];
        private boolean clear;

        public ChunkDataZ init(IsoChunk chunk, int z, CollideWithObstaclesPoly instance) {
            assert (Thread.currentThread() == GameWindow.gameThread);
            if (this.clear) {
                this.clear = false;
                this.clearInner();
            }
            if (this.data[z] == null) {
                this.data[z] = ChunkDataZ.pool.alloc();
                this.data[z].init(chunk, z, instance);
            }
            return this.data[z];
        }

        private void clearInner() {
            PZArrayUtil.forEach(this.data, e -> {
                if (e != null) {
                    e.clear();
                    ChunkDataZ.pool.release((ChunkDataZ)e);
                }
            });
            Arrays.fill(this.data, null);
        }

        public void clear() {
            this.clear = true;
        }
    }

    public static final class ChunkDataZ {
        public final ArrayList<CCObstacle> worldVehicleUnion = new ArrayList();
        public final ArrayList<CCObstacle> worldVehicleSeparate = new ArrayList();
        public final ArrayList<CCNode> nodes = new ArrayList();
        public int z;
        public static final ObjectPool<ChunkDataZ> pool = new ObjectPool<ChunkDataZ>(ChunkDataZ::new);

        public void init(IsoChunk chunk, int z, CollideWithObstaclesPoly instance) {
            this.z = z;
            Clipper clipper = instance.clipper;
            clipper.clear();
            boolean bevel = true;
            float bevelRadius = 0.19800001f;
            boolean offset = false;
            int cx = chunk.wx * 8;
            int cy = chunk.wy * 8;
            for (int y = cy - 2; y < cy + 8 + 2; ++y) {
                for (int x = cx - 2; x < cx + 8 + 2; ++x) {
                    IsoGridSquare below;
                    float moveOntoNextSquareALittle;
                    boolean bCollideN;
                    boolean bCollideW;
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (square == null || square.getObjects().isEmpty()) continue;
                    if (square.isSolid() || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable()) {
                        clipper.addAABBBevel((float)x - 0.3f, (float)y - 0.3f, (float)x + 1.0f + 0.3f, (float)y + 1.0f + 0.3f, 0.19800001f);
                    }
                    boolean bl = bCollideW = square.has(IsoFlagType.collideW) || square.hasBlockedDoor(false) || square.HasStairsNorth();
                    if (square.has(IsoFlagType.windowW) || square.has(IsoFlagType.WindowW)) {
                        bCollideW = true;
                    }
                    if (bCollideW) {
                        if (!this.isCollideW(x, y - 1, z)) {
                            // empty if block
                        }
                        boolean doorN = false;
                        if (!this.isCollideW(x, y + 1, z)) {
                            // empty if block
                        }
                        boolean doorS = false;
                        clipper.addAABBBevel((float)x - 0.3f, (float)y - (doorN ? 0.0f : 0.3f), (float)x + 0.3f, (float)y + 1.0f + (doorS ? 0.0f : 0.3f), 0.19800001f);
                    }
                    boolean bl2 = bCollideN = square.has(IsoFlagType.collideN) || square.hasBlockedDoor(true) || square.HasStairsWest();
                    if (square.has(IsoFlagType.windowN) || square.has(IsoFlagType.WindowN)) {
                        bCollideN = true;
                    }
                    if (bCollideN) {
                        if (!this.isCollideN(x - 1, y, z)) {
                            // empty if block
                        }
                        boolean doorW = false;
                        if (!this.isCollideN(x + 1, y, z)) {
                            // empty if block
                        }
                        boolean doorE = false;
                        clipper.addAABBBevel((float)x - (doorW ? 0.0f : 0.3f), (float)y - 0.3f, (float)x + 1.0f + (doorE ? 0.0f : 0.3f), (float)y + 0.3f, 0.19800001f);
                    }
                    if (square.HasStairsNorth()) {
                        IsoGridSquare below2;
                        IsoGridSquare square2 = IsoWorld.instance.currentCell.getGridSquare(x + 1, y, z);
                        if (square2 != null) {
                            clipper.addAABBBevel((float)(x + 1) - 0.3f, (float)y - 0.3f, (float)x + 1.0f + 0.3f, (float)y + 1.0f + 0.3f, 0.19800001f);
                        }
                        if (square.has(IsoObjectType.stairsTN) && ((below2 = IsoWorld.instance.currentCell.getGridSquare(x, y, z - 1)) == null || !below2.has(IsoObjectType.stairsTN))) {
                            clipper.addAABBBevel((float)x - 0.3f, (float)y - 0.3f, (float)x + 1.0f + 0.3f, (float)y + 0.3f, 0.19800001f);
                            moveOntoNextSquareALittle = 0.1f;
                            clipper.clipAABB((float)x + 0.3f, (float)y - 0.1f, (float)x + 1.0f - 0.3f, (float)y + 0.3f);
                        }
                    }
                    if (!square.HasStairsWest()) continue;
                    IsoGridSquare square2 = IsoWorld.instance.currentCell.getGridSquare(x, y + 1, z);
                    if (square2 != null) {
                        clipper.addAABBBevel((float)x - 0.3f, (float)y + 1.0f - 0.3f, (float)x + 1.0f + 0.3f, (float)y + 1.0f + 0.3f, 0.19800001f);
                    }
                    if (!square.has(IsoObjectType.stairsTW) || (below = IsoWorld.instance.currentCell.getGridSquare(x, y, z - 1)) != null && below.has(IsoObjectType.stairsTW)) continue;
                    clipper.addAABBBevel((float)x - 0.3f, (float)y - 0.3f, (float)x + 0.3f, (float)y + 1.0f + 0.3f, 0.19800001f);
                    moveOntoNextSquareALittle = 0.1f;
                    clipper.clipAABB((float)x - 0.1f, (float)y + 0.3f, (float)x + 0.3f, (float)y + 1.0f - 0.3f);
                }
            }
            ByteBuffer xyBuffer = instance.xyBuffer;
            assert (this.worldVehicleSeparate.isEmpty());
            this.clipperToObstacles(clipper, xyBuffer, this.worldVehicleSeparate);
            int x1 = chunk.wx * 8;
            int y1 = chunk.wy * 8;
            int x2 = x1 + 8;
            int y2 = y1 + 8;
            ImmutableRectF chunkBounds = instance.moveBounds.init(x1 -= 2, y1 -= 2, (x2 += 2) - x1, (y2 += 2) - y1);
            instance.getVehiclesInRect(x1 - 5, y1 - 5, x2 + 5, y2 + 5, z);
            for (int i = 0; i < instance.vehicles.size(); ++i) {
                BaseVehicle vehicle = instance.vehicles.get(i);
                VehiclePoly poly = vehicle.getPolyPlusRadius();
                float xMin = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
                float yMin = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
                float xMax = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
                float yMax = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
                instance.vehicleBounds.init(xMin, yMin, xMax - xMin, yMax - yMin);
                if (!chunkBounds.intersects(instance.vehicleBounds)) continue;
                clipper.addPolygon(poly.x1, poly.y1, poly.x4, poly.y4, poly.x3, poly.y3, poly.x2, poly.y2);
                CCNode node1 = CCNode.alloc().init(poly.x1, poly.y1, z);
                CCNode node2 = CCNode.alloc().init(poly.x2, poly.y2, z);
                CCNode node3 = CCNode.alloc().init(poly.x3, poly.y3, z);
                CCNode node4 = CCNode.alloc().init(poly.x4, poly.y4, z);
                CCObstacle obstacle = CCObstacle.alloc().init();
                obstacle.vehicle = vehicle;
                CCEdge edge1 = CCEdge.alloc().init(node1, node2, obstacle);
                CCEdge edge2 = CCEdge.alloc().init(node2, node3, obstacle);
                CCEdge edge3 = CCEdge.alloc().init(node3, node4, obstacle);
                CCEdge edge4 = CCEdge.alloc().init(node4, node1, obstacle);
                obstacle.outer.add(edge1);
                obstacle.outer.add(edge2);
                obstacle.outer.add(edge3);
                obstacle.outer.add(edge4);
                obstacle.calcBounds();
                this.worldVehicleSeparate.add(obstacle);
                this.nodes.add(node1);
                this.nodes.add(node2);
                this.nodes.add(node3);
                this.nodes.add(node4);
            }
            assert (this.worldVehicleUnion.isEmpty());
            this.clipperToObstacles(clipper, xyBuffer, this.worldVehicleUnion);
        }

        private void getEdgesFromBuffer(ByteBuffer polyBuffer, CCObstacle obstacle, boolean outer) {
            int j;
            int pointCount = polyBuffer.getShort();
            if (pointCount < 3) {
                polyBuffer.position(polyBuffer.position() + pointCount * 4 * 2);
                return;
            }
            CCEdgeRing edges = obstacle.outer;
            if (!outer) {
                edges = CCEdgeRing.pool.alloc();
                edges.clear();
                obstacle.inner.add(edges);
            }
            int nodeFirst = this.nodes.size();
            for (j = 0; j < pointCount; ++j) {
                float x = polyBuffer.getFloat();
                float y = polyBuffer.getFloat();
                CCNode node1 = CCNode.alloc().init(x, y, this.z);
                this.nodes.add(nodeFirst, node1);
            }
            for (j = nodeFirst; j < this.nodes.size() - 1; ++j) {
                CCNode node1 = this.nodes.get(j);
                CCNode node2 = this.nodes.get(j + 1);
                CCEdge edge1 = CCEdge.alloc().init(node1, node2, obstacle);
                edges.add(edge1);
            }
            CCNode node1 = this.nodes.get(this.nodes.size() - 1);
            CCNode node2 = this.nodes.get(nodeFirst);
            edges.add(CCEdge.alloc().init(node1, node2, obstacle));
        }

        private void clipperToObstacles(Clipper clipper, ByteBuffer polyBuffer, ArrayList<CCObstacle> obstacles) {
            int polyCount = clipper.generatePolygons();
            for (int i = 0; i < polyCount; ++i) {
                polyBuffer.clear();
                clipper.getPolygon(i, polyBuffer);
                CCObstacle obstacle = CCObstacle.alloc().init();
                this.getEdgesFromBuffer(polyBuffer, obstacle, true);
                int holeCount = polyBuffer.getShort();
                for (int j = 0; j < holeCount; ++j) {
                    this.getEdgesFromBuffer(polyBuffer, obstacle, false);
                }
                obstacle.calcBounds();
                obstacles.add(obstacle);
            }
        }

        private boolean isCollideW(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            return square != null && (square.has(IsoFlagType.collideW) || square.hasBlockedDoor(false) || square.HasStairsNorth());
        }

        private boolean isCollideN(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            return square != null && (square.has(IsoFlagType.collideN) || square.hasBlockedDoor(true) || square.HasStairsWest());
        }

        private boolean isOpenDoorAt(int x, int y, int z, boolean north) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            return square != null && square.getDoor(north) != null && !square.hasBlockedDoor(north);
        }

        public void clear() {
            CCNode.releaseAll(this.nodes);
            this.nodes.clear();
            CCObstacle.releaseAll(this.worldVehicleUnion);
            this.worldVehicleUnion.clear();
            CCObstacle.releaseAll(this.worldVehicleSeparate);
            this.worldVehicleSeparate.clear();
        }
    }

    private static final class CCObstacle {
        private final CCEdgeRing outer = new CCEdgeRing();
        private final ArrayList<CCEdgeRing> inner = new ArrayList();
        private BaseVehicle vehicle;
        private ImmutableRectF bounds;
        private static final ObjectPool<CCObstacle> pool = new ObjectPool<CCObstacle>(CCObstacle::new){

            @Override
            public void release(CCObstacle obj) {
                CCEdge.releaseAll(obj.outer);
                CCEdgeRing.releaseAll(obj.inner);
                obj.outer.clear();
                obj.inner.clear();
                obj.vehicle = null;
                super.release(obj);
            }
        };

        private CCObstacle() {
        }

        private CCObstacle init() {
            this.outer.clear();
            this.inner.clear();
            this.vehicle = null;
            return this;
        }

        private boolean isPointInside(float x, float y, int flags) {
            if (this.outer.isPointInPolygon_WindingNumber(x, y, flags) != EdgeRingHit.Inside) {
                return false;
            }
            if (this.inner.isEmpty()) {
                return true;
            }
            for (int i = 0; i < this.inner.size(); ++i) {
                CCEdgeRing edges = this.inner.get(i);
                if (edges.isPointInPolygon_WindingNumber(x, y, flags) == EdgeRingHit.Outside) continue;
                return false;
            }
            return true;
        }

        private boolean lineSegmentIntersects(float sx, float sy, float ex, float ey, boolean render) {
            if (this.outer.lineSegmentIntersects(sx, sy, ex, ey, render, true)) {
                return true;
            }
            for (int i = 0; i < this.inner.size(); ++i) {
                CCEdgeRing edges = this.inner.get(i);
                if (!edges.lineSegmentIntersects(sx, sy, ex, ey, render, false)) continue;
                return true;
            }
            return false;
        }

        private void lineSegmentIntersect(float x1, float y1, float x2, float y2, ClosestPointOnEdge closestPointOnEdge, boolean render) {
            this.outer.lineSegmentIntersect(x1, y1, x2, y2, closestPointOnEdge, render);
            for (int i = 0; i < this.inner.size(); ++i) {
                CCEdgeRing edges = this.inner.get(i);
                edges.lineSegmentIntersect(x1, y1, x2, y2, closestPointOnEdge, render);
            }
        }

        private void getClosestPointOnEdge(float x3, float y3, ClosestPointOnEdge out) {
            this.outer.getClosestPointOnEdge(x3, y3, out);
            for (int i = 0; i < this.inner.size(); ++i) {
                CCEdgeRing edges = this.inner.get(i);
                edges.getClosestPointOnEdge(x3, y3, out);
            }
        }

        private void calcBounds() {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float maxY = Float.MIN_VALUE;
            for (int i = 0; i < this.outer.size(); ++i) {
                CCEdge edge = (CCEdge)this.outer.get(i);
                minX = Math.min(minX, edge.node1.x);
                minY = Math.min(minY, edge.node1.y);
                maxX = Math.max(maxX, edge.node1.x);
                maxY = Math.max(maxY, edge.node1.y);
            }
            if (this.bounds != null) {
                this.bounds.release();
            }
            float epsilon = 0.01f;
            this.bounds = ImmutableRectF.alloc().init(minX - 0.01f, minY - 0.01f, maxX - minX + 0.02f, maxY - minY + 0.02f);
        }

        private void render() {
            this.outer.render(true);
            for (int i = 0; i < this.inner.size(); ++i) {
                this.inner.get(i).render(false);
            }
        }

        private static CCObstacle alloc() {
            return pool.alloc();
        }

        private void release() {
            pool.release(this);
        }

        private static void releaseAll(ArrayList<CCObstacle> objs) {
            pool.releaseAll((List<CCObstacle>)objs);
        }
    }

    private static final class CCEdge {
        private CCNode node1;
        private CCNode node2;
        private CCObstacle obstacle;
        private final Vector2 normal = new Vector2();
        private static final ObjectPool<CCEdge> pool = new ObjectPool<CCEdge>(CCEdge::new);

        private CCEdge() {
        }

        private CCEdge init(CCNode node1, CCNode node2, CCObstacle obstacle) {
            if (node1.x == node2.x && node1.y == node2.y) {
                boolean bl = false;
            }
            this.node1 = node1;
            this.node2 = node2;
            node1.edges.add(this);
            node2.edges.add(this);
            this.obstacle = obstacle;
            this.normal.set(node2.x - node1.x, node2.y - node1.y);
            this.normal.normalize();
            this.normal.rotate((float)Math.toRadians(90.0));
            return this;
        }

        private boolean hasNode(CCNode node) {
            return node == this.node1 || node == this.node2;
        }

        private void getClosestPointOnEdge(float x3, float y3, ClosestPointOnEdge out) {
            float x1 = this.node1.x;
            float y1 = this.node1.y;
            float x2 = this.node2.x;
            float y2 = this.node2.y;
            double u = (double)((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double xu = (double)x1 + u * (double)(x2 - x1);
            double yu = (double)y1 + u * (double)(y2 - y1);
            double epsilon = 0.001;
            CCNode node = null;
            if (u <= 0.001) {
                xu = x1;
                yu = y1;
                node = this.node1;
            } else if (u >= 0.999) {
                xu = x2;
                yu = y2;
                node = this.node2;
            }
            double distSq = ((double)x3 - xu) * ((double)x3 - xu) + ((double)y3 - yu) * ((double)y3 - yu);
            if (distSq < out.distSq) {
                out.point.set((float)xu, (float)yu);
                out.distSq = distSq;
                out.edge = this;
                out.node = node;
            }
        }

        private boolean isPointOn(float x3, float y3) {
            float x1 = this.node1.x;
            float y1 = this.node1.y;
            float x2 = this.node2.x;
            float y2 = this.node2.y;
            double u = (double)((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double xu = (double)x1 + u * (double)(x2 - x1);
            double yu = (double)y1 + u * (double)(y2 - y1);
            if (u <= 0.0) {
                xu = x1;
                yu = y1;
            } else if (u >= 1.0) {
                xu = x2;
                yu = y2;
            }
            double distSq = ((double)x3 - xu) * ((double)x3 - xu) + ((double)y3 - yu) * ((double)y3 - yu);
            return distSq < 1.0E-6;
        }

        private static CCEdge alloc() {
            return pool.alloc();
        }

        private void release() {
            pool.release(this);
        }

        private static void releaseAll(ArrayList<CCEdge> objs) {
            pool.releaseAll((List<CCEdge>)objs);
        }
    }

    private static final class CCNode {
        private float x;
        private float y;
        private int z;
        private final ArrayList<CCEdge> edges = new ArrayList();
        private static final ObjectPool<CCNode> pool = new ObjectPool<CCNode>(CCNode::new);

        private CCNode() {
        }

        private CCNode init(float x, float y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.edges.clear();
            return this;
        }

        private CCNode setXY(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        private boolean getNormalAndEdgeVectors(Vector2 normal, Vector2 edgeVec) {
            CCEdge edge1 = null;
            CCEdge edge2 = null;
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                if (edge1 == null) {
                    edge1 = edge;
                    continue;
                }
                if (edge1.hasNode(edge.node1) && edge1.hasNode(edge.node2)) continue;
                edge2 = edge;
            }
            if (edge1 == null || edge2 == null) {
                return false;
            }
            float vx = edge1.normal.x + edge2.normal.x;
            float vy = edge1.normal.y + edge2.normal.y;
            normal.set(vx, vy);
            normal.normalize();
            if (edge1.node1 == this) {
                edgeVec.set(edge1.node2.x - edge1.node1.x, edge1.node2.y - edge1.node1.y);
            } else {
                edgeVec.set(edge1.node1.x - edge1.node2.x, edge1.node1.y - edge1.node2.y);
            }
            edgeVec.normalize();
            return true;
        }

        private static CCNode alloc() {
            return pool.alloc();
        }

        private void release() {
            pool.release(this);
        }

        private static void releaseAll(ArrayList<CCNode> objs) {
            pool.releaseAll((List<CCNode>)objs);
        }
    }

    private static final class CCEdgeRing
    extends ArrayList<CCEdge> {
        private static final ObjectPool<CCEdgeRing> pool = new ObjectPool<CCEdgeRing>(CCEdgeRing::new){

            @Override
            public void release(CCEdgeRing obj) {
                CCEdge.releaseAll(obj);
                this.clear();
                super.release(obj);
            }
        };

        private CCEdgeRing() {
        }

        private float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
            return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        }

        private EdgeRingHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
            int wn = 0;
            for (int i = 0; i < this.size(); ++i) {
                CCEdge edge = (CCEdge)this.get(i);
                if ((flags & 0x10) != 0 && edge.isPointOn(x, y)) {
                    return EdgeRingHit.OnEdge;
                }
                if (edge.node1.y <= y) {
                    if (!(edge.node2.y > y) || !(this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) > 0.0f)) continue;
                    ++wn;
                    continue;
                }
                if (!(edge.node2.y <= y) || !(this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) < 0.0f)) continue;
                --wn;
            }
            return wn == 0 ? EdgeRingHit.Outside : EdgeRingHit.Inside;
        }

        private boolean lineSegmentIntersects(float sx, float sy, float ex, float ey, boolean render, boolean outer) {
            move.set(ex - sx, ey - sy);
            float lineSegmentLength = move.getLength();
            move.normalize();
            float dirX = CollideWithObstaclesPoly.move.x;
            float dirY = CollideWithObstaclesPoly.move.y;
            for (int j = 0; j < this.size(); ++j) {
                float t2;
                float invDbaDir;
                float doaX;
                float bY;
                float dbaY;
                float aY;
                float doaY;
                float aX;
                float bX;
                float dbaX;
                float t;
                float dot;
                CCEdge edge = (CCEdge)this.get(j);
                if (edge.isPointOn(sx, sy) || edge.isPointOn(ex, ey) || (dot = edge.normal.dot(move)) >= 0.01f || !((t = ((dbaX = (bX = edge.node2.x) - (aX = edge.node1.x)) * (doaY = sy - (aY = edge.node1.y)) - (dbaY = (bY = edge.node2.y) - aY) * (doaX = sx - aX)) * (invDbaDir = 1.0f / (dbaY * dirX - dbaX * dirY))) >= 0.0f) || !(t <= lineSegmentLength) || !((t2 = (doaY * dirX - doaX * dirY) * invDbaDir) >= 0.0f) || !(t2 <= 1.0f)) continue;
                float px = sx + t * dirX;
                float py = sy + t * dirY;
                if (render) {
                    this.render(outer);
                    LineDrawer.addRect(px - 0.05f, py - 0.05f, edge.node1.z, 0.1f, 0.1f, 1.0f, 1.0f, 1.0f);
                }
                return true;
            }
            return this.isPointInPolygon_WindingNumber((sx + ex) / 2.0f, (sy + ey) / 2.0f, 0) != EdgeRingHit.Outside;
        }

        private void lineSegmentIntersect(float x1, float y1, float x2, float y2, ClosestPointOnEdge closestPointOnEdge, boolean render) {
            move.set(x2 - x1, y2 - y1).normalize();
            for (int j = 0; j < this.size(); ++j) {
                float intersectY;
                float intersectX;
                double distSq;
                double denom;
                CCEdge edge = (CCEdge)this.get(j);
                float dot = edge.normal.dot(move);
                if (dot >= 0.0f) continue;
                float x3 = edge.node1.x;
                float y3 = edge.node1.y;
                float x4 = edge.node2.x;
                float y4 = edge.node2.y;
                float cx = x3 + 0.5f * (x4 - x3);
                float cy = y3 + 0.5f * (y4 - y3);
                if (render && DebugOptions.instance.collideWithObstacles.render.normals.getValue()) {
                    LineDrawer.addLine(cx, cy, edge.node1.z, cx + edge.normal.x, cy + edge.normal.y, edge.node1.z, 0.0f, 0.0f, 1.0f, null, true);
                }
                if ((denom = (double)((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1))) == 0.0) continue;
                double ua = (double)((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
                double ub = (double)((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
                if (!(ua >= 0.0) || !(ua <= 1.0) || !(ub >= 0.0) || !(ub <= 1.0)) continue;
                if (ub < 0.01 || ub > 0.99) {
                    CCNode node = ub < 0.01 ? edge.node1 : edge.node2;
                    double distSq2 = IsoUtils.DistanceToSquared(x1, y1, node.x, node.y);
                    if (distSq2 >= closestPointOnEdge.distSq) continue;
                    if (node.getNormalAndEdgeVectors(nodeNormal, edgeVec)) {
                        if (nodeNormal.dot(move) + 0.05f >= nodeNormal.dot(edgeVec)) continue;
                        closestPointOnEdge.edge = edge;
                        closestPointOnEdge.node = node;
                        closestPointOnEdge.distSq = distSq2;
                        continue;
                    }
                }
                if (!((distSq = (double)IsoUtils.DistanceToSquared(x1, y1, intersectX = (float)((double)x1 + ua * (double)(x2 - x1)), intersectY = (float)((double)y1 + ua * (double)(y2 - y1)))) < closestPointOnEdge.distSq)) continue;
                closestPointOnEdge.edge = edge;
                closestPointOnEdge.node = null;
                closestPointOnEdge.distSq = distSq;
            }
        }

        private void getClosestPointOnEdge(float x3, float y3, ClosestPointOnEdge out) {
            for (int i = 0; i < this.size(); ++i) {
                CCEdge edge = (CCEdge)this.get(i);
                edge.getClosestPointOnEdge(x3, y3, out);
            }
        }

        private void render(boolean outer) {
            if (this.isEmpty()) {
                return;
            }
            float r = 0.0f;
            float g = outer ? 1.0f : 0.5f;
            float b = outer ? 0.0f : 0.5f;
            BaseVehicle.Vector3fObjectPool pool = BaseVehicle.TL_vector3f_pool.get();
            for (CCEdge edge : this) {
                CCNode n1 = edge.node1;
                CCNode n2 = edge.node2;
                LineDrawer.addLine(n1.x, n1.y, n1.z, n2.x, n2.y, n2.z, 0.0f, g, b, null, true);
                boolean polymapRenderEdgeDirection = false;
            }
            CCNode node1 = ((CCEdge)this.get((int)0)).node1;
            LineDrawer.addRect(node1.x - 0.1f, node1.y - 0.1f, node1.z, 0.2f, 0.2f, 1.0f, 0.0f, 0.0f);
        }

        private static void releaseAll(ArrayList<CCEdgeRing> objs) {
            pool.releaseAll((List<CCEdgeRing>)objs);
        }
    }

    private static enum EdgeRingHit {
        OnEdge,
        Inside,
        Outside;

    }
}

