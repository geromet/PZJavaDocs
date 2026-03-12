/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.highLevel;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayList;
import zombie.iso.IsoUtils;
import zombie.pathfind.Node;
import zombie.pathfind.highLevel.HLAStar;
import zombie.pathfind.highLevel.HLChunkRegion;
import zombie.pathfind.highLevel.HLLevelTransition;
import zombie.pathfind.highLevel.HLSuccessor;
import zombie.util.list.PZArrayUtil;

public class HLSearchNode
extends ASearchNode {
    static final int CPW = 8;
    static int nextID = 1;
    Integer id;
    HLSearchNode parent;
    HLAStar astar;
    HLChunkRegion chunkRegion;
    HLLevelTransition levelTransition;
    boolean bottomOfStaircase;
    Node vgNode;
    int unloadedX = -1;
    int unloadedY = -1;
    boolean inUnloadedArea;
    boolean onEdgeOfLoadedArea;
    final ArrayList<HLSuccessor> successors = new ArrayList();

    HLSearchNode() {
        this.id = nextID++;
    }

    @Override
    public double h() {
        float x1 = this.getX();
        float y1 = this.getY();
        float z1 = this.getZ();
        float x2 = this.astar.goalNode.searchNode.getX();
        float y2 = this.astar.goalNode.searchNode.getY();
        float z2 = this.astar.goalNode.searchNode.getZ();
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0) + Math.pow((z1 - z2) * 2.5f, 2.0));
    }

    @Override
    public double c(ISearchNode successor) {
        HLSearchNode other = (HLSearchNode)successor;
        if (other.inUnloadedArea) {
            return IsoUtils.DistanceTo(this.getX(), this.getY(), this.getZ(), other.getX(), other.getY(), other.getZ());
        }
        if (this.vgNode != null && other.vgNode != null) {
            return IsoUtils.DistanceTo(this.getX(), this.getY(), this.getZ(), other.getX(), other.getY(), other.getZ());
        }
        HLSuccessor successor1 = PZArrayUtil.find(this.successors, hlSuccessor -> hlSuccessor.searchNode == other);
        return successor1.cost;
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
        this.parent = (HLSearchNode)parent;
    }

    @Override
    public Integer keyCode() {
        return this.id;
    }

    float getX() {
        if (this.chunkRegion != null) {
            return (float)(this.chunkRegion.minX + this.chunkRegion.maxX + 1) / 2.0f;
        }
        if (this.levelTransition != null) {
            return this.levelTransition.getSearchNodeX(this.bottomOfStaircase);
        }
        if (this.vgNode != null) {
            return this.vgNode.x;
        }
        return this.unloadedX;
    }

    float getY() {
        if (this.chunkRegion != null) {
            return (float)(this.chunkRegion.minY + this.chunkRegion.maxY + 1) / 2.0f;
        }
        if (this.levelTransition != null) {
            return this.levelTransition.getSearchNodeY(this.bottomOfStaircase);
        }
        if (this.vgNode != null) {
            return this.vgNode.y;
        }
        return this.unloadedY;
    }

    float getZ() {
        if (this.chunkRegion != null) {
            return this.chunkRegion.getLevel();
        }
        if (this.levelTransition != null) {
            return this.bottomOfStaircase ? (float)this.levelTransition.getBottomFloorZ() : (float)this.levelTransition.getTopFloorZ();
        }
        if (this.vgNode != null) {
            return this.vgNode.z;
        }
        return 32.0f;
    }

    boolean calculateOnEdgeOfLoadedArea() {
        if (this.chunkRegion != null) {
            return this.chunkRegion.isOnEdgeOfLoadedArea();
        }
        if (this.levelTransition != null) {
            return this.levelTransition.isOnEdgeOfLoadedArea();
        }
        if (this.vgNode != null) {
            return this.vgNode.isOnEdgeOfLoadedArea();
        }
        return false;
    }
}

