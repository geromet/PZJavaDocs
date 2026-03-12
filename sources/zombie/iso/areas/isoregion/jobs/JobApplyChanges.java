/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.jobs;

import zombie.iso.areas.isoregion.jobs.RegionJob;
import zombie.iso.areas.isoregion.jobs.RegionJobType;

public class JobApplyChanges
extends RegionJob {
    protected boolean saveToDisk;

    protected JobApplyChanges() {
        super(RegionJobType.ApplyChanges);
    }

    @Override
    protected void reset() {
        this.saveToDisk = false;
    }

    public void setSaveToDisk(boolean b) {
        this.saveToDisk = b;
    }

    public boolean isSaveToDisk() {
        return this.saveToDisk;
    }
}

