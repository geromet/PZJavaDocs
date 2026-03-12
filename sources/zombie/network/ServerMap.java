/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import zombie.GameTime;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.VirtualZombieManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Roles;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.core.ImportantAreaManager;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.stash.StashSystem;
import zombie.core.utils.OnceEvery;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.entity.GameEntityManager;
import zombie.globalObjects.SGlobalObjects;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.MetaTracker;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.WorldGenerate;
import zombie.iso.worldgen.WorldGenParams;
import zombie.network.GameServer;
import zombie.network.IsoObjectID;
import zombie.network.PacketTypes;
import zombie.network.RCONServer;
import zombie.network.ServerChunkLoader;
import zombie.network.ServerGUI;
import zombie.network.ServerLOS;
import zombie.network.ServerOptions;
import zombie.network.ServerPlayersVehicles;
import zombie.network.ServerWorldDatabase;
import zombie.network.id.ObjectIDManager;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.ZombiePopulationManager;
import zombie.radio.ZomboidRadio;
import zombie.savefile.ServerPlayerDB;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclesDB2;
import zombie.world.moddata.GlobalModData;
import zombie.worldMap.network.WorldMapServer;

public class ServerMap {
    public boolean updateLosThisFrame;
    public static final OnceEvery LOS_TICK = new OnceEvery(1.0f);
    public static final OnceEvery TIME_TICK = new OnceEvery(600.0f);
    public static final int CellSize = 64;
    public static final int ChunksPerCellWidth = 8;
    public long lastSaved;
    private static boolean mapLoading;
    public final IsoObjectID<IsoZombie> zombieMap = new IsoObjectID<IsoZombie>(IsoZombie.class);
    public boolean queuedSaveAll;
    public boolean queuedQuit;
    public static ServerMap instance;
    public ServerCell[] cellMap;
    public ArrayList<ServerCell> loadedCells = new ArrayList();
    public ArrayList<ServerCell> releventNow = new ArrayList();
    int width;
    int height;
    IsoMetaGrid grid;
    ArrayList<ServerCell> toLoad = new ArrayList();
    static final DistToCellComparator distToCellComparator;
    private final ArrayList<ServerCell> tempCells = new ArrayList();
    long lastTick;

    public short getUniqueZombieId() {
        return this.zombieMap.allocateID();
    }

    public void SaveAll() {
        long start = System.nanoTime();
        for (int n = 0; n < this.loadedCells.size(); ++n) {
            this.loadedCells.get(n).Save();
        }
        this.grid.save();
        DebugLog.log("SaveAll took " + (double)(System.nanoTime() - start) / 1000000.0 + " ms");
    }

    public void QueueSaveAll() {
        this.queuedSaveAll = true;
    }

    public void QueueQuit() {
        this.queuedQuit = true;
    }

    public int toServerCellX(int x) {
        return PZMath.coorddivision(x * 256, 64);
    }

    public int toServerCellY(int y) {
        return PZMath.coorddivision(y * 256, 64);
    }

    public int toWorldCellX(int x) {
        return PZMath.coorddivision(x * 64, 256);
    }

    public int toWorldCellY(int y) {
        return PZMath.coorddivision(y * 64, 256);
    }

    public int getMaxX() {
        int x = this.toServerCellX(this.grid.maxX + 1);
        if ((this.grid.maxX + 1) * 256 % 64 == 0) {
            --x;
        }
        return x;
    }

    public int getMaxY() {
        int y = this.toServerCellY(this.grid.maxY + 1);
        if ((this.grid.maxY + 1) * 256 % 64 == 0) {
            --y;
        }
        return y;
    }

    public int getMinX() {
        return this.toServerCellX(this.grid.minX);
    }

    public int getMinY() {
        return this.toServerCellY(this.grid.minY);
    }

    public void init(IsoMetaGrid metaGrid) {
        this.grid = metaGrid;
        this.width = this.getMaxX() - this.getMinX() + 1;
        this.height = this.getMaxY() - this.getMinY() + 1;
        assert (this.width * 64 >= metaGrid.getWidth() * 256);
        assert (this.height * 64 >= metaGrid.getHeight() * 256);
        assert (this.getMaxX() * 64 < (metaGrid.getMaxX() + 1) * 256);
        assert (this.getMaxY() * 64 < (metaGrid.getMaxY() + 1) * 256);
        int tot = this.width * this.height;
        this.cellMap = new ServerCell[tot];
        StashSystem.init();
    }

    public ServerCell getCell(int x, int y) {
        if (this.isInvalidCell(x, y)) {
            return null;
        }
        return this.cellMap[y * this.width + x];
    }

    public boolean isInvalidCell(int x, int y) {
        return x < 0 || y < 0 || x >= this.width || y >= this.height;
    }

    public void loadOrKeepRelevent(int x, int y) {
        if (this.isInvalidCell(x, y)) {
            return;
        }
        ServerCell cell = this.getCell(x, y);
        if (cell == null) {
            cell = new ServerCell();
            cell.wx = x + this.getMinX();
            cell.wy = y + this.getMinY();
            if (cell.wx == -1 && cell.wy == -1) {
                return;
            }
            if (mapLoading) {
                DebugLog.MapLoading.debugln("Loading cell: " + cell.wx + ", " + cell.wy + " (" + this.toWorldCellX(cell.wx) + ", " + this.toWorldCellY(cell.wy) + ")");
            }
            this.cellMap[y * this.width + x] = cell;
            this.toLoad.add(cell);
            this.loadedCells.add(cell);
            this.releventNow.add(cell);
        } else if (!this.releventNow.contains(cell)) {
            this.releventNow.add(cell);
        }
    }

    public void characterIn(IsoPlayer p) {
        while (this.grid == null) {
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int dist = p.onlineChunkGridWidth / 2 * 8;
        int minX = PZMath.fastfloor((p.getX() - (float)dist) / 64.0f) - this.getMinX();
        int maxX = PZMath.fastfloor((p.getX() + (float)dist) / 64.0f) - this.getMinX();
        int minY = PZMath.fastfloor((p.getY() - (float)dist) / 64.0f) - this.getMinY();
        int maxY = PZMath.fastfloor((p.getY() + (float)dist) / 64.0f) - this.getMinY();
        for (int yy = minY; yy <= maxY; ++yy) {
            for (int xx = minX; xx <= maxX; ++xx) {
                this.loadOrKeepRelevent(xx, yy);
            }
        }
    }

    public void characterIn(int wx, int wy, int chunkGridWidth) {
        while (this.grid == null) {
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int x = wx * 8;
        int y = wy * 8;
        x = PZMath.coorddivision(x, 64);
        y = PZMath.coorddivision(y, 64);
        int cx = PZMath.fastfloor(x -= this.getMinX());
        int cy = PZMath.fastfloor(y -= this.getMinY());
        int lx = wx * 8 % 64;
        int ly = wy * 8 % 64;
        int dist = chunkGridWidth / 2 * 8;
        int minX = cx;
        int minY = cy;
        int maxX = cx;
        int maxY = cy;
        if (lx < dist) {
            --minX;
        }
        if (lx > 64 - dist) {
            ++maxX;
        }
        if (ly < dist) {
            --minY;
        }
        if (ly > 64 - dist) {
            ++maxY;
        }
        for (int yy = minY; yy <= maxY; ++yy) {
            for (int xx = minX; xx <= maxX; ++xx) {
                this.loadOrKeepRelevent(xx, yy);
            }
        }
    }

    public void importantAreaIn(int sx, int sy) {
        while (this.grid == null) {
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int x = PZMath.fastfloor(sx);
        int y = PZMath.fastfloor(sy);
        x = PZMath.coorddivision(x, 64);
        y = PZMath.coorddivision(y, 64);
        this.loadOrKeepRelevent(x -= this.getMinX(), y -= this.getMinY());
    }

    public void QueuedQuit() {
        this.QueuedSaveAll();
        ByteBufferWriter b = GameServer.udpEngine.startPacket();
        PacketTypes.PacketType.ServerQuit.doPacket(b);
        GameServer.udpEngine.endPacketBroadcast(PacketTypes.PacketType.ServerQuit);
        WorldGenerate.instance.stop();
        try {
            Thread.sleep(5000L);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        Roles.save();
        PathfindNative.instance.stop();
        PathfindNative.freeMemoryAtExit();
        MapCollisionData.instance.stop();
        AnimalPopulationManager.getInstance().stop();
        ZombiePopulationManager.instance.stop();
        RCONServer.shutdown();
        ServerCell.chunkLoader.quit();
        ServerWorldDatabase.instance.close();
        ServerPlayersVehicles.instance.stop();
        ServerPlayerDB.getInstance().close();
        ObjectIDManager.getInstance().checkForSaveDataFile(true);
        ImportantAreaManager.getInstance().saveDataFile();
        VehiclesDB2.instance.Reset();
        GameServer.udpEngine.Shutdown();
        ServerGUI.shutdown();
        SteamUtils.shutdown();
    }

    public void QueuedSaveAll() {
        long start = System.nanoTime();
        this.SaveAll();
        ServerPlayerDB.getInstance().save();
        ServerCell.chunkLoader.saveLater(GameTime.instance);
        ReanimatedPlayers.instance.saveReanimatedPlayers();
        AnimalPopulationManager.getInstance().save();
        MapCollisionData.instance.save();
        SGlobalObjects.save();
        WorldGenParams.INSTANCE.save();
        InstanceTracker.save();
        MetaTracker.save();
        try {
            ZomboidRadio.getInstance().Save();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            GlobalModData.instance.save();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        GameEntityManager.Save();
        WorldMapServer.instance.writeSavefile();
        INetworkPacket.sendToAll(PacketTypes.PacketType.StopPause, new Object[0]);
        System.out.println("Saving finish");
        DebugLog.log("Saving took " + (double)(System.nanoTime() - start) / 1000000.0 + " ms");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void preupdate() {
        long currentTime;
        int saveWorldEveryMinutes;
        int cy;
        int cx;
        ServerCell cell;
        int i;
        this.lastTick = System.nanoTime();
        mapLoading = DebugType.MapLoading.isEnabled();
        for (i = 0; i < this.toLoad.size(); ++i) {
            cell = this.toLoad.get(i);
            if (!cell.loadingWasCancelled) continue;
            if (mapLoading) {
                DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + cell.wx + "," + cell.wy);
            }
            cx = cell.wx - this.getMinX();
            cy = cell.wy - this.getMinY();
            assert (this.cellMap[cx + cy * this.width] == cell);
            this.cellMap[cx + cy * this.width] = null;
            this.loadedCells.remove(cell);
            this.releventNow.remove(cell);
            ServerCell.loaded2.remove(cell);
            this.toLoad.remove(i--);
        }
        for (i = 0; i < this.loadedCells.size(); ++i) {
            cell = this.loadedCells.get(i);
            if (!cell.cancelLoading) continue;
            if (mapLoading) {
                DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + cell.wx + "," + cell.wy);
            }
            cx = cell.wx - this.getMinX();
            cy = cell.wy - this.getMinY();
            assert (this.cellMap[cx + cy * this.width] == cell);
            this.cellMap[cx + cy * this.width] = null;
            this.loadedCells.remove(i--);
            this.releventNow.remove(cell);
            ServerCell.loaded2.remove(cell);
            this.toLoad.remove(cell);
        }
        for (i = 0; i < ServerCell.loaded2.size(); ++i) {
            cell = ServerCell.loaded2.get(i);
            if (!cell.cancelLoading) continue;
            if (mapLoading) {
                DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + cell.wx + "," + cell.wy);
            }
            cx = cell.wx - this.getMinX();
            cy = cell.wy - this.getMinY();
            assert (this.cellMap[cx + cy * this.width] == cell);
            this.cellMap[cx + cy * this.width] = null;
            this.loadedCells.remove(cell);
            this.releventNow.remove(cell);
            ServerCell.loaded2.remove(cell);
            this.toLoad.remove(cell);
        }
        if (!this.toLoad.isEmpty()) {
            this.tempCells.clear();
            for (i = 0; i < this.toLoad.size(); ++i) {
                cell = this.toLoad.get(i);
                if (cell.cancelLoading || cell.startedLoading) continue;
                this.tempCells.add(cell);
            }
            if (!this.tempCells.isEmpty()) {
                distToCellComparator.init();
                this.tempCells.sort(distToCellComparator);
                for (i = 0; i < this.tempCells.size(); ++i) {
                    cell = this.tempCells.get(i);
                    ServerCell.chunkLoader.addJob(cell);
                    cell.startedLoading = true;
                }
            }
            ServerCell.chunkLoader.getLoaded(ServerCell.loaded);
            for (i = 0; i < ServerCell.loaded.size(); ++i) {
                cell = ServerCell.loaded.get(i);
                if (cell.doingRecalc) continue;
                ServerCell.chunkLoader.addRecalcJob(cell);
                cell.doingRecalc = true;
            }
            ServerCell.loaded.clear();
            ServerCell.chunkLoader.getRecalc(ServerCell.loaded2);
            if (!ServerCell.loaded2.isEmpty()) {
                try {
                    ServerLOS.instance.suspend();
                    for (int x = 0; x < ServerCell.loaded2.size(); ++x) {
                        cell = ServerCell.loaded2.get(x);
                        if (!cell.Load2()) continue;
                        --x;
                        this.toLoad.remove(cell);
                    }
                }
                finally {
                    ServerLOS.instance.resume();
                }
            }
        }
        if ((saveWorldEveryMinutes = ServerOptions.instance.saveWorldEveryMinutes.getValue()) > 0 && (currentTime = System.currentTimeMillis()) > this.lastSaved + (long)saveWorldEveryMinutes * 60L * 1000L) {
            this.queuedSaveAll = true;
            this.lastSaved = currentTime;
        }
        if (this.queuedSaveAll) {
            this.queuedSaveAll = false;
            this.QueuedSaveAll();
        }
        if (this.queuedQuit) {
            System.exit(0);
        }
        this.releventNow.clear();
        this.updateLosThisFrame = LOS_TICK.Check();
        if (TIME_TICK.Check()) {
            ServerCell.chunkLoader.saveLater(GameTime.instance);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void postupdate() {
        boolean pathfindPaused = false;
        try {
            for (int n = 0; n < this.loadedCells.size(); ++n) {
                boolean shouldBeLoaded;
                ServerCell cell = this.loadedCells.get(n);
                boolean bl = shouldBeLoaded = this.releventNow.contains(cell) || !this.outsidePlayerInfluence(cell);
                if (!cell.isLoaded) {
                    if (shouldBeLoaded || cell.cancelLoading) continue;
                    if (mapLoading) {
                        DebugLog.log(DebugType.MapLoading, "MainThread: cancelling " + cell.wx + "," + cell.wy + " cell.startedLoading=" + cell.startedLoading);
                    }
                    if (!cell.startedLoading) {
                        cell.loadingWasCancelled = true;
                    }
                    cell.cancelLoading = true;
                    continue;
                }
                if (!shouldBeLoaded) {
                    int x = cell.wx - this.getMinX();
                    int y = cell.wy - this.getMinY();
                    if (!pathfindPaused) {
                        ServerLOS.instance.suspend();
                        pathfindPaused = true;
                    }
                    this.cellMap[y * this.width + x].Unload();
                    this.cellMap[y * this.width + x] = null;
                    this.loadedCells.remove(cell);
                    --n;
                    continue;
                }
                cell.update();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if (pathfindPaused) {
                ServerLOS.instance.resume();
            }
        }
        NetworkZombiePacker.getInstance().postupdate();
        ServerCell.chunkLoader.updateSaved();
    }

    public void physicsCheck(int x, int y) {
        int cy;
        int cx = PZMath.coorddivision(x, 64) - this.getMinX();
        ServerCell cell = this.getCell(cx, cy = PZMath.coorddivision(y, 64) - this.getMinY());
        if (cell != null && cell.isLoaded) {
            cell.physicsCheck = true;
        }
    }

    private boolean outsidePlayerInfluence(ServerCell cell) {
        int x1 = cell.wx * 64;
        int y1 = cell.wy * 64;
        int x2 = (cell.wx + 1) * 64;
        int y2 = (cell.wy + 1) * 64;
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.isRelevantTo(x1, y1)) {
                return false;
            }
            if (c.isRelevantTo(x2, y1)) {
                return false;
            }
            if (c.isRelevantTo(x2, y2)) {
                return false;
            }
            if (!c.isRelevantTo(x1, y2)) continue;
            return false;
        }
        return true;
    }

    public int worldSquareToServerCellXY(int worldSquareXY) {
        return PZMath.coorddivision(worldSquareXY, 64);
    }

    public int worldChunkToServerCellXY(int worldChunkXY) {
        return PZMath.coorddivision(worldChunkXY, 8);
    }

    public static IsoGridSquare getGridSquare(Vector3 v) {
        return instance.getGridSquare(PZMath.fastfloor(v.x), PZMath.fastfloor(v.y), PZMath.fastfloor(v.z));
    }

    public IsoGridSquare getGridSquare(int x, int y, int z) {
        if (!IsoWorld.instance.isValidSquare(x, y, z)) {
            return null;
        }
        int cx = this.worldSquareToServerCellXY(x);
        int cy = this.worldSquareToServerCellXY(y);
        int chx = (x - cx * 64) / 8;
        int chy = (y - cy * 64) / 8;
        int sqx = (x - cx * 64) % 8;
        int sqy = (y - cy * 64) % 8;
        ServerCell cell = this.getCell(cx -= this.getMinX(), cy -= this.getMinY());
        if (cell == null || !cell.isLoaded) {
            return null;
        }
        IsoChunk c = cell.chunks[chx][chy];
        if (c == null) {
            return null;
        }
        return c.getGridSquare(sqx, sqy, z);
    }

    public void setGridSquare(int x, int y, int z, IsoGridSquare sq) {
        int cx = this.worldSquareToServerCellXY(x);
        int cy = this.worldSquareToServerCellXY(y);
        int chx = (x - cx * 64) / 8;
        int chy = (y - cy * 64) / 8;
        int sqx = (x - cx * 64) % 8;
        int sqy = (y - cy * 64) % 8;
        ServerCell cell = this.getCell(cx -= this.getMinX(), cy -= this.getMinY());
        if (cell == null) {
            return;
        }
        IsoChunk c = cell.chunks[chx][chy];
        if (c == null) {
            return;
        }
        c.setSquare(sqx, sqy, z, sq);
    }

    public IsoChunk getChunk(int wx, int wy) {
        int cx = this.worldChunkToServerCellXY(wx);
        int cy = this.worldChunkToServerCellXY(wy);
        int chx = (wx - cx * 8) % 8;
        int chy = (wy - cy * 8) % 8;
        ServerCell cell = this.getCell(cx -= this.getMinX(), cy -= this.getMinY());
        if (cell == null || !cell.isLoaded) {
            return null;
        }
        return cell.chunks[chx][chy];
    }

    public void setSoftResetChunk(IsoChunk chunk) {
        int cy;
        int cx = this.worldChunkToServerCellXY(chunk.wx) - this.getMinX();
        if (this.isInvalidCell(cx, cy = this.worldChunkToServerCellXY(chunk.wy) - this.getMinY())) {
            return;
        }
        ServerCell cell = this.getCell(cx, cy);
        if (cell == null) {
            cell = new ServerCell();
            cell.isLoaded = true;
            this.cellMap[cy * this.width + cx] = cell;
        }
        int chx = (chunk.wx - cx * 8) % 8;
        int chy = (chunk.wy - cy * 8) % 8;
        cell.chunks[chx][chy] = chunk;
    }

    public void clearSoftResetChunk(IsoChunk chunk) {
        int cy;
        int cx = this.worldChunkToServerCellXY(chunk.wx) - this.getMinX();
        ServerCell cell = this.getCell(cx, cy = this.worldChunkToServerCellXY(chunk.wy) - this.getMinY());
        if (cell == null) {
            return;
        }
        int chx = (chunk.wx - cx * 8) % 8;
        int chy = (chunk.wy - cy * 8) % 8;
        cell.chunks[chx][chy] = null;
    }

    static {
        instance = new ServerMap();
        distToCellComparator = new DistToCellComparator();
    }

    public static final class ServerCell {
        public int wx;
        public int wy;
        public boolean isLoaded;
        public boolean physicsCheck;
        public final IsoChunk[][] chunks = new IsoChunk[8][8];
        private final HashSet<RoomDef> unexploredRooms = new HashSet();
        private static final ServerChunkLoader chunkLoader = new ServerChunkLoader();
        private static final ArrayList<ServerCell> loaded = new ArrayList();
        private boolean startedLoading;
        public boolean cancelLoading;
        public boolean loadingWasCancelled;
        private static final ArrayList<ServerCell> loaded2 = new ArrayList();
        private boolean doingRecalc;
        private final UpdateLimit hotSaveFrequency = new UpdateLimit(1000L);

        public boolean Load2() {
            chunkLoader.getRecalc(loaded2);
            for (int i = 0; i < loaded2.size(); ++i) {
                if (loaded2.get(i) != this) continue;
                long start = System.nanoTime();
                this.RecalcAll2();
                loaded2.remove(i);
                if (mapLoading) {
                    DebugLog.MapLoading.debugln("loaded2=" + String.valueOf(loaded2));
                }
                float time = (float)(System.nanoTime() - start) / 1000000.0f;
                if (mapLoading) {
                    DebugLog.MapLoading.debugln("finish loading cell " + this.wx + "," + this.wy + " ms=" + time);
                }
                this.loadVehicles();
                return true;
            }
            return false;
        }

        private void loadVehicles() {
            for (int cx = 0; cx < 8; ++cx) {
                for (int cy = 0; cy < 8; ++cy) {
                    IsoChunk chunk = this.chunks[cx][cy];
                    if (chunk == null || chunk.isNewChunk()) continue;
                    VehiclesDB2.instance.loadChunkMain(chunk);
                }
            }
        }

        public void RecalcAll2() {
            int y;
            IsoGridSquare sq;
            int x;
            int z;
            int sx = this.wx * 8 * 8;
            int sy = this.wy * 8 * 8;
            int ex = sx + 64;
            int ey = sy + 64;
            for (RoomDef def : this.unexploredRooms) {
                --def.indoorZombies;
            }
            this.unexploredRooms.clear();
            this.isLoaded = true;
            int minLevel = Integer.MAX_VALUE;
            int maxLevel = Integer.MIN_VALUE;
            for (int chunkY = 0; chunkY < 8; ++chunkY) {
                for (int chunkX = 0; chunkX < 8; ++chunkX) {
                    IsoChunk chunk = this.getChunk(chunkX, chunkY);
                    if (chunk == null) continue;
                    minLevel = PZMath.min(minLevel, chunk.getMinLevel());
                    maxLevel = PZMath.max(maxLevel, chunk.getMaxLevel());
                }
            }
            for (z = 1; z <= maxLevel; ++z) {
                for (x = -1; x < 65; ++x) {
                    sq = instance.getGridSquare(sx + x, sy - 1, z);
                    if (sq != null && !sq.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                    } else if (x >= 0 && x < 64 && (sq = instance.getGridSquare(sx + x, sy, z)) != null && !sq.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                    }
                    sq = instance.getGridSquare(sx + x, sy + 64, z);
                    if (sq != null && !sq.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                        continue;
                    }
                    if (x < 0 || x >= 64) continue;
                    instance.getGridSquare(sx + x, sy + 64 - 1, z);
                    if (sq == null || sq.getObjects().isEmpty()) continue;
                    IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                }
                for (y = 0; y < 64; ++y) {
                    sq = instance.getGridSquare(sx - 1, sy + y, z);
                    if (sq != null && !sq.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                    } else {
                        sq = instance.getGridSquare(sx, sy + y, z);
                        if (sq != null && !sq.getObjects().isEmpty()) {
                            IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                        }
                    }
                    sq = instance.getGridSquare(sx + 64, sy + y, z);
                    if (sq != null && !sq.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                        continue;
                    }
                    sq = instance.getGridSquare(sx + 64 - 1, sy + y, z);
                    if (sq == null || sq.getObjects().isEmpty()) continue;
                    IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                }
            }
            for (z = minLevel; z <= maxLevel; ++z) {
                for (x = 0; x < 64; ++x) {
                    sq = instance.getGridSquare(sx + x, sy, z);
                    if (sq != null) {
                        sq.RecalcAllWithNeighbours(true);
                    }
                    if ((sq = instance.getGridSquare(sx + x, ey - 1, z)) == null) continue;
                    sq.RecalcAllWithNeighbours(true);
                }
                for (y = 0; y < 64; ++y) {
                    sq = instance.getGridSquare(sx, sy + y, z);
                    if (sq != null) {
                        sq.RecalcAllWithNeighbours(true);
                    }
                    if ((sq = instance.getGridSquare(ex - 1, sy + y, z)) == null) continue;
                    sq.RecalcAllWithNeighbours(true);
                }
            }
            int nSquares = 64;
            for (int cx = 0; cx < 8; ++cx) {
                for (int cy = 0; cy < 8; ++cy) {
                    IsoChunk chunk = this.chunks[cx][cy];
                    if (chunk == null) continue;
                    chunk.loaded = true;
                    for (int i = 0; i < 64; ++i) {
                        for (int z2 = chunk.minLevel; z2 <= chunk.maxLevel; ++z2) {
                            int squaresIndexOfLevel = chunk.squaresIndexOfLevel(z2);
                            IsoGridSquare g = chunk.squares[squaresIndexOfLevel][i];
                            if (g == null) continue;
                            if (g.getRoom() != null && !g.getRoom().def.explored) {
                                this.unexploredRooms.add(g.getRoom().def);
                            }
                            g.propertiesDirty = true;
                        }
                    }
                }
            }
            for (x = 0; x < 8; ++x) {
                for (int y2 = 0; y2 < 8; ++y2) {
                    if (this.chunks[x][y2] == null) continue;
                    this.chunks[x][y2].doLoadGridsquare();
                }
            }
            for (RoomDef def : this.unexploredRooms) {
                ++def.indoorZombies;
                if (def.indoorZombies != 1) continue;
                try {
                    VirtualZombieManager.instance.tryAddIndoorZombies(def, false);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.isLoaded = true;
        }

        public void Unload() {
            if (!this.isLoaded) {
                return;
            }
            if (mapLoading) {
                DebugLog.MapLoading.debugln("Unloading cell: " + this.wx + ", " + this.wy + " (" + instance.toWorldCellX(this.wx) + ", " + instance.toWorldCellY(this.wy) + ")");
            }
            for (int x = 0; x < 8; ++x) {
                for (int y = 0; y < 8; ++y) {
                    IsoChunk chunk = this.chunks[x][y];
                    if (chunk == null) continue;
                    chunk.removeFromWorld();
                    chunk.loadVehiclesObject = null;
                    for (int i = 0; i < chunk.vehicles.size(); ++i) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        VehiclesDB2.instance.updateVehicle(vehicle);
                    }
                    chunkLoader.addSaveUnloadedJob(chunk);
                    this.chunks[x][y] = null;
                }
            }
            for (RoomDef def : this.unexploredRooms) {
                --def.indoorZombies;
            }
        }

        public void Save() {
            if (!this.isLoaded) {
                return;
            }
            for (int x = 0; x < 8; ++x) {
                for (int y = 0; y < 8; ++y) {
                    IsoChunk chunk = this.chunks[x][y];
                    if (chunk == null) continue;
                    try {
                        chunkLoader.addSaveLoadedJob(chunk);
                        for (int i = 0; i < chunk.vehicles.size(); ++i) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            VehiclesDB2.instance.updateVehicle(vehicle);
                        }
                        continue;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        LoggerManager.getLogger("map").write(e);
                    }
                }
            }
            chunkLoader.updateSaved();
        }

        public void saveChunk(IsoChunk chunk) {
            if (!this.isLoaded) {
                return;
            }
            if (chunk == null) {
                return;
            }
            chunkLoader.addSaveLoadedJob(chunk);
        }

        public void update() {
            boolean shouldProcessHotSaves = !GameServer.server && this.hotSaveFrequency.Check();
            for (int x = 0; x < 8; ++x) {
                for (int y = 0; y < 8; ++y) {
                    IsoChunk chunk = this.chunks[x][y];
                    if (chunk == null) continue;
                    chunk.update();
                    if (!shouldProcessHotSaves || !chunk.requiresHotSave) continue;
                    this.saveChunk(chunk);
                    chunk.requiresHotSave = false;
                }
            }
            this.physicsCheck = false;
        }

        public IsoChunk getChunk(int x, int y) {
            IsoChunk chunk;
            if (x >= 0 && x < 8 && y >= 0 && y < 8 && (chunk = this.chunks[x][y]) != null) {
                return chunk;
            }
            return null;
        }

        public int getWX() {
            return this.wx;
        }

        public int getWY() {
            return this.wy;
        }
    }

    private static final class DistToCellComparator
    implements Comparator<ServerCell> {
        private final Vector2[] pos = new Vector2[1024];
        private int posCount;

        public DistToCellComparator() {
            for (int i = 0; i < this.pos.length; ++i) {
                this.pos[i] = new Vector2();
            }
        }

        public void init() {
            this.posCount = 0;
            for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (!c.isFullyConnected()) continue;
                for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                    if (c.players[playerIndex] == null) continue;
                    this.pos[this.posCount].set(c.players[playerIndex].getX(), c.players[playerIndex].getY());
                    ++this.posCount;
                }
            }
        }

        @Override
        public int compare(ServerCell a, ServerCell b) {
            float aScore = Float.MAX_VALUE;
            float bScore = Float.MAX_VALUE;
            for (int i = 0; i < this.posCount; ++i) {
                float x = this.pos[i].x;
                float y = this.pos[i].y;
                aScore = Math.min(aScore, this.distToCell(x, y, a));
                bScore = Math.min(bScore, this.distToCell(x, y, b));
            }
            return Float.compare(aScore, bScore);
        }

        private float distToCell(float x, float y, ServerCell cell) {
            int minX = cell.wx * 64;
            int minY = cell.wy * 64;
            int maxX = minX + 64;
            int maxY = minY + 64;
            float closestX = x;
            float closestY = y;
            if (x < (float)minX) {
                closestX = minX;
            } else if (x > (float)maxX) {
                closestX = maxX;
            }
            if (y < (float)minY) {
                closestY = minY;
            } else if (y > (float)maxY) {
                closestY = maxY;
            }
            return IsoUtils.DistanceToSquared(x, y, closestX, closestY);
        }
    }
}

