/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.regions;

import java.util.ArrayList;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;

public interface IWorldRegion {
    public ArrayList<IsoWorldRegion> getDebugConnectedNeighborCopy();

    public ArrayList<IsoWorldRegion> getNeighbors();

    public boolean isFogMask();

    public boolean isPlayerRoom();

    public boolean isFullyRoofed();

    public int getRoofCnt();

    public int getSquareSize();

    public ArrayList<IsoChunkRegion> getDebugIsoChunkRegionCopy();
}

