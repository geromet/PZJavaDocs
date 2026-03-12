/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import gnu.trove.set.hash.THashSet;
import imgui.ImGui;
import java.util.List;
import zombie.core.physics.BallisticsTarget;
import zombie.debug.debugWindows.PZDebugWindow;
import zombie.debug.debugWindows.PZImGui;
import zombie.util.IPooledObject;
import zombie.util.Pool;

public class BallisticsTargetPanel
extends PZDebugWindow {
    @Override
    public String getTitle() {
        return "Ballistics Targets";
    }

    @Override
    protected void doWindowContents() {
        ImGui.begin(this.getTitle(), 64);
        if (PZImGui.collapsingHeader("Ballistics Target Pool")) {
            int id;
            BallisticsTarget ballisticsTarget;
            Pool<IPooledObject> ballisticsTargetPool = BallisticsTarget.getBallisticsTargetPool();
            Pool.PoolStacks ballisticsTargetPoolStacks = ballisticsTargetPool.getPoolStacks().get();
            THashSet<IPooledObject> ballisticsTargetAllocated = ballisticsTargetPoolStacks.getInUse();
            List<IPooledObject> ballistictsTargetReleased = ballisticsTargetPoolStacks.getReleased();
            for (IPooledObject pooledObject : ballisticsTargetAllocated) {
                ballisticsTarget = (BallisticsTarget)pooledObject;
                if (ballisticsTarget != null && !PZImGui.collapsingHeader("InUse: " + Integer.toString(id = ballisticsTarget.getID()))) continue;
            }
            for (IPooledObject pooledObject : ballistictsTargetReleased) {
                ballisticsTarget = (BallisticsTarget)pooledObject;
                if (ballisticsTarget != null && !PZImGui.collapsingHeader("Released: " + Integer.toString(id = ballisticsTarget.getID()))) continue;
            }
        }
        ImGui.end();
    }
}

