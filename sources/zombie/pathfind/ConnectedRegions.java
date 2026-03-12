/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.HashSet;
import zombie.core.utils.BooleanGrid;
import zombie.pathfind.Chunk;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;

final class ConnectedRegions {
    PolygonalMap2 map;
    HashSet<Chunk> doneChunks = new HashSet();
    int minX;
    int minY;
    int maxX;
    int maxY;
    int minimumX;
    int minimumY;
    int width;
    int height;
    BooleanGrid visited = new BooleanGrid(this.width, this.width);
    int[] stack;
    int stackLen;
    int[] choices;
    int choicesLen;

    ConnectedRegions() {
    }

    void findAdjacentChunks(int sqx, int sqy) {
        this.doneChunks.clear();
        this.minY = Integer.MAX_VALUE;
        this.minX = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.maxX = Integer.MIN_VALUE;
        Chunk chunk = this.map.getChunkFromSquarePos(sqx, sqy);
        this.findAdjacentChunks(chunk);
    }

    void findAdjacentChunks(Chunk chunk) {
        if (chunk == null || this.doneChunks.contains(chunk)) {
            return;
        }
        this.minX = Math.min(this.minX, chunk.wx);
        this.minY = Math.min(this.minY, chunk.wy);
        this.maxX = Math.max(this.maxX, chunk.wx);
        this.maxY = Math.max(this.maxY, chunk.wy);
        this.doneChunks.add(chunk);
        Chunk w = this.map.getChunkFromChunkPos(chunk.wx - 1, chunk.wy);
        Chunk n = this.map.getChunkFromChunkPos(chunk.wx, chunk.wy - 1);
        Chunk e = this.map.getChunkFromChunkPos(chunk.wx + 1, chunk.wy);
        Chunk s = this.map.getChunkFromChunkPos(chunk.wx, chunk.wy + 1);
        this.findAdjacentChunks(w);
        this.findAdjacentChunks(n);
        this.findAdjacentChunks(e);
        this.findAdjacentChunks(s);
    }

    void floodFill(int sqx, int sqy) {
        int sq;
        this.findAdjacentChunks(sqx, sqy);
        this.minimumX = this.minX * 8;
        this.minimumY = this.minY * 8;
        this.width = (this.maxX - this.minX + 1) * 8;
        this.height = (this.maxY - this.minY + 1) * 8;
        this.visited = new BooleanGrid(this.width, this.width);
        this.stack = new int[this.width * this.width];
        this.choices = new int[this.width * this.height];
        this.stackLen = 0;
        this.choicesLen = 0;
        if (!this.push(sqx, sqy)) {
            return;
        }
        while ((sq = this.pop()) != -1) {
            int x = this.minimumX + (sq & 0xFFFF);
            int y1 = this.minimumY + (sq >> 16) & 0xFFFF;
            while (this.shouldVisit(x, y1, x, y1 - 1)) {
                --y1;
            }
            boolean spanLeft = false;
            boolean spanRight = false;
            do {
                if (!this.visit(x, y1)) {
                    return;
                }
                if (!spanLeft && this.shouldVisit(x, y1, x - 1, y1)) {
                    if (!this.push(x - 1, y1)) {
                        return;
                    }
                    spanLeft = true;
                } else if (spanLeft && !this.shouldVisit(x, y1, x - 1, y1)) {
                    spanLeft = false;
                } else if (spanLeft && !this.shouldVisit(x - 1, y1, x - 1, y1 - 1) && !this.push(x - 1, y1)) {
                    return;
                }
                if (!spanRight && this.shouldVisit(x, y1, x + 1, y1)) {
                    if (!this.push(x + 1, y1)) {
                        return;
                    }
                    spanRight = true;
                    continue;
                }
                if (spanRight && !this.shouldVisit(x, y1, x + 1, y1)) {
                    spanRight = false;
                    continue;
                }
                if (!spanRight || this.shouldVisit(x + 1, y1, x + 1, y1 - 1) || this.push(x + 1, y1)) continue;
                return;
            } while (this.shouldVisit(x, ++y1 - 1, x, y1));
        }
        System.out.println("#choices=" + this.choicesLen);
    }

    boolean shouldVisit(int x1, int y1, int x2, int y2) {
        if (x2 >= this.minimumX + this.width || x2 < this.minimumX) {
            return false;
        }
        if (y2 >= this.minimumY + this.width || y2 < this.minimumY) {
            return false;
        }
        if (this.visited.getValue(this.gridX(x2), this.gridY(y2))) {
            return false;
        }
        Square square1 = PolygonalMap2.instance.getSquare(x1, y1, 0);
        Square square2 = PolygonalMap2.instance.getSquare(x2, y2, 0);
        if (square1 == null || square2 == null) {
            return false;
        }
        return !this.isBlocked(square1, square2, false);
    }

    boolean visit(int x, int y) {
        if (this.choicesLen >= this.width * this.width) {
            return false;
        }
        this.choices[this.choicesLen++] = this.gridY(y) << 16 | (short)this.gridX(x);
        this.visited.setValue(this.gridX(x), this.gridY(y), true);
        return true;
    }

    boolean push(int x, int y) {
        if (this.stackLen >= this.width * this.width) {
            return false;
        }
        this.stack[this.stackLen++] = this.gridY(y) << 16 | (short)this.gridX(x);
        return true;
    }

    int pop() {
        return this.stackLen == 0 ? -1 : this.stack[--this.stackLen];
    }

    int gridX(int x) {
        return x - this.minimumX;
    }

    int gridY(int y) {
        return y - this.minimumY;
    }

    boolean isBlocked(Square square1, Square square2, boolean isBetween) {
        boolean diag;
        boolean colE;
        boolean testS;
        assert (Math.abs(square1.x - square2.x) <= 1);
        assert (Math.abs(square1.y - square2.y) <= 1);
        assert (square1.z == square2.z);
        assert (square1 != square2);
        boolean testW = square2.x < square1.x;
        boolean testE = square2.x > square1.x;
        boolean testN = square2.y < square1.y;
        boolean bl = testS = square2.y > square1.y;
        if (square2.isReallySolid()) {
            return true;
        }
        if (square2.y < square1.y && square1.has(64)) {
            return true;
        }
        if (square2.x < square1.x && square1.has(8)) {
            return true;
        }
        if (square2.y > square1.y && square2.x == square1.x && square2.has(64)) {
            return true;
        }
        if (square2.x > square1.x && square2.y == square1.y && square2.has(8)) {
            return true;
        }
        if (square2.x != square1.x && square2.has(448)) {
            return true;
        }
        if (square2.y != square1.y && square2.has(56)) {
            return true;
        }
        if (square2.x != square1.x && square1.has(448)) {
            return true;
        }
        if (square2.y != square1.y && square1.has(56)) {
            return true;
        }
        if (!square2.has(512) && !square2.has(504)) {
            return true;
        }
        boolean colN = testN && square1.has(4) && (square1.x != square2.x || isBetween || !square1.has(16384));
        boolean colW = testW && square1.has(2) && (square1.y != square2.y || isBetween || !square1.has(8192));
        boolean colS = testS && square2.has(4) && (square1.x != square2.x || isBetween || !square2.has(16384));
        boolean bl2 = colE = testE && square2.has(2) && (square1.y != square2.y || isBetween || !square2.has(8192));
        if (colN || colW || colS || colE) {
            return true;
        }
        boolean bl3 = diag = square2.x != square1.x && square2.y != square1.y;
        if (diag) {
            Square betweenA = PolygonalMap2.instance.getSquare(square1.x, square2.y, square1.z);
            Square betweenB = PolygonalMap2.instance.getSquare(square2.x, square1.y, square1.z);
            assert (betweenA != square1 && betweenA != square2);
            assert (betweenB != square1 && betweenB != square2);
            if (square2.x == square1.x + 1 && square2.y == square1.y + 1 && betweenA != null && betweenB != null && betweenA.has(4096) && betweenB.has(2048)) {
                return true;
            }
            if (square2.x == square1.x - 1 && square2.y == square1.y - 1 && betweenA != null && betweenB != null && betweenA.has(2048) && betweenB.has(4096)) {
                return true;
            }
            if (betweenA != null && this.isBlocked(square1, betweenA, true)) {
                return true;
            }
            if (betweenB != null && this.isBlocked(square1, betweenB, true)) {
                return true;
            }
            if (betweenA != null && this.isBlocked(square2, betweenA, true)) {
                return true;
            }
            if (betweenB != null && this.isBlocked(square2, betweenB, true)) {
                return true;
            }
        }
        return false;
    }
}

