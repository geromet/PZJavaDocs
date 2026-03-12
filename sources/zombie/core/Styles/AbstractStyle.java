/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.Styles;

import zombie.core.Styles.AlphaOp;
import zombie.core.Styles.GeometryData;
import zombie.core.Styles.Style;

public abstract class AbstractStyle
implements Style {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean getRenderSprite() {
        return false;
    }

    @Override
    public AlphaOp getAlphaOp() {
        return null;
    }

    @Override
    public int getStyleID() {
        return this.hashCode();
    }

    @Override
    public void resetState() {
    }

    @Override
    public void setupState() {
    }

    @Override
    public GeometryData build() {
        return null;
    }

    @Override
    public void render(int vertexOffset, int indexOffset) {
    }
}

