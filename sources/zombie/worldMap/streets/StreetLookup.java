/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.streets;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.Arrays;
import zombie.core.math.PZMath;
import zombie.pathfind.LiangBarsky;
import zombie.util.list.PZArrayUtil;
import zombie.worldMap.streets.WorldMapStreet;

public final class StreetLookup {
    static final int SQUARES_PER_CHUNK = 20;
    static final int CHUNKS_PER_CELL = 10;
    static final int SQUARES_PER_CELL = 200;
    static final LiangBarsky LB = new LiangBarsky();
    final TLongObjectHashMap<Cell> cellLookup = new TLongObjectHashMap();

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

    void addStreet(WorldMapStreet street) {
        int cellX1 = PZMath.fastfloor(street.getMinX() / 200.0f);
        int cellY1 = PZMath.fastfloor(street.getMinY() / 200.0f);
        int cellX2 = PZMath.fastfloor(street.getMaxX() / 200.0f);
        int cellY2 = PZMath.fastfloor(street.getMaxY() / 200.0f);
        for (int cellY = cellY1; cellY <= cellY2; ++cellY) {
            for (int cellX = cellX1; cellX <= cellX2; ++cellX) {
                if (!StreetLookup.streetIntersects(street, cellX * 200, cellY * 200, (cellX + 1) * 200, (cellY + 1) * 200)) continue;
                Cell cell = this.getOrCreateCell(cellX, cellY);
                cell.addStreet(street);
            }
        }
    }

    static boolean streetIntersects(WorldMapStreet street, int left, int top, int right, int bottom) {
        for (int i = 0; i < street.getNumPoints() - 1; ++i) {
            float y2;
            float x2;
            float y1;
            float x1 = street.getPointX(i);
            boolean intersect = LB.lineRectIntersect(x1, y1 = street.getPointY(i), (x2 = street.getPointX(i + 1)) - x1, (y2 = street.getPointY(i + 1)) - y1, left, top, right, bottom);
            if (!intersect) continue;
            return true;
        }
        return false;
    }

    void addStreets(ArrayList<WorldMapStreet> streets) {
        for (WorldMapStreet street : streets) {
            this.addStreet(street);
        }
    }

    void removeStreet(WorldMapStreet street) {
        int cellX1 = PZMath.fastfloor(street.getMinX() / 200.0f);
        int cellY1 = PZMath.fastfloor(street.getMinY() / 200.0f);
        int cellX2 = PZMath.fastfloor(street.getMaxX() / 200.0f);
        int cellY2 = PZMath.fastfloor(street.getMaxY() / 200.0f);
        for (int cellY = cellY1; cellY <= cellY2; ++cellY) {
            for (int cellX = cellX1; cellX <= cellX2; ++cellX) {
                Cell cell = this.getCell(cellX, cellY);
                if (cell == null) continue;
                cell.removeStreet(street);
            }
        }
    }

    void onStreetChanged(WorldMapStreet street) {
    }

    void getStreetsOverlapping(int minX, int minY, int maxX, int maxY, THashSet<WorldMapStreet> result) {
        int cellX1 = minX / 200;
        int cellY1 = minY / 200;
        int cellX2 = maxX / 200;
        int cellY2 = maxY / 200;
        for (int cellY = cellY1; cellY <= cellY2; ++cellY) {
            for (int cellX = cellX1; cellX <= cellX2; ++cellX) {
                Cell cell = this.getCell(cellX, cellY);
                if (cell == null) continue;
                cell.getStreetsOverlapping(minX - cellX * 200, minY - cellY * 200, maxX - cellX * 200, maxY - cellY * 200, result);
            }
        }
    }

    public void Dispose() {
        this.cellLookup.forEachValue(cell -> {
            cell.Dispose();
            return true;
        });
        this.cellLookup.clear();
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

        void addStreet(WorldMapStreet street) {
            int minX = PZMath.fastfloor(street.getMinX() - (float)(this.cellX * 200));
            int minY = PZMath.fastfloor(street.getMinY() - (float)(this.cellY * 200));
            int maxX = PZMath.fastfloor(street.getMaxX() - (float)(this.cellX * 200));
            int maxY = PZMath.fastfloor(street.getMaxY() - (float)(this.cellY * 200));
            int chunkX1 = PZMath.max(minX / 20, 0);
            int chunkY1 = PZMath.max(minY / 20, 0);
            int chunkX2 = PZMath.min(maxX / 20, 9);
            int chunkY2 = PZMath.min(maxY / 20, 9);
            int left = this.cellX * 200;
            int top = this.cellY * 200;
            for (int chunkY = chunkY1; chunkY <= chunkY2; ++chunkY) {
                for (int chunkX = chunkX1; chunkX <= chunkX2; ++chunkX) {
                    if (!StreetLookup.streetIntersects(street, left + chunkX * 20, top + chunkY * 20, left + (chunkX + 1) * 20, top + (chunkY + 1) * 20)) continue;
                    Chunk chunk = this.getOrCreateChunk(chunkX, chunkY);
                    chunk.addStreet(street);
                }
            }
        }

        void removeStreet(WorldMapStreet street) {
            for (int chunkY = 0; chunkY < 10; ++chunkY) {
                for (int chunkX = 0; chunkX < 10; ++chunkX) {
                    Chunk chunk = this.getChunk(chunkX, chunkY);
                    if (chunk == null) continue;
                    chunk.removeStreet(street);
                }
            }
        }

        void getStreetsOverlapping(int minX, int minY, int maxX, int maxY, THashSet<WorldMapStreet> result) {
            int chunkX1 = PZMath.max(minX / 20, 0);
            int chunkY1 = PZMath.max(minY / 20, 0);
            int chunkX2 = PZMath.min(maxX / 20, 9);
            int chunkY2 = PZMath.min(maxY / 20, 9);
            for (int chunkY = chunkY1; chunkY <= chunkY2; ++chunkY) {
                for (int chunkX = chunkX1; chunkX <= chunkX2; ++chunkX) {
                    Chunk chunk = this.getChunk(chunkX, chunkY);
                    if (chunk == null) continue;
                    for (int i = 0; i < chunk.streetCount; ++i) {
                        result.add(chunk.streets[i]);
                    }
                }
            }
        }

        void Dispose() {
            for (Chunk chunk : this.chunks) {
                if (chunk == null) continue;
                chunk.Dispose();
            }
            Arrays.fill(this.chunks, null);
        }
    }

    static final class Chunk {
        WorldMapStreet[] streets;
        short streetCount;

        Chunk() {
        }

        void addStreet(WorldMapStreet street) {
            if (this.contains(street)) {
                return;
            }
            this.streets = PZArrayUtil.newInstance(WorldMapStreet.class, this.streets, this.streetCount + 1, true);
            short s = this.streetCount;
            this.streetCount = (short)(s + 1);
            this.streets[s] = street;
        }

        void removeStreet(WorldMapStreet street) {
            if (this.streets == null) {
                return;
            }
            int index = this.indexOf(street);
            if (index == -1) {
                return;
            }
            System.arraycopy(this.streets, index + 1, this.streets, index, this.streetCount - index - 1);
            this.streetCount = (short)(this.streetCount - 1);
        }

        int indexOf(WorldMapStreet street) {
            return PZArrayUtil.indexOf(this.streets, (int)this.streetCount, street);
        }

        boolean contains(WorldMapStreet street) {
            return PZArrayUtil.contains(this.streets, (int)this.streetCount, street);
        }

        void Dispose() {
            if (this.streets == null) {
                return;
            }
            Arrays.fill(this.streets, null);
            this.streets = null;
            this.streetCount = 0;
        }
    }
}

