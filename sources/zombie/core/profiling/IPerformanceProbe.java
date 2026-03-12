/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.profiling;

import zombie.GameProfiler;

public interface IPerformanceProbe {
    default public boolean isProbeEnabled() {
        return this.isEnabled() && GameProfiler.isRunning();
    }

    public boolean isEnabled();

    public void setEnabled(boolean var1);
}

