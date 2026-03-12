/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion;

import zombie.erosion.ErosionRegions;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.sprite.IsoSpriteManager;

public class ErosionClient {
    public static ErosionClient instance;

    public ErosionClient(IsoSpriteManager isoSpriteManager, boolean debug) {
        instance = this;
        new ErosionIceQueen(isoSpriteManager);
        ErosionRegions.init();
    }

    public static void Reset() {
        instance = null;
    }
}

