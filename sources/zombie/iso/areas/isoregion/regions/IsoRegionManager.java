/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.regions;

import java.util.ArrayDeque;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;

public final class IsoRegionManager {
    private final ArrayDeque<IsoWorldRegion> poolIsoWorldRegion = new ArrayDeque();
    private final ArrayDeque<IsoChunkRegion> poolIsoChunkRegion = new ArrayDeque();
    private final DataRoot dataRoot;
    private final ArrayDeque<Integer> regionIdStack = new ArrayDeque();
    private int nextId;
    private int colorIndex;
    private int worldRegionCount;
    private int chunkRegionCount;

    public IsoRegionManager(DataRoot dataRoot) {
        this.dataRoot = dataRoot;
    }

    public IsoWorldRegion allocIsoWorldRegion() {
        int n;
        IsoWorldRegion region;
        IsoWorldRegion isoWorldRegion = region = !this.poolIsoWorldRegion.isEmpty() ? this.poolIsoWorldRegion.pop() : new IsoWorldRegion(this);
        if (this.regionIdStack.isEmpty()) {
            int n2 = this.nextId;
            n = n2;
            this.nextId = n2 + 1;
        } else {
            n = this.regionIdStack.pop();
        }
        int id = n;
        region.init(id);
        ++this.worldRegionCount;
        return region;
    }

    public void releaseIsoWorldRegion(IsoWorldRegion worldRegion) {
        this.dataRoot.DequeueDirtyIsoWorldRegion(worldRegion);
        if (!worldRegion.isInPool()) {
            this.regionIdStack.push(worldRegion.getID());
            worldRegion.reset();
            this.poolIsoWorldRegion.push(worldRegion);
            --this.worldRegionCount;
        } else {
            IsoRegions.warn("IsoRegionManager -> Trying to release a MasterRegion twice.");
        }
    }

    public IsoChunkRegion allocIsoChunkRegion(DataChunk dataChunk, int zLayer) {
        int n;
        IsoChunkRegion region;
        IsoChunkRegion isoChunkRegion = region = !this.poolIsoChunkRegion.isEmpty() ? this.poolIsoChunkRegion.pop() : new IsoChunkRegion(this);
        if (this.regionIdStack.isEmpty()) {
            int n2 = this.nextId;
            n = n2;
            this.nextId = n2 + 1;
        } else {
            n = this.regionIdStack.pop();
        }
        int id = n;
        region.init(id, dataChunk, zLayer);
        ++this.chunkRegionCount;
        return region;
    }

    public void releaseIsoChunkRegion(IsoChunkRegion chunkRegion) {
        if (!chunkRegion.isInPool()) {
            this.regionIdStack.push(chunkRegion.getID());
            chunkRegion.reset();
            this.poolIsoChunkRegion.push(chunkRegion);
            --this.chunkRegionCount;
        } else {
            IsoRegions.warn("IsoRegionManager -> Trying to release a ChunkRegion twice.");
        }
    }

    public Color getColor() {
        Color col = Colors.GetColorFromIndex(this.colorIndex++);
        if (this.colorIndex >= Colors.GetColorsCount()) {
            this.colorIndex = 0;
        }
        return col;
    }

    public int getWorldRegionCount() {
        return this.worldRegionCount;
    }

    public int getChunkRegionCount() {
        return this.chunkRegionCount;
    }
}

