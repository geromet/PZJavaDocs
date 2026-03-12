/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.textures.Texture;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.styles.WorldMapStyleLayer;

public class WorldMapPolygonStyleLayer
extends WorldMapStyleLayer {
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList();
    public final ArrayList<WorldMapStyleLayer.TextureStop> texture = new ArrayList();
    public final ArrayList<WorldMapStyleLayer.FloatStop> scale = new ArrayList();

    public WorldMapPolygonStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Polygon";
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args2) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args2, this.fill);
        if (fill.a < 0.01f) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            return;
        }
        float scale = this.evalFloat(args2, this.scale);
        Texture texture = this.evalTexture(args2, this.texture);
        WorldMapStyleLayer.TextureScaling scaling = this.evalTextureScaling(args2, this.texture, WorldMapStyleLayer.TextureScaling.IsoGridSquare);
        if (texture == null || !texture.isReady()) {
            args2.drawer.fillPolygon(args2, feature, fill);
        } else {
            args2.drawer.fillPolygon(args2, feature, fill, texture, scale, scaling);
        }
        WorldMapStyleLayer.RGBAf.s_pool.release(fill);
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
        renderArgs.drawer.renderVisibleCells(this);
    }
}

