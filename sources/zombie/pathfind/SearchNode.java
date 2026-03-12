/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;
import zombie.pathfind.Connection;
import zombie.pathfind.Node;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.VGAStar;

final class SearchNode
extends ASearchNode {
    VGAStar astar;
    Node vgNode;
    Square square;
    int unloadedX;
    int unloadedY;
    boolean inUnloadedArea;
    SearchNode parent;
    static int nextID = 1;
    Integer id = nextID++;
    private static final double SQRT2 = Math.sqrt(2.0);
    static final ArrayDeque<SearchNode> pool = new ArrayDeque();

    SearchNode() {
    }

    SearchNode init(VGAStar astar, Node node) {
        this.setG(0.0);
        this.astar = astar;
        this.vgNode = node;
        this.square = null;
        this.unloadedY = -1;
        this.unloadedX = -1;
        this.inUnloadedArea = false;
        this.parent = null;
        return this;
    }

    SearchNode init(VGAStar astar, Square square) {
        this.setG(0.0);
        this.astar = astar;
        this.vgNode = null;
        this.square = square;
        this.unloadedY = -1;
        this.unloadedX = -1;
        this.inUnloadedArea = false;
        this.parent = null;
        return this;
    }

    SearchNode init(VGAStar astar, int x, int y) {
        this.setG(0.0);
        this.astar = astar;
        this.vgNode = null;
        this.square = null;
        this.unloadedX = x;
        this.unloadedY = y;
        this.inUnloadedArea = true;
        this.parent = null;
        return this;
    }

    @Override
    public double h() {
        return this.dist(this.astar.goalNode.searchNode);
    }

    @Override
    public double c(ISearchNode successor) {
        boolean bOtherUnderVehicle;
        Square square2;
        boolean animalCantClimb;
        SearchNode other = (SearchNode)successor;
        if (other.inUnloadedArea) {
            return this.dist(other);
        }
        double add = 0.0;
        boolean bCrawlingZombie = this.astar.mover.isZombie() && this.astar.mover.crawling;
        boolean avoidWindows = !this.astar.mover.isZombie() || this.astar.mover.crawling;
        boolean bl = animalCantClimb = this.astar.mover.isAnimal() && !this.astar.mover.canClimbFences;
        if (animalCantClimb) {
            avoidWindows = true;
        }
        if (avoidWindows && this.square != null && other.square != null) {
            if (this.square.x == other.square.x - 1 && this.square.y == other.square.y) {
                if (other.square.has(2048)) {
                    add = !bCrawlingZombie && other.square.has(0x100000) ? 20.0 : 200.0;
                }
            } else if (this.square.x == other.square.x + 1 && this.square.y == other.square.y) {
                if (this.square.has(2048)) {
                    add = !bCrawlingZombie && this.square.has(0x100000) ? 20.0 : 200.0;
                }
            } else if (this.square.y == other.square.y - 1 && this.square.x == other.square.x) {
                if (other.square.has(4096)) {
                    add = !bCrawlingZombie && other.square.has(0x200000) ? 20.0 : 200.0;
                }
            } else if (this.square.y == other.square.y + 1 && this.square.x == other.square.x && this.square.has(4096)) {
                double d = add = !bCrawlingZombie && this.square.has(0x200000) ? 20.0 : 200.0;
            }
        }
        if (other.square != null && other.square.has(131072)) {
            add = Math.max(add, 20.0);
        }
        if (this.vgNode != null && other.vgNode != null) {
            for (int i = 0; i < this.vgNode.visible.size(); ++i) {
                Connection cxn = this.vgNode.visible.get(i);
                if (cxn.otherNode(this.vgNode) != other.vgNode) continue;
                if (this.vgNode.square != null && this.vgNode.square.has(131072) || !cxn.has(2)) break;
                add = Math.max(add, 20.0);
                break;
            }
        }
        Square square1 = this.square == null ? PolygonalMap2.instance.getSquare(PZMath.fastfloor(this.vgNode.x), PZMath.fastfloor(this.vgNode.y), this.vgNode.z) : this.square;
        Square square = square2 = other.square == null ? PolygonalMap2.instance.getSquare(PZMath.fastfloor(other.vgNode.x), PZMath.fastfloor(other.vgNode.y), other.vgNode.z) : other.square;
        if (square1 != null && square2 != null) {
            if (square1.x == square2.x - 1 && square1.y == square2.y) {
                if (square2.has(32768)) {
                    add = Math.max(add, 20.0);
                }
            } else if (square1.x == square2.x + 1 && square1.y == square2.y) {
                if (square1.has(32768)) {
                    add = Math.max(add, 20.0);
                }
            } else if (square1.y == square2.y - 1 && square1.x == square2.x) {
                if (square2.has(65536)) {
                    add = Math.max(add, 20.0);
                }
            } else if (square1.y == square2.y + 1 && square1.x == square2.x && square1.has(65536)) {
                add = Math.max(add, 20.0);
            }
            if (bCrawlingZombie || animalCantClimb) {
                if (square1.x == square2.x - 1 && square1.y == square2.y) {
                    if (square2.has(2) && square2.has(8192) && (!this.astar.mover.isAnimal() || !square2.isUnblockedDoorW())) {
                        add = Math.max(add, 20.0);
                    }
                } else if (square1.x == square2.x + 1 && square1.y == square2.y) {
                    if (square1.has(2) && square1.has(8192) && (!this.astar.mover.isAnimal() || !square1.isUnblockedDoorW())) {
                        add = Math.max(add, 20.0);
                    }
                } else if (square1.y == square2.y - 1 && square1.x == square2.x) {
                    if (square2.has(4) && square2.has(16384) && (!this.astar.mover.isAnimal() || !square2.isUnblockedDoorN())) {
                        add = Math.max(add, 20.0);
                    }
                } else if (square1.y == square2.y + 1 && square1.x == square2.x && square1.has(4) && square1.has(16384) && (!this.astar.mover.isAnimal() || !square1.isUnblockedDoorN())) {
                    add = Math.max(add, 20.0);
                }
            }
        }
        boolean bSelfUnderVehicle = this.vgNode != null && this.vgNode.hasFlag(2);
        boolean bl2 = bOtherUnderVehicle = other.vgNode != null && other.vgNode.hasFlag(2);
        if (!bSelfUnderVehicle && bOtherUnderVehicle && !this.astar.mover.ignoreCrawlCost) {
            add += 10.0;
        }
        if (other.square != null) {
            add += (double)other.square.cost;
        }
        return this.dist(other) + add;
    }

    @Override
    public void getSuccessors(ArrayList<ISearchNode> successors) {
        SearchNode searchNode;
        if (this.astar.goalNode.searchNode.inUnloadedArea && this.isOnEdgeOfLoadedArea()) {
            successors.add(this.astar.goalNode.searchNode);
        }
        if (this.vgNode != null) {
            this.vgNode.createGraphsIfNeeded();
            for (int i = 0; i < this.vgNode.visible.size(); ++i) {
                Connection cxn = this.vgNode.visible.get(i);
                Node visible = cxn.otherNode(this.vgNode);
                searchNode = this.astar.getSearchNode(visible);
                if (this.vgNode.square != null && searchNode.square != null && this.astar.isKnownBlocked(this.vgNode.square, searchNode.square) || !this.astar.mover.canCrawl && visible.hasFlag(2) || !this.astar.mover.canThump && cxn.has(2)) continue;
                successors.add(searchNode);
            }
            if (this.vgNode.graphs != null && !this.vgNode.graphs.isEmpty() && this.vgNode.hasFlag(16)) {
                SearchNode searchNode2;
                Square square1 = PolygonalMap2.instance.getSquare(this.square.x - 1, this.square.y, this.square.z);
                if (square1 != null && square1.has(32)) {
                    if (!this.astar.mover.isAllowedChunkLevel(square1)) {
                        return;
                    }
                    if (this.astar.canMoveBetween(this.square, square1, false)) {
                        searchNode2 = this.astar.getSearchNode(square1);
                        if (successors.contains(searchNode2)) {
                            boolean visible = false;
                        } else {
                            successors.add(searchNode2);
                        }
                    }
                }
                if ((square1 = PolygonalMap2.instance.getSquare(this.square.x, this.square.y - 1, this.square.z)) != null && square1.has(256)) {
                    if (!this.astar.mover.isAllowedChunkLevel(square1)) {
                        return;
                    }
                    if (this.astar.canMoveBetween(this.square, square1, false)) {
                        searchNode2 = this.astar.getSearchNode(square1);
                        if (successors.contains(searchNode2)) {
                            boolean visible = false;
                        } else {
                            successors.add(searchNode2);
                        }
                    }
                }
                return;
            }
            if (this.vgNode.graphs != null && !this.vgNode.graphs.isEmpty() && !this.vgNode.hasFlag(8)) {
                return;
            }
        }
        if (this.square != null) {
            IsoDirections dir;
            Square square1;
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    Square square12;
                    if (dx == 0 && dy == 0 || (square12 = PolygonalMap2.instance.getSquareRawZ(this.square.x + dx, this.square.y + dy, this.square.z)) == null || !this.astar.mover.isAllowedChunkLevel(square12) || this.astar.isSquareInCluster(square12) && !square12.has(504) || !this.astar.canMoveBetween(this.square, square12, false)) continue;
                    searchNode = this.astar.getSearchNode(square12);
                    if (successors.contains(searchNode)) {
                        boolean bl = false;
                        continue;
                    }
                    successors.add(searchNode);
                }
            }
            if (this.square.has(288) && (square1 = this.square.getAdjacentSquare((dir = this.square.has(256) ? IsoDirections.N : IsoDirections.W).Rot180())) != null && PolygonalMap2.instance.getExistingNodeForSquare(square1) != null && PolygonalMap2.instance.getExistingNodeForSquare(square1).hasFlag(16) && this.astar.mover.isAllowedChunkLevel(square1) && this.astar.canMoveBetween(this.square, square1, false)) {
                SearchNode searchNode3 = this.astar.getSearchNode(square1);
                if (successors.contains(searchNode3)) {
                    boolean bl = false;
                } else {
                    successors.add(searchNode3);
                }
            }
            if (this.square.z > this.astar.mover.minLevel) {
                Square square13 = PolygonalMap2.instance.getSquare(this.square.x, this.square.y + 1, this.square.z - 1);
                if (square13 != null && square13.hasTransitionToLevelAbove(IsoDirections.N) && !this.astar.isSquareInCluster(square13) && this.astar.mover.isAllowedLevelTransition(IsoDirections.N, this.square, true)) {
                    SearchNode searchNode4 = this.astar.getSearchNode(square13);
                    if (successors.contains(searchNode4)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode4);
                    }
                }
                if ((square13 = PolygonalMap2.instance.getSquare(this.square.x, this.square.y - 1, this.square.z - 1)) != null && square13.hasTransitionToLevelAbove(IsoDirections.S) && !this.astar.isSquareInCluster(square13) && this.astar.mover.isAllowedLevelTransition(IsoDirections.S, this.square, true)) {
                    SearchNode searchNode5 = this.astar.getSearchNode(square13);
                    if (successors.contains(searchNode5)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode5);
                    }
                }
                if ((square13 = PolygonalMap2.instance.getSquare(this.square.x + 1, this.square.y, this.square.z - 1)) != null && square13.hasTransitionToLevelAbove(IsoDirections.W) && !this.astar.isSquareInCluster(square13) && this.astar.mover.isAllowedLevelTransition(IsoDirections.W, this.square, true)) {
                    SearchNode searchNode6 = this.astar.getSearchNode(square13);
                    if (successors.contains(searchNode6)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode6);
                    }
                }
                if ((square13 = PolygonalMap2.instance.getSquare(this.square.x - 1, this.square.y, this.square.z - 1)) != null && square13.hasTransitionToLevelAbove(IsoDirections.E) && !this.astar.isSquareInCluster(square13) && this.astar.mover.isAllowedLevelTransition(IsoDirections.E, this.square, true)) {
                    SearchNode searchNode7 = this.astar.getSearchNode(square13);
                    if (successors.contains(searchNode7)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode7);
                    }
                }
            }
            if (this.square.z < this.astar.mover.maxLevel) {
                Square square14;
                Square square15;
                Square square16;
                Square square17;
                if (this.square.hasTransitionToLevelAbove(IsoDirections.N) && (square17 = PolygonalMap2.instance.getSquareRawZ(this.square.x, this.square.y - 1, this.square.z + 1)) != null && !this.astar.isSquareInCluster(square17) && this.astar.mover.isAllowedLevelTransition(IsoDirections.N, square17, true)) {
                    SearchNode searchNode8 = this.astar.getSearchNode(square17);
                    if (successors.contains(searchNode8)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode8);
                    }
                }
                if (this.square.hasTransitionToLevelAbove(IsoDirections.S) && (square16 = PolygonalMap2.instance.getSquareRawZ(this.square.x, this.square.y + 1, this.square.z + 1)) != null && !this.astar.isSquareInCluster(square16) && this.astar.mover.isAllowedLevelTransition(IsoDirections.S, square16, true)) {
                    SearchNode searchNode9 = this.astar.getSearchNode(square16);
                    if (successors.contains(searchNode9)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode9);
                    }
                }
                if (this.square.hasTransitionToLevelAbove(IsoDirections.W) && (square15 = PolygonalMap2.instance.getSquareRawZ(this.square.x - 1, this.square.y, this.square.z + 1)) != null && !this.astar.isSquareInCluster(square15) && this.astar.mover.isAllowedLevelTransition(IsoDirections.W, square15, true)) {
                    SearchNode searchNode10 = this.astar.getSearchNode(square15);
                    if (successors.contains(searchNode10)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode10);
                    }
                }
                if (this.square.hasTransitionToLevelAbove(IsoDirections.E) && (square14 = PolygonalMap2.instance.getSquareRawZ(this.square.x + 1, this.square.y, this.square.z + 1)) != null && !this.astar.isSquareInCluster(square14) && this.astar.mover.isAllowedLevelTransition(IsoDirections.E, square14, true)) {
                    SearchNode searchNode11 = this.astar.getSearchNode(square14);
                    if (successors.contains(searchNode11)) {
                        boolean bl = false;
                    } else {
                        successors.add(searchNode11);
                    }
                }
            }
        }
    }

    @Override
    public ISearchNode getParent() {
        return this.parent;
    }

    @Override
    public void setParent(ISearchNode parent) {
        this.parent = (SearchNode)parent;
    }

    @Override
    public Integer keyCode() {
        return this.id;
    }

    public float getX() {
        if (this.square != null) {
            return (float)this.square.x + 0.5f;
        }
        if (this.vgNode != null) {
            return this.vgNode.x;
        }
        return this.unloadedX;
    }

    public float getY() {
        if (this.square != null) {
            return (float)this.square.y + 0.5f;
        }
        if (this.vgNode != null) {
            return this.vgNode.y;
        }
        return this.unloadedY;
    }

    public float getZ() {
        if (this.square != null) {
            return this.square.z;
        }
        if (this.vgNode != null) {
            return this.vgNode.z;
        }
        return 32.0f;
    }

    boolean isOnEdgeOfLoadedArea() {
        int x = PZMath.fastfloor(this.getX());
        int y = PZMath.fastfloor(this.getY());
        boolean bOnEdgeOfLoadedArea = false;
        if (PZMath.coordmodulo(x, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(x - 1, y) == null) {
            bOnEdgeOfLoadedArea = true;
        }
        if (PZMath.coordmodulo(x, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(x + 1, y) == null) {
            bOnEdgeOfLoadedArea = true;
        }
        if (PZMath.coordmodulo(y, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(x, y - 1) == null) {
            bOnEdgeOfLoadedArea = true;
        }
        if (PZMath.coordmodulo(y, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(x, y + 1) == null) {
            bOnEdgeOfLoadedArea = true;
        }
        return bOnEdgeOfLoadedArea;
    }

    public double dist(SearchNode other) {
        if (this.square != null && other.square != null && Math.abs(this.square.x - other.square.x) <= 1 && Math.abs(this.square.y - other.square.y) <= 1) {
            if (this.square.x != other.square.x && this.square.y != other.square.y) {
                return SQRT2;
            }
            return 1.0;
        }
        float x1 = this.getX();
        float y1 = this.getY();
        float z1 = this.getZ();
        float x2 = other.getX();
        float y2 = other.getY();
        float z2 = other.getZ();
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0) + Math.pow((z1 - z2) * 2.5f, 2.0));
    }

    float getApparentZ() {
        if (this.square == null) {
            return this.vgNode.z;
        }
        if (this.square.has(8) || this.square.has(64)) {
            return (float)this.square.z + 0.75f;
        }
        if (this.square.has(16) || this.square.has(128)) {
            return (float)this.square.z + 0.5f;
        }
        if (this.square.has(32) || this.square.has(256)) {
            return (float)this.square.z + 0.25f;
        }
        return this.square.z;
    }

    static SearchNode alloc() {
        return pool.isEmpty() ? new SearchNode() : pool.pop();
    }

    void release() {
        assert (!pool.contains(this));
        pool.push(this);
    }
}

