/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.pathfind.ChunkData;
import zombie.pathfind.ChunkLevel;
import zombie.pathfind.ChunkUpdateTask;
import zombie.pathfind.ChunkUpdateTaskLevel;
import zombie.pathfind.SlopedSurface;
import zombie.pathfind.Square;
import zombie.pathfind.SquareUpdateTask;
import zombie.pathfind.VisibilityGraph;

public final class Chunk {
    public short wx;
    public short wy;
    ChunkLevel[] levels = new ChunkLevel[1];
    final ChunkData collision = new ChunkData();
    int minLevel = 32;
    int maxLevel = 32;
    final ArrayList<VisibilityGraph> visibilityGraphs = new ArrayList();
    static final ArrayDeque<Chunk> pool = new ArrayDeque();

    Chunk() {
        this.levels[0] = ChunkLevel.alloc().init(this, this.minLevel);
    }

    Chunk init(int wx, int wy) {
        this.wx = (short)wx;
        this.wy = (short)wy;
        return this;
    }

    void clear() {
        for (int z = this.minLevel; z <= this.maxLevel; ++z) {
            ChunkLevel chunkLevel = this.levels[z - this.minLevel];
            if (chunkLevel == null) continue;
            chunkLevel.clear();
            chunkLevel.release();
            this.levels[z - this.minLevel] = null;
        }
        this.wy = (short)-1;
        this.wx = (short)-1;
        this.levels = new ChunkLevel[1];
        this.maxLevel = 32;
        this.minLevel = 32;
        this.levels[0] = ChunkLevel.alloc().init(this, this.minLevel);
    }

    public int getMinX() {
        return this.wx * 8;
    }

    public int getMinY() {
        return this.wy * 8;
    }

    public int getMaxX() {
        return (this.wx + 1) * 8 - 1;
    }

    public int getMaxY() {
        return (this.wy + 1) * 8 - 1;
    }

    public int getMinLevel() {
        return this.minLevel;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public boolean isValidLevel(int level) {
        return level >= this.getMinLevel() && level <= this.getMaxLevel();
    }

    public boolean contains(int worldX, int worldY) {
        return worldX >= this.getMinX() && worldY >= this.getMinY() && worldX <= this.getMaxX() && worldY <= this.getMaxY();
    }

    void setMinMaxLevel(int minLevel, int maxLevel) {
        if (minLevel == this.minLevel && maxLevel == this.maxLevel) {
            return;
        }
        for (int z = this.minLevel; z <= this.maxLevel; ++z) {
            ChunkLevel chunkLevel;
            if (z >= minLevel && z <= maxLevel || (chunkLevel = this.levels[z - this.minLevel]) == null) continue;
            chunkLevel.clear();
            chunkLevel.release();
            this.levels[z - this.minLevel] = null;
        }
        ChunkLevel[] newLevels = new ChunkLevel[maxLevel - minLevel + 1];
        for (int z = minLevel; z <= maxLevel; ++z) {
            newLevels[z - minLevel] = this.isValidLevel(z) ? this.levels[z - this.minLevel] : ChunkLevel.alloc().init(this, z);
        }
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.levels = newLevels;
    }

    public ChunkLevel getLevelData(int level) {
        if (this.isValidLevel(level)) {
            return this.levels[level - this.minLevel];
        }
        return null;
    }

    Square getSquare(int x, int y, int z) {
        ChunkLevel chunkLevel = this.getLevelData(z);
        if (chunkLevel == null) {
            return null;
        }
        return chunkLevel.getSquare(x, y);
    }

    public Square[][] getSquaresForLevel(int z) {
        ChunkLevel chunkLevel = this.getLevelData(z);
        if (chunkLevel == null) {
            return null;
        }
        return chunkLevel.squares;
    }

    void setData(ChunkUpdateTask task) {
        this.setMinMaxLevel(task.minLevel, task.maxLevel);
        for (int z = this.minLevel; z <= this.maxLevel; ++z) {
            ChunkUpdateTaskLevel taskLevel = task.levels[z - this.minLevel];
            ChunkLevel chunkLevel = this.getLevelData(z);
            for (int y = 0; y < 8; ++y) {
                for (int x = 0; x < 8; ++x) {
                    int bits = taskLevel.data[x][y];
                    short cost = taskLevel.cost[x][y];
                    chunkLevel.setBits(x, y, bits, cost);
                    Square square = chunkLevel.squares[x][y];
                    if (square == null) continue;
                    square.slopedSurfaceDirection = null;
                    square.slopedSurfaceHeightMin = 0.0f;
                    square.slopedSurfaceHeightMax = 0.0f;
                }
            }
            for (int i = 0; i < taskLevel.slopedSurfaces.size(); ++i) {
                SlopedSurface slopedSurface = taskLevel.slopedSurfaces.get(i);
                Square square = chunkLevel.squares[slopedSurface.x][slopedSurface.y];
                square.slopedSurfaceDirection = slopedSurface.direction;
                square.slopedSurfaceHeightMin = slopedSurface.heightMin;
                square.slopedSurfaceHeightMax = slopedSurface.heightMax;
            }
        }
    }

    boolean setData(SquareUpdateTask task) {
        int x = task.x - this.wx * 8;
        int y = task.y - this.wy * 8;
        if (x < 0 || x >= 8) {
            return false;
        }
        if (y < 0 || y >= 8) {
            return false;
        }
        Square[][] squares = this.getSquaresForLevel(task.z);
        Square square = squares[x][y];
        if (task.bits == 0) {
            if (square != null) {
                square.release();
                squares[x][y] = null;
                return true;
            }
        } else {
            boolean bChanged;
            if (square == null) {
                squares[x][y] = square = Square.alloc().init(task.x, task.y, task.z);
            }
            boolean bl = bChanged = square.bits != task.bits || square.cost != task.cost;
            if (task.slopedSurface == null) {
                bChanged |= square.slopedSurfaceDirection != null || square.slopedSurfaceHeightMin != 0.0f || square.slopedSurfaceHeightMax != 0.0f;
                square.slopedSurfaceDirection = null;
                square.slopedSurfaceHeightMin = 0.0f;
                square.slopedSurfaceHeightMax = 0.0f;
            } else {
                bChanged |= square.slopedSurfaceDirection != task.slopedSurface.direction || square.slopedSurfaceHeightMin != task.slopedSurface.heightMin || square.slopedSurfaceHeightMax != task.slopedSurface.heightMax;
                square.slopedSurfaceDirection = task.slopedSurface.direction;
                square.slopedSurfaceHeightMin = task.slopedSurface.heightMin;
                square.slopedSurfaceHeightMax = task.slopedSurface.heightMax;
            }
            if (bChanged) {
                square.bits = task.bits;
                square.cost = task.cost;
                return true;
            }
        }
        return false;
    }

    void addVisibilityGraph(VisibilityGraph graph) {
        if (this.visibilityGraphs.contains(graph)) {
            return;
        }
        this.visibilityGraphs.add(graph);
    }

    void removeVisibilityGraph(VisibilityGraph graph) {
        this.visibilityGraphs.remove(graph);
    }

    static Chunk alloc() {
        return pool.isEmpty() ? new Chunk() : pool.pop();
    }

    void release() {
        assert (!pool.contains(this));
        pool.push(this);
    }
}

