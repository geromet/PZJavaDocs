/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;
import zombie.iso.sprite.shapers.WallPaddingShaper;
import zombie.iso.sprite.shapers.WallShaper;

public class WallShaperSliceW
extends WallShaper {
    public static final WallShaperSliceW instance = new WallShaperSliceW();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        float slice = 7.0f;
        float frac = 7.0f / (float)texd.tex.getWidthHW();
        texd.x0 = texd.x1 - 7.0f;
        texd.x3 = texd.x2 - 7.0f;
        texd.u0 = texd.u1 - frac;
        texd.u3 = texd.u2 - frac;
        if (texd.tex1 != null) {
            frac = 7.0f / (float)texd.tex1.getWidthHW();
            texd.tex1U0 = texd.tex1U1 - frac;
            texd.tex1U3 = texd.tex1U2 - frac;
        }
        if (texd.tex2 != null) {
            frac = 7.0f / (float)texd.tex2.getWidthHW();
            texd.tex2U0 = texd.tex2U1 - frac;
            texd.tex2U3 = texd.tex2U2 - frac;
        }
        WallPaddingShaper.instance.accept(texd);
    }
}

