/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.joml.Vector2f;
import zombie.characters.IsoGameCharacter;
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
import zombie.vehicles.BaseVehicle;

public final class CollideWithObstacles {
    static final float RADIUS = 0.3f;
    private final ArrayList<CCObstacle> obstacles = new ArrayList();
    private final ArrayList<CCNode> nodes = new ArrayList();
    private final ArrayList<CCIntersection> intersections = new ArrayList();
    private final ImmutableRectF moveBounds = new ImmutableRectF();
    private final ImmutableRectF vehicleBounds = new ImmutableRectF();
    private final Vector2 move = new Vector2();
    private final Vector2 closest = new Vector2();
    private final Vector2 nodeNormal = new Vector2();
    private final Vector2 edgeVec = new Vector2();
    private final ArrayList<BaseVehicle> vehicles = new ArrayList();
    CCObjectOutline[][] oo = new CCObjectOutline[5][5];
    ArrayList<CCNode> obstacleTraceNodes = new ArrayList();
    CompareIntersection comparator = new CompareIntersection();

    void getVehiclesInRect(float x1, float y1, float x2, float y2, int z) {
        this.vehicles.clear();
        int chunkX1 = PZMath.fastfloor(x1 / 8.0f);
        int chunkY1 = PZMath.fastfloor(y1 / 8.0f);
        int chunkX2 = (int)Math.ceil(x2 / 8.0f);
        int chunkY2 = (int)Math.ceil(y2 / 8.0f);
        for (int y = chunkY1; y < chunkY2; ++y) {
            for (int x = chunkX1; x < chunkX2; ++x) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunkForGridSquare(x * 8, y * 8, 0);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (vehicle.getScript() == null || PZMath.fastfloor(vehicle.getZ()) != z) continue;
                    this.vehicles.add(vehicle);
                }
            }
        }
    }

    void getObstaclesInRect(float x1, float y1, float x2, float y2, int chrX, int chrY, int z) {
        int y;
        this.nodes.clear();
        this.obstacles.clear();
        this.moveBounds.init(x1 - 1.0f, y1 - 1.0f, x2 - x1 + 2.0f, y2 - y1 + 2.0f);
        this.getVehiclesInRect(x1 - 1.0f - 4.0f, y1 - 1.0f - 4.0f, x2 + 2.0f + 8.0f, y2 + 2.0f + 8.0f, z);
        for (int i = 0; i < this.vehicles.size(); ++i) {
            BaseVehicle vehicle = this.vehicles.get(i);
            VehiclePoly poly = vehicle.getPolyPlusRadius();
            float xMin = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
            float yMin = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
            float xMax = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
            float yMax = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
            this.vehicleBounds.init(xMin, yMin, xMax - xMin, yMax - yMin);
            if (!this.moveBounds.intersects(this.vehicleBounds)) continue;
            int polyZ = PZMath.fastfloor(poly.z);
            CCNode node1 = CCNode.alloc().init(poly.x1, poly.y1, polyZ);
            CCNode node2 = CCNode.alloc().init(poly.x2, poly.y2, polyZ);
            CCNode node3 = CCNode.alloc().init(poly.x3, poly.y3, polyZ);
            CCNode node4 = CCNode.alloc().init(poly.x4, poly.y4, polyZ);
            CCObstacle obstacle = CCObstacle.alloc().init();
            CCEdge edge1 = CCEdge.alloc().init(node1, node2, obstacle);
            CCEdge edge2 = CCEdge.alloc().init(node2, node3, obstacle);
            CCEdge edge3 = CCEdge.alloc().init(node3, node4, obstacle);
            CCEdge edge4 = CCEdge.alloc().init(node4, node1, obstacle);
            obstacle.edges.add(edge1);
            obstacle.edges.add(edge2);
            obstacle.edges.add(edge3);
            obstacle.edges.add(edge4);
            obstacle.calcBounds();
            this.obstacles.add(obstacle);
            this.nodes.add(node1);
            this.nodes.add(node2);
            this.nodes.add(node3);
            this.nodes.add(node4);
        }
        if (this.obstacles.isEmpty()) {
            return;
        }
        int boundsLeft = chrX - 2;
        int boundsTop = chrY - 2;
        int boundsRight = chrX + 2 + 1;
        int boundsBottom = chrY + 2 + 1;
        for (y = boundsTop; y < boundsBottom; ++y) {
            for (int x = boundsLeft; x < boundsRight; ++x) {
                CCObjectOutline.get(x - boundsLeft, y - boundsTop, z, this.oo).init(x - boundsLeft, y - boundsTop, z);
            }
        }
        for (y = boundsTop; y < boundsBottom - 1; ++y) {
            for (int x = boundsLeft; x < boundsRight - 1; ++x) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                if (square == null) continue;
                if (square.isSolid() || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable() || square.has(IsoObjectType.stairsMN) || square.has(IsoObjectType.stairsTN) || square.has(IsoObjectType.stairsMW) || square.has(IsoObjectType.stairsTW)) {
                    CCObjectOutline.setSolid(x - boundsLeft, y - boundsTop, z, this.oo);
                }
                boolean collideW = square.has(IsoFlagType.collideW);
                if (square.has(IsoFlagType.windowW) || square.has(IsoFlagType.WindowW)) {
                    collideW = true;
                }
                if (collideW && square.has(IsoFlagType.doorW)) {
                    collideW = false;
                }
                boolean collideN = square.has(IsoFlagType.collideN);
                if (square.has(IsoFlagType.windowN) || square.has(IsoFlagType.WindowN)) {
                    collideN = true;
                }
                if (collideN && square.has(IsoFlagType.doorN)) {
                    collideN = false;
                }
                if (collideW || square.hasBlockedDoor(false) || square.has(IsoObjectType.stairsBN)) {
                    CCObjectOutline.setWest(x - boundsLeft, y - boundsTop, z, this.oo);
                }
                if (collideN || square.hasBlockedDoor(true) || square.has(IsoObjectType.stairsBW)) {
                    CCObjectOutline.setNorth(x - boundsLeft, y - boundsTop, z, this.oo);
                }
                if (square.has(IsoObjectType.stairsBN) && x != boundsRight - 2) {
                    square = IsoWorld.instance.currentCell.getGridSquare(x + 1, y, z);
                    if (square == null) continue;
                    CCObjectOutline.setWest(x + 1 - boundsLeft, y - boundsTop, z, this.oo);
                    continue;
                }
                if (!square.has(IsoObjectType.stairsBW) || y == boundsBottom - 2 || (square = IsoWorld.instance.currentCell.getGridSquare(x, y + 1, z)) == null) continue;
                CCObjectOutline.setNorth(x - boundsLeft, y + 1 - boundsTop, z, this.oo);
            }
        }
        for (y = 0; y < boundsBottom - boundsTop; ++y) {
            for (int x = 0; x < boundsRight - boundsLeft; ++x) {
                CCObjectOutline f = CCObjectOutline.get(x, y, z, this.oo);
                if (f == null || !f.nw || !f.nwW || !f.nwN) continue;
                f.trace(this.oo, this.obstacleTraceNodes);
                if (f.nodes.isEmpty()) continue;
                CCObstacle obstacle = CCObstacle.alloc().init();
                CCNode node0 = f.nodes.get(f.nodes.size() - 1);
                for (int i = f.nodes.size() - 1; i > 0; --i) {
                    CCNode node1 = f.nodes.get(i);
                    CCNode node2 = f.nodes.get(i - 1);
                    node1.x += (float)boundsLeft;
                    node1.y += (float)boundsTop;
                    CCEdge edge = CCEdge.alloc().init(node1, node2, obstacle);
                    float n2x = node2.x + (node2 != node0 ? (float)boundsLeft : 0.0f);
                    float n2y = node2.y + (node2 != node0 ? (float)boundsTop : 0.0f);
                    edge.normal.set(n2x - node1.x, n2y - node1.y);
                    edge.normal.normalize();
                    edge.normal.rotate((float)Math.toRadians(90.0));
                    obstacle.edges.add(edge);
                    this.nodes.add(node1);
                }
                obstacle.calcBounds();
                this.obstacles.add(obstacle);
            }
        }
    }

    void checkEdgeIntersection() {
        int k;
        int j;
        CCObstacle obstacle1;
        int i;
        boolean render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        for (i = 0; i < this.obstacles.size(); ++i) {
            obstacle1 = this.obstacles.get(i);
            for (j = i + 1; j < this.obstacles.size(); ++j) {
                CCObstacle obstacle2 = this.obstacles.get(j);
                if (!obstacle1.bounds.intersects(obstacle2.bounds)) continue;
                for (k = 0; k < obstacle1.edges.size(); ++k) {
                    CCEdge edge1 = obstacle1.edges.get(k);
                    for (int m = 0; m < obstacle2.edges.size(); ++m) {
                        CCEdge edge2 = obstacle2.edges.get(m);
                        CCIntersection intersection = this.getIntersection(edge1, edge2);
                        if (intersection == null) continue;
                        edge1.intersections.add(intersection);
                        edge2.intersections.add(intersection);
                        if (render) {
                            LineDrawer.addLine(intersection.nodeSplit.x - 0.1f, intersection.nodeSplit.y - 0.1f, edge1.node1.z, intersection.nodeSplit.x + 0.1f, intersection.nodeSplit.y + 0.1f, edge1.node1.z, 1.0f, 0.0f, 0.0f, null, false);
                        }
                        if (!edge1.hasNode(intersection.nodeSplit) && !edge2.hasNode(intersection.nodeSplit)) {
                            this.nodes.add(intersection.nodeSplit);
                        }
                        this.intersections.add(intersection);
                    }
                }
            }
        }
        for (i = 0; i < this.obstacles.size(); ++i) {
            obstacle1 = this.obstacles.get(i);
            for (j = obstacle1.edges.size() - 1; j >= 0; --j) {
                CCEdge edge1 = obstacle1.edges.get(j);
                if (edge1.intersections.isEmpty()) continue;
                this.comparator.edge = edge1;
                Collections.sort(edge1.intersections, this.comparator);
                for (k = edge1.intersections.size() - 1; k >= 0; --k) {
                    CCIntersection intersection = edge1.intersections.get(k);
                    CCEdge cCEdge = intersection.split(edge1);
                }
            }
        }
    }

    boolean collinear(float ax, float ay, float bx, float by, float cx, float cy) {
        float f = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
        return f >= -0.05f && f < 0.05f;
    }

    boolean within(float p, float q, float r) {
        return p <= q && q <= r || r <= q && q <= p;
    }

    boolean is_on(float ax, float ay, float bx, float by, float cx, float cy) {
        return this.collinear(ax, ay, bx, by, cx, cy) && (ax != bx ? this.within(ax, cx, bx) : this.within(ay, cy, by));
    }

    public CCIntersection getIntersection(CCEdge edge1, CCEdge edge2) {
        float y4 = edge2.node2.y;
        float y3 = edge2.node1.y;
        float x2 = edge1.node2.x;
        float x1 = edge1.node1.x;
        float x4 = edge2.node2.x;
        float x3 = edge2.node1.x;
        float y2 = edge1.node2.y;
        float y1 = edge1.node1.y;
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom > -0.01 && denom < 0.01) {
            return null;
        }
        double ua = (double)((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        double ub = (double)((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
        if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
            float intersectX = (float)((double)x1 + ua * (double)(x2 - x1));
            float intersectY = (float)((double)y1 + ua * (double)(y2 - y1));
            CCNode node1 = null;
            CCNode node2 = null;
            if (ua < (double)0.01f) {
                node1 = edge1.node1;
            } else if (ua > (double)0.99f) {
                node1 = edge1.node2;
            }
            if (ub < (double)0.01f) {
                node2 = edge2.node1;
            } else if (ub > (double)0.99f) {
                node2 = edge2.node2;
            }
            if (node1 != null && node2 != null) {
                CCIntersection intersection = CCIntersection.alloc().init(edge1, edge2, (float)ua, (float)ub, node1);
                edge1.intersections.add(intersection);
                this.intersections.add(intersection);
                intersection = CCIntersection.alloc().init(edge1, edge2, (float)ua, (float)ub, node2);
                edge2.intersections.add(intersection);
                this.intersections.add(intersection);
                LineDrawer.addLine(intersection.nodeSplit.x - 0.1f, intersection.nodeSplit.y - 0.1f, edge1.node1.z, intersection.nodeSplit.x + 0.1f, intersection.nodeSplit.y + 0.1f, edge1.node1.z, 1.0f, 0.0f, 0.0f, null, false);
                return null;
            }
            if (node1 == null && node2 == null) {
                return CCIntersection.alloc().init(edge1, edge2, (float)ua, (float)ub, intersectX, intersectY);
            }
            return CCIntersection.alloc().init(edge1, edge2, (float)ua, (float)ub, node1 == null ? node2 : node1);
        }
        return null;
    }

    void checkNodesInObstacles() {
        block0: for (int i = 0; i < this.nodes.size(); ++i) {
            CCNode node = this.nodes.get(i);
            for (int j = 0; j < this.obstacles.size(); ++j) {
                CCObstacle obstacle = this.obstacles.get(j);
                boolean isIntersect = false;
                for (int k = 0; k < this.intersections.size(); ++k) {
                    CCIntersection intersection = this.intersections.get(k);
                    if (intersection.nodeSplit != node) continue;
                    if (intersection.edge1.obstacle != obstacle && intersection.edge2.obstacle != obstacle) break;
                    isIntersect = true;
                    break;
                }
                if (isIntersect || !obstacle.isNodeInsideOf(node)) continue;
                node.ignore = true;
                continue block0;
            }
        }
    }

    boolean isVisible(CCNode node1, CCNode node2) {
        if (node1.sharesEdge(node2)) {
            return !node1.onSameShapeButDoesNotShareAnEdge(node2);
        }
        return !node1.sharesShape(node2);
    }

    void calculateNodeVisibility() {
        for (int i = 0; i < this.obstacles.size(); ++i) {
            CCObstacle obstacle = this.obstacles.get(i);
            for (int j = 0; j < obstacle.edges.size(); ++j) {
                CCEdge edge = obstacle.edges.get(j);
                if (edge.node1.ignore || edge.node2.ignore || !this.isVisible(edge.node1, edge.node2)) continue;
                edge.node1.visible.add(edge.node2);
                edge.node2.visible.add(edge.node1);
            }
        }
    }

    Vector2f resolveCollision(IsoGameCharacter chr, float nx, float ny, Vector2f finalPos) {
        if (chr.getCurrentSquare() != null && chr.getCurrentSquare().HasStairs()) {
            finalPos.set(nx, ny);
            return finalPos;
        }
        float x1 = chr.getX();
        float y1 = chr.getY();
        float z1 = chr.getZ();
        return this.resolveCollision(x1, y1, z1, nx, ny, finalPos);
    }

    boolean resolveCollision(IsoGameCharacter chr, float radius, Vector2f finalPos) {
        if (chr.getCurrentSquare() != null && chr.getCurrentSquare().HasStairs()) {
            finalPos.set(chr.getX(), chr.getY());
            return false;
        }
        float x1 = chr.getX();
        float y1 = chr.getY();
        float z1 = chr.getZ();
        return this.resolveCollision(x1, y1, z1, radius, finalPos);
    }

    private Vector2f resolveCollision(float x1, float y1, float z1, float x2, float y2, Vector2f finalPos) {
        int i;
        float dot;
        boolean render;
        finalPos.set(x2, y2);
        boolean bl = render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        if (render) {
            LineDrawer.addLine(x1, y1, PZMath.fastfloor(z1), x2, y2, PZMath.fastfloor(z1), 1.0f, 1.0f, 1.0f, null, true);
        }
        if (x1 == x2 && y1 == y2) {
            return finalPos;
        }
        Vector2 move = this.move;
        move.set(x2 - x1, y2 - y1);
        move.normalize();
        this.cleanupLastCall();
        this.getObstaclesInRect(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), PZMath.fastfloor(x1), PZMath.fastfloor(y1), PZMath.fastfloor(z1));
        this.checkEdgeIntersection();
        this.checkNodesInObstacles();
        this.calculateNodeVisibility();
        if (render) {
            for (CCNode node : this.nodes) {
                for (CCNode visible : node.visible) {
                    LineDrawer.addLine(node.x, node.y, node.z, visible.x, visible.y, visible.z, 0.0f, 1.0f, 0.0f, null, true);
                }
                if (DebugOptions.instance.collideWithObstacles.render.normals.getValue() && node.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)) {
                    LineDrawer.addLine(node.x, node.y, node.z, node.x + this.nodeNormal.x, node.y + this.nodeNormal.y, node.z, 0.0f, 0.0f, 1.0f, null, true);
                }
                if (!node.ignore) continue;
                LineDrawer.addLine(node.x - 0.05f, node.y - 0.05f, node.z, node.x + 0.05f, node.y + 0.05f, node.z, 1.0f, 1.0f, 0.0f, null, false);
            }
        }
        CCEdge closestEdge = null;
        CCNode nodeAtXY = null;
        double closestDist = Double.MAX_VALUE;
        for (int i2 = 0; i2 < this.obstacles.size(); ++i2) {
            CCObstacle obstacle = this.obstacles.get(i2);
            boolean flags = false;
            if (!obstacle.isPointInside(x1, y1, 0)) continue;
            for (int j = 0; j < obstacle.edges.size(); ++j) {
                CCEdge edge = obstacle.edges.get(j);
                if (!edge.node1.visible.contains(edge.node2)) continue;
                CCNode node = edge.closestPoint(x1, y1, this.closest);
                double distSq = (x1 - this.closest.x) * (x1 - this.closest.x) + (y1 - this.closest.y) * (y1 - this.closest.y);
                if (!(distSq < closestDist)) continue;
                closestDist = distSq;
                closestEdge = edge;
                nodeAtXY = node;
            }
        }
        if (closestEdge != null && (dot = closestEdge.normal.dot(move)) >= 0.01f) {
            closestEdge = null;
        }
        if (nodeAtXY != null && nodeAtXY.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec) && this.nodeNormal.dot(move) + 0.05f >= this.nodeNormal.dot(this.edgeVec)) {
            nodeAtXY = null;
            closestEdge = null;
        }
        if (closestEdge == null) {
            double closestDistSq = Double.MAX_VALUE;
            closestEdge = null;
            nodeAtXY = null;
            block4: for (i = 0; i < this.obstacles.size(); ++i) {
                CCObstacle obstacle1 = this.obstacles.get(i);
                for (int j = 0; j < obstacle1.edges.size(); ++j) {
                    float intersectY;
                    float intersectX;
                    double distSq;
                    double denom;
                    CCEdge edge = obstacle1.edges.get(j);
                    if (!edge.node1.visible.contains(edge.node2)) continue;
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
                    float dot2 = edge.normal.dot(move);
                    if (dot2 >= 0.0f || !(ua >= 0.0) || !(ua <= 1.0) || !(ub >= 0.0) || !(ub <= 1.0)) continue;
                    if (ub < 0.01 || ub > 0.99) {
                        CCNode node;
                        CCNode cCNode = node = ub < 0.01 ? edge.node1 : edge.node2;
                        if (node.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)) {
                            if (this.nodeNormal.dot(move) + 0.05f >= this.nodeNormal.dot(this.edgeVec)) continue;
                            closestEdge = edge;
                            nodeAtXY = node;
                            continue block4;
                        }
                    }
                    if (!((distSq = (double)IsoUtils.DistanceToSquared(x1, y1, intersectX = (float)((double)x1 + ua * (double)(x2 - x1)), intersectY = (float)((double)y1 + ua * (double)(y2 - y1)))) < closestDistSq)) continue;
                    closestDistSq = distSq;
                    closestEdge = edge;
                }
            }
        }
        if (nodeAtXY != null) {
            CCEdge edge1 = closestEdge;
            CCEdge edge2 = null;
            for (i = 0; i < nodeAtXY.edges.size(); ++i) {
                CCEdge edge = nodeAtXY.edges.get(i);
                if (!edge.node1.visible.contains(edge.node2) || edge == closestEdge || edge1.node1.x == edge.node1.x && edge1.node1.y == edge.node1.y && edge1.node2.x == edge.node2.x && edge1.node2.y == edge.node2.y || edge1.node1.x == edge.node2.x && edge1.node1.y == edge.node2.y && edge1.node2.x == edge.node1.x && edge1.node2.y == edge.node1.y || edge1.hasNode(edge.node1) && edge1.hasNode(edge.node2)) continue;
                edge2 = edge;
            }
            if (edge1 != null && edge2 != null) {
                if (closestEdge == edge1) {
                    CCNode nodeOther = nodeAtXY == edge2.node1 ? edge2.node2 : edge2.node1;
                    this.edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                    this.edgeVec.normalize();
                    if (move.dot(this.edgeVec) >= 0.0f) {
                        closestEdge = edge2;
                    }
                } else if (closestEdge == edge2) {
                    CCNode nodeOther = nodeAtXY == edge1.node1 ? edge1.node2 : edge1.node1;
                    this.edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                    this.edgeVec.normalize();
                    if (move.dot(this.edgeVec) >= 0.0f) {
                        closestEdge = edge1;
                    }
                }
            }
        }
        if (closestEdge != null) {
            float lineX1 = closestEdge.node1.x;
            float lineY1 = closestEdge.node1.y;
            float lineX2 = closestEdge.node2.x;
            float lineY2 = closestEdge.node2.y;
            if (render) {
                LineDrawer.addLine(lineX1, lineY1, closestEdge.node1.z, lineX2, lineY2, closestEdge.node1.z, 0.0f, 1.0f, 1.0f, null, true);
            }
            closestEdge.closestPoint(x2, y2, this.closest);
            finalPos.set(this.closest.x, this.closest.y);
        }
        return finalPos;
    }

    private boolean resolveCollision(float x1, float y1, float z1, float radius, Vector2f finalPos) {
        Vector2 vc;
        boolean render;
        finalPos.set(x1, y1);
        if ((double)radius <= 0.0) {
            return false;
        }
        boolean bl = render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        if (render) {
            LineDrawer.DrawIsoCircle(x1, y1, z1, radius, 16, 1.0f, 0.0f, 1.0f, 1.0f);
        }
        this.cleanupLastCall();
        this.getObstaclesInRect(x1 - radius, y1 - radius, x1 + radius, y1 + radius, PZMath.fastfloor(x1), PZMath.fastfloor(y1), PZMath.fastfloor(z1));
        this.checkEdgeIntersection();
        this.checkNodesInObstacles();
        this.calculateNodeVisibility();
        if (render) {
            for (CCNode node : this.nodes) {
                for (CCNode visible : node.visible) {
                    LineDrawer.addLine(node.x, node.y, node.z, visible.x, visible.y, visible.z, 0.0f, 1.0f, 0.0f, null, true);
                }
                if (DebugOptions.instance.collideWithObstacles.render.normals.getValue() && node.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)) {
                    LineDrawer.addLine(node.x, node.y, node.z, node.x + this.nodeNormal.x, node.y + this.nodeNormal.y, node.z, 0.0f, 0.0f, 1.0f, null, true);
                }
                if (!node.ignore) continue;
                LineDrawer.addLine(node.x - 0.05f, node.y - 0.05f, node.z, node.x + 0.05f, node.y + 0.05f, node.z, 1.0f, 1.0f, 0.0f, null, false);
            }
        }
        CCEdge closestEdge = null;
        CCNode nodeAtXY = null;
        double closestDist = Double.MAX_VALUE;
        for (int i = 0; i < this.obstacles.size(); ++i) {
            CCObstacle obstacle = this.obstacles.get(i);
            boolean flags = false;
            if (!obstacle.isPointInside(x1, y1, 0)) continue;
            for (int j = 0; j < obstacle.edges.size(); ++j) {
                CCEdge edge = obstacle.edges.get(j);
                if (!edge.node1.visible.contains(edge.node2)) continue;
                CCNode node = edge.closestPoint(x1, y1, this.closest);
                double distSq = (x1 - this.closest.x) * (x1 - this.closest.x) + (y1 - this.closest.y) * (y1 - this.closest.y);
                if (!(distSq < closestDist)) continue;
                closestDist = distSq;
                closestEdge = edge;
                nodeAtXY = node;
            }
        }
        if (nodeAtXY != null && nodeAtXY.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec) && this.nodeNormal.dot(this.move) + 0.05f >= this.nodeNormal.dot(this.edgeVec)) {
            nodeAtXY = null;
            closestEdge = null;
        }
        if (closestEdge == null) {
            return false;
        }
        if (closestDist > (double)radius) {
            return false;
        }
        Vector2 pc = this.closest;
        closestEdge.closestPoint(x1, y1, pc);
        if (nodeAtXY == null) {
            vc = closestEdge.normal;
        } else {
            vc = this.nodeNormal;
            nodeAtXY.getNormalAndEdgeVectors(vc, this.edgeVec);
        }
        float vpcX = pc.x - x1;
        float vpcY = pc.y - y1;
        float vpcDotN = vpcX * vc.x + vpcY * vc.y;
        if ((double)vpcDotN < 0.0) {
            return false;
        }
        finalPos.x = pc.x + vc.x * radius;
        finalPos.y = pc.y + vc.y * radius;
        return true;
    }

    private void cleanupLastCall() {
        int i;
        for (i = 0; i < this.nodes.size(); ++i) {
            this.nodes.get(i).release();
        }
        for (i = 0; i < this.obstacles.size(); ++i) {
            CCObstacle obstacle = this.obstacles.get(i);
            for (int j = 0; j < obstacle.edges.size(); ++j) {
                obstacle.edges.get(j).release();
            }
            obstacle.release();
        }
        for (i = 0; i < this.intersections.size(); ++i) {
            this.intersections.get(i).release();
        }
        this.intersections.clear();
    }

    private static final class ImmutableRectF {
        private float x;
        private float y;
        private float w;
        private float h;
        static ArrayDeque<ImmutableRectF> pool = new ArrayDeque();

        private ImmutableRectF() {
        }

        ImmutableRectF init(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        float left() {
            return this.x;
        }

        float top() {
            return this.y;
        }

        float right() {
            return this.x + this.w;
        }

        float bottom() {
            return this.y + this.h;
        }

        float width() {
            return this.w;
        }

        float height() {
            return this.h;
        }

        boolean containsPoint(float x, float y) {
            return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
        }

        boolean intersects(ImmutableRectF other) {
            return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
        }

        static ImmutableRectF alloc() {
            return pool.isEmpty() ? new ImmutableRectF() : pool.pop();
        }

        void release() {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }

    private static final class CCObjectOutline {
        int x;
        int y;
        int z;
        boolean nw;
        boolean nwW;
        boolean nwN;
        boolean nwE;
        boolean nwS;
        boolean wW;
        boolean wE;
        boolean wCutoff;
        boolean nN;
        boolean nS;
        boolean nCutoff;
        ArrayList<CCNode> nodes;
        static ArrayDeque<CCObjectOutline> pool = new ArrayDeque();

        private CCObjectOutline() {
        }

        CCObjectOutline init(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nwE = false;
            this.nwN = false;
            this.nwW = false;
            this.nw = false;
            this.wCutoff = false;
            this.wE = false;
            this.wW = false;
            this.nCutoff = false;
            this.nS = false;
            this.nN = false;
            return this;
        }

        static void setSolid(int x, int y, int z, CCObjectOutline[][] oo) {
            CCObjectOutline.setWest(x, y, z, oo);
            CCObjectOutline.setNorth(x, y, z, oo);
            CCObjectOutline.setWest(x + 1, y, z, oo);
            CCObjectOutline.setNorth(x, y + 1, z, oo);
        }

        static void setWest(int x, int y, int z, CCObjectOutline[][] oo) {
            CCObjectOutline f1 = CCObjectOutline.get(x, y, z, oo);
            if (f1 != null) {
                if (f1.nw) {
                    f1.nwS = false;
                } else {
                    f1.nw = true;
                    f1.nwW = true;
                    f1.nwN = true;
                    f1.nwE = true;
                    f1.nwS = false;
                }
                f1.wW = true;
                f1.wE = true;
            }
            CCObjectOutline fff = f1;
            f1 = CCObjectOutline.get(x, y + 1, z, oo);
            if (f1 == null) {
                if (fff != null) {
                    fff.wCutoff = true;
                }
            } else if (f1.nw) {
                f1.nwN = false;
            } else {
                f1.nw = true;
                f1.nwN = false;
                f1.nwW = true;
                f1.nwE = true;
                f1.nwS = true;
            }
        }

        static void setNorth(int x, int y, int z, CCObjectOutline[][] oo) {
            CCObjectOutline f1 = CCObjectOutline.get(x, y, z, oo);
            if (f1 != null) {
                if (f1.nw) {
                    f1.nwE = false;
                } else {
                    f1.nw = true;
                    f1.nwW = true;
                    f1.nwN = true;
                    f1.nwE = false;
                    f1.nwS = true;
                }
                f1.nN = true;
                f1.nS = true;
            }
            CCObjectOutline fff = f1;
            f1 = CCObjectOutline.get(x + 1, y, z, oo);
            if (f1 == null) {
                if (fff != null) {
                    fff.nCutoff = true;
                }
            } else if (f1.nw) {
                f1.nwW = false;
            } else {
                f1.nw = true;
                f1.nwN = true;
                f1.nwW = false;
                f1.nwE = true;
                f1.nwS = true;
            }
        }

        static CCObjectOutline get(int x, int y, int z, CCObjectOutline[][] oo) {
            if (x < 0 || x >= oo.length) {
                return null;
            }
            if (y < 0 || y >= oo[0].length) {
                return null;
            }
            if (oo[x][y] == null) {
                oo[x][y] = CCObjectOutline.alloc().init(x, y, z);
            }
            return oo[x][y];
        }

        void trace_NW_N(CCObjectOutline[][] oo, CCNode extend) {
            if (extend != null) {
                extend.setXY((float)this.x + 0.3f, (float)this.y - 0.3f);
            } else {
                CCNode node2 = CCNode.alloc().init((float)this.x + 0.3f, (float)this.y - 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.nwN = false;
            if (this.nwE) {
                this.trace_NW_E(oo, null);
            } else if (this.nN) {
                this.trace_N_N(oo, this.nodes.get(this.nodes.size() - 1));
            }
        }

        void trace_NW_S(CCObjectOutline[][] oo, CCNode extend) {
            if (extend != null) {
                extend.setXY((float)this.x - 0.3f, (float)this.y + 0.3f);
            } else {
                CCNode node2 = CCNode.alloc().init((float)this.x - 0.3f, (float)this.y + 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.nwS = false;
            if (this.nwW) {
                this.trace_NW_W(oo, null);
            } else {
                CCObjectOutline f1 = CCObjectOutline.get(this.x - 1, this.y, this.z, oo);
                if (f1 == null) {
                    return;
                }
                if (f1.nS) {
                    f1.nodes = this.nodes;
                    f1.trace_N_S(oo, this.nodes.get(this.nodes.size() - 1));
                }
            }
        }

        void trace_NW_W(CCObjectOutline[][] oo, CCNode extend) {
            if (extend != null) {
                extend.setXY((float)this.x - 0.3f, (float)this.y - 0.3f);
            } else {
                CCNode node2 = CCNode.alloc().init((float)this.x - 0.3f, (float)this.y - 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.nwW = false;
            if (this.nwN) {
                this.trace_NW_N(oo, null);
            } else {
                CCObjectOutline f1 = CCObjectOutline.get(this.x, this.y - 1, this.z, oo);
                if (f1 == null) {
                    return;
                }
                if (f1.wW) {
                    f1.nodes = this.nodes;
                    f1.trace_W_W(oo, this.nodes.get(this.nodes.size() - 1));
                }
            }
        }

        void trace_NW_E(CCObjectOutline[][] oo, CCNode extend) {
            if (extend != null) {
                extend.setXY((float)this.x + 0.3f, (float)this.y + 0.3f);
            } else {
                CCNode node2 = CCNode.alloc().init((float)this.x + 0.3f, (float)this.y + 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.nwE = false;
            if (this.nwS) {
                this.trace_NW_S(oo, null);
            } else if (this.wE) {
                this.trace_W_E(oo, this.nodes.get(this.nodes.size() - 1));
            }
        }

        void trace_W_E(CCObjectOutline[][] oo, CCNode extend) {
            CCNode node2;
            if (extend != null) {
                extend.setXY((float)this.x + 0.3f, (float)(this.y + 1) - 0.3f);
            } else {
                node2 = CCNode.alloc().init((float)this.x + 0.3f, (float)(this.y + 1) - 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.wE = false;
            if (this.wCutoff) {
                node2 = this.nodes.get(this.nodes.size() - 1);
                node2.setXY((float)this.x + 0.3f, (float)(this.y + 1) + 0.3f);
                node2 = CCNode.alloc().init((float)this.x - 0.3f, (float)(this.y + 1) + 0.3f, this.z);
                this.nodes.add(node2);
                node2 = CCNode.alloc().init((float)this.x - 0.3f, (float)(this.y + 1) - 0.3f, this.z);
                this.nodes.add(node2);
                this.trace_W_W(oo, node2);
                return;
            }
            CCObjectOutline f1 = CCObjectOutline.get(this.x, this.y + 1, this.z, oo);
            if (f1 == null) {
                return;
            }
            if (f1.nw && f1.nwE) {
                f1.nodes = this.nodes;
                f1.trace_NW_E(oo, this.nodes.get(this.nodes.size() - 1));
            } else if (f1.nN) {
                f1.nodes = this.nodes;
                f1.trace_N_N(oo, null);
            }
        }

        void trace_W_W(CCObjectOutline[][] oo, CCNode extend) {
            if (extend != null) {
                extend.setXY((float)this.x - 0.3f, (float)this.y + 0.3f);
            } else {
                CCNode node2 = CCNode.alloc().init((float)this.x - 0.3f, (float)this.y + 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.wW = false;
            if (this.nwW) {
                this.trace_NW_W(oo, this.nodes.get(this.nodes.size() - 1));
            } else {
                CCObjectOutline f1 = CCObjectOutline.get(this.x - 1, this.y, this.z, oo);
                if (f1 == null) {
                    return;
                }
                if (f1.nS) {
                    f1.nodes = this.nodes;
                    f1.trace_N_S(oo, null);
                }
            }
        }

        void trace_N_N(CCObjectOutline[][] oo, CCNode extend) {
            CCNode node2;
            if (extend != null) {
                extend.setXY((float)(this.x + 1) - 0.3f, (float)this.y - 0.3f);
            } else {
                node2 = CCNode.alloc().init((float)(this.x + 1) - 0.3f, (float)this.y - 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.nN = false;
            if (this.nCutoff) {
                node2 = this.nodes.get(this.nodes.size() - 1);
                node2.setXY((float)(this.x + 1) + 0.3f, (float)this.y - 0.3f);
                node2 = CCNode.alloc().init((float)(this.x + 1) + 0.3f, (float)this.y + 0.3f, this.z);
                this.nodes.add(node2);
                node2 = CCNode.alloc().init((float)(this.x + 1) - 0.3f, (float)this.y + 0.3f, this.z);
                this.nodes.add(node2);
                this.trace_N_S(oo, node2);
                return;
            }
            CCObjectOutline f1 = CCObjectOutline.get(this.x + 1, this.y, this.z, oo);
            if (f1 == null) {
                return;
            }
            if (f1.nwN) {
                f1.nodes = this.nodes;
                f1.trace_NW_N(oo, this.nodes.get(this.nodes.size() - 1));
            } else {
                f1 = CCObjectOutline.get(this.x + 1, this.y - 1, this.z, oo);
                if (f1 == null) {
                    return;
                }
                if (f1.wW) {
                    f1.nodes = this.nodes;
                    f1.trace_W_W(oo, null);
                }
            }
        }

        void trace_N_S(CCObjectOutline[][] oo, CCNode extend) {
            if (extend != null) {
                extend.setXY((float)this.x + 0.3f, (float)this.y + 0.3f);
            } else {
                CCNode node2 = CCNode.alloc().init((float)this.x + 0.3f, (float)this.y + 0.3f, this.z);
                this.nodes.add(node2);
            }
            this.nS = false;
            if (this.nwS) {
                this.trace_NW_S(oo, this.nodes.get(this.nodes.size() - 1));
            } else if (this.wE) {
                this.trace_W_E(oo, null);
            }
        }

        void trace(CCObjectOutline[][] oo, ArrayList<CCNode> nodes) {
            nodes.clear();
            this.nodes = nodes;
            CCNode node1 = CCNode.alloc().init((float)this.x - 0.3f, (float)this.y - 0.3f, this.z);
            nodes.add(node1);
            this.trace_NW_N(oo, null);
            if (nodes.size() == 2 || node1.x != nodes.get((int)(nodes.size() - 1)).x || node1.y != nodes.get((int)(nodes.size() - 1)).y) {
                nodes.clear();
            } else {
                nodes.get(nodes.size() - 1).release();
                nodes.set(nodes.size() - 1, node1);
            }
        }

        static CCObjectOutline alloc() {
            return pool.isEmpty() ? new CCObjectOutline() : pool.pop();
        }

        void release() {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }

    static final class CompareIntersection
    implements Comparator<CCIntersection> {
        CCEdge edge;

        CompareIntersection() {
        }

        @Override
        public int compare(CCIntersection o1, CCIntersection o2) {
            float dist2;
            float dist1 = this.edge == o1.edge1 ? o1.dist1 : o1.dist2;
            float f = dist2 = this.edge == o2.edge1 ? o2.dist1 : o2.dist2;
            if (dist1 < dist2) {
                return -1;
            }
            if (dist1 > dist2) {
                return 1;
            }
            return 0;
        }
    }

    private static final class CCNode {
        float x;
        float y;
        int z;
        boolean ignore;
        final ArrayList<CCEdge> edges = new ArrayList();
        final ArrayList<CCNode> visible = new ArrayList();
        static ArrayList<CCObstacle> tempObstacles = new ArrayList();
        static ArrayDeque<CCNode> pool = new ArrayDeque();

        private CCNode() {
        }

        CCNode init(float x, float y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ignore = false;
            this.edges.clear();
            this.visible.clear();
            return this;
        }

        CCNode setXY(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        boolean sharesEdge(CCNode other) {
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                if (!edge.hasNode(other)) continue;
                return true;
            }
            return false;
        }

        boolean sharesShape(CCNode other) {
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                for (int j = 0; j < other.edges.size(); ++j) {
                    CCEdge edgeOther = other.edges.get(j);
                    if (edge.obstacle == null || edge.obstacle != edgeOther.obstacle) continue;
                    return true;
                }
            }
            return false;
        }

        void getObstacles(ArrayList<CCObstacle> obstacles) {
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                if (obstacles.contains(edge.obstacle)) continue;
                obstacles.add(edge.obstacle);
            }
        }

        boolean onSameShapeButDoesNotShareAnEdge(CCNode other) {
            tempObstacles.clear();
            this.getObstacles(tempObstacles);
            for (int i = 0; i < tempObstacles.size(); ++i) {
                CCObstacle obstacle = tempObstacles.get(i);
                if (!obstacle.hasNode(other) || obstacle.hasAdjacentNodes(this, other)) continue;
                return true;
            }
            return false;
        }

        boolean getNormalAndEdgeVectors(Vector2 normal, Vector2 edgeVec) {
            CCEdge edge1 = null;
            CCEdge edge2 = null;
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                if (!edge.node1.visible.contains(edge.node2)) continue;
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

        static CCNode alloc() {
            if (pool.isEmpty()) {
                boolean bl = false;
            } else {
                boolean bl = false;
            }
            return pool.isEmpty() ? new CCNode() : pool.pop();
        }

        void release() {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }

    private static final class CCObstacle {
        final ArrayList<CCEdge> edges = new ArrayList();
        ImmutableRectF bounds;
        static ArrayDeque<CCObstacle> pool = new ArrayDeque();

        private CCObstacle() {
        }

        CCObstacle init() {
            this.edges.clear();
            return this;
        }

        boolean hasNode(CCNode node) {
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                if (!edge.hasNode(node)) continue;
                return true;
            }
            return false;
        }

        boolean hasAdjacentNodes(CCNode node1, CCNode node2) {
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                if (!edge.hasNode(node1) || !edge.hasNode(node2)) continue;
                return true;
            }
            return false;
        }

        boolean isPointInPolygon_CrossingNumber(float x, float y) {
            int cn = 0;
            for (int i = 0; i < this.edges.size(); ++i) {
                float vt;
                CCEdge edge = this.edges.get(i);
                if (!(edge.node1.y <= y && edge.node2.y > y) && (!(edge.node1.y > y) || !(edge.node2.y <= y)) || !(x < edge.node1.x + (vt = (y - edge.node1.y) / (edge.node2.y - edge.node1.y)) * (edge.node2.x - edge.node1.x))) continue;
                ++cn;
            }
            return cn % 2 == 1;
        }

        float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
            return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        }

        EdgeRingHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
            int wn = 0;
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
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

        boolean isPointInside(float x, float y, int flags) {
            return this.isPointInPolygon_WindingNumber(x, y, flags) == EdgeRingHit.Inside;
        }

        boolean isNodeInsideOf(CCNode node) {
            if (this.hasNode(node)) {
                return false;
            }
            if (!this.bounds.containsPoint(node.x, node.y)) {
                return false;
            }
            boolean flags = false;
            return this.isPointInside(node.x, node.y, 0);
        }

        CCNode getClosestPointOnEdge(float x3, float y3, Vector2 out) {
            double closestDist = Double.MAX_VALUE;
            CCNode closestNode = null;
            float closestX = Float.MAX_VALUE;
            float closestY = Float.MAX_VALUE;
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
                if (!edge.node1.visible.contains(edge.node2)) continue;
                CCNode node = edge.closestPoint(x3, y3, out);
                double distSq = (x3 - out.x) * (x3 - out.x) + (y3 - out.y) * (y3 - out.y);
                if (!(distSq < closestDist)) continue;
                closestX = out.x;
                closestY = out.y;
                closestNode = node;
                closestDist = distSq;
            }
            out.set(closestX, closestY);
            return closestNode;
        }

        void calcBounds() {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float maxY = Float.MIN_VALUE;
            for (int i = 0; i < this.edges.size(); ++i) {
                CCEdge edge = this.edges.get(i);
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

        static CCObstacle alloc() {
            return pool.isEmpty() ? new CCObstacle() : pool.pop();
        }

        void release() {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }

    private static final class CCEdge {
        CCNode node1;
        CCNode node2;
        CCObstacle obstacle;
        final ArrayList<CCIntersection> intersections = new ArrayList();
        final Vector2 normal = new Vector2();
        static ArrayDeque<CCEdge> pool = new ArrayDeque();

        private CCEdge() {
        }

        CCEdge init(CCNode node1, CCNode node2, CCObstacle obstacle) {
            if (node1.x == node2.x && node1.y == node2.y) {
                boolean bl = false;
            }
            this.node1 = node1;
            this.node2 = node2;
            node1.edges.add(this);
            node2.edges.add(this);
            this.obstacle = obstacle;
            this.intersections.clear();
            this.normal.set(node2.x - node1.x, node2.y - node1.y);
            this.normal.normalize();
            this.normal.rotate((float)Math.toRadians(90.0));
            return this;
        }

        boolean hasNode(CCNode node) {
            return node == this.node1 || node == this.node2;
        }

        CCEdge split(CCNode nodeSplit) {
            CCEdge edgeNew = CCEdge.alloc().init(nodeSplit, this.node2, this.obstacle);
            this.obstacle.edges.add(this.obstacle.edges.indexOf(this) + 1, edgeNew);
            this.node2.edges.remove(this);
            this.node2 = nodeSplit;
            this.node2.edges.add(this);
            return edgeNew;
        }

        CCNode closestPoint(float x3, float y3, Vector2 closest) {
            float x1 = this.node1.x;
            float y1 = this.node1.y;
            float x2 = this.node2.x;
            float y2 = this.node2.y;
            double u = (double)((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double epsilon = 0.001;
            if (u <= 0.001) {
                closest.set(x1, y1);
                return this.node1;
            }
            if (u >= 0.999) {
                closest.set(x2, y2);
                return this.node2;
            }
            double xu = (double)x1 + u * (double)(x2 - x1);
            double yu = (double)y1 + u * (double)(y2 - y1);
            closest.set((float)xu, (float)yu);
            return null;
        }

        boolean isPointOn(float x3, float y3) {
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

        static CCEdge alloc() {
            return pool.isEmpty() ? new CCEdge() : pool.pop();
        }

        void release() {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }

    private static final class CCIntersection {
        CCEdge edge1;
        CCEdge edge2;
        float dist1;
        float dist2;
        CCNode nodeSplit;
        static ArrayDeque<CCIntersection> pool = new ArrayDeque();

        private CCIntersection() {
        }

        CCIntersection init(CCEdge edge1, CCEdge edge2, float dist1, float dist2, float x, float y) {
            this.edge1 = edge1;
            this.edge2 = edge2;
            this.dist1 = dist1;
            this.dist2 = dist2;
            this.nodeSplit = CCNode.alloc().init(x, y, edge1.node1.z);
            return this;
        }

        CCIntersection init(CCEdge edge1, CCEdge edge2, float dist1, float dist2, CCNode nodeSplit) {
            this.edge1 = edge1;
            this.edge2 = edge2;
            this.dist1 = dist1;
            this.dist2 = dist2;
            this.nodeSplit = nodeSplit;
            return this;
        }

        CCEdge split(CCEdge edge) {
            if (edge.hasNode(this.nodeSplit)) {
                return null;
            }
            if (edge.node1.x == this.nodeSplit.x && edge.node1.y == this.nodeSplit.y) {
                return null;
            }
            if (edge.node2.x == this.nodeSplit.x && edge.node2.y == this.nodeSplit.y) {
                return null;
            }
            return edge.split(this.nodeSplit);
        }

        static CCIntersection alloc() {
            return pool.isEmpty() ? new CCIntersection() : pool.pop();
        }

        void release() {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }

    private static enum EdgeRingHit {
        OnEdge,
        Inside,
        Outside;

    }
}

