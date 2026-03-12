/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.jobs;

import zombie.iso.areas.isoregion.jobs.RegionJobType;

public abstract class RegionJob {
    private final RegionJobType type;

    protected RegionJob(RegionJobType type) {
        this.type = type;
    }

    protected void reset() {
    }

    public RegionJobType getJobType() {
        return this.type;
    }
}

