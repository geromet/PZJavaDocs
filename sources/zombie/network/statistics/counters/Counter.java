/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.statistics.counters;

import zombie.network.statistics.counters.ICounter;
import zombie.network.statistics.data.Statistic;

public class Counter
implements ICounter {
    protected final String name;
    protected final String tooltip;
    protected final String units;
    protected final double defaultValue;
    protected final ICounter lambda;
    protected double value;

    public Counter(Statistic statistic, String name, double value, ICounter lambda2, String tooltip, String units) {
        this.name = name;
        this.tooltip = tooltip;
        this.units = units;
        this.value = value;
        this.defaultValue = value;
        this.lambda = lambda2;
        statistic.store(this);
    }

    public String getName() {
        return this.name;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    public String getUnits() {
        return this.units;
    }

    public void clear() {
        this.value = this.defaultValue;
    }

    public void increase() {
        this.value += 1.0;
    }

    public void increase(double value) {
        this.value += value;
    }

    @Override
    public double get() {
        return this.lambda == null ? this.value : this.lambda.get();
    }

    public void set(double value) {
        this.value = value;
    }
}

