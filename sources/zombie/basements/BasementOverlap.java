/*
 * Decompiled with CFR 0.152.
 */
package zombie.basements;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;

public final class BasementOverlap {
    static final int SQUARES_PER_CHUNK = 20;
    static final int CHUNKS_PER_CELL = 10;
    static final int SQUARES_PER_CELL = 200;
    final TLongObjectHashMap<Cell> cellLookup = new TLongObjectHashMap();
    final ArrayList<Basement> basements = new ArrayList();

    long getCellKey(int cellX, int cellY) {
        return (long)cellY << 32 | (long)cellX;
    }

    Cell getCell(int cellX, int cellY) {
        long cellKey = this.getCellKey(cellX, cellY);
        return this.cellLookup.get(cellKey);
    }

    Cell createCell(int cellX, int cellY) {
        Cell cell = new Cell(cellX, cellY);
        long cellKey = this.getCellKey(cellX, cellY);
        this.cellLookup.put(cellKey, cell);
        return cell;
    }

    Cell getOrCreateCell(int cellX, int cellY) {
        Cell cell = this.getCell(cellX, cellY);
        if (cell == null) {
            cell = this.createCell(cellX, cellY);
        }
        return cell;
    }

    void addBasement(BuildingDef buildingDef, int placeX, int placeY, int placeZ) {
        Basement basement = new Basement();
        basement.buildingDef = buildingDef;
        basement.placeX = placeX;
        basement.placeY = placeY;
        basement.placeZ = placeZ;
        this.basements.add(basement);
        int cellX1 = placeX / 200;
        int cellY1 = placeY / 200;
        int cellX2 = (placeX + buildingDef.getW() - 1) / 200;
        int cellY2 = (placeY + buildingDef.getH() - 1) / 200;
        for (int cellY = cellY1; cellY <= cellY2; ++cellY) {
            for (int cellX = cellX1; cellX <= cellX2; ++cellX) {
                Cell cell = this.getOrCreateCell(cellX, cellY);
                cell.addBasement(basement);
            }
        }
    }

    boolean checkOverlap(BuildingDef buildingDef, int placeX, int placeY, int placeZ) {
        int cellX1 = placeX / 200;
        int cellY1 = placeY / 200;
        int cellX2 = (placeX + buildingDef.getW() - 1) / 200;
        int cellY2 = (placeY + buildingDef.getH() - 1) / 200;
        for (int cellY = cellY1; cellY <= cellY2; ++cellY) {
            for (int cellX = cellX1; cellX <= cellX2; ++cellX) {
                Cell cell = this.getCell(cellX, cellY);
                if (cell == null || !cell.checkOverlap(buildingDef, placeX, placeY, placeZ)) continue;
                return true;
            }
        }
        return false;
    }

    public void Dispose() {
        this.cellLookup.forEachValue(cell -> {
            cell.Dispose();
            return true;
        });
        this.cellLookup.clear();
        this.basements.forEach(basement -> basement.Dispose());
        this.basements.clear();
    }

    static final class Cell {
        final int cellX;
        final int cellY;
        final Chunk[] chunks = new Chunk[100];

        Cell(int cellX, int cellY) {
            this.cellX = cellX;
            this.cellY = cellY;
        }

        Chunk getChunk(int chunkX, int chunkY) {
            return this.chunks[chunkX + chunkY * 10];
        }

        Chunk createChunk(int chunkX, int chunkY) {
            Chunk chunk;
            this.chunks[chunkX + chunkY * 10] = chunk = new Chunk();
            return chunk;
        }

        Chunk getOrCreateChunk(int chunkX, int chunkY) {
            Chunk chunk = this.getChunk(chunkX, chunkY);
            if (chunk == null) {
                chunk = this.createChunk(chunkX, chunkY);
            }
            return chunk;
        }

        void addBasement(Basement basement) {
            int placeX = basement.placeX - this.cellX * 200;
            int placeY = basement.placeY - this.cellY * 200;
            int chunkX1 = Math.max(placeX / 20, 0);
            int chunkY1 = Math.max(placeY / 20, 0);
            int chunkX2 = Math.min((placeX + basement.buildingDef.getW() - 1) / 20, 9);
            int chunkY2 = Math.min((placeY + basement.buildingDef.getH() - 1) / 20, 9);
            for (int chunkY = chunkY1; chunkY <= chunkY2; ++chunkY) {
                for (int chunkX = chunkX1; chunkX <= chunkX2; ++chunkX) {
                    Chunk chunk = this.getOrCreateChunk(chunkX, chunkY);
                    chunk.addBasement(basement);
                }
            }
        }

        boolean checkOverlap(BuildingDef buildingDef, int placeXOrig, int placeYOrig, int placeZ) {
            int placeX = placeXOrig - this.cellX * 200;
            int placeY = placeYOrig - this.cellY * 200;
            int chunkX1 = Math.max(placeX / 20, 0);
            int chunkY1 = Math.max(placeY / 20, 0);
            int chunkX2 = Math.min((placeX + buildingDef.getW() - 1) / 20, 9);
            int chunkY2 = Math.min((placeY + buildingDef.getH() - 1) / 20, 9);
            for (int chunkY = chunkY1; chunkY <= chunkY2; ++chunkY) {
                for (int chunkX = chunkX1; chunkX <= chunkX2; ++chunkX) {
                    Chunk chunk = this.getChunk(chunkX, chunkY);
                    if (chunk == null || !chunk.checkOverlap(buildingDef, placeXOrig, placeYOrig, placeZ)) continue;
                    return true;
                }
            }
            return false;
        }

        void Dispose() {
            for (Chunk chunk : this.chunks) {
                if (chunk == null) continue;
                chunk.Dispose();
            }
            Arrays.fill(this.chunks, null);
        }
    }

    static final class Basement {
        BuildingDef buildingDef;
        int placeX;
        int placeY;
        int placeZ;

        Basement() {
        }

        boolean checkOverlap(BuildingDef buildingDef, int placeX, int placeY, int placeZ) {
            int zMin1 = this.placeZ;
            int zMax1 = this.placeZ + this.buildingDef.getMaxLevel();
            int zMin2 = placeZ;
            int zMax2 = placeZ + buildingDef.getMaxLevel();
            if (zMin2 > zMax1 || zMax2 < zMin1) {
                return false;
            }
            if (placeX >= this.placeX + this.buildingDef.getW() || placeX + buildingDef.getW() <= this.placeX) {
                return false;
            }
            if (placeY >= this.placeY + this.buildingDef.getH() || placeY + buildingDef.getH() <= this.placeY) {
                return false;
            }
            int zMin = Math.max(zMin1, zMin2);
            int zMax = Math.min(zMax1, zMax2);
            for (int z = zMin; z <= zMax; ++z) {
                int z1 = z - zMin1;
                int z2 = z - zMin2;
                if (!this.checkOverlap(this.buildingDef, this.placeX, this.placeY, z1, buildingDef, placeX, placeY, z2)) continue;
                return true;
            }
            return true;
        }

        boolean checkOverlap(BuildingDef buildingDef1, int x1, int y1, int level1, BuildingDef buildingDef2, int x2, int y2, int level2) {
            for (int roomIndex1 = 0; roomIndex1 < buildingDef1.rooms.size(); ++roomIndex1) {
                RoomDef roomDef1 = buildingDef1.rooms.get(roomIndex1);
                if (roomDef1.level != level1) continue;
                for (int roomIndex2 = 0; roomIndex2 < buildingDef2.rooms.size(); ++roomIndex2) {
                    RoomDef roomDef2 = buildingDef2.rooms.get(roomIndex2);
                    if (roomDef2.level != level2 || !this.checkOverlap(roomDef1, x1, y1, roomDef2, x2, y2)) continue;
                    return true;
                }
            }
            return false;
        }

        boolean checkOverlap(RoomDef roomDef1, int x1, int y1, RoomDef roomDef2, int x2, int y2) {
            int minX1 = x1 + roomDef1.x;
            int minY1 = y1 + roomDef1.y;
            int maxX1 = x1 + roomDef1.x2;
            int maxY1 = y1 + roomDef1.y2;
            int minX2 = x2 + roomDef2.x;
            int minY2 = y2 + roomDef2.y;
            int maxX2 = x2 + roomDef2.x2;
            int maxY2 = y2 + roomDef2.y2;
            if (minX1 >= maxX2 || maxX1 <= minX2) {
                return false;
            }
            if (minY1 >= maxY2 || maxY1 <= minY2) {
                return false;
            }
            for (int i = 0; i < roomDef2.rects.size(); ++i) {
                RoomDef.RoomRect roomRect2 = roomDef2.rects.get(i);
                if (!roomDef1.intersects(x2 + roomRect2.x - x1, y2 + roomRect2.y - y1, roomRect2.w, roomRect2.h)) continue;
                return true;
            }
            return false;
        }

        void Dispose() {
            this.buildingDef = null;
        }
    }

    static final class Chunk {
        final ArrayList<Basement> basements = new ArrayList();

        Chunk() {
        }

        void addBasement(Basement basement) {
            this.basements.add(basement);
        }

        boolean checkOverlap(BuildingDef buildingDef, int placeX, int placeY, int placeZ) {
            for (int i = 0; i < this.basements.size(); ++i) {
                Basement basement = this.basements.get(i);
                if (!basement.checkOverlap(buildingDef, placeX, placeY, placeZ)) continue;
                return true;
            }
            return false;
        }

        void Dispose() {
            this.basements.clear();
        }
    }
}

