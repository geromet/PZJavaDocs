/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;
import zombie.pathfind.SearchNode;

final class GoalNode
implements IGoalNode {
    SearchNode searchNode;

    GoalNode() {
    }

    GoalNode init(SearchNode node) {
        this.searchNode = node;
        return this;
    }

    @Override
    public boolean inGoal(ISearchNode other) {
        return other == this.searchNode;
    }
}

