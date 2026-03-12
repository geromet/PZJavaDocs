/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.profiling;

import zombie.GameProfiler;
import zombie.core.profiling.PerformanceProfileProbe;

public class PerformanceProfileFrameProbe
extends PerformanceProfileProbe {
    public PerformanceProfileFrameProbe(String name) {
        super(name);
    }

    @Override
    public void start() {
        GameProfiler.getInstance().startFrame(this.name);
        super.start();
    }

    @Override
    public void end() {
        super.end();
        GameProfiler.getInstance().endFrame();
    }
}

