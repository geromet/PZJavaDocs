/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import java.io.File;
import java.util.ArrayList;
import zombie.worldMap.WorldMap;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapImages;
import zombie.worldMap.styles.WorldMapStyleLayer;

public class WorldMapPyramidStyleLayer
extends WorldMapStyleLayer {
    public String fileName;
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList();

    public WorldMapPyramidStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Pyramid";
    }

    @Override
    public boolean ignoreFeatures() {
        return true;
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args2) {
    }

    @Override
    public void renderCell(WorldMapStyleLayer.RenderArgs args2) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args2, this.fill);
        if (fill.a < 0.01f) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            return;
        }
        args2.drawer.drawImagePyramid(args2.cellX, args2.cellY, this.fileName, fill);
        WorldMapStyleLayer.RGBAf.s_pool.release(fill);
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
        WorldMap worldMap = renderArgs.drawer.worldMap;
        for (int i = worldMap.getImagesCount() - 1; i >= 0; --i) {
            WorldMapImages images = worldMap.getImagesByIndex(i);
            if (!images.getAbsolutePath().endsWith(File.separator + this.fileName)) continue;
            renderArgs.drawer.renderImagePyramid(images);
        }
    }

    public boolean isVisible(WorldMapStyleLayer.RenderArgs args2) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args2, this.fill);
        boolean bVisible = fill.a > 0.0f;
        WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        return bVisible;
    }

    public WorldMapStyleLayer.RGBAf getFill(WorldMapStyleLayer.RenderArgs args2) {
        return this.evalColor(args2, this.fill);
    }
}

