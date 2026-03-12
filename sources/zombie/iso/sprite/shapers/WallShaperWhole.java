/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;
import zombie.iso.sprite.shapers.WallPaddingShaper;
import zombie.iso.sprite.shapers.WallShaper;

public class WallShaperWhole
extends WallShaper {
    public static final WallShaperWhole instance = new WallShaperWhole();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        WallPaddingShaper.instance.accept(texd);
    }
}

