/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoLot;
import zombie.iso.MapFiles;

public final class LotHeader {
    public static final int VERSION0 = 0;
    public static final int VERSION1 = 1;
    public static final int VERSION_LATEST = 1;
    public static final byte[] LOTHEADER_MAGIC = new byte[]{76, 79, 84, 72};
    public static final byte[] LOTPACK_MAGIC = new byte[]{76, 79, 84, 80};
    public final int cellX;
    public final int cellY;
    public int width;
    public int height;
    public int minLevel;
    public int maxLevel;
    public int version;
    public boolean fixed2x;
    public final ArrayList<String> tilesUsed = new ArrayList();
    private final byte[] zombieIntensity = new byte[1024];
    public MapFiles mapFiles;
    public String fileName;
    public String absoluteFilePath;
    public final boolean[] adjacentCells = new boolean[8];

    public LotHeader(int cellX, int cellY) {
        this.cellX = cellX;
        this.cellY = cellY;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    public int getMinLevel() {
        return this.minLevel;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public int getNumLevels() {
        return this.getMaxLevel() - this.getMinLevel() + 1;
    }

    public byte[] getZombieIntensity() {
        return this.zombieIntensity;
    }

    public byte getZombieIntensity(int i) {
        return this.zombieIntensity[i];
    }

    public void setZombieIntensity(int i, byte zombieIntensity) {
        this.zombieIntensity[i] = zombieIntensity;
    }

    public static int getZombieIntensityForChunk(LotHeader lotHeader, int chunkX, int chunkY) {
        if (chunkX < 0 || chunkY < 0 || chunkX >= 32 || chunkY >= 32) {
            return -1;
        }
        if (lotHeader == null) {
            return -1;
        }
        for (int j = lotHeader.mapFiles.priority; j < IsoLot.MapFiles.size(); ++j) {
            int cell300Y;
            MapFiles mapFiles = IsoLot.MapFiles.get(j);
            int cell300X = PZMath.fastfloor((float)(lotHeader.cellX * 256 + chunkX * 8) / 300.0f);
            if (!mapFiles.bgHasCell300.getValue(cell300X - mapFiles.minCell300X, (cell300Y = PZMath.fastfloor((float)(lotHeader.cellY * 256 + chunkY * 8) / 300.0f)) - mapFiles.minCell300Y)) continue;
            LotHeader lotHeader2 = mapFiles.getLotHeader(lotHeader.cellX, lotHeader.cellY);
            return lotHeader2.getZombieIntensity(chunkX + chunkY * 32) & 0xFF;
        }
        return -1;
    }

    public void Dispose() {
        this.tilesUsed.clear();
    }
}

