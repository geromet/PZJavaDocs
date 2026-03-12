/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.WorldStreamer;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.popman.ZombiePopulationRenderer;
import zombie.util.ByteBufferBackedInputStream;
import zombie.util.ByteBufferOutputStream;
import zombie.util.PZSQLUtils;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class VehiclesDB2 {
    public static final int INVALID_ID = -1;
    private static final int MIN_ID = 1;
    public static final VehiclesDB2 instance = new VehiclesDB2();
    private static final ThreadLocal<ByteBuffer> TL_SliceBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(32768));
    private static final ThreadLocal<byte[]> TL_Bytes = ThreadLocal.withInitial(() -> new byte[1024]);
    private final MainThread main = new MainThread();
    private final WorldStreamerThread worldStreamer = new WorldStreamerThread();

    public void init() {
        this.worldStreamer.store.init(this.main.usedIds, this.main.seenChunks);
    }

    public void Reset() {
        assert (WorldStreamer.instance.worldStreamer == null);
        this.updateWorldStreamer();
        QueueItem item = this.main.queue.poll();
        while (item != null) {
            item.release();
            item = this.main.queue.poll();
        }
        this.main.Reset();
        this.worldStreamer.Reset();
    }

    public void updateMain() throws IOException {
        this.main.update();
    }

    public void updateWorldStreamer() {
        this.worldStreamer.update();
    }

    public void setForceSave() {
        this.main.forceSave = true;
    }

    public void renderDebug(ZombiePopulationRenderer renderer) {
    }

    public void setChunkSeen(int wx, int wy) {
        this.main.setChunkSeen(wx, wy);
    }

    public boolean isChunkSeen(int wx, int wy) {
        return this.main.isChunkSeen(wx, wy);
    }

    public void setVehicleLoaded(BaseVehicle vehicle) {
        this.main.setVehicleLoaded(vehicle);
    }

    public void setVehicleUnloaded(BaseVehicle vehicle) {
        this.main.setVehicleUnloaded(vehicle);
    }

    public boolean isVehicleLoaded(BaseVehicle vehicle) {
        return this.main.loadedIds.contains(vehicle.sqlId);
    }

    public void loadChunkMain(IsoChunk chunk) {
        this.main.loadChunk(chunk);
    }

    public void loadChunk(IsoChunk chunk) throws IOException {
        this.worldStreamer.loadChunk(chunk);
    }

    public void unloadChunk(IsoChunk chunk) {
        if (Thread.currentThread() != WorldStreamer.instance.worldStreamer) {
            boolean bl = true;
        }
        this.worldStreamer.unloadChunk(chunk);
    }

    public void addVehicle(BaseVehicle vehicle) {
        try {
            this.main.addVehicle(vehicle);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public void removeVehicle(BaseVehicle vehicle) {
        this.main.removeVehicle(vehicle);
    }

    public void updateVehicle(BaseVehicle vehicle) {
        try {
            this.main.updateVehicle(vehicle);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public void updateVehicleAndTrailer(BaseVehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        this.updateVehicle(vehicle);
        BaseVehicle trailer = vehicle.getVehicleTowing();
        if (trailer != null) {
            this.updateVehicle(trailer);
        }
    }

    public void importPlayersFromOldDB(IImportPlayerFromOldDB consumer) {
        SQLStore sqlStore = Type.tryCastTo(this.worldStreamer.store, SQLStore.class);
        if (sqlStore == null || sqlStore.conn == null) {
            return;
        }
        try {
            DatabaseMetaData metaData = sqlStore.conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "localPlayers", null);){
                if (!rs.next()) {
                    return;
                }
            }
        }
        catch (Exception e1) {
            ExceptionLogger.logException(e1);
            return;
        }
        String sql = "SELECT id, name, wx, wy, x, y, z, worldversion, data, isDead FROM localPlayers";
        try (PreparedStatement pstmt = sqlStore.conn.prepareStatement("SELECT id, name, wx, wy, x, y, z, worldversion, data, isDead FROM localPlayers");){
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int sqlId = rs.getInt(1);
                String name = rs.getString(2);
                int wx = rs.getInt(3);
                int wy = rs.getInt(4);
                float x = rs.getFloat(5);
                float y = rs.getFloat(6);
                float z = rs.getFloat(7);
                int worldVersion = rs.getInt(8);
                byte[] buf = rs.getBytes(9);
                boolean isDead = rs.getBoolean(10);
                consumer.accept(sqlId, name, wx, wy, x, y, z, worldVersion, buf, isDead);
            }
        }
        catch (Exception e1) {
            ExceptionLogger.logException(e1);
        }
        try {
            Statement stmt = sqlStore.conn.createStatement();
            stmt.executeUpdate("DROP TABLE localPlayers");
            stmt.executeUpdate("DROP TABLE networkPlayers");
            sqlStore.conn.commit();
        }
        catch (Exception e1) {
            ExceptionLogger.logException(e1);
        }
    }

    private static final class MainThread {
        final TIntHashSet seenChunks = new TIntHashSet();
        final TIntHashSet usedIds = new TIntHashSet();
        final TIntHashSet loadedIds = new TIntHashSet();
        boolean forceSave;
        final ConcurrentLinkedQueue<QueueItem> queue = new ConcurrentLinkedQueue();

        MainThread() {
            this.seenChunks.setAutoCompactionFactor(0.0f);
            this.usedIds.setAutoCompactionFactor(0.0f);
            this.loadedIds.setAutoCompactionFactor(0.0f);
        }

        void Reset() {
            this.seenChunks.clear();
            this.usedIds.clear();
            this.loadedIds.clear();
            assert (this.queue.isEmpty());
            this.queue.clear();
            this.forceSave = false;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void update() throws IOException {
            if (!GameClient.client && !GameServer.server && this.forceSave) {
                this.forceSave = false;
                for (int i = 0; i < 4; ++i) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null || player.getVehicle() == null || !player.getVehicle().isEngineRunning()) continue;
                    this.updateVehicle(player.getVehicle());
                    BaseVehicle trailer = player.getVehicle().getVehicleTowing();
                    if (trailer == null) continue;
                    this.updateVehicle(trailer);
                }
            }
            QueueItem item = this.queue.poll();
            while (item != null) {
                try {
                    item.processMain();
                }
                finally {
                    item.release();
                }
                item = this.queue.poll();
            }
        }

        void setChunkSeen(int wx, int wy) {
            int key = wy << 16 | wx;
            this.seenChunks.add(key);
        }

        boolean isChunkSeen(int wx, int wy) {
            int key = wy << 16 | wx;
            return this.seenChunks.contains(key);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        int allocateID() {
            TIntHashSet tIntHashSet = this.usedIds;
            synchronized (tIntHashSet) {
                for (int i = 1; i < Integer.MAX_VALUE; ++i) {
                    if (this.usedIds.contains(i)) continue;
                    this.usedIds.add(i);
                    return i;
                }
            }
            throw new RuntimeException("ran out of unused vehicle ids");
        }

        void setVehicleLoaded(BaseVehicle vehicle) {
            if (vehicle.sqlId == -1) {
                vehicle.sqlId = this.allocateID();
            }
            assert (!this.loadedIds.contains(vehicle.sqlId));
            this.loadedIds.add(vehicle.sqlId);
        }

        void setVehicleUnloaded(BaseVehicle vehicle) {
            if (vehicle.sqlId == -1) {
                return;
            }
            this.loadedIds.remove(vehicle.sqlId);
        }

        void addVehicle(BaseVehicle vehicle) throws IOException {
            if (vehicle.sqlId == -1) {
                vehicle.sqlId = this.allocateID();
            }
            QueueAddVehicle item = QueueAddVehicle.s_pool.alloc();
            item.init(vehicle);
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }

        void removeVehicle(BaseVehicle vehicle) {
            QueueRemoveVehicle item = QueueRemoveVehicle.s_pool.alloc();
            item.init(vehicle);
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }

        void updateVehicle(BaseVehicle vehicle) throws IOException {
            if (vehicle.sqlId == -1) {
                vehicle.sqlId = this.allocateID();
            }
            QueueUpdateVehicle item = QueueUpdateVehicle.s_pool.alloc();
            item.init(vehicle);
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }

        void loadChunk(IsoChunk chunk) {
            QueueLoadChunk item = QueueLoadChunk.s_pool.alloc();
            item.init(chunk.wx, chunk.wy);
            chunk.loadVehiclesObject = item;
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }
    }

    private static final class WorldStreamerThread {
        final IVehicleStore store = new SQLStore();
        final ConcurrentLinkedQueue<QueueItem> queue = new ConcurrentLinkedQueue();
        final VehicleBuffer vehicleBuffer = new VehicleBuffer();

        private WorldStreamerThread() {
        }

        void Reset() {
            this.store.Reset();
            assert (this.queue.isEmpty());
            this.queue.clear();
        }

        void update() {
            QueueItem item = this.queue.poll();
            while (item != null) {
                try {
                    item.processWorldStreamer();
                }
                finally {
                    VehiclesDB2.instance.main.queue.add(item);
                }
                item = this.queue.poll();
            }
        }

        void loadChunk(IsoChunk chunk) throws IOException {
            this.store.loadChunk(chunk, this::vehicleLoaded);
        }

        void vehicleLoaded(IsoChunk chunk, VehicleBuffer vehicleBuffer) throws IOException {
            assert (vehicleBuffer.id >= 1);
            int chunksPerWidth = 8;
            IsoGridSquare sq = chunk.getGridSquare((int)(vehicleBuffer.x - (float)(chunk.wx * 8)), (int)(vehicleBuffer.y - (float)(chunk.wy * 8)), 0);
            BaseVehicle vehicle = new BaseVehicle(IsoWorld.instance.currentCell);
            vehicle.setSquare(sq);
            vehicle.setCurrent(sq);
            try {
                vehicle.load(vehicleBuffer.bb, vehicleBuffer.worldVersion);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
                DebugLog.General.error("vehicle %d is being deleted because an error occurred loading it", vehicleBuffer.id);
                this.store.removeVehicle(vehicleBuffer.id);
                return;
            }
            vehicle.sqlId = vehicleBuffer.id;
            vehicle.chunk = chunk;
            if (chunk.jobType == IsoChunk.JobType.SoftReset) {
                vehicle.softReset();
            }
            chunk.vehicles.add(vehicle);
        }

        void unloadChunk(IsoChunk chunk) {
            for (int i = 0; i < chunk.vehicles.size(); ++i) {
                try {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    this.vehicleBuffer.set(vehicle);
                    this.store.updateVehicle(this.vehicleBuffer);
                    continue;
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
            }
        }
    }

    private static abstract class IVehicleStore {
        private IVehicleStore() {
        }

        abstract void init(TIntHashSet var1, TIntHashSet var2);

        abstract void Reset();

        abstract void loadChunk(IsoChunk var1, ThrowingBiConsumer<IsoChunk, VehicleBuffer, IOException> var2) throws IOException;

        abstract void loadChunk(int var1, int var2, ThrowingConsumer<VehicleBuffer, IOException> var3) throws IOException;

        abstract void updateVehicle(VehicleBuffer var1);

        abstract void removeVehicle(int var1);
    }

    private static abstract class QueueItem
    extends PooledObject {
        private QueueItem() {
        }

        abstract void processMain();

        abstract void processWorldStreamer();
    }

    private static final class SQLStore
    extends IVehicleStore {
        Connection conn;
        final VehicleBuffer vehicleBuffer = new VehicleBuffer();

        private SQLStore() {
        }

        @Override
        void init(TIntHashSet usedIDs, TIntHashSet seenChunks) {
            usedIDs.clear();
            seenChunks.clear();
            if (Core.getInstance().isNoSave()) {
                return;
            }
            this.create();
            try {
                this.initUsedIDs(usedIDs, seenChunks);
            }
            catch (SQLException ex) {
                ExceptionLogger.logException(ex);
            }
        }

        @Override
        void Reset() {
            if (this.conn == null) {
                return;
            }
            try {
                this.conn.close();
            }
            catch (SQLException ex) {
                ExceptionLogger.logException(ex);
            }
            this.conn = null;
        }

        @Override
        void loadChunk(IsoChunk chunk, ThrowingBiConsumer<IsoChunk, VehicleBuffer, IOException> consumer) throws IOException {
            if (this.conn == null || chunk == null) {
                return;
            }
            String sql = "SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?";
            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?");){
                pstmt.setInt(1, chunk.wx);
                pstmt.setInt(2, chunk.wy);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    boolean serialise;
                    this.vehicleBuffer.id = rs.getInt(1);
                    this.vehicleBuffer.wx = chunk.wx;
                    this.vehicleBuffer.wy = chunk.wy;
                    this.vehicleBuffer.x = rs.getFloat(2);
                    this.vehicleBuffer.y = rs.getFloat(3);
                    InputStream input = rs.getBinaryStream(4);
                    this.vehicleBuffer.setBytes(input);
                    this.vehicleBuffer.worldVersion = rs.getInt(5);
                    boolean bl = serialise = this.vehicleBuffer.bb.get() != 0;
                    byte classID = this.vehicleBuffer.bb.get();
                    if (classID != IsoObject.getFactoryVehicle().getClassID() || !serialise) continue;
                    consumer.accept(chunk, this.vehicleBuffer);
                }
            }
            catch (Exception e1) {
                ExceptionLogger.logException(e1);
            }
        }

        @Override
        void loadChunk(int wx, int wy, ThrowingConsumer<VehicleBuffer, IOException> consumer) throws IOException {
            if (this.conn == null) {
                return;
            }
            String sql = "SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?";
            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?");){
                pstmt.setInt(1, wx);
                pstmt.setInt(2, wy);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    boolean serialise;
                    this.vehicleBuffer.id = rs.getInt(1);
                    this.vehicleBuffer.wx = wx;
                    this.vehicleBuffer.wy = wy;
                    this.vehicleBuffer.x = rs.getFloat(2);
                    this.vehicleBuffer.y = rs.getFloat(3);
                    InputStream input = rs.getBinaryStream(4);
                    this.vehicleBuffer.setBytes(input);
                    this.vehicleBuffer.worldVersion = rs.getInt(5);
                    boolean bl = serialise = this.vehicleBuffer.bb.get() != 0;
                    byte classID = this.vehicleBuffer.bb.get();
                    if (classID != IsoObject.getFactoryVehicle().getClassID() || !serialise) continue;
                    consumer.accept(this.vehicleBuffer);
                }
            }
            catch (Exception e1) {
                ExceptionLogger.logException(e1);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        void updateVehicle(VehicleBuffer vehicleBuffer) {
            if (this.conn == null) {
                return;
            }
            assert (vehicleBuffer.id >= 1);
            TIntHashSet tIntHashSet = VehiclesDB2.instance.main.usedIds;
            synchronized (tIntHashSet) {
                assert (VehiclesDB2.instance.main.usedIds.contains(vehicleBuffer.id));
            }
            try {
                if (this.isInDB(vehicleBuffer.id)) {
                    this.updateDB(vehicleBuffer);
                } else {
                    this.addToDB(vehicleBuffer);
                }
            }
            catch (Exception e1) {
                ExceptionLogger.logException(e1);
                this.rollback();
            }
        }

        boolean isInDB(int id) throws SQLException {
            String sql = "SELECT 1 FROM vehicles WHERE id=?";
            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT 1 FROM vehicles WHERE id=?");){
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                boolean bl = rs.next();
                return bl;
            }
        }

        void addToDB(VehicleBuffer vehicleBuffer) throws SQLException {
            String sql = "INSERT INTO vehicles(wx,wy,x,y,worldversion,data,id) VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO vehicles(wx,wy,x,y,worldversion,data,id) VALUES(?,?,?,?,?,?,?)");){
                pstmt.setInt(1, vehicleBuffer.wx);
                pstmt.setInt(2, vehicleBuffer.wy);
                pstmt.setFloat(3, vehicleBuffer.x);
                pstmt.setFloat(4, vehicleBuffer.y);
                pstmt.setInt(5, vehicleBuffer.worldVersion);
                ByteBuffer bb = vehicleBuffer.bb;
                bb.rewind();
                pstmt.setBinaryStream(6, (InputStream)new ByteBufferBackedInputStream(bb), bb.remaining());
                pstmt.setInt(7, vehicleBuffer.id);
                int rowAffected = pstmt.executeUpdate();
                this.conn.commit();
            }
            catch (Exception ex) {
                this.rollback();
                throw ex;
            }
        }

        void updateDB(VehicleBuffer vehicleBuffer) throws SQLException {
            String sqlUpdate = "UPDATE vehicles SET wx = ?, wy = ?, x = ?, y = ?, worldversion = ?, data = ? WHERE id=?";
            try (PreparedStatement pstmt = this.conn.prepareStatement("UPDATE vehicles SET wx = ?, wy = ?, x = ?, y = ?, worldversion = ?, data = ? WHERE id=?");){
                pstmt.setInt(1, vehicleBuffer.wx);
                pstmt.setInt(2, vehicleBuffer.wy);
                pstmt.setFloat(3, vehicleBuffer.x);
                pstmt.setFloat(4, vehicleBuffer.y);
                pstmt.setInt(5, vehicleBuffer.worldVersion);
                ByteBuffer bb = vehicleBuffer.bb;
                bb.rewind();
                pstmt.setBinaryStream(6, (InputStream)new ByteBufferBackedInputStream(bb), bb.remaining());
                pstmt.setInt(7, vehicleBuffer.id);
                int rowAffected = pstmt.executeUpdate();
                this.conn.commit();
            }
            catch (Exception ex) {
                this.rollback();
                throw ex;
            }
        }

        @Override
        void removeVehicle(int sqlID) {
            if (this.conn == null || sqlID < 1) {
                return;
            }
            String sql = "DELETE FROM vehicles WHERE id=?";
            try (PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM vehicles WHERE id=?");){
                pstmt.setInt(1, sqlID);
                int rowAffected = pstmt.executeUpdate();
                this.conn.commit();
            }
            catch (Exception e1) {
                ExceptionLogger.logException(e1);
                this.rollback();
            }
        }

        void create() {
            Statement stat;
            String filename = ZomboidFileSystem.instance.getCurrentSaveDir();
            File dbDir = new File(filename);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            File dbFile = new File(filename + File.separator + "vehicles.db");
            dbFile.setReadable(true, false);
            dbFile.setExecutable(true, false);
            dbFile.setWritable(true, false);
            if (!dbFile.exists()) {
                try {
                    dbFile.createNewFile();
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                    stat = this.conn.createStatement();
                    stat.executeUpdate("CREATE TABLE vehicles (id   INTEGER PRIMARY KEY NOT NULL,wx    INTEGER,wy    INTEGER,x    FLOAT,y    FLOAT,worldversion    INTEGER,data BLOB);");
                    stat.executeUpdate("CREATE INDEX ivwx ON vehicles (wx);");
                    stat.executeUpdate("CREATE INDEX ivwy ON vehicles (wy);");
                    stat.close();
                }
                catch (Exception e) {
                    ExceptionLogger.logException(e);
                    DebugLog.log("failed to create vehicles database");
                    System.exit(1);
                }
            }
            if (this.conn == null) {
                try {
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                }
                catch (Exception ex) {
                    DebugLog.log("failed to create vehicles database");
                    ExceptionLogger.logException(ex);
                    System.exit(1);
                }
            }
            try {
                stat = this.conn.createStatement();
                stat.executeQuery("PRAGMA JOURNAL_MODE=TRUNCATE;");
                stat.close();
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                System.exit(1);
            }
            try {
                this.conn.setAutoCommit(false);
            }
            catch (SQLException e) {
                ExceptionLogger.logException(e);
            }
        }

        private String searchPathForSqliteLib(String library) {
            for (String path : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
                File file = new File(path, library);
                if (!file.exists()) continue;
                return path;
            }
            return "";
        }

        void initUsedIDs(TIntHashSet usedIDs, TIntHashSet seenChunks) throws SQLException {
            String sql = "SELECT wx,wy,id FROM vehicles";
            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT wx,wy,id FROM vehicles");){
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int wx = rs.getInt(1);
                    int wy = rs.getInt(2);
                    seenChunks.add(wy << 16 | wx);
                    usedIDs.add(rs.getInt(3));
                }
            }
        }

        private void rollback() {
            if (this.conn == null) {
                return;
            }
            try {
                this.conn.rollback();
            }
            catch (SQLException e) {
                ExceptionLogger.logException(e);
            }
        }
    }

    public static interface IImportPlayerFromOldDB {
        public void accept(int var1, String var2, int var3, int var4, float var5, float var6, float var7, int var8, byte[] var9, boolean var10);
    }

    private static final class QueueUpdateVehicle
    extends QueueItem {
        static final Pool<QueueUpdateVehicle> s_pool = new Pool<QueueUpdateVehicle>(QueueUpdateVehicle::new);
        final VehicleBuffer vehicleBuffer = new VehicleBuffer();

        private QueueUpdateVehicle() {
        }

        void init(BaseVehicle vehicle) throws IOException {
            this.vehicleBuffer.set(vehicle);
        }

        @Override
        void processMain() {
        }

        @Override
        void processWorldStreamer() {
            VehiclesDB2.instance.worldStreamer.store.updateVehicle(this.vehicleBuffer);
        }
    }

    private static class QueueRemoveVehicle
    extends QueueItem {
        static final Pool<QueueRemoveVehicle> s_pool = new Pool<QueueRemoveVehicle>(QueueRemoveVehicle::new);
        int id;

        private QueueRemoveVehicle() {
        }

        void init(BaseVehicle vehicle) {
            this.id = vehicle.sqlId;
        }

        @Override
        void processMain() {
        }

        @Override
        void processWorldStreamer() {
            VehiclesDB2.instance.worldStreamer.store.removeVehicle(this.id);
        }
    }

    private static class QueueLoadChunk
    extends QueueItem {
        static final Pool<QueueLoadChunk> s_pool = new Pool<QueueLoadChunk>(QueueLoadChunk::new);
        int wx;
        int wy;
        final ArrayList<BaseVehicle> vehicles = new ArrayList();
        IsoGridSquare dummySquare;

        private QueueLoadChunk() {
        }

        void init(int wx, int wy) {
            this.wx = wx;
            this.wy = wy;
            this.vehicles.clear();
            if (this.dummySquare == null) {
                this.dummySquare = IsoGridSquare.getNew(IsoWorld.instance.currentCell, null, 0, 0, 0);
            }
        }

        @Override
        void processMain() {
            IsoChunk chunk = ServerMap.instance.getChunk(this.wx, this.wy);
            if (chunk == null) {
                this.vehicles.clear();
                return;
            }
            if (chunk.loadVehiclesObject != this) {
                this.vehicles.clear();
                return;
            }
            chunk.loadVehiclesObject = null;
            int chunksPerWidth = 8;
            for (int i = 0; i < this.vehicles.size(); ++i) {
                BaseVehicle vehicle = this.vehicles.get(i);
                IsoGridSquare sq = chunk.getGridSquare(PZMath.fastfloor(vehicle.getX() - (float)(chunk.wx * 8)), PZMath.fastfloor(vehicle.getY() - (float)(chunk.wy * 8)), 0);
                vehicle.setSquare(sq);
                vehicle.setCurrent(sq);
                vehicle.chunk = chunk;
                if (chunk.jobType == IsoChunk.JobType.SoftReset) {
                    vehicle.softReset();
                }
                if (!vehicle.addedToWorld && instance.isVehicleLoaded(vehicle)) {
                    vehicle.removeFromSquare();
                    this.vehicles.remove(i);
                    --i;
                    continue;
                }
                chunk.vehicles.add(vehicle);
                if (vehicle.addedToWorld) continue;
                vehicle.addToWorld();
            }
            this.vehicles.clear();
        }

        @Override
        void processWorldStreamer() {
            try {
                VehiclesDB2.instance.worldStreamer.store.loadChunk(this.wx, this.wy, this::vehicleLoaded);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }

        void vehicleLoaded(VehicleBuffer vehicleBuffer) throws IOException {
            assert (vehicleBuffer.id >= 1);
            int chunksPerWidth = 8;
            int squareX = PZMath.fastfloor(vehicleBuffer.x - (float)(this.wx * 8));
            int squareY = PZMath.fastfloor(vehicleBuffer.y - (float)(this.wy * 8));
            this.dummySquare.x = squareX;
            this.dummySquare.y = squareY;
            IsoGridSquare sq = this.dummySquare;
            BaseVehicle vehicle = new BaseVehicle(IsoWorld.instance.currentCell);
            vehicle.setSquare(sq);
            vehicle.setCurrent(sq);
            try {
                vehicle.load(vehicleBuffer.bb, vehicleBuffer.worldVersion);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
                DebugLog.General.error("vehicle %d is being deleted because an error occurred loading it", vehicleBuffer.id);
                VehiclesDB2.instance.worldStreamer.store.removeVehicle(vehicleBuffer.id);
                return;
            }
            vehicle.sqlId = vehicleBuffer.id;
            this.vehicles.add(vehicle);
        }
    }

    private static final class QueueAddVehicle
    extends QueueItem {
        static final Pool<QueueAddVehicle> s_pool = new Pool<QueueAddVehicle>(QueueAddVehicle::new);
        final VehicleBuffer vehicleBuffer = new VehicleBuffer();

        private QueueAddVehicle() {
        }

        void init(BaseVehicle vehicle) throws IOException {
            this.vehicleBuffer.set(vehicle);
        }

        @Override
        void processMain() {
        }

        @Override
        void processWorldStreamer() {
            VehiclesDB2.instance.worldStreamer.store.updateVehicle(this.vehicleBuffer);
        }
    }

    private static class MemoryStore
    extends IVehicleStore {
        final TIntObjectHashMap<VehicleBuffer> idToVehicle = new TIntObjectHashMap();
        final TIntObjectHashMap<ArrayList<VehicleBuffer>> chunkToVehicles = new TIntObjectHashMap();

        private MemoryStore() {
        }

        @Override
        void init(TIntHashSet usedIDs, TIntHashSet seenChunks) {
            usedIDs.clear();
            seenChunks.clear();
        }

        @Override
        void Reset() {
            this.idToVehicle.clear();
            this.chunkToVehicles.clear();
        }

        @Override
        void loadChunk(IsoChunk chunk, ThrowingBiConsumer<IsoChunk, VehicleBuffer, IOException> consumer) throws IOException {
            int key = chunk.wy << 16 | chunk.wx;
            ArrayList<VehicleBuffer> vehicles = this.chunkToVehicles.get(key);
            if (vehicles == null) {
                return;
            }
            for (int i = 0; i < vehicles.size(); ++i) {
                VehicleBuffer vehicleBuffer = vehicles.get(i);
                vehicleBuffer.bb.rewind();
                boolean serialize = vehicleBuffer.bb.get() != 0;
                int hashCode = vehicleBuffer.bb.getInt();
                consumer.accept(chunk, vehicleBuffer);
            }
        }

        @Override
        void loadChunk(int wx, int wy, ThrowingConsumer<VehicleBuffer, IOException> consumer) throws IOException {
            int key = wy << 16 | wx;
            ArrayList<VehicleBuffer> vehicles = this.chunkToVehicles.get(key);
            if (vehicles == null) {
                return;
            }
            for (int i = 0; i < vehicles.size(); ++i) {
                VehicleBuffer vehicleBuffer = vehicles.get(i);
                vehicleBuffer.bb.rewind();
                boolean serialize = vehicleBuffer.bb.get() != 0;
                int hashCode = vehicleBuffer.bb.getInt();
                consumer.accept(vehicleBuffer);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        void updateVehicle(VehicleBuffer vehicle) {
            int key;
            assert (vehicle.id >= 1);
            TIntHashSet tIntHashSet = VehiclesDB2.instance.main.usedIds;
            synchronized (tIntHashSet) {
                assert (VehiclesDB2.instance.main.usedIds.contains(vehicle.id));
            }
            vehicle.bb.rewind();
            VehicleBuffer vehicle2 = this.idToVehicle.get(vehicle.id);
            if (vehicle2 == null) {
                vehicle2 = new VehicleBuffer();
                vehicle2.id = vehicle.id;
                this.idToVehicle.put(vehicle.id, vehicle2);
            } else {
                key = vehicle2.wy << 16 | vehicle2.wx;
                this.chunkToVehicles.get(key).remove(vehicle2);
            }
            vehicle2.wx = vehicle.wx;
            vehicle2.wy = vehicle.wy;
            vehicle2.x = vehicle.x;
            vehicle2.y = vehicle.y;
            vehicle2.worldVersion = vehicle.worldVersion;
            vehicle2.setBytes(vehicle.bb);
            key = vehicle2.wy << 16 | vehicle2.wx;
            if (this.chunkToVehicles.get(key) == null) {
                this.chunkToVehicles.put(key, new ArrayList());
            }
            this.chunkToVehicles.get(key).add(vehicle2);
        }

        @Override
        void removeVehicle(int id) {
            VehicleBuffer vehicle = this.idToVehicle.remove(id);
            if (vehicle == null) {
                return;
            }
            int key = vehicle.wy << 16 | vehicle.wx;
            this.chunkToVehicles.get(key).remove(vehicle);
        }
    }

    @FunctionalInterface
    public static interface ThrowingBiConsumer<T1, T2, E extends Exception> {
        public void accept(T1 var1, T2 var2) throws E;
    }

    @FunctionalInterface
    public static interface ThrowingConsumer<T1, E extends Exception> {
        public void accept(T1 var1) throws E;
    }

    private static final class VehicleBuffer {
        int id = -1;
        int wx;
        int wy;
        float x;
        float y;
        int worldVersion;
        ByteBuffer bb = ByteBuffer.allocate(32768);

        private VehicleBuffer() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void set(BaseVehicle vehicle) throws IOException {
            assert (vehicle.sqlId >= 1);
            TIntHashSet tIntHashSet = VehiclesDB2.instance.main.usedIds;
            synchronized (tIntHashSet) {
                assert (VehiclesDB2.instance.main.usedIds.contains(vehicle.sqlId));
            }
            this.id = vehicle.sqlId;
            this.wx = vehicle.chunk.wx;
            this.wy = vehicle.chunk.wy;
            this.x = vehicle.getX();
            this.y = vehicle.getY();
            this.worldVersion = IsoWorld.getWorldVersion();
            ByteBuffer bb = TL_SliceBuffer.get();
            bb.clear();
            while (true) {
                try {
                    vehicle.save(bb);
                }
                catch (BufferOverflowException ex) {
                    if (bb.capacity() >= 0x200000) {
                        DebugLog.General.error("the vehicle %d cannot be saved", vehicle.sqlId);
                        throw ex;
                    }
                    bb = ByteBuffer.allocate(bb.capacity() + 32768);
                    TL_SliceBuffer.set(bb);
                    continue;
                }
                break;
            }
            bb.flip();
            this.setBytes(bb);
        }

        void setBytes(ByteBuffer bytes) {
            int count;
            bytes.rewind();
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.bb, true);
            bbos.clear();
            byte[] temp = TL_Bytes.get();
            for (int total = bytes.limit(); total > 0; total -= count) {
                count = Math.min(temp.length, total);
                bytes.get(temp, 0, count);
                bbos.write(temp, 0, count);
            }
            bbos.flip();
            this.bb = bbos.getWrappedBuffer();
        }

        void setBytes(byte[] bytes) {
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.bb, true);
            bbos.clear();
            bbos.write(bytes);
            bbos.flip();
            this.bb = bbos.getWrappedBuffer();
        }

        void setBytes(InputStream input) throws IOException {
            int count;
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.bb, true);
            bbos.clear();
            byte[] temp = TL_Bytes.get();
            while ((count = input.read(temp)) >= 1) {
                bbos.write(temp, 0, count);
            }
            bbos.flip();
            this.bb = bbos.getWrappedBuffer();
        }
    }
}

