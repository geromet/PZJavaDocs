/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import org.lwjglx.BufferUtils;
import zombie.ChunkMapFilenames;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoChunk;
import zombie.network.ChunkChecksum;
import zombie.network.ClientChunkRequest;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.SentChunkPacket;

public final class PlayerDownloadServer {
    public WorkerThread workerThread;
    private final UdpConnection connection;
    private boolean networkFileDebug;
    private final CRC32 crc32 = new CRC32();
    private final ByteBuffer bb = ByteBuffer.allocate(1000000);
    private final ByteBuffer sb = BufferUtils.createByteBuffer(1000000);
    private final ByteBufferWriter bbw = new ByteBufferWriter(this.bb);
    public final ArrayList<ClientChunkRequest> ccrWaiting = new ArrayList();

    public PlayerDownloadServer(UdpConnection connection) {
        this.connection = connection;
        this.workerThread = new WorkerThread(this);
        this.workerThread.setDaemon(true);
        this.workerThread.setName("PlayerDownloadServer" + Rand.Next(Integer.MAX_VALUE));
        this.workerThread.start();
    }

    public void destroy() {
        this.workerThread.putCommand(EThreadCommand.Quit, null);
        while (this.workerThread.isAlive()) {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedException) {}
        }
        this.workerThread = null;
    }

    public ClientChunkRequest getClientChunkRequest() {
        ClientChunkRequest ccr = this.workerThread.freeRequests.poll();
        if (ccr == null) {
            ccr = new ClientChunkRequest();
        }
        return ccr;
    }

    public final int getWaitingRequests() {
        return this.ccrWaiting.size();
    }

    public void update() {
        this.networkFileDebug = DebugType.NetworkFileDebug.isEnabled();
        if (!this.workerThread.ready) {
            return;
        }
        this.removeOlderDuplicateRequests();
        if (this.ccrWaiting.isEmpty()) {
            if (this.workerThread.cancelQ.isEmpty() && !this.workerThread.cancelled.isEmpty()) {
                this.workerThread.cancelled.clear();
            }
            return;
        }
        ClientChunkRequest ccr = this.ccrWaiting.remove(0);
        for (int i = 0; i < ccr.chunks.size(); ++i) {
            ClientChunkRequest.Chunk reqChunk = ccr.chunks.get(i);
            if (this.workerThread.isRequestCancelled(reqChunk)) {
                ccr.chunks.remove(i--);
                ccr.releaseChunk(reqChunk);
                continue;
            }
            IsoChunk chunk = ServerMap.instance.getChunk(reqChunk.wx, reqChunk.wy);
            if (chunk == null) continue;
            try {
                ccr.getByteBuffer(reqChunk);
                chunk.SaveLoadedChunk(reqChunk, this.crc32);
                continue;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                LoggerManager.getLogger("map").write(ex);
                this.workerThread.sendNotRequired(reqChunk, false);
                ccr.chunks.remove(i--);
                ccr.releaseChunk(reqChunk);
            }
        }
        if (ccr.chunks.isEmpty()) {
            this.workerThread.freeRequests.add(ccr);
            return;
        }
        this.workerThread.ready = false;
        this.workerThread.putCommand(EThreadCommand.RequestZipArray, ccr);
    }

    private void removeOlderDuplicateRequests() {
        for (int i = this.ccrWaiting.size() - 1; i >= 0; --i) {
            ClientChunkRequest ccr1 = this.ccrWaiting.get(i);
            for (int j = 0; j < ccr1.chunks.size(); ++j) {
                ClientChunkRequest.Chunk chunk1 = ccr1.chunks.get(j);
                if (this.workerThread.isRequestCancelled(chunk1)) {
                    ccr1.chunks.remove(j--);
                    ccr1.releaseChunk(chunk1);
                    continue;
                }
                for (int k = i - 1; k >= 0; --k) {
                    ClientChunkRequest ccr2 = this.ccrWaiting.get(k);
                    if (!this.cancelDuplicateChunk(ccr2, chunk1.wx, chunk1.wy)) continue;
                }
            }
            if (!ccr1.chunks.isEmpty()) continue;
            this.ccrWaiting.remove(i);
            this.workerThread.freeRequests.add(ccr1);
        }
    }

    private boolean cancelDuplicateChunk(ClientChunkRequest ccr, int wx, int wy) {
        for (int i = 0; i < ccr.chunks.size(); ++i) {
            ClientChunkRequest.Chunk reqChunk = ccr.chunks.get(i);
            if (this.workerThread.isRequestCancelled(reqChunk)) {
                ccr.chunks.remove(i--);
                ccr.releaseChunk(reqChunk);
                continue;
            }
            if (reqChunk.wx != wx || reqChunk.wy != wy) continue;
            this.workerThread.sendNotRequired(reqChunk, false);
            ccr.chunks.remove(i);
            ccr.releaseChunk(reqChunk);
            return true;
        }
        return false;
    }

    private void sendPacket(PacketTypes.PacketType packetType) {
        this.bb.flip();
        this.sb.put(this.bb);
        this.sb.flip();
        this.connection.getPeer().SendRaw(this.sb, packetType.packetPriority, packetType.packetReliability, (byte)0, this.connection.getConnectedGUID(), false);
        this.sb.clear();
    }

    private ByteBufferWriter startPacket() {
        this.bb.clear();
        return this.bbw;
    }

    public final class WorkerThread
    extends Thread {
        boolean quit;
        volatile boolean ready;
        final LinkedBlockingQueue<WorkerThreadCommand> commandQ;
        final ConcurrentLinkedQueue<ClientChunkRequest> freeRequests;
        public final ConcurrentLinkedQueue<Integer> cancelQ;
        final HashSet<Integer> cancelled;
        final CRC32 crcMaker;
        byte[] inMemoryZip;
        final Deflater compressor;
        final /* synthetic */ PlayerDownloadServer this$0;

        public WorkerThread(PlayerDownloadServer this$0) {
            PlayerDownloadServer playerDownloadServer = this$0;
            Objects.requireNonNull(playerDownloadServer);
            this.this$0 = playerDownloadServer;
            this.ready = true;
            this.commandQ = new LinkedBlockingQueue();
            this.freeRequests = new ConcurrentLinkedQueue();
            this.cancelQ = new ConcurrentLinkedQueue();
            this.cancelled = new HashSet();
            this.crcMaker = new CRC32();
            this.inMemoryZip = new byte[20480];
            this.compressor = new Deflater();
        }

        @Override
        public void run() {
            while (!this.quit) {
                try {
                    this.runInner();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void runInner() throws InterruptedException, IOException {
            WorkerThreadCommand command = this.commandQ.take();
            switch (command.e.ordinal()) {
                case 0: {
                    try {
                        this.sendLargeArea(command.ccr);
                        break;
                    }
                    finally {
                        this.ready = true;
                    }
                }
                case 1: {
                    try {
                        this.sendArray(command.ccr);
                        break;
                    }
                    finally {
                        this.ready = true;
                    }
                }
                case 2: {
                    this.quit = true;
                }
            }
        }

        void putCommand(EThreadCommand e, ClientChunkRequest ccr) {
            WorkerThreadCommand command = new WorkerThreadCommand();
            command.e = e;
            command.ccr = ccr;
            while (true) {
                try {
                    this.commandQ.put(command);
                }
                catch (InterruptedException interruptedException) {
                    continue;
                }
                break;
            }
        }

        public int compressChunk(ClientChunkRequest.Chunk chunk) {
            this.compressor.reset();
            this.compressor.setInput(chunk.bb.array(), 0, chunk.bb.limit());
            this.compressor.finish();
            if ((double)this.inMemoryZip.length < (double)chunk.bb.limit() * 1.5) {
                this.inMemoryZip = new byte[(int)((double)chunk.bb.limit() * 1.5)];
            }
            return this.compressor.deflate(this.inMemoryZip, 0, this.inMemoryZip.length, 3);
        }

        private void sendChunk(ClientChunkRequest.Chunk chunk) {
            try {
                SentChunkPacket packet = new SentChunkPacket();
                int filesize = this.compressChunk(chunk);
                packet.setChunk(chunk, filesize, this.inMemoryZip);
                while (packet.hasData()) {
                    ByteBufferWriter b = this.this$0.startPacket();
                    PacketTypes.PacketType.SentChunk.doPacket(b);
                    packet.write(b);
                    this.this$0.sendPacket(PacketTypes.PacketType.SentChunk);
                }
            }
            catch (Exception ex) {
                DebugLog.Multiplayer.printException(ex, "sendChunk error", LogSeverity.Error);
                this.sendNotRequired(chunk, false);
            }
        }

        private void sendNotRequired(ClientChunkRequest.Chunk chunk, boolean sameOnServer) {
            ByteBufferWriter b = this.this$0.startPacket();
            PacketTypes.PacketType.NotRequiredInZip.doPacket(b);
            b.putInt(1);
            b.putInt(chunk.requestNumber);
            b.putBoolean(sameOnServer);
            this.this$0.sendPacket(PacketTypes.PacketType.NotRequiredInZip);
        }

        private void sendLargeArea(ClientChunkRequest ccr) throws IOException {
            for (int n = 0; n < ccr.chunks.size(); ++n) {
                ClientChunkRequest.Chunk reqChunk = ccr.chunks.get(n);
                int wx = reqChunk.wx;
                int wy = reqChunk.wy;
                if (reqChunk.bb != null) {
                    reqChunk.bb.limit(reqChunk.bb.position());
                    reqChunk.bb.position(0);
                    this.sendChunk(reqChunk);
                    ccr.releaseBuffer(reqChunk);
                    continue;
                }
                File inFile = ChunkMapFilenames.instance.getFilename(wx, wy);
                if (!inFile.exists()) continue;
                ccr.getByteBuffer(reqChunk);
                reqChunk.bb = IsoChunk.SafeRead(wx, wy, reqChunk.bb);
                this.sendChunk(reqChunk);
                ccr.releaseBuffer(reqChunk);
            }
            ClientChunkRequest.freeBuffers.clear();
            ccr.chunks.clear();
        }

        private void sendArray(ClientChunkRequest ccr) throws IOException {
            int n;
            for (n = 0; n < ccr.chunks.size(); ++n) {
                ClientChunkRequest.Chunk reqChunk = ccr.chunks.get(n);
                if (this.isRequestCancelled(reqChunk)) continue;
                int wx = reqChunk.wx;
                int wy = reqChunk.wy;
                long crc = reqChunk.crc;
                if (reqChunk.bb != null) {
                    boolean add = true;
                    if (reqChunk.crc != 0L) {
                        this.crcMaker.reset();
                        this.crcMaker.update(reqChunk.bb.array(), 0, reqChunk.bb.position());
                        boolean bl = add = reqChunk.crc != this.crcMaker.getValue();
                        if (add && this.this$0.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln(wx + "," + wy + ": crc server=" + this.crcMaker.getValue() + " client=" + reqChunk.crc);
                        }
                    }
                    if (add) {
                        if (this.this$0.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln(wx + "," + wy + ": send=true loaded=true");
                        }
                        reqChunk.bb.limit(reqChunk.bb.position());
                        reqChunk.bb.position(0);
                        this.sendChunk(reqChunk);
                    } else {
                        if (this.this$0.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln(wx + "," + wy + ": send=false loaded=true");
                        }
                        this.sendNotRequired(reqChunk, true);
                    }
                    ccr.releaseBuffer(reqChunk);
                    continue;
                }
                File inFile = ChunkMapFilenames.instance.getFilename(wx, wy);
                if (inFile.exists()) {
                    long crcCached = ChunkChecksum.getChecksum(wx, wy);
                    if (crcCached != 0L && crcCached == reqChunk.crc) {
                        if (this.this$0.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln(wx + "," + wy + ": send=false loaded=false file=true");
                        }
                        this.sendNotRequired(reqChunk, true);
                        continue;
                    }
                    ccr.getByteBuffer(reqChunk);
                    reqChunk.bb = IsoChunk.SafeRead(wx, wy, reqChunk.bb);
                    boolean add = true;
                    if (reqChunk.crc != 0L) {
                        this.crcMaker.reset();
                        this.crcMaker.update(reqChunk.bb.array(), 0, reqChunk.bb.limit());
                        boolean bl = add = reqChunk.crc != this.crcMaker.getValue();
                    }
                    if (add) {
                        if (this.this$0.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln(wx + "," + wy + ": send=true loaded=false file=true");
                        }
                        this.sendChunk(reqChunk);
                    } else {
                        if (this.this$0.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln(wx + "," + wy + ": send=false loaded=false file=true");
                        }
                        this.sendNotRequired(reqChunk, true);
                    }
                    ccr.releaseBuffer(reqChunk);
                    continue;
                }
                if (this.this$0.networkFileDebug) {
                    DebugLog.NetworkFileDebug.debugln(wx + "," + wy + ": send=false loaded=false file=false");
                }
                this.sendNotRequired(reqChunk, crc == 0L);
            }
            for (n = 0; n < ccr.chunks.size(); ++n) {
                ccr.releaseChunk(ccr.chunks.get(n));
            }
            ccr.chunks.clear();
            this.freeRequests.add(ccr);
        }

        private boolean isRequestCancelled(ClientChunkRequest.Chunk reqChunk) {
            Integer requestNumber = this.cancelQ.poll();
            while (requestNumber != null) {
                this.cancelled.add(requestNumber);
                requestNumber = this.cancelQ.poll();
            }
            if (this.cancelled.remove(reqChunk.requestNumber)) {
                if (this.this$0.networkFileDebug) {
                    DebugLog.NetworkFileDebug.debugln("cancelled request #" + reqChunk.requestNumber);
                }
                return true;
            }
            return false;
        }
    }

    private static enum EThreadCommand {
        RequestLargeArea,
        RequestZipArray,
        Quit;

    }

    private static final class WorkerThreadCommand {
        EThreadCommand e;
        ClientChunkRequest ccr;

        private WorkerThreadCommand() {
        }
    }
}

