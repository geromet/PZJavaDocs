/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.Styles;

import zombie.core.Styles.AlphaOp;
import zombie.core.Styles.GeometryData;

public interface Style {
    public void setupState();

    public void resetState();

    public int getStyleID();

    public AlphaOp getAlphaOp();

    public boolean getRenderSprite();

    public GeometryData build();

    public void render(int var1, int var2);
}

