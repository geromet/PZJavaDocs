/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.Arrays;
import zombie.pathfind.Chunk;
import zombie.pathfind.ChunkDataZ;
import zombie.util.list.PZArrayUtil;

final class ChunkData {
    final ChunkDataZ[] data = new ChunkDataZ[64];

    ChunkData() {
    }

    public ChunkDataZ init(Chunk chunk, int z) {
        if (this.data[z] == null) {
            this.data[z] = ChunkDataZ.pool.alloc();
            this.data[z].init(chunk, z);
        } else if (this.data[z].epoch != ChunkDataZ.epochCount) {
            this.data[z].clear();
            this.data[z].init(chunk, z);
        }
        return this.data[z];
    }

    public void clear() {
        PZArrayUtil.forEach(this.data, e -> {
            if (e != null) {
                e.clear();
                ChunkDataZ.pool.release((ChunkDataZ)e);
            }
        });
        Arrays.fill(this.data, null);
    }
}

