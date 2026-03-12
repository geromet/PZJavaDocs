/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.jobs;

import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.isoregion.jobs.JobApplyChanges;
import zombie.iso.areas.isoregion.jobs.JobChunkUpdate;
import zombie.iso.areas.isoregion.jobs.JobDebugResetAllData;
import zombie.iso.areas.isoregion.jobs.JobServerSendFullData;
import zombie.iso.areas.isoregion.jobs.JobSquareUpdate;
import zombie.iso.areas.isoregion.jobs.RegionJob;

public final class RegionJobManager {
    private static final ConcurrentLinkedQueue<JobSquareUpdate> poolSquareUpdate = new ConcurrentLinkedQueue();
    private static final ConcurrentLinkedQueue<JobChunkUpdate> poolChunkUpdate = new ConcurrentLinkedQueue();
    private static final ConcurrentLinkedQueue<JobApplyChanges> poolApplyChanges = new ConcurrentLinkedQueue();
    private static final ConcurrentLinkedQueue<JobServerSendFullData> poolServerSendFullData = new ConcurrentLinkedQueue();
    private static final ConcurrentLinkedQueue<JobDebugResetAllData> poolDebugResetAllData = new ConcurrentLinkedQueue();

    public static JobSquareUpdate allocSquareUpdate(int x, int y, int z, byte flags) {
        JobSquareUpdate j = poolSquareUpdate.poll();
        if (j == null) {
            j = new JobSquareUpdate();
        }
        j.worldSquareX = x;
        j.worldSquareY = y;
        j.worldSquareZ = z;
        j.newSquareFlags = flags;
        return j;
    }

    public static JobChunkUpdate allocChunkUpdate() {
        JobChunkUpdate j = poolChunkUpdate.poll();
        if (j == null) {
            j = new JobChunkUpdate();
        }
        return j;
    }

    public static JobApplyChanges allocApplyChanges(boolean saveToDisk) {
        JobApplyChanges j = poolApplyChanges.poll();
        if (j == null) {
            j = new JobApplyChanges();
        }
        j.saveToDisk = saveToDisk;
        return j;
    }

    public static JobServerSendFullData allocServerSendFullData(UdpConnection conn) {
        JobServerSendFullData j = poolServerSendFullData.poll();
        if (j == null) {
            j = new JobServerSendFullData();
        }
        j.targetConn = conn;
        return j;
    }

    public static JobDebugResetAllData allocDebugResetAllData() {
        JobDebugResetAllData j = poolDebugResetAllData.poll();
        if (j == null) {
            j = new JobDebugResetAllData();
        }
        return j;
    }

    public static void release(RegionJob job) {
        job.reset();
        switch (job.getJobType()) {
            case SquareUpdate: {
                poolSquareUpdate.add((JobSquareUpdate)job);
                break;
            }
            case ApplyChanges: {
                poolApplyChanges.add((JobApplyChanges)job);
                break;
            }
            case ChunkUpdate: {
                poolChunkUpdate.add((JobChunkUpdate)job);
                break;
            }
            case ServerSendFullData: {
                poolServerSendFullData.add((JobServerSendFullData)job);
                break;
            }
            case DebugResetAllData: {
                poolDebugResetAllData.add((JobDebugResetAllData)job);
                break;
            }
            default: {
                if (!Core.debug) break;
                throw new RuntimeException("No pooling for this job type?");
            }
        }
    }
}

