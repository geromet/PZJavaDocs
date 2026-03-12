/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;
import zombie.characters.animals.pathfind.LowLevelSearchNode;

public final class LowLevelGoalNode
implements IGoalNode {
    LowLevelSearchNode searchNode;

    @Override
    public boolean inGoal(ISearchNode other) {
        return other == this.searchNode;
    }
}

