/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.profiling;

import java.util.Stack;
import zombie.GameProfiler;
import zombie.core.profiling.AbstractPerformanceProfileProbe;

public class PerformanceProfileProbe
extends AbstractPerformanceProfileProbe {
    private final Stack<GameProfiler.ProfileArea> currentArea = new Stack();

    public PerformanceProfileProbe(String name) {
        this(name, true);
    }

    public PerformanceProfileProbe(String name, boolean isEnabled) {
        super(name);
        this.setEnabled(isEnabled);
    }

    @Override
    protected void onStart() {
        this.currentArea.push(GameProfiler.getInstance().start(this.name));
    }

    @Override
    protected void onEnd() {
        GameProfiler.getInstance().end(this.currentArea.pop());
    }
}

