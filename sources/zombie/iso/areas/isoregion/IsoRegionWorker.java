/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import zombie.GameWindow;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.iso.areas.isoregion.IsoRegionException;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.jobs.JobApplyChanges;
import zombie.iso.areas.isoregion.jobs.JobChunkUpdate;
import zombie.iso.areas.isoregion.jobs.JobServerSendFullData;
import zombie.iso.areas.isoregion.jobs.JobSquareUpdate;
import zombie.iso.areas.isoregion.jobs.RegionJob;
import zombie.iso.areas.isoregion.jobs.RegionJobManager;
import zombie.iso.areas.isoregion.jobs.RegionJobType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.util.list.PZArrayUtil;

public final class IsoRegionWorker {
    private Thread thread;
    private boolean finished;
    protected static final AtomicBoolean isRequestingBufferSwap = new AtomicBoolean(false);
    private static IsoRegionWorker instance;
    private DataRoot rootBuffer = new DataRoot();
    private List<Integer> discoveredChunks = new ArrayList<Integer>();
    private final List<Integer> threadDiscoveredChunks = new ArrayList<Integer>();
    private int lastThreadDiscoveredChunksSize;
    private final ConcurrentLinkedQueue<RegionJob> jobQueue = new ConcurrentLinkedQueue();
    private final ConcurrentLinkedQueue<JobChunkUpdate> jobOutgoingQueue = new ConcurrentLinkedQueue();
    private final List<RegionJob> jobBatchedProcessing = new ArrayList<RegionJob>();
    private final ConcurrentLinkedQueue<RegionJob> finishedJobQueue = new ConcurrentLinkedQueue();
    private static final ByteBuffer byteBuffer;

    protected IsoRegionWorker() {
        instance = this;
    }

    protected void create() {
        if (this.thread != null) {
            return;
        }
        this.finished = false;
        this.thread = new Thread(ThreadGroups.Workers, () -> {
            while (!this.finished) {
                try {
                    this.thread_main_loop();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.thread.setPriority(5);
        this.thread.setDaemon(true);
        this.thread.setName("IsoRegionWorker");
        this.thread.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        this.thread.start();
    }

    protected void stop() {
        if (this.thread == null) {
            return;
        }
        if (this.thread != null) {
            this.finished = true;
            isRequestingBufferSwap.set(false);
            while (this.thread.isAlive()) {
            }
            this.thread = null;
        }
        if (!this.jobQueue.isEmpty()) {
            DebugType.IsoRegion.warn("IsoRegionWorker -> JobQueue has items remaining");
        }
        if (!this.jobBatchedProcessing.isEmpty()) {
            DebugType.IsoRegion.warn("IsoRegionWorker -> JobBatchedProcessing has items remaining");
        }
        this.jobQueue.clear();
        this.jobOutgoingQueue.clear();
        this.jobBatchedProcessing.clear();
        this.finishedJobQueue.clear();
        this.rootBuffer = null;
        this.discoveredChunks = null;
    }

    protected void EnqueueJob(RegionJob j) {
        this.jobQueue.add(j);
    }

    protected void ApplyChunkChanges() {
        this.ApplyChunkChanges(true);
    }

    protected void ApplyChunkChanges(boolean saveToDisk) {
        JobApplyChanges j = RegionJobManager.allocApplyChanges(saveToDisk);
        this.jobQueue.add(j);
    }

    private void thread_main_loop() throws InterruptedException, IsoRegionException {
        IsoRegions.printD = DebugLog.isEnabled(DebugType.IsoRegion);
        int applyChangesIndex = PZArrayUtil.indexOf(this.jobBatchedProcessing, regionJob -> regionJob instanceof JobApplyChanges);
        RegionJob job = this.jobQueue.poll();
        while (job != null) {
            switch (job.getJobType()) {
                case ServerSendFullData: {
                    if (!GameServer.server) break;
                    UdpConnection target = ((JobServerSendFullData)job).getTargetConn();
                    if (target == null) {
                        if (Core.debug) {
                            throw new IsoRegionException("IsoRegion: Server send full data target connection == null");
                        }
                        IsoRegions.warn("IsoRegion: Server send full data target connection == null");
                        break;
                    }
                    IsoRegions.log("IsoRegion: Server Send Full Data to " + target.getIDStr());
                    ArrayList<DataChunk> allChunks = new ArrayList<DataChunk>();
                    this.rootBuffer.getAllChunks(allChunks);
                    JobChunkUpdate outJob = RegionJobManager.allocChunkUpdate();
                    outJob.setTargetConn(target);
                    for (DataChunk c : allChunks) {
                        if (!outJob.canAddChunk()) {
                            this.jobOutgoingQueue.add(outJob);
                            outJob = RegionJobManager.allocChunkUpdate();
                            outJob.setTargetConn(target);
                        }
                        outJob.addChunkFromDataChunk(c);
                    }
                    if (outJob.getChunkCount() > 0) {
                        this.jobOutgoingQueue.add(outJob);
                    } else {
                        RegionJobManager.release(outJob);
                    }
                    this.finishedJobQueue.add(job);
                    break;
                }
                case DebugResetAllData: {
                    IsoRegions.log("IsoRegion: Debug Reset All Data");
                    for (int cycle = 0; cycle < 2; ++cycle) {
                        this.rootBuffer.resetAllData();
                        if (cycle != 0) continue;
                        isRequestingBufferSwap.set(true);
                        while (isRequestingBufferSwap.get() && !this.finished) {
                            Thread.sleep(5L);
                        }
                    }
                    this.finishedJobQueue.add(job);
                    break;
                }
                case SquareUpdate: 
                case ChunkUpdate: 
                case ApplyChanges: {
                    IsoRegions.log("IsoRegion: Queueing " + String.valueOf((Object)job.getJobType()) + " for batched processing.");
                    if (job instanceof JobApplyChanges) {
                        JobApplyChanges jac = (JobApplyChanges)job;
                        if (applyChangesIndex != -1) {
                            JobApplyChanges job1 = (JobApplyChanges)this.jobBatchedProcessing.remove(applyChangesIndex);
                            jac.setSaveToDisk(jac.isSaveToDisk() || job1.isSaveToDisk());
                            this.finishedJobQueue.add(job1);
                        }
                        applyChangesIndex = this.jobBatchedProcessing.size();
                    }
                    this.jobBatchedProcessing.add(job);
                    break;
                }
                default: {
                    this.finishedJobQueue.add(job);
                }
            }
            job = this.jobQueue.poll();
        }
        if (applyChangesIndex != -1) {
            this.thread_run_batched_jobs();
            this.jobBatchedProcessing.clear();
        }
        Thread.sleep(20L);
    }

    private void thread_run_batched_jobs() throws InterruptedException {
        IsoRegions.log("IsoRegion: Apply changes -> Batched processing " + this.jobBatchedProcessing.size() + " jobs.");
        for (int cycle = 0; cycle < 2; ++cycle) {
            block8: for (int index = 0; index < this.jobBatchedProcessing.size(); ++index) {
                RegionJob j = this.jobBatchedProcessing.get(index);
                switch (j.getJobType()) {
                    case SquareUpdate: {
                        RegionJob job = (JobSquareUpdate)j;
                        this.rootBuffer.updateExistingSquare(((JobSquareUpdate)job).getWorldSquareX(), ((JobSquareUpdate)job).getWorldSquareY(), ((JobSquareUpdate)job).getWorldSquareZ(), ((JobSquareUpdate)job).getNewSquareFlags());
                        continue block8;
                    }
                    case ChunkUpdate: {
                        RegionJob job = (JobChunkUpdate)j;
                        ((JobChunkUpdate)job).readChunksPacket(this.rootBuffer, this.threadDiscoveredChunks);
                        continue block8;
                    }
                    case ApplyChanges: {
                        this.rootBuffer.processDirtyChunks();
                        if (cycle == 0) {
                            isRequestingBufferSwap.set(true);
                            while (isRequestingBufferSwap.get()) {
                                Thread.sleep(5L);
                            }
                            continue block8;
                        }
                        RegionJob job = (JobApplyChanges)j;
                        if (!GameClient.client && ((JobApplyChanges)job).isSaveToDisk()) {
                            for (int i = this.jobBatchedProcessing.size() - 1; i >= 0; --i) {
                                JobChunkUpdate chunkJob;
                                RegionJob batchedJob = this.jobBatchedProcessing.get(i);
                                if (batchedJob.getJobType() != RegionJobType.ChunkUpdate && batchedJob.getJobType() != RegionJobType.SquareUpdate) continue;
                                if (batchedJob.getJobType() == RegionJobType.SquareUpdate) {
                                    JobSquareUpdate squareJob = (JobSquareUpdate)batchedJob;
                                    this.rootBuffer.select.reset(squareJob.getWorldSquareX(), squareJob.getWorldSquareY(), squareJob.getWorldSquareZ(), true, false);
                                    chunkJob = RegionJobManager.allocChunkUpdate();
                                    chunkJob.addChunkFromDataChunk(this.rootBuffer.select.chunk);
                                } else {
                                    this.jobBatchedProcessing.remove(i);
                                    chunkJob = (JobChunkUpdate)batchedJob;
                                }
                                chunkJob.saveChunksToDisk();
                                if (!GameServer.server) continue;
                                this.jobOutgoingQueue.add(chunkJob);
                            }
                            if (!this.threadDiscoveredChunks.isEmpty() && this.threadDiscoveredChunks.size() > this.lastThreadDiscoveredChunksSize && !Core.getInstance().isNoSave()) {
                                IsoRegions.log("IsoRegion: Apply changes -> Saving header file to disk.");
                                File saveFile = IsoRegions.getHeaderFile();
                                try {
                                    DataOutputStream output = new DataOutputStream(new FileOutputStream(saveFile));
                                    output.writeInt(244);
                                    output.writeInt(this.threadDiscoveredChunks.size());
                                    for (Integer integer : this.threadDiscoveredChunks) {
                                        output.writeInt(integer);
                                    }
                                    output.flush();
                                    output.close();
                                    this.lastThreadDiscoveredChunksSize = this.threadDiscoveredChunks.size();
                                }
                                catch (Exception e) {
                                    DebugLog.log(e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                        this.finishedJobQueue.addAll(this.jobBatchedProcessing);
                    }
                }
            }
        }
    }

    protected DataRoot getRootBuffer() {
        return this.rootBuffer;
    }

    protected void setRootBuffer(DataRoot root) {
        this.rootBuffer = root;
    }

    protected void load() {
        IsoRegions.log("IsoRegion: Load save map.");
        if (!GameClient.client) {
            this.loadSaveMap();
        } else {
            GameClient.sendIsoRegionDataRequest();
        }
    }

    protected void update() {
        RegionJob job = this.finishedJobQueue.poll();
        while (job != null) {
            RegionJobManager.release(job);
            job = this.finishedJobQueue.poll();
        }
        job = this.jobOutgoingQueue.poll();
        while (job != null) {
            if (GameServer.server) {
                IsoRegions.log("IsoRegion: sending changed datachunks packet.");
                try {
                    for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                        UdpConnection c = GameServer.udpEngine.connections.get(n);
                        if (((JobChunkUpdate)job).getTargetConn() != null && ((JobChunkUpdate)job).getTargetConn() != c) continue;
                        ByteBufferWriter bbw = c.startPacket();
                        PacketTypes.PacketType.IsoRegionServerPacket.doPacket(bbw);
                        ByteBufferWriter output = bbw;
                        output.putLong(System.nanoTime());
                        ((JobChunkUpdate)job).saveChunksToNetBuffer(bbw);
                        PacketTypes.PacketType.IsoRegionServerPacket.send(c);
                    }
                }
                catch (Exception e) {
                    DebugLog.log(e.getMessage());
                    e.printStackTrace();
                }
            }
            RegionJobManager.release(job);
            job = this.jobOutgoingQueue.poll();
        }
    }

    protected void readServerUpdatePacket(ByteBufferReader input) {
        if (GameClient.client) {
            IsoRegions.log("IsoRegion: Receiving changed datachunk packet from server");
            try {
                JobChunkUpdate j = RegionJobManager.allocChunkUpdate();
                long serverUpdateTime = input.getLong();
                j.readChunksFromNetBuffer(input, serverUpdateTime);
                this.EnqueueJob(j);
                this.ApplyChunkChanges();
            }
            catch (Exception e) {
                DebugLog.log(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected void readClientRequestFullUpdatePacket(ByteBufferReader input, UdpConnection conn) {
        if (GameServer.server && conn != null) {
            IsoRegions.log("IsoRegion: Receiving request full data packet from client");
            try {
                JobServerSendFullData j = RegionJobManager.allocServerSendFullData(conn);
                this.EnqueueJob(j);
            }
            catch (Exception e) {
                DebugLog.log(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected void addDebugResetJob() {
        if (!GameServer.server && !GameClient.client) {
            this.EnqueueJob(RegionJobManager.allocDebugResetAllData());
        }
    }

    protected void addSquareChangedJob(int x, int y, int z, boolean isRemoval, byte flags) {
        if (z < 0) {
            return;
        }
        int chunkX = x / 8;
        int chunkY = y / 8;
        int chunkHashId = IsoRegions.hash(chunkX, chunkY);
        if (this.discoveredChunks.contains(chunkHashId)) {
            IsoRegions.log("Update square only, plus any unprocessed chunks in a 7x7 grid.", Colors.Magenta);
            JobSquareUpdate j = RegionJobManager.allocSquareUpdate(x, y, z, flags);
            this.EnqueueJob(j);
            this.readSurroundingChunks(chunkX, chunkY, 7, false);
            this.ApplyChunkChanges();
        } else {
            if (isRemoval) {
                return;
            }
            IsoRegions.log("Adding new chunk, plus any unprocessed chunks in a 7x7 grid.", Colors.Magenta);
            this.readSurroundingChunks(chunkX, chunkY, 7, true);
        }
    }

    protected void readSurroundingChunks(int chunkX, int chunkY, int dimension, boolean applyChanges) {
        this.readSurroundingChunks(chunkX, chunkY, dimension, applyChanges, false);
    }

    protected void readSurroundingChunks(int chunkX, int chunkY, int dimension, boolean applyChanges, boolean forceRecalc) {
        int sides = 1;
        if (dimension > 0 && dimension <= IsoChunkMap.chunkGridWidth && (sides = dimension / 2) + sides >= IsoChunkMap.chunkGridWidth) {
            --sides;
        }
        int chunkMinX = chunkX - sides;
        int chunkMinY = chunkY - sides;
        int chunkMaxX = chunkX + sides;
        int chunkMaxY = chunkY + sides;
        JobChunkUpdate j = RegionJobManager.allocChunkUpdate();
        boolean hasAddedChunks = false;
        for (int xx = chunkMinX; xx <= chunkMaxX; ++xx) {
            for (int yy = chunkMinY; yy <= chunkMaxY; ++yy) {
                IsoChunk c;
                IsoChunk isoChunk = c = GameServer.server ? ServerMap.instance.getChunk(xx, yy) : IsoWorld.instance.getCell().getChunk(xx, yy);
                if (c == null) continue;
                int hashid = IsoRegions.hash(c.wx, c.wy);
                if (!forceRecalc && this.discoveredChunks.contains(hashid)) continue;
                this.discoveredChunks.add(hashid);
                if (!j.canAddChunk()) {
                    this.EnqueueJob(j);
                    j = RegionJobManager.allocChunkUpdate();
                }
                j.addChunkFromIsoChunk(c);
                hasAddedChunks = true;
            }
        }
        if (j.getChunkCount() > 0) {
            this.EnqueueJob(j);
        } else {
            RegionJobManager.release(j);
        }
        if (hasAddedChunks && applyChanges) {
            this.ApplyChunkChanges();
        }
    }

    private void loadSaveMap() {
        try {
            boolean hasReadHeader = false;
            ArrayList<Integer> headerKnownChunks = new ArrayList<Integer>();
            File header = IsoRegions.getHeaderFile();
            if (header.exists()) {
                DataInputStream input = new DataInputStream(new FileInputStream(header));
                hasReadHeader = true;
                int worldVers = input.readInt();
                int size = input.readInt();
                for (int i = 0; i < size; ++i) {
                    int id = input.readInt();
                    headerKnownChunks.add(id);
                }
                input.close();
            }
            File dir = IsoRegions.getDirectory();
            File[] files = dir.listFiles(new FilenameFilter(this){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("datachunk_") && name.endsWith(".bin");
                }
            });
            JobChunkUpdate j = RegionJobManager.allocChunkUpdate();
            ByteBuffer bb = byteBuffer;
            boolean loadedStuff = false;
            if (files != null) {
                for (File chunkFile : files) {
                    try (FileInputStream inStream = new FileInputStream(chunkFile);){
                        bb.clear();
                        int len = inStream.read(bb.array());
                        bb.limit(len);
                        bb.mark();
                        int l = bb.getInt();
                        int vers = bb.getInt();
                        int x = bb.getInt();
                        int y = bb.getInt();
                        bb.reset();
                        int hashid = IsoRegions.hash(x, y);
                        if (!this.discoveredChunks.contains(hashid)) {
                            this.discoveredChunks.add(hashid);
                        }
                        if (headerKnownChunks.contains(hashid)) {
                            headerKnownChunks.remove(headerKnownChunks.indexOf(hashid));
                        } else {
                            IsoRegions.warn("IsoRegion: A chunk save has been found that was not in header known chunks list.");
                        }
                        if (!j.canAddChunk()) {
                            this.EnqueueJob(j);
                            j = RegionJobManager.allocChunkUpdate();
                        }
                        j.addChunkFromFile(bb);
                        loadedStuff = true;
                    }
                }
            }
            if (j.getChunkCount() > 0) {
                this.EnqueueJob(j);
            } else {
                RegionJobManager.release(j);
            }
            if (loadedStuff) {
                this.ApplyChunkChanges(false);
            }
            if (hasReadHeader && !headerKnownChunks.isEmpty()) {
                IsoRegions.warn("IsoRegion: " + headerKnownChunks.size() + " previously discovered chunks have not been loaded.");
                throw new IsoRegionException("IsoRegion: " + headerKnownChunks.size() + " previously discovered chunks have not been loaded.");
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    static {
        byteBuffer = ByteBuffer.allocate(2076);
    }
}

