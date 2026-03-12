/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.profiling;

import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.util.list.PZArrayUtil;

public class PerformanceProfileProbeList<Probe extends PerformanceProfileProbe> {
    final String prefix;
    final Probe[] layers;

    public static <Probe extends PerformanceProfileProbe> PerformanceProfileProbeList<Probe> construct(String prefix, int count, Class<Probe> type, Constructor<Probe> ctor) {
        return new PerformanceProfileProbeList<Probe>(prefix, count, type, ctor);
    }

    protected PerformanceProfileProbeList(String prefix, int count, Class<Probe> type, Constructor<Probe> ctor) {
        this.prefix = prefix;
        this.layers = (PerformanceProfileProbe[])PZArrayUtil.newInstance(type, count + 1);
        for (int i = 0; i < count; ++i) {
            this.layers[i] = ctor.get(prefix + "_" + i);
        }
        this.layers[count] = ctor.get(prefix + "_etc");
    }

    public int count() {
        return this.layers.length;
    }

    private Probe at(int layerIdx) {
        return this.layers[layerIdx < this.count() ? layerIdx : this.count() - 1];
    }

    public Probe start(int layerIdx) {
        Probe probe = this.at(layerIdx);
        ((AbstractPerformanceProfileProbe)probe).start();
        return probe;
    }

    public static interface Constructor<Probe extends PerformanceProfileProbe> {
        public Probe get(String var1);
    }
}

