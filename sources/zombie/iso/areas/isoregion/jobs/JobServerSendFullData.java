/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.jobs;

import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.isoregion.jobs.RegionJob;
import zombie.iso.areas.isoregion.jobs.RegionJobType;

public class JobServerSendFullData
extends RegionJob {
    protected UdpConnection targetConn;

    protected JobServerSendFullData() {
        super(RegionJobType.ServerSendFullData);
    }

    @Override
    protected void reset() {
        this.targetConn = null;
    }

    public UdpConnection getTargetConn() {
        return this.targetConn;
    }
}

