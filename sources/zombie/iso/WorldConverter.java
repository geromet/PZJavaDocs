/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionRegions;
import zombie.erosion.season.ErosionIceQueen;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.IngameState;
import zombie.globalObjects.GlobalObjectLookup;
import zombie.iso.ChunkSaveWorker;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.WorldStreamer;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.CoopSlave;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.scripting.ScriptManager;
import zombie.vehicles.VehicleManager;
import zombie.world.WorldDictionary;
import zombie.world.WorldDictionaryException;

public final class WorldConverter {
    public static final WorldConverter instance = new WorldConverter();
    public static final int MIN_VERSION = 1;
    public static int convertingVersion;
    public static boolean converting;
    public HashMap<Integer, Integer> tilesetConversions;
    int oldId;

    public void convert(String worldName, IsoSpriteManager manager) throws IOException {
        File inFile = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + worldName + File.separator + "map_ver.bin");
        if (inFile.exists()) {
            converting = true;
            FileInputStream inStream = new FileInputStream(inFile);
            DataInputStream input = new DataInputStream(inStream);
            convertingVersion = input.readInt();
            input.close();
            if (convertingVersion < 244) {
                if (convertingVersion < 1) {
                    GameLoadingState.worldVersionError = true;
                    return;
                }
                DebugLog.General.println("WorldConverter.convert() start");
                try {
                    this.convert(worldName, convertingVersion, 244);
                }
                catch (Exception ex) {
                    IngameState.createWorld(worldName);
                    IngameState.copyWorld(worldName + "_backup", worldName);
                    ExceptionLogger.logException(ex);
                }
                DebugLog.General.println("WorldConverter.convert() end");
            }
            converting = false;
        }
    }

    private void convert(String worldName, int from, int to) {
        if (GameClient.client) {
            return;
        }
        GameLoadingState.convertingWorld = true;
        String old = Core.gameSaveWorld;
        IngameState.createWorld(worldName + "_backup");
        IngameState.copyWorld(worldName, Core.gameSaveWorld);
        Core.gameSaveWorld = old;
        if (to >= 14 && from < 14) {
            try {
                this.convertchunks(worldName, 25, 25);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } else if (from == 7) {
            try {
                this.convertchunks(worldName);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (from <= 4) {
            this.loadconversionmap(from, "tiledefinitions");
            this.loadconversionmap(from, "newtiledefinitions");
            try {
                this.convertchunks(worldName);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        GameLoadingState.convertingWorld = false;
    }

    private void convertchunks(String worldName) throws IOException {
        String[] internalNames;
        IsoCell cell = new IsoCell(256, 256);
        IsoChunkMap map = new IsoChunkMap(cell);
        File fo = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + worldName + File.separator);
        if (!fo.exists()) {
            fo.mkdir();
        }
        for (String name : internalNames = fo.list()) {
            if (!name.contains(".bin") || name.equals("map.bin") || name.equals("map_p.bin") || name.matches("p[0-9]+\\.bin") || name.equals("map_t.bin") || name.equals("map_c.bin") || name.equals("map_ver.bin") || name.equals("map_sand.bin") || name.equals("map_mov.bin") || name.equals("map_meta.bin") || name.equals("map_cm.bin") || name.equals("pc.bin") || name.startsWith("zpop_") || name.startsWith("chunkdata_")) continue;
            String[] split = name.replace(".bin", "").replace("map_", "").split("_");
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            map.LoadChunkForLater(x, y, 0, 0);
            map.SwapChunkBuffers();
            map.getChunk(0, 0).Save(true);
        }
    }

    private void convertchunks(String worldName, int offx, int offy) throws IOException {
        IsoCell cell = new IsoCell(256, 256);
        IsoChunkMap map = new IsoChunkMap(cell);
        File fo = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + worldName + File.separator);
        if (!fo.exists()) {
            fo.mkdir();
        }
        String[] internalNames = fo.list();
        IsoWorld.saveoffsetx = offx;
        IsoWorld.saveoffsety = offy;
        IsoWorld.instance.metaGrid.Create();
        WorldStreamer.instance.create();
        for (String name : internalNames) {
            if (!name.contains(".bin") || name.equals("map.bin") || name.equals("map_p.bin") || name.matches("map_p[0-9]+\\.bin") || name.equals("map_t.bin") || name.equals("map_c.bin") || name.equals("map_ver.bin") || name.equals("map_sand.bin") || name.equals("map_mov.bin") || name.equals("map_meta.bin") || name.equals("map_cm.bin") || name.equals("pc.bin") || name.startsWith("zpop_") || name.startsWith("chunkdata_")) continue;
            String[] split = name.replace(".bin", "").replace("map_", "").split("_");
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            IsoChunk chunk = new IsoChunk(cell);
            chunk.refs.add(cell.chunkMap[0]);
            WorldStreamer.instance.addJobConvert(chunk, 0, 0, x, y);
            while (!chunk.loaded) {
                try {
                    Thread.sleep(20L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            chunk.wx += offx * 32;
            chunk.wy += offy * 32;
            chunk.jobType = IsoChunk.JobType.Convert;
            chunk.Save(true);
            File file = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + worldName + File.separator + name);
            while (!ChunkSaveWorker.instance.toSaveQueue.isEmpty()) {
                try {
                    Thread.sleep(13L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
    }

    private void loadconversionmap(int from, String filename) {
        String newDefinitionTile = "media/" + filename + "_" + from + ".tiles";
        File fo = new File(newDefinitionTile);
        if (fo.exists()) {
            try {
                RandomAccessFile in = new RandomAccessFile(fo.getAbsolutePath(), "r");
                int numTilesheets = IsoWorld.readInt(in);
                for (int n = 0; n < numTilesheets; ++n) {
                    Thread.sleep(4L);
                    String str = IsoWorld.readString(in);
                    String name = str.trim();
                    IsoWorld.readString(in);
                    int wTiles = IsoWorld.readInt(in);
                    int hTiles = IsoWorld.readInt(in);
                    int nTiles = IsoWorld.readInt(in);
                    for (int m = 0; m < nTiles; ++m) {
                        IsoSprite spr = IsoSpriteManager.instance.namedMap.get(name + "_" + m);
                        if (this.tilesetConversions == null) {
                            this.tilesetConversions = new HashMap();
                        }
                        this.tilesetConversions.put(this.oldId, spr.id);
                        ++this.oldId;
                        int nProps = IsoWorld.readInt(in);
                        for (int l = 0; l < nProps; ++l) {
                            str = IsoWorld.readString(in);
                            String prop = str.trim();
                            str = IsoWorld.readString(in);
                            String string = str.trim();
                        }
                    }
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void softreset() throws IOException, WorldDictionaryException {
        String worldName;
        Core.gameSaveWorld = worldName = LuaManager.GlobalObject.sanitizeWorldName(GameServer.serverName);
        IsoCell cell = new IsoCell(256, 256);
        IsoChunk chunk = new IsoChunk(cell);
        chunk.assignLoadID();
        File fo = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + worldName + File.separator);
        if (!fo.exists()) {
            fo.mkdir();
        }
        ArrayList<Path> filePaths = this.gatherFiles();
        if (CoopSlave.instance != null) {
            CoopSlave.instance.sendMessage("softreset-count", null, Integer.toString(filePaths.size()));
        }
        IsoWorld.instance.metaGrid.Create();
        ServerMap.instance.init(IsoWorld.instance.metaGrid);
        new ErosionIceQueen(IsoSpriteManager.instance);
        ErosionRegions.init();
        WorldStreamer.instance.create();
        VehicleManager.instance = new VehicleManager();
        WorldDictionary.init();
        ScriptManager.instance.PostWorldDictionaryInit();
        GlobalObjectLookup.init(IsoWorld.instance.getMetaGrid());
        LuaEventManager.triggerEvent("OnSGlobalObjectSystemInit");
        int remaining = filePaths.size();
        DebugLog.log("processing " + remaining + " files");
        for (Path filePath : filePaths) {
            --remaining;
            String pathStr = filePath.toString();
            if (pathStr.contains("blam")) continue;
            String name = filePath.getFileName().toString();
            if (name.startsWith("zpop_")) {
                WorldConverter.deleteFile(filePath);
                continue;
            }
            if (name.equals("map_t.bin")) {
                WorldConverter.deleteFile(filePath);
                continue;
            }
            if (name.equals("map_meta.bin") || name.equals("map_zone.bin") || name.equals("map_animals.bin") || name.equals("map_basements.bin")) {
                WorldConverter.deleteFile(filePath);
                continue;
            }
            if (name.equals("reanimated.bin")) {
                WorldConverter.deleteFile(filePath);
                continue;
            }
            if (!name.matches("map_[0-9]+_[0-9]+\\.bin")) continue;
            System.out.println("Soft clearing chunk: " + name);
            String[] split = name.replace(".bin", "").replace("map_", "").split("_");
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            chunk.refs.add(cell.chunkMap[0]);
            chunk.wx = x;
            chunk.wy = y;
            ServerMap.instance.setSoftResetChunk(chunk);
            WorldStreamer.instance.addJobWipe(chunk, 0, 0, x, y);
            while (!chunk.loaded) {
                try {
                    Thread.sleep(20L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            chunk.jobType = IsoChunk.JobType.Convert;
            chunk.floorBloodSplats.clear();
            try {
                chunk.Save(true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            ServerMap.instance.clearSoftResetChunk(chunk);
            int to = 64;
            for (int n = chunk.minLevel; n <= chunk.maxLevel; ++n) {
                for (int m = 0; m < 64; ++m) {
                    IsoGridSquare sq = chunk.squares[chunk.squaresIndexOfLevel(n)][m];
                    if (sq == null) continue;
                    for (int i = 0; i < sq.getObjects().size(); ++i) {
                        IsoObject obj = sq.getObjects().get(i);
                        obj.removeFromWorld(false);
                    }
                }
            }
            chunk.doReuseGridsquares();
            IsoChunkMap.chunkStore.remove(chunk);
            if (remaining % 100 == 0) {
                DebugLog.log(remaining + " files to go");
            }
            if (CoopSlave.instance == null || remaining % 10 != 0) continue;
            CoopSlave.instance.sendMessage("softreset-remaining", null, Integer.toString(remaining));
        }
        GameServer.resetId = Rand.Next(10000000);
        ServerOptions.instance.putSaveOption("ResetID", String.valueOf(GameServer.resetId));
        IsoWorld.instance.currentCell = null;
        DebugLog.log("soft-reset complete, server terminated");
        if (CoopSlave.instance != null) {
            CoopSlave.instance.sendMessage("softreset-finished", null, "");
        }
        SteamUtils.shutdown();
        System.exit(0);
    }

    private ArrayList<Path> gatherFiles() throws IOException {
        final ArrayList<Path> result = new ArrayList<Path>();
        Path path = Paths.get(ZomboidFileSystem.instance.getCurrentSaveDir(), new String[0]);
        Files.walkFileTree(path, (FileVisitor<? super Path>)new FileVisitor<Path>(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                result.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                ExceptionLogger.logException(exc);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    private static void deleteFile(Path filePath) throws IOException {
        Files.delete(filePath);
    }
}

