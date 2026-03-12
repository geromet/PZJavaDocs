/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.Styles;

import org.lwjgl.opengl.GL14;
import zombie.core.Styles.AbstractStyle;
import zombie.core.Styles.AlphaOp;

public final class UIFBOStyle
extends AbstractStyle {
    public static final UIFBOStyle instance = new UIFBOStyle();

    @Override
    public void setupState() {
        GL14.glBlendFuncSeparate(770, 771, 1, 771);
    }

    @Override
    public AlphaOp getAlphaOp() {
        return AlphaOp.KEEP;
    }

    @Override
    public int getStyleID() {
        return 1;
    }

    @Override
    public boolean getRenderSprite() {
        return true;
    }
}

