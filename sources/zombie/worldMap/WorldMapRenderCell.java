/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.util.ArrayList;
import zombie.popman.ObjectPool;
import zombie.worldMap.WorldMapRenderLayer;

public final class WorldMapRenderCell {
    int cellX;
    int cellY;
    final ArrayList<WorldMapRenderLayer> renderLayers = new ArrayList();
    static ObjectPool<WorldMapRenderCell> pool = new ObjectPool<WorldMapRenderCell>(WorldMapRenderCell::new);

    static WorldMapRenderCell alloc() {
        return pool.alloc();
    }
}

