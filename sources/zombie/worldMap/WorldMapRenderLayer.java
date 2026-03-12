/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.util.ArrayList;
import zombie.popman.ObjectPool;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapRenderCell;
import zombie.worldMap.styles.WorldMapStyleLayer;

public final class WorldMapRenderLayer {
    WorldMapRenderCell renderCell;
    WorldMapStyleLayer styleLayer;
    final ArrayList<WorldMapFeature> features = new ArrayList();
    static ObjectPool<WorldMapRenderLayer> pool = new ObjectPool<WorldMapRenderLayer>(WorldMapRenderLayer::new);
}

