/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.jobs;

import zombie.iso.areas.isoregion.jobs.RegionJob;
import zombie.iso.areas.isoregion.jobs.RegionJobType;

public class JobSquareUpdate
extends RegionJob {
    protected int worldSquareX;
    protected int worldSquareY;
    protected int worldSquareZ;
    protected byte newSquareFlags;

    protected JobSquareUpdate() {
        super(RegionJobType.SquareUpdate);
    }

    public int getWorldSquareX() {
        return this.worldSquareX;
    }

    public int getWorldSquareY() {
        return this.worldSquareY;
    }

    public int getWorldSquareZ() {
        return this.worldSquareZ;
    }

    public byte getNewSquareFlags() {
        return this.newSquareFlags;
    }
}

