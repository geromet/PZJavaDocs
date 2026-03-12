/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.statistics.data;

import zombie.network.statistics.counters.Counter;

public interface IStatistic {
    public String getName();

    public void store(Counter var1);

    default public void init() {
    }

    public void update();
}

