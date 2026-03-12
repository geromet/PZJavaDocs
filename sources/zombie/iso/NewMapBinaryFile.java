/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.CellLoader;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.MetaObject;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.BufferedRandomAccessFile;
import zombie.util.SharedStrings;

public class NewMapBinaryFile {
    public final boolean pot;
    public final int chunkDim;
    public final int chunksPerCell;
    public final int cellDim;
    private final ArrayList<RoomDef> tempRooms = new ArrayList();
    private final SharedStrings sharedStrings = new SharedStrings();

    public NewMapBinaryFile(boolean pot) {
        this.pot = pot;
        this.chunkDim = pot ? 8 : 10;
        this.chunksPerCell = pot ? 32 : 30;
        this.cellDim = pot ? 256 : 300;
    }

    public static void SpawnBasement(String name, int x, int y) throws IOException {
        NewMapBinaryFile file = new NewMapBinaryFile(true);
        String fileName = "media/binmap/" + name + ".pzby";
        Header header = file.loadHeader(fileName);
        IsoPlayer player = IsoPlayer.getInstance();
        player.ensureOnTile();
        IsoChunk chunk = player.getSquare().chunk;
        int cellX = chunk.wx * 8 / 256;
        int cellY = chunk.wy * 8 / 256;
        IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY);
        if (metaCell == null) {
            return;
        }
        int minChunkWorldX = x / 8;
        int minChunkWorldY = y / 8;
        int maxChunkWorldX = x / 8 + header.width - 1;
        int maxChunkWorldY = y / 8 + header.height - 1;
        if (minChunkWorldX / 32 != maxChunkWorldX / 32) {
            return;
        }
        if (minChunkWorldY / 32 != maxChunkWorldY / 32) {
            return;
        }
        file.addBasementRoomsToMetaGrid(metaCell, header, x, y);
        for (int ly = 0; ly < header.height; ++ly) {
            for (int lx = 0; lx < header.width; ++lx) {
                ChunkData chunkData = file.loadChunk(header, lx, ly);
                file.setChunkInWorldArb(chunkData, 0, 0, 0, x + lx * file.chunkDim, y + ly * file.chunkDim, -header.levels);
            }
        }
        for (int cx = minChunkWorldX; cx <= maxChunkWorldX; ++cx) {
            for (int cy = minChunkWorldY; cy <= maxChunkWorldY; ++cy) {
                IsoChunk cc = IsoCell.getInstance().getChunk(cx, cy);
                if (cc == null) continue;
                for (IsoRoomLight roomLight : cc.roomLights) {
                    if (IsoCell.getInstance().roomLights.contains(roomLight)) continue;
                    IsoCell.getInstance().roomLights.add(roomLight);
                }
            }
        }
        for (int xx = x; xx < x + header.width * file.chunkDim; ++xx) {
            block6: for (int yy = y; yy < y + header.height * file.chunkDim; ++yy) {
                IsoGridSquare sq = IsoCell.getInstance().getGridSquare(xx, yy, 0);
                if (sq == null || !sq.HasStairsBelow()) continue;
                IsoObject[] el = sq.getObjects().getElements();
                for (int i = 0; i < sq.getObjects().size(); ++i) {
                    IsoObject isoObject = el[i];
                    if (!isoObject.isFloor()) continue;
                    sq.DeleteTileObject(isoObject);
                    continue block6;
                }
            }
        }
        IsoCell.getInstance().chunkMap[IsoPlayer.getPlayerIndex()].calculateZExtentsForChunkMap();
    }

    public static void SpawnBasementInChunk(IsoChunk chunk, String name, int x, int y, int bottomZ) throws IOException {
        DebugType.Basement.println("SpawnBasementInChunk : " + name + ", at: " + x + ", " + y);
        boolean isBasementAccess = name.startsWith("ba_");
        NewMapBinaryFile file = new NewMapBinaryFile(true);
        String fileName = "media/binmap/" + name + ".pzby";
        if (isBasementAccess) {
            fileName = "media/basement_access/" + name + ".pzby";
        }
        Header header = file.loadHeader(fileName);
        int chunksPerWidth = 8;
        int cx = chunk.wx * 8;
        int cy = chunk.wy * 8;
        int bx1 = Math.max(cx, x) - x;
        int by1 = Math.max(cy, y) - y;
        int bx2 = Math.min(cx + 8, x + header.width * file.chunkDim) - 1 - x;
        int by2 = Math.min(cy + 8, y + header.height * file.chunkDim) - 1 - y;
        for (int ly = by1 / file.chunkDim; ly <= by2 / file.chunkDim; ++ly) {
            for (int lx = bx1 / file.chunkDim; lx <= bx2 / file.chunkDim; ++lx) {
                ChunkData chunkData = file.loadChunk(header, lx, ly);
                file.addToIsoChunk(chunk, chunkData, x + lx * file.chunkDim, y + ly * file.chunkDim, bottomZ, isBasementAccess);
            }
        }
    }

    private boolean addBasementRoomsToMetaGrid(IsoMetaCell metaCell, Header header, int x, int y) {
        ArrayList<RoomDef> roomDefs = new ArrayList<RoomDef>();
        metaCell.getRoomsIntersecting(x, y, header.width * header.file.chunkDim, header.height * header.file.chunkDim, roomDefs);
        int cellX = x / 256;
        int cellY = y / 256;
        if (roomDefs.isEmpty()) {
            return false;
        }
        BuildingDef building = roomDefs.get((int)0).building;
        int offX = x - building.x;
        int offY = y - building.y;
        ArrayList<RoomDef> roomDefList = header.roomDefList;
        for (int i = 0; i < roomDefList.size(); ++i) {
            RoomDef roomDef = roomDefList.get(i);
            RoomDef newRoom = new RoomDef(RoomID.makeID(metaCell.info.cellX, metaCell.info.cellY, metaCell.rooms.size()), roomDef.name);
            newRoom.level = roomDef.level - header.levels;
            for (RoomDef.RoomRect rect : roomDef.rects) {
                newRoom.rects.add(new RoomDef.RoomRect(rect.x + x, rect.y + y, rect.getW(), rect.getH()));
            }
            newRoom.CalculateBounds();
            newRoom.building = building;
            building.rooms.add(newRoom);
            metaCell.addRoom(newRoom, cellX * 256, cellY * 256);
            metaCell.rooms.put(newRoom.id, newRoom);
            IsoRoom isoRoom = newRoom.getIsoRoom();
            isoRoom.createLights(false);
            IsoChunk c = IsoCell.getInstance().getChunk(newRoom.x / 8, newRoom.y / 8);
            if (c == null) continue;
            for (IsoRoomLight roomLight : isoRoom.roomLights) {
                c.roomLights.add(roomLight);
            }
        }
        building.CalculateBounds(new ArrayList<RoomDef>(building.rooms));
        return true;
    }

    private void mergeBuildingsIntoMetaGrid(IsoMetaCell metaCell, Header header, int minChunkWorldX, int minChunkWorldY) {
        RoomDef roomDef;
        int i;
        ArrayList<RoomDef> roomDefs = new ArrayList<RoomDef>();
        metaCell.getRoomsIntersecting(minChunkWorldX * 8, minChunkWorldY * 8, header.width * header.file.chunkDim, header.height * header.file.chunkDim, roomDefs);
        for (i = 0; i < roomDefs.size(); ++i) {
            roomDef = roomDefs.get(i);
            metaCell.roomList.remove(roomDef);
            metaCell.rooms.remove(roomDef.id);
            metaCell.isoRooms.remove(roomDef.id);
            metaCell.buildings.remove(roomDef.building);
            metaCell.isoBuildings.remove(roomDef.building.id);
        }
        for (i = 0; i < header.buildingDefList.size(); ++i) {
            BuildingDef buildingDef = header.buildingDefList.get(i);
            buildingDef.id = BuildingID.makeID(metaCell.getX(), metaCell.getY(), metaCell.buildings.size() + i);
            metaCell.buildings.add(buildingDef);
        }
        for (i = 0; i < header.roomDefList.size(); ++i) {
            roomDef = header.roomDefList.get(i);
            roomDef.id = RoomID.makeID(metaCell.getX(), metaCell.getY(), metaCell.roomList.size() + i);
            metaCell.roomList.add(roomDef);
            metaCell.rooms.put(roomDef.id, roomDef);
        }
    }

    public Header loadHeader(String fileName) throws IOException {
        fileName = ZomboidFileSystem.instance.getString(fileName);
        try (BufferedRandomAccessFile in = new BufferedRandomAccessFile(fileName, "r", 4096);){
            Header header = this.loadHeaderInternal(fileName, in);
            return header;
        }
    }

    private Header loadHeaderInternal(String fileName, BufferedRandomAccessFile in) throws IOException {
        Header header = new Header();
        header.file = this;
        header.fileName = fileName;
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if (b1 != 80 || b2 != 90 || b3 != 66 || b4 != 89) {
            throw new IOException("unrecognized file format");
        }
        header.version = IsoLot.readInt(in);
        int numTileNames = IsoLot.readInt(in);
        for (int n = 0; n < numTileNames; ++n) {
            String str = IsoLot.readString(in);
            header.usedTileNames.add(this.sharedStrings.get(str.trim()));
        }
        header.width = IsoLot.readInt(in);
        header.height = IsoLot.readInt(in);
        header.levels = IsoLot.readInt(in);
        int numRooms = IsoLot.readInt(in);
        for (int n = 0; n < numRooms; ++n) {
            String str = IsoLot.readString(in);
            RoomDef roomDef = new RoomDef(n, this.sharedStrings.get(str));
            roomDef.level = IsoLot.readInt(in);
            int numRoomRects = IsoLot.readInt(in);
            for (int rc = 0; rc < numRoomRects; ++rc) {
                RoomDef.RoomRect rect = new RoomDef.RoomRect(IsoLot.readInt(in), IsoLot.readInt(in), IsoLot.readInt(in), IsoLot.readInt(in));
                roomDef.rects.add(rect);
            }
            roomDef.CalculateBounds();
            header.roomDefMap.put(roomDef.id, roomDef);
            header.roomDefList.add(roomDef);
            int nObjects = IsoLot.readInt(in);
            for (int m = 0; m < nObjects; ++m) {
                int e = IsoLot.readInt(in);
                int x = IsoLot.readInt(in);
                int y = IsoLot.readInt(in);
                roomDef.objects.add(new MetaObject(e, x - roomDef.x, y - roomDef.y, roomDef));
            }
            roomDef.lightsActive = Rand.Next(2) == 0;
        }
        int numBuildings = IsoLot.readInt(in);
        for (int n = 0; n < numBuildings; ++n) {
            BuildingDef buildingDef = new BuildingDef();
            numRooms = IsoLot.readInt(in);
            buildingDef.id = n;
            for (int x = 0; x < numRooms; ++x) {
                RoomDef rr = header.roomDefMap.get(IsoLot.readInt(in));
                rr.building = buildingDef;
                if (rr.isEmptyOutside()) {
                    buildingDef.emptyoutside.add(rr);
                    continue;
                }
                buildingDef.rooms.add(rr);
            }
            buildingDef.CalculateBounds(this.tempRooms);
            header.buildingDefList.add(buildingDef);
        }
        header.chunkTablePosition = in.getFilePointer();
        return header;
    }

    public ChunkData loadChunk(Header header, int chunkX, int chunkY) throws IOException {
        try (BufferedRandomAccessFile in = new BufferedRandomAccessFile(header.fileName, "r", 4096);){
            ChunkData chunkData = this.loadChunkInner(in, header, chunkX, chunkY);
            return chunkData;
        }
    }

    private ChunkData loadChunkInner(RandomAccessFile in, Header header, int chunkX, int chunkY) throws IOException {
        int count;
        int y;
        int x;
        int z;
        ChunkData chunk = new ChunkData();
        chunk.header = header;
        chunk.data = new int[this.chunkDim][this.chunkDim][header.levels][];
        int index = chunkY * header.width + chunkX;
        in.seek(header.chunkTablePosition + (long)(index * 8));
        int pos = IsoLot.readInt(in);
        in.seek(pos);
        int skip = 0;
        chunk.attributes = new int[header.levels][this.chunkDim * this.chunkDim];
        for (z = 0; z < header.levels; ++z) {
            for (x = 0; x < this.chunkDim; ++x) {
                for (y = 0; y < this.chunkDim; ++y) {
                    if (skip > 0) {
                        --skip;
                        chunk.attributes[z][x + y * this.chunkDim] = 0;
                        continue;
                    }
                    count = IsoLot.readInt(in);
                    if (count == -1 && (skip = IsoLot.readInt(in)) > 0) {
                        --skip;
                        chunk.attributes[z][x + y * this.chunkDim] = 0;
                        continue;
                    }
                    chunk.attributes[z][x + y * this.chunkDim] = count > 1 ? IsoLot.readInt(in) : 0;
                }
            }
        }
        skip = 0;
        for (z = 0; z < header.levels; ++z) {
            for (x = 0; x < this.chunkDim; ++x) {
                for (y = 0; y < this.chunkDim; ++y) {
                    if (skip > 0) {
                        --skip;
                        chunk.data[x][y][z] = null;
                        continue;
                    }
                    count = IsoLot.readInt(in);
                    if (count == -1 && (skip = IsoLot.readInt(in)) > 0) {
                        --skip;
                        chunk.data[x][y][z] = null;
                        continue;
                    }
                    if (count > 1) {
                        chunk.data[x][y][z] = new int[count - 1];
                        for (int n = 1; n < count; ++n) {
                            chunk.data[x][y][z][n - 1] = IsoLot.readInt(in);
                        }
                        continue;
                    }
                    chunk.data[x][y][z] = null;
                }
            }
        }
        return chunk;
    }

    public void setChunkInWorldArb(ChunkData chunkData, int sx, int sy, int sz, int tx, int ty, int tz) {
        IsoCell cell = IsoWorld.instance.currentCell;
        try {
            this.setChunkInWorldInnerArb(chunkData, sx, sy, sz, tx, ty, tz);
            for (int x = tx; x < tx + chunkData.header.file.chunkDim; ++x) {
                for (int y = ty; y < ty + chunkData.header.file.chunkDim; ++y) {
                    for (int z = tz; z < tz + chunkData.header.levels; ++z) {
                        IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, z);
                        if (sq == null) continue;
                        sq.RecalcAllWithNeighbours(true);
                        if (!sq.HasStairsBelow()) continue;
                        sq.removeUnderground();
                    }
                }
            }
        }
        catch (Exception ex) {
            DebugLog.log("Failed to load chunk, blocking out area");
            ExceptionLogger.logException(ex);
            for (int x = tx; x < tx + chunkData.header.file.chunkDim; ++x) {
                for (int y = ty; y < ty + chunkData.header.file.chunkDim; ++y) {
                    for (int z = tz; z < tz + chunkData.header.levels; ++z) {
                        IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, z);
                        if (sq == null) continue;
                        sq.RecalcAllWithNeighbours(true);
                    }
                }
            }
        }
    }

    public void setChunkInWorld(ChunkData chunkData, int sx, int sy, int sz, IsoChunk ch, int wx, int wy) {
        IsoCell cell = IsoWorld.instance.currentCell;
        wx *= 8;
        wy *= 8;
        try {
            this.setChunkInWorldInner(chunkData, sx, sy, sz, ch, wx, wy);
        }
        catch (Exception ex) {
            DebugLog.log("Failed to load chunk, blocking out area");
            ExceptionLogger.logException(ex);
            for (int x = wx + sx; x < wx + sx + 8; ++x) {
                for (int y = wy + sy; y < wy + sy + 8; ++y) {
                    for (int z = sz; z < sz + chunkData.header.levels; ++z) {
                        ch.setSquare(x - wx, y - wy, z, null);
                        cell.setCacheGridSquare(x, y, z, null);
                    }
                }
            }
        }
    }

    private void setChunkInWorldInner(ChunkData chunkData, int sx, int sy, int sz, IsoChunk ch, int wx, int wy) {
        IsoCell cell = IsoWorld.instance.currentCell;
        Stack bedList = new Stack();
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        for (int x = wx + sx; x < wx + sx + 8; ++x) {
            for (int y = wy + sy; y < wy + sy + 8; ++y) {
                for (int z = sz; z < sz + chunkData.header.levels; ++z) {
                    RoomDef roomDef;
                    int[] ints;
                    boolean bClearExistingObjects = false;
                    if (x >= wx + 8 || y >= wy + 8 || x < wx || y < wy || z < 0 || (ints = chunkData.data[x - (wx + sx)][y - (wy + sy)][z - sz]) == null || ints.length == 0) continue;
                    int s = ints.length;
                    IsoGridSquare square = ch.getGridSquare(x - wx, y - wy, z);
                    if (square == null) {
                        square = IsoGridSquare.getNew(cell, null, x, y, z);
                        square.setX(x);
                        square.setY(y);
                        square.setZ(z);
                        ch.setSquare(x - wx, y - wy, z, square);
                    }
                    for (int xx = -1; xx <= 1; ++xx) {
                        for (int yy = -1; yy <= 1; ++yy) {
                            IsoGridSquare square2;
                            if (xx == 0 && yy == 0 || xx + x - wx < 0 || xx + x - wx >= 8 || yy + y - wy < 0 || yy + y - wy >= 8 || (square2 = ch.getGridSquare(x + xx - wx, y + yy - wy, z)) != null) continue;
                            square2 = IsoGridSquare.getNew(cell, null, x + xx, y + yy, z);
                            ch.setSquare(x + xx - wx, y + yy - wy, z, square2);
                        }
                    }
                    if (s > 1 && z > IsoCell.maxHeight) {
                        IsoCell.maxHeight = z;
                    }
                    long roomID = (roomDef = metaGrid.getRoomAt(x, y, z)) != null ? roomDef.id : -1L;
                    square.setRoomID(roomID);
                    square.ResetIsoWorldRegion();
                    roomDef = metaGrid.getEmptyOutsideAt(x, y, z);
                    if (roomDef != null) {
                        IsoRoom room = ch.getRoom(roomDef.id);
                        square.roofHideBuilding = room == null ? null : room.building;
                    }
                    for (int n = 0; n < s; ++n) {
                        IsoSprite spr;
                        String tile = chunkData.header.usedTileNames.get(ints[n]);
                        if (!chunkData.header.fixed2x) {
                            tile = IsoChunk.Fix2x(tile);
                        }
                        if ((spr = IsoSpriteManager.instance.namedMap.get(tile)) == null) {
                            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                            continue;
                        }
                        if (n == 0 && spr.getProperties().has(IsoFlagType.solidfloor) && (!spr.properties.has(IsoFlagType.hidewalls) || ints.length > 1)) {
                            bClearExistingObjects = true;
                        }
                        if (bClearExistingObjects && n == 0) {
                            square.getObjects().clear();
                        }
                        CellLoader.DoTileObjectCreation(spr, spr.getTileType(), square, cell, x, y, z, tile);
                    }
                    square.FixStackableObjects();
                }
            }
        }
    }

    private void setChunkInWorldInnerArb(ChunkData chunkData, int sx, int sy, int sz, int tx, int ty, int tz) {
        IsoCell cell = IsoWorld.instance.currentCell;
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        for (int x = tx; x < tx + this.chunkDim; ++x) {
            for (int y = ty; y < ty + this.chunkDim; ++y) {
                for (int z = tz; z < tz + chunkData.header.levels; ++z) {
                    RoomDef roomDef;
                    if (x == 12030 && y == 2601 && z == -1) {
                        boolean bl = false;
                    }
                    boolean bClearExistingObjects = false;
                    int[] ints = chunkData.data[x - tx][y - ty][z - tz];
                    if (ints == null || ints.length == 0) continue;
                    int tileCount = ints.length;
                    IsoGridSquare square = IsoCell.getInstance().getOrCreateGridSquare(x, y, z);
                    square.EnsureSurroundNotNull();
                    if (tileCount > 1 && z > IsoCell.maxHeight) {
                        IsoCell.maxHeight = z;
                    }
                    long roomID = (roomDef = metaGrid.getRoomAt(x, y, z)) != null ? roomDef.id : -1L;
                    square.setRoomID(roomID);
                    if (roomID != -1L && !square.getRoom().getBuilding().rooms.contains(square.getRoom())) {
                        square.getRoom().getBuilding().rooms.add(square.getRoom());
                    }
                    square.ResetIsoWorldRegion();
                    for (int n = 0; n < tileCount; ++n) {
                        IsoSprite spr;
                        String tile = chunkData.header.usedTileNames.get(ints[n]);
                        if (!chunkData.header.fixed2x) {
                            tile = IsoChunk.Fix2x(tile);
                        }
                        if ((spr = IsoSpriteManager.instance.namedMap.get(tile)) == null) {
                            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                            continue;
                        }
                        if (n == 0) {
                            bClearExistingObjects = true;
                        }
                        if (bClearExistingObjects && n == 0) {
                            int numObjs = square.getObjects().size();
                            for (int i = numObjs - 1; i >= 0; --i) {
                                square.getObjects().get(i).removeFromWorld();
                                square.getObjects().get(i).removeFromSquare();
                            }
                        }
                        if (tile.contains("lighting")) {
                            boolean bl = false;
                        }
                        CellLoader.DoTileObjectCreation(spr, spr.getTileType(), square, cell, x, y, z, tile);
                    }
                    square.FixStackableObjects();
                }
            }
        }
    }

    private void addToIsoChunk(IsoChunk chunk, ChunkData chunkData, int tx, int ty, int tz, boolean isBasementAccess) {
        IsoCell cell = IsoWorld.instance.getCell();
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int chunksPerWidth = 8;
        int x1 = Math.max(chunk.wx * 8, tx);
        int y1 = Math.max(chunk.wy * 8, ty);
        int x2 = Math.min(chunk.wx * 8 + 8, tx + this.chunkDim);
        int y2 = Math.min(chunk.wy * 8 + 8, ty + this.chunkDim);
        for (int x = x1; x < x2; ++x) {
            for (int y = y1; y < y2; ++y) {
                for (int z = tz; z < tz + chunkData.header.levels; ++z) {
                    boolean bKeepExistingOther;
                    RoomDef roomDef;
                    int[] ints = chunkData.data[x - tx][y - ty][z - tz];
                    if (ints == null || ints.length == 0) continue;
                    int tileCount = ints.length;
                    IsoGridSquare square = chunk.getGridSquare(x - chunk.wx * 8, y - chunk.wy * 8, z);
                    if (square == null) {
                        square = IsoGridSquare.getNew(cell, null, x, y, z);
                        square.setX(x);
                        square.setY(y);
                        square.setZ(z);
                        chunk.setSquare(x - chunk.wx * 8, y - chunk.wy * 8, z, square);
                    }
                    long roomID = (roomDef = metaGrid.getRoomAt(x, y, z)) != null ? roomDef.id : -1L;
                    square.setRoomID(roomID);
                    int attributes = chunkData.attributes[z - tz][x - tx + (y - ty) * this.chunkDim];
                    boolean bKeepExistingFloors = (attributes & 1) != 0;
                    boolean bKeepExistingWalls = (attributes & 2) != 0;
                    boolean bl = bKeepExistingOther = (attributes & 4) != 0;
                    if (bKeepExistingFloors || bKeepExistingWalls || bKeepExistingOther) {
                        for (int i = square.getObjects().size() - 1; i >= 0; --i) {
                            boolean isOther;
                            IsoObject object = square.getObjects().get(i);
                            boolean bKeep = false;
                            boolean isFloor = object.sprite != null && object.sprite.solidfloor;
                            boolean isWall = object.sprite != null && (object.sprite.getProperties().has(IsoFlagType.WallW) || object.sprite.getProperties().has(IsoFlagType.WallN) || object.sprite.getProperties().has(IsoFlagType.WallNW) || object.isWallSE());
                            boolean bl2 = isOther = !isFloor && !isWall;
                            if (bKeepExistingFloors && isFloor) {
                                bKeep = true;
                            }
                            if (bKeepExistingWalls && isWall) {
                                bKeep = true;
                            }
                            if (bKeepExistingOther && isOther) {
                                bKeep = true;
                            }
                            if (bKeep) continue;
                            square.getObjects().remove(i);
                            square.getSpecialObjects().remove(object);
                        }
                    } else {
                        square.getObjects().clear();
                        square.getSpecialObjects().clear();
                    }
                    for (int n = 0; n < tileCount; ++n) {
                        IsoSprite spr;
                        String tile = chunkData.header.usedTileNames.get(ints[n]);
                        if (!chunkData.header.fixed2x) {
                            tile = IsoChunk.Fix2x(tile);
                        }
                        if ((spr = IsoSpriteManager.instance.namedMap.get(tile)) == null) {
                            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                            continue;
                        }
                        CellLoader.DoTileObjectCreation(spr, spr.getTileType(), square, cell, x, y, z, tile);
                    }
                    square.FixStackableObjects();
                }
                IsoGridSquare square = chunk.getGridSquare(x - chunk.wx * 8, y - chunk.wy * 8, tz + chunkData.header.levels);
                this.removeFloorsAboveStairs(square);
            }
        }
    }

    void removeFloorsAboveStairs(IsoGridSquare square) {
        if (square == null) {
            return;
        }
        int chunksPerWidth = 8;
        int cx = square.chunk.wx * 8;
        int cy = square.chunk.wy * 8;
        IsoGridSquare below = square.chunk.getGridSquare(square.x - cx, square.y - cy, square.z - 1);
        if (below == null) {
            return;
        }
        below.RecalcProperties();
        if (!below.HasStairs() && !below.hasSlopedSurface()) {
            return;
        }
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject isoObject = objects[i];
            if (!isoObject.isFloor()) continue;
            square.getObjects().remove(isoObject);
            break;
        }
    }

    public static final class Header {
        NewMapBinaryFile file;
        public int width;
        public int height;
        public int levels;
        public int version;
        public final TLongObjectHashMap<RoomDef> roomDefMap = new TLongObjectHashMap();
        public final ArrayList<RoomDef> roomDefList = new ArrayList();
        public final ArrayList<BuildingDef> buildingDefList = new ArrayList();
        public boolean fixed2x = true;
        protected final ArrayList<String> usedTileNames = new ArrayList();
        String fileName;
        private long chunkTablePosition;

        public void Dispose() {
            for (int i = 0; i < this.buildingDefList.size(); ++i) {
                BuildingDef buildingDef = this.buildingDefList.get(i);
                buildingDef.Dispose();
            }
            this.roomDefMap.clear();
            this.roomDefList.clear();
            this.buildingDefList.clear();
            this.usedTileNames.clear();
        }
    }

    public static final class ChunkData {
        Header header;
        int[][][][] data;
        int[][] attributes;
    }
}

