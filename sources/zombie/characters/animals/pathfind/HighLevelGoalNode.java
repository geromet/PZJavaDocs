/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;
import zombie.characters.animals.pathfind.HighLevelSearchNode;

public final class HighLevelGoalNode
implements IGoalNode {
    HighLevelSearchNode searchNode;

    HighLevelGoalNode init(HighLevelSearchNode node) {
        this.searchNode = node;
        return this;
    }

    @Override
    public boolean inGoal(ISearchNode other) {
        return other == this.searchNode;
    }
}

