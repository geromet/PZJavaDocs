/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.HashMap;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;

public final class CorpseCount {
    public static final CorpseCount instance = new CorpseCount();
    public static int maxCorpseCount = 25;

    public void chunkLoaded(IsoChunk chunk) {
        if (chunk.corpseCount == null) {
            chunk.corpseCount = new ChunkData(chunk);
        }
        chunk.corpseCount.reset();
    }

    public void corpseAdded(int x, int y, int z) {
        if (z < -32 || z > 31) {
            DebugLog.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
            return;
        }
        ChunkData chunkData = this.getChunkData(x, y);
        if (chunkData == null) {
            return;
        }
        chunkData.corpseAdded(x, y, z);
    }

    public void corpseRemoved(int x, int y, int z) {
        if (z < -32 || z > 31) {
            DebugLog.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
            return;
        }
        ChunkData chunkData = this.getChunkData(x, y);
        if (chunkData == null) {
            return;
        }
        chunkData.corpseRemoved(x, y, z);
    }

    public int getCorpseCount(IsoGameCharacter chr) {
        if (chr == null || chr.getCurrentSquare() == null) {
            return 0;
        }
        int DIM = 8;
        return this.getCorpseCount(PZMath.coorddivision(chr.getXi(), 8), PZMath.coorddivision(chr.getYi(), 8), chr.getZi(), chr.getBuilding());
    }

    public int getCorpseCount(int wx, int wy, int z, IsoBuilding building) {
        int count = 0;
        int DIM = 8;
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                Integer countObj;
                ChunkLevelData levelData;
                ChunkData chunkData = this.getChunkData((wx + dx) * 8, (wy + dy) * 8);
                if (chunkData == null || (levelData = chunkData.getLevelData(z)) == null) continue;
                if (building == null) {
                    count += levelData.corpseCount;
                } else if (levelData.buildingCorpseCount != null && (countObj = levelData.buildingCorpseCount.get(building)) != null) {
                    count += countObj.intValue();
                }
                if (count < maxCorpseCount) continue;
                return count;
            }
        }
        if (SandboxOptions.instance.zombieHealthImpact.getValue()) {
            int x = wx * 8;
            int y = wy * 8;
            int offset = 12;
            for (int dy = -12; dy <= 12; ++dy) {
                for (int dx = -12; dx <= 12; ++dx) {
                    IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(x + dx, y + dy, z);
                    if (sq == null) continue;
                    if (building == sq.getBuilding()) {
                        count += sq.getZombieCount();
                    }
                    if (count < maxCorpseCount) continue;
                    return count;
                }
            }
        }
        return count;
    }

    private ChunkData getChunkData(int x, int y) {
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(x, y, 0);
        if (chunk != null) {
            return chunk.corpseCount;
        }
        return null;
    }

    public boolean hasBuildingCorpseCount(IsoChunk chunk, int z, IsoBuilding building) {
        ChunkLevelData chunkLevelData = chunk.corpseCount.getLevelData(z);
        if (chunkLevelData instanceof ChunkLevelData) {
            ChunkLevelData levelData = chunkLevelData;
            if (levelData.buildingCorpseCount != null) {
                return levelData.buildingCorpseCount.containsKey(building);
            }
        }
        return false;
    }

    public void reset() {
    }

    public static final class ChunkData {
        private final IsoChunk chunk;

        private ChunkData(IsoChunk chunk) {
            this.chunk = chunk;
        }

        private ChunkLevelData getOrCreateLevelData(int z) {
            IsoChunkLevel levelData = this.chunk.getLevelData(z);
            if (levelData == null) {
                return null;
            }
            if (levelData.corpseCount == null) {
                levelData.corpseCount = new ChunkLevelData();
            }
            return levelData.corpseCount;
        }

        private ChunkLevelData getLevelData(int z) {
            IsoChunkLevel levelData = this.chunk.getLevelData(z);
            return levelData == null ? null : levelData.corpseCount;
        }

        private void corpseAdded(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            IsoBuilding building = square == null ? null : square.getBuilding();
            ChunkLevelData levelData = this.getOrCreateLevelData(z);
            if (levelData != null) {
                levelData.corpseAdded(building);
            }
        }

        private void corpseRemoved(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            IsoBuilding building = square == null ? null : square.getBuilding();
            ChunkLevelData levelData = this.getOrCreateLevelData(z);
            if (levelData != null) {
                levelData.corpseRemoved(building);
            }
        }

        public void removeFromWorld() {
            for (int z = this.chunk.getMinLevel(); z <= this.chunk.getMaxLevel(); ++z) {
                ChunkLevelData chunkLevelData = this.getLevelData(z);
                if (!(chunkLevelData instanceof ChunkLevelData)) continue;
                ChunkLevelData levelData = chunkLevelData;
                levelData.reset();
            }
        }

        private void reset() {
            for (int z = this.chunk.getMinLevel(); z <= this.chunk.getMaxLevel(); ++z) {
                ChunkLevelData chunkLevelData = this.getLevelData(z);
                if (!(chunkLevelData instanceof ChunkLevelData)) continue;
                ChunkLevelData levelData = chunkLevelData;
                levelData.reset();
            }
        }
    }

    public static final class ChunkLevelData {
        int corpseCount;
        HashMap<IsoBuilding, Integer> buildingCorpseCount;

        ChunkLevelData() {
        }

        void corpseAdded(IsoBuilding building) {
            if (building == null) {
                ++this.corpseCount;
            } else {
                Integer count;
                if (this.buildingCorpseCount == null) {
                    this.buildingCorpseCount = new HashMap();
                }
                if ((count = this.buildingCorpseCount.get(building)) == null) {
                    this.buildingCorpseCount.put(building, 1);
                } else {
                    this.buildingCorpseCount.put(building, count + 1);
                }
            }
        }

        void corpseRemoved(IsoBuilding building) {
            Integer count;
            if (building == null) {
                --this.corpseCount;
            } else if (this.buildingCorpseCount != null && (count = this.buildingCorpseCount.get(building)) != null) {
                if (count > 1) {
                    this.buildingCorpseCount.put(building, count - 1);
                } else {
                    this.buildingCorpseCount.remove(building);
                }
            }
        }

        void reset() {
            this.corpseCount = 0;
            if (this.buildingCorpseCount != null) {
                this.buildingCorpseCount.clear();
            }
        }
    }
}

