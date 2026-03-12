/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapRenderer;
import zombie.worldMap.styles.WorldMapStyleLayer;

public final class WorldMapTextStyleLayer
extends WorldMapStyleLayer {
    public UIFont font = UIFont.Handwritten;
    public int lineHeight = 40;
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList();

    public WorldMapTextStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Text";
    }

    @Override
    public boolean ignoreFeatures() {
        return true;
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args2) {
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
    }

    public UIFont getFont() {
        return this.font;
    }

    public float calculateScale(UIWorldMap ui) {
        return (float)this.lineHeight / (float)TextManager.instance.getFontHeight(this.getFont());
    }

    public float calculateScale(WorldMapRenderer.Drawer drawer) {
        return (float)this.lineHeight / (float)TextManager.instance.getFontHeight(this.getFont());
    }
}

