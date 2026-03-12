/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import gnu.trove.list.array.TIntArrayList;
import java.awt.geom.Line2D;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.pathfind.AdjustStartEndNodeData;
import zombie.pathfind.Chunk;
import zombie.pathfind.ClosestPointOnEdge;
import zombie.pathfind.ClusterOutline;
import zombie.pathfind.ClusterOutlineGrid;
import zombie.pathfind.Connection;
import zombie.pathfind.Edge;
import zombie.pathfind.EdgeRing;
import zombie.pathfind.ImmutableRectF;
import zombie.pathfind.Intersection;
import zombie.pathfind.Node;
import zombie.pathfind.ObjectOutline;
import zombie.pathfind.Obstacle;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.Vehicle;
import zombie.pathfind.VehicleCluster;
import zombie.pathfind.VehiclePoly;
import zombie.pathfind.VehicleRect;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;

public final class VisibilityGraph {
    boolean created;
    public VehicleCluster cluster;
    final ArrayList<Node> nodes = new ArrayList();
    public final ArrayList<Edge> edges = new ArrayList();
    final ArrayList<Obstacle> obstacles = new ArrayList();
    final ArrayList<Node> intersectNodes = new ArrayList();
    public final ArrayList<Node> perimeterNodes = new ArrayList();
    public final ArrayList<Edge> perimeterEdges = new ArrayList();
    final ArrayList<Node> obstacleTraceNodes = new ArrayList();
    final ArrayList<Chunk> overlappedChunks = new ArrayList();
    final TIntArrayList splitXy = new TIntArrayList();
    static final CompareIntersection comparator = new CompareIntersection();
    private static final ClusterOutlineGrid clusterOutlineGrid = new ClusterOutlineGrid();
    private static final ArrayDeque<VisibilityGraph> pool = new ArrayDeque();

    VisibilityGraph init(VehicleCluster cluster) {
        this.created = false;
        this.cluster = cluster;
        this.edges.clear();
        this.nodes.clear();
        this.obstacles.clear();
        this.intersectNodes.clear();
        this.perimeterEdges.clear();
        this.perimeterNodes.clear();
        return this;
    }

    public boolean isCreated() {
        return this.created;
    }

    void addEdgesForVehicle(Vehicle vehicle) {
        VehiclePoly poly = vehicle.polyPlusRadius;
        int z = PZMath.fastfloor(poly.z);
        Node nodeFrontRight = Node.alloc().init(poly.x1, poly.y1, z);
        Node nodeFrontLeft = Node.alloc().init(poly.x2, poly.y2, z);
        Node nodeRearLeft = Node.alloc().init(poly.x3, poly.y3, z);
        Node nodeRearRight = Node.alloc().init(poly.x4, poly.y4, z);
        Obstacle obstacle = Obstacle.alloc().init(vehicle);
        this.obstacles.add(obstacle);
        Edge edgeFront = Edge.alloc().init(nodeFrontRight, nodeFrontLeft, obstacle, obstacle.outer);
        Edge edgeLeft = Edge.alloc().init(nodeFrontLeft, nodeRearLeft, obstacle, obstacle.outer);
        Edge edgeRear = Edge.alloc().init(nodeRearLeft, nodeRearRight, obstacle, obstacle.outer);
        Edge edgeRight = Edge.alloc().init(nodeRearRight, nodeFrontRight, obstacle, obstacle.outer);
        obstacle.outer.add(edgeFront);
        obstacle.outer.add(edgeLeft);
        obstacle.outer.add(edgeRear);
        obstacle.outer.add(edgeRight);
        obstacle.calcBounds();
        this.nodes.add(nodeFrontRight);
        this.nodes.add(nodeFrontLeft);
        this.nodes.add(nodeRearLeft);
        this.nodes.add(nodeRearRight);
        this.edges.add(edgeFront);
        this.edges.add(edgeLeft);
        this.edges.add(edgeRear);
        this.edges.add(edgeRight);
        if (vehicle.upVectorDot < 0.95f) {
            return;
        }
        obstacle.nodeCrawlFront = Node.alloc().init((poly.x1 + poly.x2) / 2.0f, (poly.y1 + poly.y2) / 2.0f, z);
        obstacle.nodeCrawlRear = Node.alloc().init((poly.x3 + poly.x4) / 2.0f, (poly.y3 + poly.y4) / 2.0f, z);
        obstacle.nodeCrawlFront.flags |= 1;
        obstacle.nodeCrawlRear.flags |= 1;
        this.nodes.add(obstacle.nodeCrawlFront);
        this.nodes.add(obstacle.nodeCrawlRear);
        Edge edgeCrawl1 = edgeFront.split(obstacle.nodeCrawlFront);
        Edge edgeCrawl3 = edgeRear.split(obstacle.nodeCrawlRear);
        this.edges.add(edgeCrawl1);
        this.edges.add(edgeCrawl3);
        BaseVehicle.Vector2fObjectPool pool = BaseVehicle.TL_vector2f_pool.get();
        Vector2f v1 = (Vector2f)pool.alloc();
        Vector2f v2 = (Vector2f)pool.alloc();
        obstacle.crawlNodes.clear();
        for (int i = 0; i < vehicle.crawlOffsets.size(); ++i) {
            float frac = vehicle.crawlOffsets.get(i);
            v1.set(nodeRearLeft.x, nodeRearLeft.y);
            v2.set(nodeFrontLeft.x, nodeFrontLeft.y);
            v2.sub(v1).mul(frac).add(v1);
            Node nodeLeft = Node.alloc().init(v2.x, v2.y, z);
            nodeLeft.flags |= 1;
            v1.set(nodeRearRight.x, nodeRearRight.y);
            v2.set(nodeFrontRight.x, nodeFrontRight.y);
            v2.sub(v1).mul(frac).add(v1);
            Node nodeRight = Node.alloc().init(v2.x, v2.y, z);
            nodeRight.flags |= 1;
            Node nodeMid = Node.alloc().init((nodeLeft.x + nodeRight.x) / 2.0f, (nodeLeft.y + nodeRight.y) / 2.0f, z);
            nodeMid.flags |= 3;
            obstacle.crawlNodes.add(nodeLeft);
            obstacle.crawlNodes.add(nodeMid);
            obstacle.crawlNodes.add(nodeRight);
            this.nodes.add(nodeLeft);
            this.nodes.add(nodeMid);
            this.nodes.add(nodeRight);
            Edge edgeLeft2 = edgeLeft.split(nodeLeft);
            edgeRight = edgeRight.split(nodeRight);
            this.edges.add(edgeLeft2);
            this.edges.add(edgeRight);
        }
        pool.release(v1);
        pool.release(v2);
    }

    boolean isVisible(Node node1, Node node2) {
        Edge edge;
        int i;
        if (node1.sharesEdge(node2)) {
            return !node1.onSameShapeButDoesNotShareAnEdge(node2);
        }
        if (node1.sharesShape(node2)) {
            return false;
        }
        for (i = 0; i < this.edges.size(); ++i) {
            edge = this.edges.get(i);
            if (!this.intersects(node1, node2, edge)) continue;
            return false;
        }
        for (i = 0; i < this.perimeterEdges.size(); ++i) {
            edge = this.perimeterEdges.get(i);
            if (!this.intersects(node1, node2, edge)) continue;
            return false;
        }
        return true;
    }

    boolean intersects(Node node1, Node node2, Edge edge) {
        if (edge.hasNode(node1) || edge.hasNode(node2)) {
            return false;
        }
        return Line2D.linesIntersect(node1.x, node1.y, node2.x, node2.y, edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y);
    }

    public Intersection getIntersection(Edge edge1, Edge edge2) {
        float y4 = edge2.node2.y;
        float y3 = edge2.node1.y;
        float x2 = edge1.node2.x;
        float x1 = edge1.node1.x;
        float x4 = edge2.node2.x;
        float x3 = edge2.node1.x;
        float y2 = edge1.node2.y;
        float y1 = edge1.node1.y;
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) {
            return null;
        }
        double ua = (double)((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        double ub = (double)((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
        if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
            float intersectX = (float)((double)x1 + ua * (double)(x2 - x1));
            float intersectY = (float)((double)y1 + ua * (double)(y2 - y1));
            return new Intersection(edge1, edge2, (float)ua, (float)ub, intersectX, intersectY);
        }
        return null;
    }

    @Deprecated
    void addWorldObstacles() {
        int x;
        int y;
        VehicleRect bounds = this.cluster.bounds();
        --bounds.x;
        --bounds.y;
        bounds.w += 3;
        bounds.h += 3;
        ObjectOutline[][] oo = new ObjectOutline[bounds.w][bounds.h];
        int z = this.cluster.z;
        for (y = bounds.top(); y < bounds.bottom() - 1; ++y) {
            for (x = bounds.left(); x < bounds.right() - 1; ++x) {
                Square square = PolygonalMap2.instance.getSquare(x, y, z);
                if (square == null || !this.contains(square, 1)) continue;
                if (square.has(504) || square.isReallySolid()) {
                    ObjectOutline.setSolid(x - bounds.left(), y - bounds.top(), z, oo);
                }
                if (square.has(2)) {
                    ObjectOutline.setWest(x - bounds.left(), y - bounds.top(), z, oo);
                }
                if (square.has(4)) {
                    ObjectOutline.setNorth(x - bounds.left(), y - bounds.top(), z, oo);
                }
                if (square.has(262144)) {
                    ObjectOutline.setWest(x - bounds.left() + 1, y - bounds.top(), z, oo);
                }
                if (!square.has(524288)) continue;
                ObjectOutline.setNorth(x - bounds.left(), y - bounds.top() + 1, z, oo);
            }
        }
        for (y = 0; y < bounds.h; ++y) {
            for (x = 0; x < bounds.w; ++x) {
                ObjectOutline f = ObjectOutline.get(x, y, z, oo);
                if (f == null || !f.nw || !f.nwW || !f.nwN) continue;
                f.trace(oo, this.obstacleTraceNodes);
                if (f.nodes.isEmpty()) continue;
                Obstacle obstacle = Obstacle.alloc().init((IsoGridSquare)null);
                for (int i = 0; i < f.nodes.size() - 1; ++i) {
                    Node node1 = f.nodes.get(i);
                    Node node2 = f.nodes.get(i + 1);
                    node1.x += (float)bounds.left();
                    node1.y += (float)bounds.top();
                    if (!this.contains(node1.x, node1.y, node1.z)) {
                        node1.ignore = true;
                    }
                    Edge edge = Edge.alloc().init(node1, node2, obstacle, obstacle.outer);
                    obstacle.outer.add(edge);
                    this.nodes.add(node1);
                }
                obstacle.calcBounds();
                this.obstacles.add(obstacle);
                this.edges.addAll(obstacle.outer);
            }
        }
        for (y = 0; y < bounds.h; ++y) {
            for (x = 0; x < bounds.w; ++x) {
                if (oo[x][y] == null) continue;
                oo[x][y].release();
            }
        }
        bounds.release();
    }

    void addWorldObstaclesClipper() {
        VehicleRect bounds = this.cluster.bounds();
        --bounds.x;
        --bounds.y;
        bounds.w += 2;
        bounds.h += 2;
        if (PolygonalMap2.instance.clipperThread == null) {
            PolygonalMap2.instance.clipperThread = new Clipper();
        }
        Clipper clipper = PolygonalMap2.instance.clipperThread;
        clipper.clear();
        int z = this.cluster.z;
        for (int y = bounds.top(); y < bounds.bottom(); ++y) {
            for (int x = bounds.left(); x < bounds.right(); ++x) {
                Square square = PolygonalMap2.instance.getSquare(x, y, z);
                if (square == null || !this.contains(square, 1)) continue;
                if (square.has(504) || square.isReallySolid()) {
                    clipper.addAABB((float)x - 0.3f, (float)y - 0.3f, (float)(x + 1) + 0.3f, (float)(y + 1) + 0.3f);
                }
                boolean bCollideW = square.has(2);
                bCollideW |= square.isSlopedSurfaceEdgeBlocked(IsoDirections.W);
                Square squareW = square.getAdjacentSquare(IsoDirections.W);
                if (squareW != null && squareW.isSlopedSurfaceEdgeBlocked(IsoDirections.E)) {
                    bCollideW = true;
                }
                if (bCollideW) {
                    clipper.addAABB((float)x - 0.3f, (float)y - 0.3f, (float)x + 0.3f, (float)(y + 1) + 0.3f);
                }
                boolean bCollideN = square.has(4);
                bCollideN |= square.isSlopedSurfaceEdgeBlocked(IsoDirections.N);
                Square squareN = square.getAdjacentSquare(IsoDirections.N);
                if (squareN != null && squareN.isSlopedSurfaceEdgeBlocked(IsoDirections.S)) {
                    bCollideN = true;
                }
                if (!bCollideN) continue;
                clipper.addAABB((float)x - 0.3f, (float)y - 0.3f, (float)(x + 1) + 0.3f, (float)y + 0.3f);
            }
        }
        bounds.release();
        ByteBuffer polyBuffer = PolygonalMap2.instance.xyBufferThread;
        int polyCount = clipper.generatePolygons();
        for (int i = 0; i < polyCount; ++i) {
            int j;
            polyBuffer.clear();
            clipper.getPolygon(i, polyBuffer);
            Obstacle obstacle = Obstacle.alloc().init((IsoGridSquare)null);
            this.getEdgesFromBuffer(polyBuffer, obstacle, true, z);
            int holeCount = polyBuffer.getShort();
            for (j = 0; j < holeCount; ++j) {
                this.getEdgesFromBuffer(polyBuffer, obstacle, false, z);
            }
            obstacle.calcBounds();
            this.obstacles.add(obstacle);
            this.edges.addAll(obstacle.outer);
            for (j = 0; j < obstacle.inner.size(); ++j) {
                this.edges.addAll(obstacle.inner.get(j));
            }
        }
    }

    void getEdgesFromBuffer(ByteBuffer polyBuffer, Obstacle obstacle, boolean outer, int z) {
        int j;
        short pointCount = polyBuffer.getShort();
        if (pointCount < 3) {
            polyBuffer.position(polyBuffer.position() + pointCount * 4 * 2);
            return;
        }
        EdgeRing edges = obstacle.outer;
        if (!outer) {
            edges = EdgeRing.alloc();
            edges.clear();
            obstacle.inner.add(edges);
        }
        int nodeFirst = this.nodes.size();
        for (j = pointCount - 1; j >= 0; --j) {
            float x = polyBuffer.getFloat();
            float y = polyBuffer.getFloat();
            Node node1 = Node.alloc().init(x, y, z);
            this.nodes.add(node1);
        }
        for (j = nodeFirst; j < this.nodes.size() - 1; ++j) {
            Node node1 = this.nodes.get(j);
            Node node2 = this.nodes.get(j + 1);
            if (!this.contains(node1.x, node1.y, node1.z)) {
                node1.ignore = true;
            }
            Edge edge1 = Edge.alloc().init(node1, node2, obstacle, edges);
            edges.add(edge1);
        }
        Node node1 = this.nodes.get(this.nodes.size() - 1);
        Node node2 = this.nodes.get(nodeFirst);
        Edge edge1 = Edge.alloc().init(node1, node2, obstacle, edges);
        edges.add(edge1);
    }

    void trySplit(Edge edge, VehicleRect rect, TIntArrayList splitXY) {
        if (Math.abs(edge.node1.x - edge.node2.x) > Math.abs(edge.node1.y - edge.node2.y)) {
            float edgeLeft = Math.min(edge.node1.x, edge.node2.x);
            float edgeRight = Math.max(edge.node1.x, edge.node2.x);
            float edgeY = edge.node1.y;
            if ((float)rect.left() > edgeLeft && (float)rect.left() < edgeRight && (float)rect.top() < edgeY && (float)rect.bottom() > edgeY && !splitXY.contains(rect.left()) && !this.contains((float)rect.left() - 0.5f, edgeY, this.cluster.z)) {
                splitXY.add(rect.left());
            }
            if ((float)rect.right() > edgeLeft && (float)rect.right() < edgeRight && (float)rect.top() < edgeY && (float)rect.bottom() > edgeY && !splitXY.contains(rect.right()) && !this.contains((float)rect.right() + 0.5f, edgeY, this.cluster.z)) {
                splitXY.add(rect.right());
            }
        } else {
            float edgeTop = Math.min(edge.node1.y, edge.node2.y);
            float edgeBottom = Math.max(edge.node1.y, edge.node2.y);
            float edgeX = edge.node1.x;
            if ((float)rect.top() > edgeTop && (float)rect.top() < edgeBottom && (float)rect.left() < edgeX && (float)rect.right() > edgeX && !splitXY.contains(rect.top()) && !this.contains(edgeX, (float)rect.top() - 0.5f, this.cluster.z)) {
                splitXY.add(rect.top());
            }
            if ((float)rect.bottom() > edgeTop && (float)rect.bottom() < edgeBottom && (float)rect.left() < edgeX && (float)rect.right() > edgeX && !splitXY.contains(rect.bottom()) && !this.contains(edgeX, (float)rect.bottom() + 0.5f, this.cluster.z)) {
                splitXY.add(rect.bottom());
            }
        }
    }

    void splitWorldObstacleEdges(EdgeRing edges) {
        for (int j = edges.size() - 1; j >= 0; --j) {
            Edge edgeNew;
            Node nodeSplit;
            int n;
            Edge edge = (Edge)edges.get(j);
            this.splitXy.clear();
            for (int m = 0; m < this.cluster.rects.size(); ++m) {
                VehicleRect rect = this.cluster.rects.get(m);
                this.trySplit(edge, rect, this.splitXy);
            }
            if (this.splitXy.isEmpty()) continue;
            this.splitXy.sort();
            if (Math.abs(edge.node1.x - edge.node2.x) > Math.abs(edge.node1.y - edge.node2.y)) {
                if (edge.node1.x < edge.node2.x) {
                    for (n = this.splitXy.size() - 1; n >= 0; --n) {
                        nodeSplit = Node.alloc().init(this.splitXy.get(n), edge.node1.y, this.cluster.z);
                        edgeNew = edge.split(nodeSplit);
                        this.nodes.add(nodeSplit);
                        this.edges.add(edgeNew);
                    }
                    continue;
                }
                for (n = 0; n < this.splitXy.size(); ++n) {
                    nodeSplit = Node.alloc().init(this.splitXy.get(n), edge.node1.y, this.cluster.z);
                    edgeNew = edge.split(nodeSplit);
                    this.nodes.add(nodeSplit);
                    this.edges.add(edgeNew);
                }
                continue;
            }
            if (edge.node1.y < edge.node2.y) {
                for (n = this.splitXy.size() - 1; n >= 0; --n) {
                    nodeSplit = Node.alloc().init(edge.node1.x, this.splitXy.get(n), this.cluster.z);
                    edgeNew = edge.split(nodeSplit);
                    this.nodes.add(nodeSplit);
                    this.edges.add(edgeNew);
                }
                continue;
            }
            for (n = 0; n < this.splitXy.size(); ++n) {
                nodeSplit = Node.alloc().init(edge.node1.x, this.splitXy.get(n), this.cluster.z);
                edgeNew = edge.split(nodeSplit);
                this.nodes.add(nodeSplit);
                this.edges.add(edgeNew);
            }
        }
    }

    void getStairSquares(ArrayList<Square> squares) {
        VehicleRect bounds = this.cluster.bounds();
        bounds.x -= 4;
        bounds.w += 4;
        ++bounds.w;
        bounds.y -= 4;
        bounds.h += 4;
        ++bounds.h;
        for (int y = bounds.top(); y < bounds.bottom(); ++y) {
            for (int x = bounds.left(); x < bounds.right(); ++x) {
                Square square = PolygonalMap2.instance.getSquare(x, y, this.cluster.z);
                if (square == null || !square.has(72) || squares.contains(square)) continue;
                squares.add(square);
            }
        }
        bounds.release();
    }

    void getCanPathSquares(ArrayList<Square> squares) {
        VehicleRect bounds = this.cluster.bounds();
        --bounds.x;
        bounds.w += 2;
        --bounds.y;
        bounds.h += 2;
        for (int y = bounds.top(); y < bounds.bottom(); ++y) {
            for (int x = bounds.left(); x < bounds.right(); ++x) {
                Square square = PolygonalMap2.instance.getSquare(x, y, this.cluster.z);
                if (square == null || !square.isCanPathW() && !square.isCanPathN() || squares.contains(square)) continue;
                squares.add(square);
            }
        }
        bounds.release();
    }

    void connectVehicleCrawlNodes() {
        for (int i = 0; i < this.obstacles.size(); ++i) {
            int j;
            Obstacle obstacle = this.obstacles.get(i);
            if (obstacle.vehicle == null || obstacle.nodeCrawlFront == null) continue;
            for (j = 0; j < obstacle.crawlNodes.size(); j += 3) {
                Node nodeLeft = obstacle.crawlNodes.get(j);
                Node nodeMid = obstacle.crawlNodes.get(j + 1);
                Node nodeRight = obstacle.crawlNodes.get(j + 2);
                PolygonalMap2.instance.connectTwoNodes(nodeLeft, nodeMid);
                PolygonalMap2.instance.connectTwoNodes(nodeRight, nodeMid);
                if (j + 3 >= obstacle.crawlNodes.size()) continue;
                Node nodeMidNext = obstacle.crawlNodes.get(j + 3 + 1);
                PolygonalMap2.instance.connectTwoNodes(nodeMid, nodeMidNext);
            }
            if (!obstacle.crawlNodes.isEmpty()) {
                int n = obstacle.crawlNodes.size() - 2;
                Node nodeMid = obstacle.crawlNodes.get(n);
                PolygonalMap2.instance.connectTwoNodes(obstacle.nodeCrawlFront, nodeMid);
                n = 1;
                nodeMid = obstacle.crawlNodes.get(n);
                PolygonalMap2.instance.connectTwoNodes(obstacle.nodeCrawlRear, nodeMid);
            }
            if (!obstacle.crawlNodes.isEmpty()) {
                ImmutableRectF bounds = obstacle.bounds;
                int minX = PZMath.fastfloor(bounds.left());
                int minY = PZMath.fastfloor(bounds.top());
                int maxX = (int)Math.ceil(bounds.right());
                int maxY = (int)Math.ceil(bounds.bottom());
                for (int y = minY; y < maxY; ++y) {
                    for (int x = minX; x < maxX; ++x) {
                        Square squareCanPathInside = PolygonalMap2.instance.getSquare(x, y, this.cluster.z);
                        if (squareCanPathInside == null || !obstacle.isPointInside((float)x + 0.5f, (float)y + 0.5f)) continue;
                        Node nodeInside = PolygonalMap2.instance.getNodeForSquare(squareCanPathInside);
                        for (int j2 = nodeInside.visible.size() - 1; j2 >= 0; --j2) {
                            Connection cxn = nodeInside.visible.get(j2);
                            if (!cxn.has(1)) continue;
                            Node nodeOutside = cxn.otherNode(nodeInside);
                            Node nodeMid = obstacle.getClosestInteriorCrawlNode(nodeInside.x, nodeInside.y);
                            for (int k = 0; k < obstacle.outer.size(); ++k) {
                                Edge edge = (Edge)obstacle.outer.get(k);
                                float y4 = cxn.node2.y;
                                float y3 = cxn.node1.y;
                                float x2 = edge.node2.x;
                                float x1 = edge.node1.x;
                                float x4 = cxn.node2.x;
                                float x3 = cxn.node1.x;
                                float y2 = edge.node2.y;
                                float y1 = edge.node1.y;
                                double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
                                if (denom == 0.0) continue;
                                double ua = (double)((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
                                double ub = (double)((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
                                if (!(ua >= 0.0) || !(ua <= 1.0) || !(ub >= 0.0) || !(ub <= 1.0)) continue;
                                float intersectX = (float)((double)x1 + ua * (double)(x2 - x1));
                                float intersectY = (float)((double)y1 + ua * (double)(y2 - y1));
                                Node nodeSplit = Node.alloc().init(intersectX, intersectY, this.cluster.z);
                                nodeSplit.flags |= 1;
                                boolean bConnected = edge.node1.isConnectedTo(edge.node2);
                                Edge edgeNew = edge.split(nodeSplit);
                                if (bConnected) {
                                    PolygonalMap2.instance.connectTwoNodes(edge.node1, edge.node2);
                                    PolygonalMap2.instance.connectTwoNodes(edgeNew.node1, edgeNew.node2);
                                }
                                this.edges.add(edgeNew);
                                this.nodes.add(nodeSplit);
                                PolygonalMap2.instance.connectTwoNodes(nodeOutside, nodeSplit, cxn.flags & 2 | 1);
                                PolygonalMap2.instance.connectTwoNodes(nodeSplit, nodeMid, 0);
                                break;
                            }
                            PolygonalMap2.instance.breakConnection(cxn);
                        }
                    }
                }
            }
            for (j = i + 1; j < this.obstacles.size(); ++j) {
                Obstacle obstacle2 = this.obstacles.get(j);
                if (obstacle2.vehicle == null || obstacle2.nodeCrawlFront == null) continue;
                obstacle.connectCrawlNodes(this, obstacle2);
                obstacle2.connectCrawlNodes(this, obstacle);
            }
        }
    }

    void checkEdgeIntersection() {
        int j;
        int i;
        for (i = 0; i < this.obstacles.size(); ++i) {
            Obstacle obstacle1 = this.obstacles.get(i);
            for (j = i + 1; j < this.obstacles.size(); ++j) {
                Obstacle obstacle2 = this.obstacles.get(j);
                if (!obstacle1.bounds.intersects(obstacle2.bounds)) continue;
                this.checkEdgeIntersection(obstacle1.outer, obstacle2.outer);
                for (int m = 0; m < obstacle2.inner.size(); ++m) {
                    EdgeRing inner2 = obstacle2.inner.get(m);
                    this.checkEdgeIntersection(obstacle1.outer, inner2);
                }
                for (int k = 0; k < obstacle1.inner.size(); ++k) {
                    EdgeRing inner1 = obstacle1.inner.get(k);
                    this.checkEdgeIntersection(inner1, obstacle2.outer);
                    for (int m = 0; m < obstacle2.inner.size(); ++m) {
                        EdgeRing inner2 = obstacle2.inner.get(m);
                        this.checkEdgeIntersection(inner1, inner2);
                    }
                }
            }
        }
        for (i = 0; i < this.obstacles.size(); ++i) {
            Obstacle obstacle = this.obstacles.get(i);
            this.checkEdgeIntersectionSplit(obstacle.outer);
            for (j = 0; j < obstacle.inner.size(); ++j) {
                this.checkEdgeIntersectionSplit(obstacle.inner.get(j));
            }
        }
    }

    void checkEdgeIntersection(EdgeRing edges1, EdgeRing edges2) {
        for (int i = 0; i < edges1.size(); ++i) {
            Edge edge1 = (Edge)edges1.get(i);
            for (int j = 0; j < edges2.size(); ++j) {
                Intersection intersection;
                Edge edge2 = (Edge)edges2.get(j);
                if (!this.intersects(edge1.node1, edge1.node2, edge2) || (intersection = this.getIntersection(edge1, edge2)) == null) continue;
                edge1.intersections.add(intersection);
                edge2.intersections.add(intersection);
                this.nodes.add(intersection.nodeSplit);
                this.intersectNodes.add(intersection.nodeSplit);
            }
        }
    }

    void checkEdgeIntersectionSplit(EdgeRing edges) {
        for (int i = edges.size() - 1; i >= 0; --i) {
            Edge edge = (Edge)edges.get(i);
            if (edge.intersections.isEmpty()) continue;
            VisibilityGraph.comparator.edge = edge;
            Collections.sort(edge.intersections, comparator);
            for (int k = edge.intersections.size() - 1; k >= 0; --k) {
                Intersection intersection = edge.intersections.get(k);
                Edge edgeNew = intersection.split(edge);
                this.edges.add(edgeNew);
            }
        }
    }

    void checkNodesInObstacles() {
        Obstacle obstacle;
        int j;
        Node node;
        int i;
        block0: for (i = 0; i < this.nodes.size(); ++i) {
            node = this.nodes.get(i);
            for (j = 0; j < this.obstacles.size(); ++j) {
                obstacle = this.obstacles.get(j);
                if (!obstacle.isNodeInsideOf(node)) continue;
                node.ignore = true;
                continue block0;
            }
        }
        block2: for (i = 0; i < this.perimeterNodes.size(); ++i) {
            node = this.perimeterNodes.get(i);
            for (j = 0; j < this.obstacles.size(); ++j) {
                obstacle = this.obstacles.get(j);
                if (!obstacle.isNodeInsideOf(node)) continue;
                node.ignore = true;
                continue block2;
            }
        }
    }

    void addPerimeterEdges() {
        int y;
        VehicleRect bounds = this.cluster.bounds();
        --bounds.x;
        --bounds.y;
        bounds.w += 2;
        bounds.h += 2;
        ClusterOutlineGrid cog = clusterOutlineGrid.setSize(bounds.w, bounds.h);
        int z = this.cluster.z;
        for (int i = 0; i < this.cluster.rects.size(); ++i) {
            VehicleRect rect = this.cluster.rects.get(i);
            rect = VehicleRect.alloc().init(rect.x - 1, rect.y - 1, rect.w + 2, rect.h + 2, rect.z);
            for (int y2 = rect.top(); y2 < rect.bottom(); ++y2) {
                for (int x = rect.left(); x < rect.right(); ++x) {
                    cog.setInner(x - bounds.left(), y2 - bounds.top(), z);
                }
            }
            rect.release();
        }
        for (y = 0; y < bounds.h; ++y) {
            for (int x = 0; x < bounds.w; ++x) {
                ClusterOutline f = cog.get(x, y, z);
                if (!f.inner) continue;
                if (!cog.isInner(x - 1, y, z)) {
                    f.w = true;
                }
                if (!cog.isInner(x, y - 1, z)) {
                    f.n = true;
                }
                if (!cog.isInner(x + 1, y, z)) {
                    f.e = true;
                }
                if (cog.isInner(x, y + 1, z)) continue;
                f.s = true;
            }
        }
        for (y = 0; y < bounds.h; ++y) {
            for (int x = 0; x < bounds.w; ++x) {
                ArrayList<Node> nodes;
                Square square;
                ClusterOutline f = cog.get(x, y, z);
                if (f != null && (f.w || f.n || f.e || f.s || f.innerCorner) && (square = PolygonalMap2.instance.getSquare(bounds.x + x, bounds.y + y, z)) != null && !square.isNonThumpableSolid() && !square.has(504)) {
                    Node node = PolygonalMap2.instance.getNodeForSquare(square);
                    node.flags |= 8;
                    node.addGraph(this);
                    this.perimeterNodes.add(node);
                }
                if (f == null || !f.n || !f.w || !f.inner || f.tw | f.tn | f.te | f.ts || (nodes = cog.trace(f)).isEmpty()) continue;
                for (int i = 0; i < nodes.size() - 1; ++i) {
                    Node node1 = nodes.get(i);
                    Node node2 = nodes.get(i + 1);
                    node1.x += (float)bounds.left();
                    node1.y += (float)bounds.top();
                    Edge edge = Edge.alloc().init(node1, node2, null, null);
                    this.perimeterEdges.add(edge);
                }
                if (nodes.get(nodes.size() - 1) == nodes.get(0)) continue;
                nodes.get((int)(nodes.size() - 1)).x += (float)bounds.left();
                nodes.get((int)(nodes.size() - 1)).y += (float)bounds.top();
            }
        }
        cog.releaseElements();
        bounds.release();
    }

    void calculateNodeVisibility() {
        ArrayList<Node> nodes1 = new ArrayList<Node>();
        nodes1.addAll(this.nodes);
        nodes1.addAll(this.perimeterNodes);
        for (int i = 0; i < nodes1.size(); ++i) {
            Node node1 = (Node)nodes1.get(i);
            if (node1.ignore || node1.square != null && node1.square.has(504)) continue;
            for (int j = i + 1; j < nodes1.size(); ++j) {
                Node node2 = (Node)nodes1.get(j);
                if (node2.ignore || node2.square != null && node2.square.has(504) || node1.hasFlag(8) && node2.hasFlag(8)) continue;
                if (node1.isConnectedTo(node2)) {
                    if ((node1.square == null || !node1.square.isCanPathW() && !node1.square.isCanPathN()) && (node2.square == null || !node2.square.isCanPathW() && !node2.square.isCanPathN())) assert (false);
                    continue;
                }
                if (!this.isVisible(node1, node2)) continue;
                PolygonalMap2.instance.connectTwoNodes(node1, node2);
            }
        }
    }

    public void addNode(Node node) {
        if (this.created && !node.ignore) {
            ArrayList<Node> nodes1 = new ArrayList<Node>();
            nodes1.addAll(this.nodes);
            nodes1.addAll(this.perimeterNodes);
            for (int i = 0; i < nodes1.size(); ++i) {
                Node node1 = (Node)nodes1.get(i);
                if (node1.ignore || !this.isVisible(node1, node)) continue;
                PolygonalMap2.instance.connectTwoNodes(node, node1);
            }
        }
        this.nodes.add(node);
    }

    public void removeNode(Node node) {
        this.nodes.remove(node);
        for (int i = node.visible.size() - 1; i >= 0; --i) {
            Connection visible = node.visible.get(i);
            PolygonalMap2.instance.breakConnection(visible);
        }
    }

    boolean contains(float x, float y, int z) {
        for (int i = 0; i < this.cluster.rects.size(); ++i) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (!rect.containsPoint(x, y, z)) continue;
            return true;
        }
        return false;
    }

    boolean contains(float x, float y, int z, int expand) {
        for (int i = 0; i < this.cluster.rects.size(); ++i) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (!rect.containsPoint(x, y, z, expand)) continue;
            return true;
        }
        return false;
    }

    public boolean contains(Square square) {
        for (int i = 0; i < this.cluster.rects.size(); ++i) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (!rect.containsPoint((float)square.x + 0.5f, (float)square.y + 0.5f, square.z)) continue;
            return true;
        }
        return false;
    }

    public boolean contains(Square square, int expand) {
        for (int i = 0; i < this.cluster.rects.size(); ++i) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (!rect.containsPoint((float)square.x + 0.5f, (float)square.y + 0.5f, square.z, expand)) continue;
            return true;
        }
        return false;
    }

    public boolean intersects(int squareMinX, int squareMinY, int squareMaxX, int squareMaxY, int expand) {
        for (int i = 0; i < this.cluster.rects.size(); ++i) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (!rect.intersects(squareMinX, squareMinY, squareMaxX, squareMaxY, expand)) continue;
            return true;
        }
        return false;
    }

    public int getPointOutsideObstacles(float x, float y, float z, AdjustStartEndNodeData adjust) {
        ClosestPointOnEdge closestPointOnEdge = PolygonalMap2.instance.closestPointOnEdge;
        double closestDistSq = Double.MAX_VALUE;
        Edge closestEdge = null;
        Node closestNode = null;
        float closestX = 0.0f;
        float closestY = 0.0f;
        for (int i = 0; i < this.obstacles.size(); ++i) {
            Obstacle obstacle = this.obstacles.get(i);
            if (!obstacle.bounds.containsPoint(x, y) || !obstacle.isPointInside(x, y)) continue;
            obstacle.getClosestPointOnEdge(x, y, closestPointOnEdge);
            if (closestPointOnEdge.edge == null || !(closestPointOnEdge.distSq < closestDistSq)) continue;
            closestDistSq = closestPointOnEdge.distSq;
            closestEdge = closestPointOnEdge.edge;
            closestNode = closestPointOnEdge.node;
            closestX = closestPointOnEdge.point.x;
            closestY = closestPointOnEdge.point.y;
        }
        if (closestEdge != null) {
            closestPointOnEdge.edge = closestEdge;
            closestPointOnEdge.node = closestNode;
            closestPointOnEdge.point.set(closestX, closestY);
            closestPointOnEdge.distSq = closestDistSq;
            if (closestEdge.obstacle.splitEdgeAtNearestPoint(closestPointOnEdge, PZMath.fastfloor(z), adjust)) {
                adjust.graph = this;
                if (adjust.isNodeNew) {
                    this.edges.add(adjust.newEdge);
                    this.addNode(adjust.node);
                }
                return 1;
            }
            return -1;
        }
        return 0;
    }

    Node getClosestNodeTo(float x, float y) {
        Node closest = null;
        float closestDistSq = Float.MAX_VALUE;
        for (int i = 0; i < this.nodes.size(); ++i) {
            Node node = this.nodes.get(i);
            float distSq = IsoUtils.DistanceToSquared(node.x, node.y, x, y);
            if (!(distSq < closestDistSq)) continue;
            closest = node;
            closestDistSq = distSq;
        }
        return closest;
    }

    public void create() {
        int i;
        for (i = 0; i < this.cluster.rects.size(); ++i) {
            VehicleRect rect = this.cluster.rects.get(i);
            this.addEdgesForVehicle(rect.vehicle);
        }
        this.addWorldObstaclesClipper();
        for (i = 0; i < this.obstacles.size(); ++i) {
            Obstacle obstacle = this.obstacles.get(i);
            if (obstacle.vehicle != null) continue;
            this.splitWorldObstacleEdges(obstacle.outer);
            for (int j = 0; j < obstacle.inner.size(); ++j) {
                this.splitWorldObstacleEdges(obstacle.inner.get(j));
            }
        }
        this.checkEdgeIntersection();
        this.checkNodesInObstacles();
        this.calculateNodeVisibility();
        this.connectVehicleCrawlNodes();
        this.created = true;
    }

    void initOverlappedChunks() {
        this.clearOverlappedChunks();
        VehicleRect bounds = this.cluster.bounds();
        boolean expand = true;
        int chunkMinX = PZMath.coorddivision(bounds.left() - 1, 8);
        int chunkMinY = PZMath.coorddivision(bounds.top() - 1, 8);
        int chunkMaxX = (int)PZMath.ceil((float)(bounds.right() + 1 - 1) / 8.0f);
        int chunkMaxY = (int)PZMath.ceil((float)(bounds.bottom() + 1 - 1) / 8.0f);
        for (int y = chunkMinY; y < chunkMaxY; ++y) {
            for (int x = chunkMinX; x < chunkMaxX; ++x) {
                Chunk chunk = PolygonalMap2.instance.getChunkFromChunkPos(x, y);
                if (chunk == null || !this.intersects(chunk.getMinX(), chunk.getMinY(), chunk.getMaxX() + 1, chunk.getMaxY() + 1, 1)) continue;
                this.overlappedChunks.add(chunk);
                chunk.addVisibilityGraph(this);
            }
        }
        bounds.release();
    }

    void clearOverlappedChunks() {
        for (int i = 0; i < this.overlappedChunks.size(); ++i) {
            Chunk chunk = this.overlappedChunks.get(i);
            chunk.removeVisibilityGraph(this);
        }
        this.overlappedChunks.clear();
    }

    static VisibilityGraph alloc() {
        return pool.isEmpty() ? new VisibilityGraph() : pool.pop();
    }

    void release() {
        int i;
        for (i = 0; i < this.nodes.size(); ++i) {
            if (PolygonalMap2.instance.squareToNode.containsValue(this.nodes.get(i))) continue;
            this.nodes.get(i).release();
        }
        for (i = 0; i < this.perimeterEdges.size(); ++i) {
            this.perimeterEdges.get((int)i).node1.release();
            this.perimeterEdges.get(i).release();
        }
        for (i = 0; i < this.obstacles.size(); ++i) {
            Obstacle obstacle = this.obstacles.get(i);
            obstacle.release();
        }
        for (i = 0; i < this.cluster.rects.size(); ++i) {
            this.cluster.rects.get(i).release();
        }
        this.cluster.release();
        this.clearOverlappedChunks();
        assert (!pool.contains(this));
        pool.push(this);
    }

    void render() {
        int clusterZ = this.cluster.z - 32;
        float r = 1.0f;
        for (Edge edge : this.perimeterEdges) {
            LineDrawer.addLine(edge.node1.x, edge.node1.y, clusterZ, edge.node2.x, edge.node2.y, clusterZ, r, 0.5f, 0.5f, null, true);
            r = 1.0f - r;
        }
        for (Obstacle obstacle : this.obstacles) {
            r = 1.0f;
            for (Edge edge : obstacle.outer) {
                LineDrawer.addLine(edge.node1.x, edge.node1.y, clusterZ, edge.node2.x, edge.node2.y, clusterZ, r, 0.5f, 0.5f, null, true);
                r = 1.0f - r;
            }
            for (EdgeRing edges : obstacle.inner) {
                for (Edge edge : edges) {
                    LineDrawer.addLine(edge.node1.x, edge.node1.y, clusterZ, edge.node2.x, edge.node2.y, clusterZ, r, 0.5f, 0.5f, null, true);
                    r = 1.0f - r;
                }
            }
            if (!DebugOptions.instance.polymapRenderCrawling.getValue()) continue;
            for (Node node : obstacle.crawlNodes) {
                LineDrawer.addLine(node.x - 0.05f, node.y - 0.05f, clusterZ, node.x + 0.05f, node.y + 0.05f, clusterZ, 0.5f, 1.0f, 0.5f, null, false);
                for (Connection cxn : node.visible) {
                    Node nodeVis = cxn.otherNode(node);
                    if (!nodeVis.hasFlag(1)) continue;
                    LineDrawer.addLine(node.x, node.y, clusterZ, nodeVis.x, nodeVis.y, clusterZ, 0.5f, 1.0f, 0.5f, null, true);
                }
            }
        }
        for (Node node1 : this.perimeterNodes) {
            if (DebugOptions.instance.polymapRenderConnections.getValue()) {
                for (Connection cxn : node1.visible) {
                    Node node2 = cxn.otherNode(node1);
                    LineDrawer.addLine(node1.x, node1.y, clusterZ, node2.x, node2.y, clusterZ, 0.0f, 0.25f, 0.0f, null, true);
                }
            }
            if (!DebugOptions.instance.polymapRenderNodes.getValue()) continue;
            float r1 = 1.0f;
            float g1 = 0.5f;
            float b1 = 0.0f;
            if (node1.ignore) {
                g1 = 1.0f;
            }
            LineDrawer.addLine(node1.x - 0.05f, node1.y - 0.05f, clusterZ, node1.x + 0.05f, node1.y + 0.05f, clusterZ, 1.0f, g1, 0.0f, null, false);
        }
        for (Node node1 : this.nodes) {
            if (DebugOptions.instance.polymapRenderConnections.getValue()) {
                for (Connection cxn : node1.visible) {
                    Node node2 = cxn.otherNode(node1);
                    if (!this.nodes.contains(node2)) continue;
                    LineDrawer.addLine(node1.x, node1.y, clusterZ, node2.x, node2.y, clusterZ, 0.0f, 1.0f, 0.0f, null, true);
                }
            }
            if (!DebugOptions.instance.polymapRenderNodes.getValue() && !node1.ignore) continue;
            LineDrawer.addLine(node1.x - 0.05f, node1.y - 0.05f, clusterZ, node1.x + 0.05f, node1.y + 0.05f, clusterZ, 1.0f, 1.0f, 0.0f, null, false);
        }
        for (Node node1 : this.intersectNodes) {
            LineDrawer.addLine(node1.x - 0.1f, node1.y - 0.1f, clusterZ, node1.x + 0.1f, node1.y + 0.1f, clusterZ, 1.0f, 0.0f, 0.0f, null, false);
        }
    }

    static final class CompareIntersection
    implements Comparator<Intersection> {
        Edge edge;

        CompareIntersection() {
        }

        @Override
        public int compare(Intersection o1, Intersection o2) {
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
}

