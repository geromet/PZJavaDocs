/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;
import zombie.iso.sprite.shapers.WallPaddingShaper;
import zombie.iso.sprite.shapers.WallShaper;

public class WallShaperSliceN
extends WallShaper {
    public static final WallShaperSliceN instance = new WallShaperSliceN();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        float slice = 5.0f;
        float frac = 5.0f / (float)texd.tex.getWidthHW();
        texd.x1 = texd.x0 + 5.0f;
        texd.x2 = texd.x3 + 5.0f;
        texd.u1 = texd.u0 + frac;
        texd.u2 = texd.u3 + frac;
        if (texd.tex1 != null) {
            frac = 5.0f / (float)texd.tex1.getWidthHW();
            texd.tex1U1 = texd.tex1U0 + frac;
            texd.tex1U2 = texd.tex1U3 + frac;
        }
        if (texd.tex2 != null) {
            frac = 5.0f / (float)texd.tex2.getWidthHW();
            texd.tex2U1 = texd.tex2U0 + frac;
            texd.tex2U2 = texd.tex2U3 + frac;
        }
        WallPaddingShaper.instance.accept(texd);
    }
}

