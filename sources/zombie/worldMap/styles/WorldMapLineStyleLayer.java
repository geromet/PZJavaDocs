/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.styles.WorldMapStyleLayer;

public class WorldMapLineStyleLayer
extends WorldMapStyleLayer {
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList();
    public final ArrayList<WorldMapStyleLayer.FloatStop> lineWidth = new ArrayList();

    public WorldMapLineStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Line";
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args2) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args2, this.fill);
        if (fill.a < 0.01f) {
            return;
        }
        float lineWidth = feature.properties.containsKey("width") ? PZMath.tryParseFloat((String)feature.properties.get("width"), 1.0f) * args2.drawer.getWorldScale() : this.evalFloat(args2, this.lineWidth);
        args2.drawer.drawLineString(args2, feature, fill, lineWidth);
        WorldMapStyleLayer.RGBAf.s_pool.release(fill);
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
        renderArgs.drawer.renderVisibleCells(this);
    }
}

