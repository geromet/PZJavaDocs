/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.highLevel;

import astar.IGoalNode;
import astar.ISearchNode;
import zombie.pathfind.highLevel.HLSearchNode;

public class HLGoalNode
implements IGoalNode {
    HLSearchNode searchNode;

    HLGoalNode init(HLSearchNode node) {
        this.searchNode = node;
        return this;
    }

    @Override
    public boolean inGoal(ISearchNode other) {
        return other == this.searchNode;
    }
}

