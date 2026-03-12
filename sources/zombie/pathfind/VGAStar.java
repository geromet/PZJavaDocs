/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import astar.AStar;
import astar.ISearchNode;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import java.util.ArrayList;
import java.util.Objects;
import zombie.ai.KnownBlockedEdges;
import zombie.iso.IsoDirections;
import zombie.pathfind.Chunk;
import zombie.pathfind.GoalNode;
import zombie.pathfind.Node;
import zombie.pathfind.PMMover;
import zombie.pathfind.PathFindRequest;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.SearchNode;
import zombie.pathfind.Square;
import zombie.pathfind.VisibilityGraph;

final class VGAStar
extends AStar {
    ArrayList<VisibilityGraph> graphs;
    final ArrayList<SearchNode> searchNodes = new ArrayList();
    final TIntObjectHashMap<SearchNode> nodeMap = new TIntObjectHashMap();
    final GoalNode goalNode = new GoalNode();
    final TIntObjectHashMap<SearchNode> squareToNode = new TIntObjectHashMap();
    PMMover mover = new PMMover();
    final TIntObjectHashMap<KnownBlockedEdges> knownBlockedEdges = new TIntObjectHashMap();
    final InitProc initProc = new InitProc(this);

    VGAStar() {
    }

    VGAStar init(ArrayList<VisibilityGraph> graphs, TIntObjectHashMap<Node> s2n) {
        this.setMaxSteps(5000);
        this.graphs = graphs;
        this.searchNodes.clear();
        this.nodeMap.clear();
        this.squareToNode.clear();
        s2n.forEachEntry(this.initProc);
        return this;
    }

    VisibilityGraph getVisGraphForSquare(Square square) {
        Chunk chunk = PolygonalMap2.instance.getChunkFromSquarePos(square.x, square.y);
        if (chunk == null) {
            return null;
        }
        for (int i = 0; i < chunk.visibilityGraphs.size(); ++i) {
            VisibilityGraph graph = chunk.visibilityGraphs.get(i);
            if (!graph.contains(square)) continue;
            return graph;
        }
        return null;
    }

    boolean isSquareInCluster(Square square) {
        return this.getVisGraphForSquare(square) != null;
    }

    SearchNode getSearchNode(Node vgNode) {
        if (vgNode.square != null) {
            return this.getSearchNode(vgNode.square);
        }
        SearchNode searchNode = this.nodeMap.get(vgNode.id);
        if (searchNode == null) {
            searchNode = SearchNode.alloc().init(this, vgNode);
            this.searchNodes.add(searchNode);
            this.nodeMap.put(vgNode.id, searchNode);
        }
        return searchNode;
    }

    SearchNode getSearchNode(Square square) {
        SearchNode searchNode = this.squareToNode.get(square.id);
        if (searchNode == null) {
            searchNode = SearchNode.alloc().init(this, square);
            this.searchNodes.add(searchNode);
            this.squareToNode.put(square.id, searchNode);
        }
        return searchNode;
    }

    SearchNode getSearchNode(int x, int y) {
        SearchNode searchNode = SearchNode.alloc().init(this, x, y);
        this.searchNodes.add(searchNode);
        return searchNode;
    }

    ArrayList<ISearchNode> shortestPath(PathFindRequest request, SearchNode startNode, SearchNode goalNode) {
        this.mover.set(request);
        this.goalNode.init(goalNode);
        return this.shortestPath(startNode, this.goalNode);
    }

    boolean canMoveBetween(Square square1, Square square2, boolean isBetween) {
        return !this.canNotMoveBetween(square1, square2, isBetween);
    }

    boolean canNotMoveBetween(Square square1, Square square2, boolean isBetween) {
        boolean diag;
        boolean testS;
        assert (Math.abs(square1.x - square2.x) <= 1);
        assert (Math.abs(square1.y - square2.y) <= 1);
        assert (square1.z == square2.z);
        assert (square1 != square2);
        boolean testW = square2.x < square1.x;
        boolean testE = square2.x > square1.x;
        boolean testN = square2.y < square1.y;
        boolean bl = testS = square2.y > square1.y;
        if (square2.isNonThumpableSolid() || !this.mover.canThump && square2.isReallySolid()) {
            return true;
        }
        if (square2.y < square1.y && square1.has(64)) {
            return true;
        }
        if (square2.x < square1.x && square1.has(8)) {
            return true;
        }
        if (square2.y > square1.y && square2.x == square1.x && square2.has(64)) {
            return true;
        }
        if (square2.x > square1.x && square2.y == square1.y && square2.has(8)) {
            return true;
        }
        if (square2.x != square1.x && square2.has(448)) {
            return true;
        }
        if (square2.y != square1.y && square2.has(56)) {
            return true;
        }
        if (square2.x != square1.x && square1.has(448)) {
            return true;
        }
        if (square2.y != square1.y && square1.has(56)) {
            return true;
        }
        if (square2.z == square1.z) {
            if (square2.x == square1.x && square2.y == square1.y - 1 && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.N) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.S))) {
                return true;
            }
            if (square2.x == square1.x && square2.y == square1.y + 1 && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.S) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.N))) {
                return true;
            }
            if (square2.x == square1.x - 1 && square2.y == square1.y && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.W) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.E))) {
                return true;
            }
            if (square2.x == square1.x + 1 && square2.y == square1.y && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.E) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.W))) {
                return true;
            }
        }
        if (!square2.has(512) && !square2.has(504)) {
            return true;
        }
        if (this.isKnownBlocked(square1, square2)) {
            return true;
        }
        if (this.mover.isAnimal()) {
            boolean colE;
            boolean colW;
            boolean bOpenDoorN = testN && square1.isUnblockedDoorN();
            boolean bOpenDoorW = testW && square1.isUnblockedDoorW();
            colN = testN && (square1.isCollideN() || square1.isThumpN()) && !bOpenDoorN;
            boolean bl2 = colW = testW && (square1.isCollideW() || square1.isThumpW()) && !bOpenDoorW;
            if (testN && square1.isCanPathN() && (square1.x != square2.x || isBetween)) {
                return true;
            }
            if (testW && square1.isCanPathW() && (square1.y != square2.y || isBetween)) {
                return true;
            }
            if ((colN || colW) && !this.canAnimalBreakObstacle(square1, square2, colW, colN, false, false)) {
                return true;
            }
            boolean bOpenDoorS = testS && square2.isUnblockedDoorN();
            boolean bOpenDoorE = testE && square2.isUnblockedDoorW();
            boolean colS = testS && (square2.isCollideN() || square2.isThumpN()) && !bOpenDoorS;
            boolean bl3 = colE = testE && (square2.isCollideW() || square2.isThumpW()) && !bOpenDoorE;
            if (testS && square2.isCanPathN() && (square1.x != square2.x || isBetween)) {
                return true;
            }
            if (testE && square2.isCanPathW() && (square1.y != square2.y || isBetween)) {
                return true;
            }
            if ((colS || colE) && !this.canAnimalBreakObstacle(square1, square2, false, false, colE, colS)) {
                return true;
            }
        } else {
            boolean colE;
            boolean canPathN = square1.isCanPathN() && (this.mover.canThump || !square1.isThumpN());
            boolean canPathW = square1.isCanPathW() && (this.mover.canThump || !square1.isThumpW());
            colN = testN && square1.isCollideN() && (square1.x != square2.x || isBetween || !canPathN);
            boolean colW = testW && square1.isCollideW() && (square1.y != square2.y || isBetween || !canPathW);
            canPathN = square2.isCanPathN() && (this.mover.canThump || !square2.isThumpN());
            canPathW = square2.isCanPathW() && (this.mover.canThump || !square2.isThumpW());
            boolean colS = testS && square2.has(131076) && (square1.x != square2.x || isBetween || !canPathN);
            boolean bl4 = colE = testE && square2.has(131074) && (square1.y != square2.y || isBetween || !canPathW);
            if (colN || colW || colS || colE) {
                return true;
            }
        }
        boolean bl5 = diag = square2.x != square1.x && square2.y != square1.y;
        if (diag) {
            Square betweenA = PolygonalMap2.instance.getSquareRawZ(square1.x, square2.y, square1.z);
            Square betweenB = PolygonalMap2.instance.getSquareRawZ(square2.x, square1.y, square1.z);
            assert (betweenA != square1 && betweenA != square2);
            assert (betweenB != square1 && betweenB != square2);
            if (square2.x == square1.x + 1 && square2.y == square1.y + 1 && betweenA != null && betweenB != null) {
                if (betweenA.has(4096) && betweenB.has(2048)) {
                    return true;
                }
                if (betweenA.isThumpN() && betweenB.isThumpW()) {
                    return true;
                }
            }
            if (square2.x == square1.x - 1 && square2.y == square1.y - 1 && betweenA != null && betweenB != null) {
                if (betweenA.has(2048) && betweenB.has(4096)) {
                    return true;
                }
                if (betweenA.isThumpW() && betweenB.isThumpN()) {
                    return true;
                }
            }
            if (betweenA == null || this.canNotMoveBetween(square1, betweenA, true)) {
                return true;
            }
            if (betweenB == null || this.canNotMoveBetween(square1, betweenB, true)) {
                return true;
            }
            if (betweenA == null || this.canNotMoveBetween(square2, betweenA, true)) {
                return true;
            }
            if (betweenB == null || this.canNotMoveBetween(square2, betweenB, true)) {
                return true;
            }
        }
        return false;
    }

    boolean isKnownBlocked(Square square1, Square square2) {
        if (square1.z != square2.z) {
            return false;
        }
        KnownBlockedEdges kbe1 = this.knownBlockedEdges.get(square1.id);
        KnownBlockedEdges kbe2 = this.knownBlockedEdges.get(square2.id);
        if (kbe1 != null && kbe1.isBlocked(square2.x, square2.y)) {
            return true;
        }
        return kbe2 != null && kbe2.isBlocked(square1.x, square1.y);
    }

    boolean canAnimalBreakObstacle(Square square1, Square square2, boolean colW, boolean colN, boolean colE, boolean colS) {
        if (!this.mover.canThump) {
            return false;
        }
        if (colW) {
            return square1.has(2) && square1.has(8192);
        }
        if (colN) {
            return square1.has(4) && square1.has(16384);
        }
        if (colE) {
            return square2.has(2) && square2.has(8192);
        }
        if (colS) {
            return square2.has(4) && square2.has(16384);
        }
        return false;
    }

    final class InitProc
    implements TIntObjectProcedure<Node> {
        final /* synthetic */ VGAStar this$0;

        InitProc(VGAStar this$0) {
            VGAStar vGAStar = this$0;
            Objects.requireNonNull(vGAStar);
            this.this$0 = vGAStar;
        }

        @Override
        public boolean execute(int id, Node vgNode) {
            SearchNode searchNode = SearchNode.alloc().init(this.this$0, vgNode);
            searchNode.square = vgNode.square;
            this.this$0.squareToNode.put(id, searchNode);
            this.this$0.nodeMap.put(vgNode.id, searchNode);
            this.this$0.searchNodes.add(searchNode);
            return true;
        }
    }
}

