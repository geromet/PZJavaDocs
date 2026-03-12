/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.zones;

import gnu.trove.list.array.TIntArrayList;
import java.awt.Rectangle;
import java.lang.invoke.LambdaMetafactory;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ChunkMapFilenames;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.characters.animals.AnimalManagerWorker;
import zombie.characters.animals.AnimalZone;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.ItemConfigurator;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.MapFiles;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.enums.MetaCellPresence;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.WorldGenReader;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.worldgen.utils.ChunkCoord;
import zombie.iso.worldgen.zones.AnimalsPathConfig;
import zombie.iso.zones.ZoneGeometryType;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class ZoneGenerator {
    private final BiomeMap map;
    private final List<AnimalsPathConfig> animalsPathConfig;
    private final ArrayList<TreeCount> treeCounts = new ArrayList();

    public ZoneGenerator(BiomeMap biomeMap) {
        this.map = biomeMap;
        WorldGenReader reader = new WorldGenReader();
        this.animalsPathConfig = reader.loadAnimalsPath((KahluaTable)LuaManager.env.rawget("animals_path_config"));
    }

    public void genForaging(int chunkX, int chunkY) {
        int metaCellY;
        int metaCellX;
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        if (metaGrid.hasCellData(metaCellX = PZMath.fastfloor((float)chunkX / 32.0f), metaCellY = PZMath.fastfloor((float)chunkY / 32.0f)) == MetaCellPresence.NOT_LOADED) {
            metaGrid.setCellData(metaCellX, metaCellY, new IsoMetaCell(metaCellX, metaCellY));
        }
        IsoMetaChunk metaChunk = metaGrid.getChunkData(chunkX, chunkY);
        if (metaGrid.hasCellData(metaCellX, metaCellY) == MetaCellPresence.LOADED && !metaChunk.doesHaveForaging()) {
            int cellSquareX = metaCellX * 256;
            int cellSquareY = metaCellY * 256;
            int minChunkX = PZMath.fastfloor((float)cellSquareX / 8.0f);
            int minChunkY = PZMath.fastfloor((float)cellSquareY / 8.0f);
            HashMap<Integer, Boolean[]> foraging = new HashMap<Integer, Boolean[]>();
            for (int x = 0; x < 32; ++x) {
                for (int i = 0; i < 32; ++i) {
                    Set requestedZones;
                    int coords = i * 32 + x;
                    try {
                        int[] samples = this.map.getZones(minChunkX + x, minChunkY + i, BiomeMap.Type.ZONE);
                        requestedZones = Arrays.stream(samples).distinct().boxed().collect(HashSet::new, HashSet::add, AbstractCollection::addAll);
                    }
                    catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                        requestedZones = Set.of(Integer.valueOf(255));
                    }
                    boolean hasForaging = metaGrid.getChunkData(x + minChunkX, i + minChunkY).doesHaveForaging();
                    for (Integer zones : requestedZones) {
                        foraging.computeIfAbsent(zones, (Function<Integer, Boolean[]>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$genForaging$0(java.lang.Integer ), (Ljava/lang/Integer;)[Ljava/lang/Boolean;)())[coords] = hasForaging;
                    }
                }
            }
            for (Integer n : foraging.keySet()) {
                ItemConfigurator.registerZone(this.map.getZoneName(n));
            }
            for (Map.Entry entry : foraging.entrySet()) {
                Integer key = (Integer)entry.getKey();
                if (this.map.getZoneName(key) == null) {
                    DebugLog.log("Zone " + key + " not found in ZONE_MAP");
                    continue;
                }
                Boolean[] hasForaging = (Boolean[])entry.getValue();
                int yoffset = 0;
                while (!Arrays.stream(hasForaging).allMatch(b -> b)) {
                    int x;
                    int minX = 0;
                    int maxX = 0;
                    boolean found = false;
                    for (x = 0; x < 32; ++x) {
                        if (hasForaging[yoffset * 32 + x].booleanValue()) continue;
                        minX = x;
                        found = true;
                        break;
                    }
                    if (!found) {
                        ++yoffset;
                        continue;
                    }
                    for (x = minX; x <= 32; ++x) {
                        if (x != 32 && !hasForaging[yoffset * 32 + x].booleanValue()) continue;
                        maxX = x - 1;
                        break;
                    }
                    int minY = yoffset;
                    int maxY = 32;
                    block10: for (int y = minY + 1; y < 32; ++y) {
                        for (int x2 = minX; x2 <= maxX; ++x2) {
                            if (!hasForaging[y * 32 + x2].booleanValue()) continue;
                            maxY = y;
                            break block10;
                        }
                    }
                    IsoWorld.instance.getMetaGrid().registerZone("", this.map.getZoneName(key), (minX + minChunkX) * 8, (minY + minChunkY) * 8, 0, (maxX + 1 - minX) * 8, (maxY - minY) * 8);
                    for (int x3 = 0; x3 < 32; ++x3) {
                        for (int y = 0; y < 32; ++y) {
                            int n = y * 32 + x3;
                            Boolean.valueOf(hasForaging[n] | metaGrid.getChunkData(x3 + minChunkX, y + minChunkY).doesHaveZone(this.map.getZoneName(key)));
                        }
                    }
                    yoffset = 0;
                }
            }
        }
    }

    public void genAnimalsPath(int chunkX, int chunkY) {
        int metaCellY;
        int metaCellX;
        if (SandboxOptions.getInstance().animalPathChance.getValue() == 1) {
            return;
        }
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        if (metaGrid.hasCellData(metaCellX = PZMath.fastfloor((float)chunkX / 32.0f), metaCellY = PZMath.fastfloor((float)chunkY / 32.0f)) == MetaCellPresence.NOT_LOADED) {
            metaGrid.setCellData(metaCellX, metaCellY, new IsoMetaCell(metaCellX, metaCellY));
        }
        IsoMetaCell metaCell = metaGrid.getCellData(metaCellX, metaCellY);
        if (metaGrid.hasCellData(metaCellX, metaCellY) != MetaCellPresence.LOADED) {
            return;
        }
        metaGrid.addCellToSave(metaCell);
        if (metaCell.getAnimalZonesSize() != 0) {
            return;
        }
        int cellSquareX = metaCellX * 256;
        int cellSquareY = metaCellY * 256;
        ArrayList<Rectangle> aabbs = new ArrayList<Rectangle>();
        for (int configIndex = 0; configIndex < this.animalsPathConfig.size(); ++configIndex) {
            int extensionMax;
            AnimalsPathConfig config = this.animalsPathConfig.get(configIndex);
            Random rnd = WorldGenParams.INSTANCE.getRandom(metaCellX, metaCellY, config.getNameHash() + configIndex);
            boolean lineWidth = false;
            String name = "";
            String type = "Animal";
            String action = "Follow";
            String animalType = config.animalType();
            int pointsMin = config.points()[0];
            int pointsMax = config.points().length > 1 ? config.points()[1] : config.points()[0];
            int radiusMin = config.radius()[0];
            int radiusMax = config.radius().length > 1 ? config.radius()[1] : config.radius()[0];
            int extensionMin = config.extension()[0];
            int n = extensionMax = config.extension().length > 1 ? config.extension()[1] : config.extension()[0];
            if (pointsMin < 0 || pointsMin == 0 && pointsMax == 0 || radiusMin < 0 || radiusMin == 0 && radiusMax == 0 || extensionMin < 0 || extensionMin == 0 && extensionMax == 0) continue;
            float chance = switch (SandboxOptions.getInstance().animalPathChance.getValue()) {
                case 2 -> 0.01f;
                case 3 -> 0.05f;
                case 4 -> 0.1f;
                case 5 -> 0.2f;
                default -> 0.65f;
            };
            block7: for (int c = 0; c < config.count(); ++c) {
                int i;
                if (rnd.nextFloat() > chance) continue;
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                int centerX = radiusMax + rnd.nextInt(Math.max(256 - 2 * radiusMax, 1));
                int centerY = radiusMax + rnd.nextInt(Math.max(256 - 2 * radiusMax, 1));
                int pointsCount = rnd.nextInt(pointsMax - pointsMin + 1) + pointsMin;
                int rotation = rnd.nextInt(360);
                ArrayList<AnimalZone> zoneExts = new ArrayList<AnimalZone>();
                TIntArrayList points = new TIntArrayList();
                int extensionCount = 0;
                for (i = 0; i < pointsCount; ++i) {
                    int radius = rnd.nextInt(radiusMax - radiusMin + 1) + radiusMin;
                    double angle = Math.toRadians(360.0 / (double)pointsCount * (double)i + (double)rotation);
                    int x = (int)Math.min(Math.max((double)radius * Math.cos(angle) + (double)centerX + (double)cellSquareX, (double)(cellSquareX + 1)), (double)(cellSquareX + 256 - 2));
                    int y = (int)Math.min(Math.max((double)radius * Math.sin(angle) + (double)centerY + (double)cellSquareY, (double)(cellSquareY + 1)), (double)(cellSquareY + 256 - 2));
                    points.add(x);
                    points.add(y);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    if (!(rnd.nextFloat() < config.extensionChance() || i == pointsCount - 2 && extensionCount == 0) && (i != pointsCount - 1 || extensionCount != 1)) continue;
                    AnimalZone zoneExt = this.getExtensionZone("", "Animal", extensionCount % 2 == 0 ? "Eat" : "Sleep", 0, x, y, rnd, extensionMin, extensionMax);
                    zoneExts.add(zoneExt);
                    ++extensionCount;
                }
                for (i = 0; i < points.size(); i += 2) {
                    int x = points.get(i);
                    int y = points.get(i + 1);
                    for (Rectangle aabb : aabbs) {
                        if (!aabb.contains(x, y)) continue;
                        continue block7;
                    }
                    IsoMetaChunk metaChunk = metaCell.getChunk((x - cellSquareX) / 8, (y - cellSquareY) / 8);
                    if (metaChunk.doesHaveZone("TrailerPark") || metaChunk.doesHaveZone("TownZone") || metaChunk.doesHaveZone("Vegitation") || metaChunk.doesHaveZone("Water") || !metaChunk.doesHaveForaging()) continue block7;
                }
                aabbs.add(new Rectangle(minX, minY, maxX - minX, maxY - minY));
                AnimalZone zone = new AnimalZone("", "Animal", minX, minY, 0, maxX - minX + 1, maxY - minY + 1, "Follow", animalType, true);
                zone.geometryType = ZoneGeometryType.Polyline;
                zone.points.addAll(points);
                zone.polylineWidth = 0;
                AnimalZone zoneLoop = this.getLoopingZone("", "Animal", "Follow", animalType, 0, points);
                metaGrid.registerAnimalZone(zone);
                metaGrid.registerAnimalZone(zoneLoop);
                zoneExts.forEach(metaGrid::registerAnimalZone);
            }
        }
        if (metaCell.getAnimalZonesSize() != 0) {
            AnimalManagerWorker.getInstance().allocCell(metaCellX, metaCellY);
        }
        this.resetTreeCounts();
    }

    private AnimalZone getLoopingZone(String name, String type, String action, String animalType, int lineWidth, TIntArrayList points) {
        TIntArrayList pointsLoop = new TIntArrayList();
        pointsLoop.add(points.get(points.size() - 2));
        pointsLoop.add(points.get(points.size() - 1));
        pointsLoop.add(points.get(0));
        pointsLoop.add(points.get(1));
        AnimalZone zoneLoop = new AnimalZone(name, type, pointsLoop.get(0), pointsLoop.get(1), 0, pointsLoop.get(2) - pointsLoop.get(0) + 1, pointsLoop.get(3) - pointsLoop.get(1) + 1, action, animalType, false);
        zoneLoop.geometryType = ZoneGeometryType.Polyline;
        zoneLoop.points.addAll(pointsLoop);
        zoneLoop.polylineWidth = lineWidth;
        return zoneLoop;
    }

    private AnimalZone getExtensionZone(String name, String type, String action, int lineWidth, int x, int y, Random rnd, int extensionMin, int extensionMax) {
        TIntArrayList pointsExt = new TIntArrayList();
        pointsExt.add(x);
        pointsExt.add(y);
        if (GameServer.server) {
            int rotExt = rnd.nextInt(360);
            int radiusExt = rnd.nextInt(extensionMax - extensionMin + 1) + extensionMin;
            pointsExt.add((int)((double)radiusExt * Math.cos(rotExt)) + x);
            pointsExt.add((int)((double)radiusExt * Math.sin(rotExt)) + y);
        } else if (!GameClient.client) {
            this.applyRandomLookup(pointsExt, x, y, extensionMin, extensionMax, rnd);
        }
        AnimalZone zoneExt = new AnimalZone(name, type, pointsExt.get(0), pointsExt.get(1), 0, pointsExt.get(2) - pointsExt.get(0) + 1, pointsExt.get(3) - pointsExt.get(1) + 1, action, null, false);
        zoneExt.geometryType = ZoneGeometryType.Polyline;
        zoneExt.points.addAll(pointsExt);
        zoneExt.polylineWidth = lineWidth;
        return zoneExt;
    }

    private void applyRandomLookup(TIntArrayList pointsExt, int x, int y, int extensionMin, int extensionMax, Random rnd) {
        int treeLimit = 3;
        int chunkX = x / 8;
        int chunkY = y / 8;
        int r = (int)Math.ceil((float)extensionMax / 8.0f);
        int r2 = r * r;
        int chunkXMin = chunkX - r;
        int chunkXMax = chunkX + r;
        int chunkYMin = chunkY - r;
        int chunkYMax = chunkY + r;
        ArrayList<ChunkCoord> validChunks = new ArrayList<ChunkCoord>();
        for (int xx = chunkXMin; xx <= chunkXMax; ++xx) {
            for (int yy = chunkYMin; yy <= chunkYMax; ++yy) {
                int dx = xx - chunkX;
                int dy = yy - chunkY;
                int d2 = dx * dx + dy * dy;
                if (d2 - r2 > 0 || this.getTrees(xx, yy) > 3) continue;
                validChunks.add(new ChunkCoord(xx, yy));
            }
        }
        if (!validChunks.isEmpty()) {
            ChunkCoord validChunk = (ChunkCoord)validChunks.get(rnd.nextInt(validChunks.size()));
            pointsExt.add(validChunk.x() * 8 + 4);
            pointsExt.add(validChunk.y() * 8 + 4);
        } else {
            int rotExt = rnd.nextInt(360);
            int radiusExt = rnd.nextInt(extensionMax - extensionMin + 1) + extensionMin;
            pointsExt.add((int)((double)radiusExt * Math.cos(rotExt)) + x);
            pointsExt.add((int)((double)radiusExt * Math.sin(rotExt)) + y);
        }
    }

    private TreeCount getTreeCount(int cellX, int cellY) {
        for (int i = 0; i < this.treeCounts.size(); ++i) {
            TreeCount treeCount = this.treeCounts.get(i);
            if (treeCount.cellX != cellX || treeCount.cellY != cellY) continue;
            return treeCount;
        }
        return null;
    }

    private void resetTreeCounts() {
        this.treeCounts.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private int getTrees(int chunkX, int chunkY) {
        boolean[] bTreeSquares;
        TreeCount treeCount;
        int cellY;
        int cellX;
        block13: {
            cellX = PZMath.fastfloor((float)chunkX / 32.0f);
            treeCount = this.getTreeCount(cellX, cellY = PZMath.fastfloor((float)chunkY / 32.0f));
            if (treeCount != null && treeCount.hasCount(chunkX, chunkY)) {
                return treeCount.getCount(chunkX, chunkY);
            }
            String key = ChunkMapFilenames.instance.getHeader(cellX, cellY);
            LotHeader lotHeader = IsoLot.InfoHeaders.get(key);
            if (lotHeader == null) {
                return 0;
            }
            bTreeSquares = new boolean[64];
            IsoLot lot = IsoLot.get(lotHeader.mapFiles, cellX, cellY, chunkX, chunkY, null);
            try {
                boolean[] bDoneSquares = new boolean[64];
                int nonEmptySquareCount = this.PlaceLot(lot, lot.minLevel, bTreeSquares, bDoneSquares);
                if (nonEmptySquareCount >= 64) break block13;
                for (int i = lot.info.mapFiles.priority + 1; i < IsoLot.MapFiles.size(); ++i) {
                    IsoLot lot2;
                    block14: {
                        MapFiles mapFiles = IsoLot.MapFiles.get(i);
                        if (!mapFiles.hasCell(cellX, cellY)) continue;
                        lot2 = null;
                        try {
                            lot2 = IsoLot.get(mapFiles, cellX, cellY, chunkX, chunkY, null);
                            nonEmptySquareCount = this.PlaceLot(lot2, lot2.minLevel, bTreeSquares, bDoneSquares);
                            if (nonEmptySquareCount != 64) break block14;
                            if (lot2 == null) break block13;
                        }
                        catch (Throwable throwable) {
                            if (lot2 != null) {
                                IsoLot.put(lot2);
                            }
                            throw throwable;
                        }
                        IsoLot.put(lot2);
                        break;
                    }
                    if (lot2 == null) continue;
                    IsoLot.put(lot2);
                }
            }
            finally {
                if (lot != null) {
                    IsoLot.put(lot);
                }
            }
        }
        int trees = 0;
        for (int xx = 0; xx < 8; ++xx) {
            for (int yy = 0; yy < 8; ++yy) {
                if (!bTreeSquares[xx + yy * 8]) continue;
                ++trees;
            }
        }
        if (treeCount == null) {
            treeCount = new TreeCount();
            treeCount.cellX = cellX;
            treeCount.cellY = cellY;
            this.treeCounts.add(treeCount);
        }
        treeCount.setCount(chunkX, chunkY, trees);
        return trees;
    }

    private int PlaceLot(IsoLot lot, int sz, boolean[] bTreeSquares, boolean[] bDoneSquares) {
        boolean z = false;
        int minLevel = Math.max(sz, -32);
        int maxLevel = Math.min(sz + lot.maxLevel - lot.minLevel - 1, 31);
        if (0 < minLevel || 0 > maxLevel) {
            return 0;
        }
        int nonEmptySquareCount = 0;
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                int numInts;
                if (bDoneSquares[x + y * 8]) {
                    ++nonEmptySquareCount;
                    continue;
                }
                int lz = 0 - lot.info.minLevel;
                int squareXYZ = x + y * 8 + lz * 8 * 8;
                int offsetInData = lot.offsetInData[squareXYZ];
                if (offsetInData == -1 || (numInts = lot.data.getQuick(offsetInData)) <= 0) continue;
                if (!bDoneSquares[x + y * 8]) {
                    bDoneSquares[x + y * 8] = true;
                    ++nonEmptySquareCount;
                }
                for (int n = 0; n < numInts; ++n) {
                    String tile = lot.info.tilesUsed.get(lot.data.get(offsetInData + 1 + n));
                    IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
                    if (spr == null || spr.getTileType() != IsoObjectType.tree) continue;
                    bTreeSquares[x + y * 8] = true;
                }
            }
        }
        return nonEmptySquareCount;
    }

    private static /* synthetic */ Boolean[] lambda$genForaging$0(Integer k) {
        Object[] booleans = new Boolean[1024];
        Arrays.fill(booleans, (Object)true);
        return booleans;
    }

    private static final class TreeCount {
        int cellX;
        int cellY;
        final byte[] countPerChunk = new byte[1024];

        TreeCount() {
            Arrays.fill(this.countPerChunk, (byte)-1);
        }

        boolean hasCount(int chunkX, int chunkY) {
            return this.getCount(chunkX, chunkY) >= 0;
        }

        int getCount(int chunkX, int chunkY) {
            int index = chunkX - this.cellX * 32 + (chunkY - this.cellY * 32) * 32;
            return this.countPerChunk[index];
        }

        void setCount(int chunkX, int chunkY, int count) {
            int index = chunkX - this.cellX * 32 + (chunkY - this.cellY * 32) * 32;
            this.countPerChunk[index] = (byte)PZMath.clamp(count, 0, 127);
        }
    }
}

