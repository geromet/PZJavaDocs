/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion;

import zombie.erosion.ErosionClient;
import zombie.erosion.ErosionMain;
import zombie.erosion.ErosionRegions;
import zombie.erosion.season.ErosionIceQueen;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.sprite.IsoSpriteManager;

public final class ErosionGlobals {
    public static final boolean EROSION_DEBUG = true;

    public static void Boot(IsoSpriteManager isoSpriteManager) {
        ErosionMain erosionMain = new ErosionMain(isoSpriteManager, true);
    }

    public static void Reset() {
        ErosionMain.Reset();
        ErosionClient.Reset();
        ErosionIceQueen.Reset();
        ErosionSeason.Reset();
        ErosionRegions.Reset();
    }
}

