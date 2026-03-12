/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import zombie.pathfind.Chunk;
import zombie.pathfind.Square;
import zombie.pathfind.highLevel.HLAStar;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.popman.ObjectPool;

public final class ChunkLevel {
    Chunk chunk;
    int z;
    final Square[][] squares = new Square[8][8];
    final HLChunkLevel hlChunkLevel = new HLChunkLevel(this);
    static final ObjectPool<ChunkLevel> pool = new ObjectPool<ChunkLevel>(ChunkLevel::new);

    ChunkLevel() {
    }

    public Chunk getChunk() {
        return this.chunk;
    }

    public int getLevel() {
        return this.z;
    }

    public Square getSquare(int x, int y) {
        if ((x -= this.chunk.getMinX()) < 0 || x >= 8 || (y -= this.chunk.getMinY()) < 0 || y >= 8) {
            return null;
        }
        return this.squares[x][y];
    }

    public void setBits(int x, int y, int bits, short cost) {
        if (bits == 0) {
            Square square = this.squares[x][y];
            if (square != null) {
                square.release();
                this.squares[x][y] = null;
            }
        } else {
            Square square = this.squares[x][y];
            if (square == null) {
                this.squares[x][y] = square = Square.alloc();
            }
            square.x = this.chunk.getMinX() + x;
            square.y = this.chunk.getMinY() + y;
            square.z = this.z;
            square.bits = bits;
            square.cost = cost;
        }
    }

    public ChunkLevel init(Chunk chunk, int z) {
        this.chunk = chunk;
        this.z = z;
        return this;
    }

    public void clear() {
        for (int i = 0; i < 64; ++i) {
            Square square = this.squares[i % 8][i / 8];
            if (square == null) continue;
            square.release();
            this.squares[i % 8][i / 8] = null;
        }
        this.hlChunkLevel.removeFromWorld();
    }

    public HLChunkLevel getHighLevelData() {
        if (this.hlChunkLevel.modificationCount != HLAStar.modificationCount) {
            this.hlChunkLevel.modificationCount = HLAStar.modificationCount;
            this.hlChunkLevel.initRegions();
        }
        return this.hlChunkLevel;
    }

    static ChunkLevel alloc() {
        return pool.alloc();
    }

    void release() {
        pool.release(this);
    }
}

