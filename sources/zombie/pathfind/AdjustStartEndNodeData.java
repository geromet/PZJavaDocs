/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import zombie.pathfind.Edge;
import zombie.pathfind.Node;
import zombie.pathfind.Obstacle;
import zombie.pathfind.VisibilityGraph;

public final class AdjustStartEndNodeData {
    public Obstacle obstacle;
    public Node node;
    public Edge newEdge;
    public boolean isNodeNew;
    public VisibilityGraph graph;
}

