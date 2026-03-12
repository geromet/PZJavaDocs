/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.textures.Texture;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.styles.WorldMapStyleLayer;

public class WorldMapTextureStyleLayer
extends WorldMapStyleLayer {
    public int worldX1;
    public int worldY1;
    public int worldX2;
    public int worldY2;
    public boolean useWorldBounds;
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList();
    public final ArrayList<WorldMapStyleLayer.TextureStop> texture = new ArrayList();
    public boolean tile;

    public WorldMapTextureStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Texture";
    }

    @Override
    public boolean ignoreFeatures() {
        return true;
    }

    @Override
    public boolean filter(WorldMapFeature feature, WorldMapStyleLayer.FilterArgs args2) {
        return false;
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args2) {
    }

    @Override
    public void renderCell(WorldMapStyleLayer.RenderArgs args2) {
        if (this.useWorldBounds) {
            this.worldX1 = args2.renderer.getWorldMap().getMinXInSquares();
            this.worldY1 = args2.renderer.getWorldMap().getMinYInSquares();
            this.worldX2 = args2.renderer.getWorldMap().getMaxXInSquares() + 1;
            this.worldY2 = args2.renderer.getWorldMap().getMaxYInSquares() + 1;
        }
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args2, this.fill);
        if (fill.a < 0.01f) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            return;
        }
        Texture texture = this.evalTexture(args2, this.texture);
        if (texture == null) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            return;
        }
        if (this.tile) {
            args2.drawer.drawTextureTiled(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2, args2.cellX, args2.cellY);
        } else {
            args2.drawer.drawTexture(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2, args2.cellX, args2.cellY);
        }
        WorldMapStyleLayer.RGBAf.s_pool.release(fill);
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs args2) {
        if (this.useWorldBounds) {
            this.worldX1 = args2.renderer.getWorldMap().getMinXInSquares();
            this.worldY1 = args2.renderer.getWorldMap().getMinYInSquares();
            this.worldX2 = args2.renderer.getWorldMap().getMaxXInSquares() + 1;
            this.worldY2 = args2.renderer.getWorldMap().getMaxYInSquares() + 1;
        }
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args2, this.fill);
        if (fill.a < 0.01f) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            return;
        }
        Texture texture = this.evalTexture(args2, this.texture);
        if (texture == null) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            return;
        }
        if (this.tile) {
            args2.drawer.drawTextureTiled(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2);
        } else {
            args2.drawer.drawTexture(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2);
        }
        WorldMapStyleLayer.RGBAf.s_pool.release(fill);
    }
}

