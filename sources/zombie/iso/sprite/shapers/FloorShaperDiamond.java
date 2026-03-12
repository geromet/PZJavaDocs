/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;
import zombie.iso.sprite.shapers.DiamondShaper;
import zombie.iso.sprite.shapers.FloorShaper;

public class FloorShaperDiamond
extends FloorShaper {
    public static final FloorShaperDiamond instance = new FloorShaperDiamond();

    @Override
    public void accept(TextureDraw ddraw) {
        super.accept(ddraw);
        DiamondShaper.instance.accept(ddraw);
    }
}

