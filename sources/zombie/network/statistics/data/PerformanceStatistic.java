/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.statistics.data;

import zombie.network.statistics.counters.Counter;
import zombie.network.statistics.data.IStatistic;
import zombie.network.statistics.data.Statistic;

public class PerformanceStatistic
extends Statistic
implements IStatistic {
    private static final PerformanceStatistic instance = new PerformanceStatistic("performance");
    public final Counter memoryFree = new Counter(this, "memory-free", 0.0, () -> Runtime.getRuntime().freeMemory() / 1000000L, "Free memory", "bytes");
    public final Counter memoryTotal = new Counter(this, "memory-total", 0.0, () -> Runtime.getRuntime().totalMemory() / 1000000L, "Total memory", "bytes");
    public final Counter memoryUsed = new Counter(this, "memory-used", 0.0, () -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000L, "Used memory", "bytes");
    public final Counter memoryMax = new Counter(this, "memory-max", 0.0, () -> Runtime.getRuntime().maxMemory() / 1000000L, "Maximum memory", "bytes");
    public final Counter minUpdatePeriod = new Counter(this, "min-update-period", 9.223372036854776E18, null, "Minimum update cycle duration", "ms");
    public final Counter maxUpdatePeriod = new Counter(this, "max-update-period", 0.0, null, "Maximum update cycle duration", "ms");
    public final Counter avgUpdatePeriod = new Counter(this, "avg-update-period", 0.0, null, "Average update cycle duration", "ms");
    public final Counter fps = new Counter(this, "fps", 0.0, null, "Current update cycle duration", "ms");

    private PerformanceStatistic(String application) {
        super(application);
    }

    public static PerformanceStatistic getInstance() {
        return instance;
    }

    public void addUpdate(long period) {
        if ((double)period > this.maxUpdatePeriod.get()) {
            this.maxUpdatePeriod.set(period);
        }
        if ((double)period < this.minUpdatePeriod.get()) {
            this.minUpdatePeriod.set(period);
        }
        this.avgUpdatePeriod.set((long)(((double)period - this.avgUpdatePeriod.get()) * (double)0.05f));
        this.fps.set(period);
    }
}

