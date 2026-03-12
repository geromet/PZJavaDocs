/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.characters.animals.pathfind.LowLevelAStar;
import zombie.characters.animals.pathfind.MeshList;
import zombie.popman.ObjectPool;

public final class LowLevelSearchNode
extends ASearchNode {
    static int nextID = 1;
    Integer id;
    LowLevelSearchNode parent;
    LowLevelAStar astar;
    MeshList meshList;
    int meshIdx;
    int triangleIdx;
    int edgeIdx;
    float x = Float.NaN;
    float y = Float.NaN;
    static final ObjectPool<LowLevelSearchNode> pool = new ObjectPool<LowLevelSearchNode>(LowLevelSearchNode::new);

    LowLevelSearchNode() {
        this.id = nextID++;
    }

    @Override
    public double h() {
        float x1 = this.getX();
        float y1 = this.getY();
        float x2 = this.astar.goalNode.searchNode.getX();
        float y2 = this.astar.goalNode.searchNode.getY();
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
    }

    @Override
    public double c(ISearchNode successor) {
        LowLevelSearchNode other = (LowLevelSearchNode)successor;
        float x1 = this.getX();
        float y1 = this.getY();
        float x2 = other.getX();
        float y2 = other.getY();
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
    }

    @Override
    public void getSuccessors(ArrayList<ISearchNode> successors) {
        this.astar.getSuccessors(this, successors);
    }

    @Override
    public ISearchNode getParent() {
        return this.parent;
    }

    @Override
    public void setParent(ISearchNode parent) {
        this.parent = (LowLevelSearchNode)parent;
    }

    @Override
    public Integer keyCode() {
        return this.id;
    }

    public float getEdgeMidPointX() {
        if (this.edgeIdx == -1) {
            return this.getCentroidX();
        }
        ArrayList<Vector2f> triangles = this.meshList.get((int)this.meshIdx).triangles;
        Vector2f p1 = triangles.get(this.triangleIdx + this.edgeIdx);
        Vector2f p2 = triangles.get(this.triangleIdx + (this.edgeIdx + 1) % 3);
        return (p1.x + p2.x) / 2.0f;
    }

    public float getEdgeMidPointY() {
        if (this.edgeIdx == -1) {
            return this.getCentroidY();
        }
        ArrayList<Vector2f> triangles = this.meshList.get((int)this.meshIdx).triangles;
        Vector2f p1 = triangles.get(this.triangleIdx + this.edgeIdx);
        Vector2f p2 = triangles.get(this.triangleIdx + (this.edgeIdx + 1) % 3);
        return (p1.y + p2.y) / 2.0f;
    }

    public float getCentroidX() {
        ArrayList<Vector2f> triangles = this.meshList.get((int)this.meshIdx).triangles;
        Vector2f p1 = triangles.get(this.triangleIdx);
        Vector2f p2 = triangles.get(this.triangleIdx + 1);
        Vector2f p3 = triangles.get(this.triangleIdx + 2);
        return (p1.x + p2.x + p3.x) / 3.0f;
    }

    public float getCentroidY() {
        ArrayList<Vector2f> triangles = this.meshList.get((int)this.meshIdx).triangles;
        Vector2f p1 = triangles.get(this.triangleIdx);
        Vector2f p2 = triangles.get(this.triangleIdx + 1);
        Vector2f p3 = triangles.get(this.triangleIdx + 2);
        return (p1.y + p2.y + p3.y) / 3.0f;
    }

    public float getX() {
        return !Float.isNaN(this.x) ? this.x : this.getEdgeMidPointX();
    }

    public float getY() {
        return !Float.isNaN(this.y) ? this.y : this.getEdgeMidPointY();
    }

    public float getZ() {
        return this.meshList.z;
    }
}

