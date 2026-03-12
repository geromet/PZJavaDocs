/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.awt.geom.Line2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.iso.IsoUtils;
import zombie.pathfind.PathNode;
import zombie.pathfind.SearchNode;

public final class Path {
    final ArrayList<PathNode> nodes = new ArrayList();
    final ArrayDeque<PathNode> nodePool = new ArrayDeque();

    public void clear() {
        for (int i = 0; i < this.nodes.size(); ++i) {
            if (DebugOptions.instance.checks.objectPoolContains.getValue() && this.nodePool.contains(this.nodes.get(i))) {
                boolean bl = true;
            }
            this.nodePool.push(this.nodes.get(i));
        }
        this.nodes.clear();
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public int size() {
        return this.nodes.size();
    }

    public PathNode addNode(float x, float y, float z) {
        return this.addNode(x, y, z, 0);
    }

    PathNode addNode(float x, float y, float z, int flags) {
        PathNode node = this.nodePool.isEmpty() ? new PathNode() : this.nodePool.pop();
        node.init(x, y, z, flags);
        this.nodes.add(node);
        return node;
    }

    PathNode addNodeRawZ(float x, float y, float z, int flags) {
        PathNode node = this.nodePool.isEmpty() ? new PathNode() : this.nodePool.pop();
        node.init(x, y, z + 32.0f, flags);
        this.nodes.add(node);
        return node;
    }

    PathNode addNode(SearchNode node) {
        return this.addNode(node.getX(), node.getY(), node.getZ(), node.vgNode == null ? 0 : node.vgNode.flags);
    }

    public PathNode getNode(int index) {
        return this.nodes.get(index);
    }

    PathNode getLastNode() {
        return this.nodes.get(this.nodes.size() - 1);
    }

    public void copyFrom(Path other) {
        assert (this != other);
        this.clear();
        for (int i = 0; i < other.nodes.size(); ++i) {
            PathNode node = other.nodes.get(i);
            this.addNode(node.x, node.y, node.z, node.flags);
        }
    }

    public float length() {
        float dist = 0.0f;
        for (int i = 0; i < this.nodes.size() - 1; ++i) {
            PathNode node1 = this.nodes.get(i);
            PathNode node2 = this.nodes.get(i + 1);
            dist += IsoUtils.DistanceTo(node1.x, node1.y, node1.z, node2.x, node2.y, node2.z);
        }
        return dist;
    }

    public boolean crossesSquare(int x, int y, int z) {
        for (int i = 0; i < this.nodes.size() - 1; ++i) {
            PathNode node1 = this.nodes.get(i);
            PathNode node2 = this.nodes.get(i + 1);
            if (PZMath.fastfloor(node1.z) != z && PZMath.fastfloor(node2.z) != z) continue;
            if (Line2D.linesIntersect(node1.x, node1.y, node2.x, node2.y, x, y, x + 1, y)) {
                return true;
            }
            if (Line2D.linesIntersect(node1.x, node1.y, node2.x, node2.y, x + 1, y, x + 1, y + 1)) {
                return true;
            }
            if (Line2D.linesIntersect(node1.x, node1.y, node2.x, node2.y, x + 1, y + 1, x, y + 1)) {
                return true;
            }
            if (!Line2D.linesIntersect(node1.x, node1.y, node2.x, node2.y, x, y + 1, x, y)) continue;
            return true;
        }
        return false;
    }
}

