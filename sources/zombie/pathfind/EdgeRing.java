/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.iso.Vector2;
import zombie.pathfind.ClosestPointOnEdge;
import zombie.pathfind.Edge;
import zombie.pathfind.EdgeRingHit;
import zombie.pathfind.L_lineSegmentIntersects;
import zombie.pathfind.Node;

final class EdgeRing
extends ArrayList<Edge> {
    static final ArrayDeque<EdgeRing> pool = new ArrayDeque();

    EdgeRing() {
    }

    @Override
    public boolean add(Edge obj) {
        assert (!this.contains(obj));
        return super.add(obj);
    }

    public boolean hasNode(Node node) {
        for (int i = 0; i < this.size(); ++i) {
            Edge edge = (Edge)this.get(i);
            if (!edge.hasNode(node)) continue;
            return true;
        }
        return false;
    }

    boolean hasAdjacentNodes(Node node1, Node node2) {
        for (int i = 0; i < this.size(); ++i) {
            Edge edge = (Edge)this.get(i);
            if (!edge.hasNode(node1) || !edge.hasNode(node2)) continue;
            return true;
        }
        return false;
    }

    boolean isPointInPolygon_CrossingNumber(float x, float y) {
        int cn = 0;
        for (int i = 0; i < this.size(); ++i) {
            float vt;
            Edge edge = (Edge)this.get(i);
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
        for (int i = 0; i < this.size(); ++i) {
            Edge edge = (Edge)this.get(i);
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

    boolean lineSegmentIntersects(float sx, float sy, float ex, float ey) {
        Vector2 move = L_lineSegmentIntersects.v1;
        move.set(ex - sx, ey - sy);
        float lineSegmentLength = move.getLength();
        move.normalize();
        float dirX = move.x;
        float dirY = move.y;
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
            Edge edge = (Edge)this.get(j);
            if (edge.isPointOn(sx, sy) || edge.isPointOn(ex, ey)) continue;
            float dot = edge.normal.dot(move);
            if (dot >= 0.01f) {
                // empty if block
            }
            if (!((t = ((dbaX = (bX = edge.node2.x) - (aX = edge.node1.x)) * (doaY = sy - (aY = edge.node1.y)) - (dbaY = (bY = edge.node2.y) - aY) * (doaX = sx - aX)) * (invDbaDir = 1.0f / (dbaY * dirX - dbaX * dirY))) >= 0.0f) || !(t <= lineSegmentLength) || !((t2 = (doaY * dirX - doaX * dirY) * invDbaDir) >= 0.0f) || !(t2 <= 1.0f)) continue;
            return true;
        }
        return this.isPointInPolygon_WindingNumber((sx + ex) / 2.0f, (sy + ey) / 2.0f, 0) != EdgeRingHit.Outside;
    }

    void getClosestPointOnEdge(float x3, float y3, ClosestPointOnEdge out) {
        for (int i = 0; i < this.size(); ++i) {
            Edge edge = (Edge)this.get(i);
            edge.getClosestPointOnEdge(x3, y3, out);
        }
    }

    static EdgeRing alloc() {
        return pool.isEmpty() ? new EdgeRing() : pool.pop();
    }

    public void release() {
        Edge.releaseAll(this);
    }

    static void releaseAll(ArrayList<EdgeRing> objs) {
        for (int i = 0; i < objs.size(); ++i) {
            objs.get(i).release();
        }
    }
}

