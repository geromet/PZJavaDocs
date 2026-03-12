/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import zombie.ai.astar.Mover;
import zombie.pathfind.Path;

public interface IPathfinder {
    public void Succeeded(Path var1, Mover var2);

    public void Failed(Mover var1);
}

