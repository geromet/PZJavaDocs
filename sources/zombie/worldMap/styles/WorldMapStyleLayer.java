/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.popman.ObjectPool;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapRenderer;

public abstract class WorldMapStyleLayer {
    public String id;
    public float minZoom;
    public IWorldMapStyleFilter filter;
    public String filterKey;
    public String filterValue;

    public WorldMapStyleLayer(String id) {
        this.id = id;
    }

    public abstract String getTypeString();

    public boolean ignoreFeatures() {
        return false;
    }

    static <S extends Stop> int findStop(float zoom, ArrayList<S> stops) {
        if (stops.isEmpty()) {
            return -2;
        }
        if (zoom <= ((Stop)stops.get((int)0)).zoom) {
            return -1;
        }
        for (int i = 0; i < stops.size() - 1; ++i) {
            if (!(zoom <= ((Stop)stops.get((int)(i + 1))).zoom)) continue;
            return i;
        }
        return stops.size() - 1;
    }

    protected RGBAf evalColor(RenderArgs args2, ArrayList<ColorStop> stops) {
        return this.evalColor(args2.drawer.zoomF, stops);
    }

    public RGBAf evalColor(float zoom, ArrayList<ColorStop> stops) {
        if (stops.isEmpty()) {
            return RGBAf.s_pool.alloc().init(1.0f, 1.0f, 1.0f, 1.0f);
        }
        int stopIndex = WorldMapStyleLayer.findStop(zoom, stops);
        int stopIndex1 = stopIndex == -1 ? 0 : stopIndex;
        int stopIndex2 = PZMath.min(stopIndex + 1, stops.size() - 1);
        ColorStop stop1 = stops.get(stopIndex1);
        ColorStop stop2 = stops.get(stopIndex2);
        float zoomAlpha = stopIndex1 == stopIndex2 ? 1.0f : (PZMath.clamp(zoom, stop1.zoom, stop2.zoom) - stop1.zoom) / (stop2.zoom - stop1.zoom);
        float r = PZMath.lerp(stop1.r, stop2.r, zoomAlpha) / 255.0f;
        float g = PZMath.lerp(stop1.g, stop2.g, zoomAlpha) / 255.0f;
        float b = PZMath.lerp(stop1.b, stop2.b, zoomAlpha) / 255.0f;
        float a = PZMath.lerp(stop1.a, stop2.a, zoomAlpha) / 255.0f;
        return RGBAf.s_pool.alloc().init(r, g, b, a);
    }

    protected float evalFloat(RenderArgs args2, ArrayList<FloatStop> stops) {
        return this.evalFloat(args2, stops, 1.0f);
    }

    protected float evalFloat(RenderArgs args2, ArrayList<FloatStop> stops, float emptyValue) {
        float zoomF = args2.drawer.zoomF;
        return this.evalFloat(zoomF, stops, emptyValue);
    }

    protected float evalFloat(float zoomF, ArrayList<FloatStop> stops, float emptyValue) {
        if (stops.isEmpty()) {
            return emptyValue;
        }
        int stopIndex = WorldMapStyleLayer.findStop(zoomF, stops);
        int stopIndex1 = stopIndex == -1 ? 0 : stopIndex;
        int stopIndex2 = PZMath.min(stopIndex + 1, stops.size() - 1);
        FloatStop stop1 = stops.get(stopIndex1);
        FloatStop stop2 = stops.get(stopIndex2);
        float zoomAlpha = stopIndex1 == stopIndex2 ? 1.0f : (PZMath.clamp(zoomF, stop1.zoom, stop2.zoom) - stop1.zoom) / (stop2.zoom - stop1.zoom);
        return PZMath.lerp(stop1.f, stop2.f, zoomAlpha);
    }

    protected Texture evalTexture(RenderArgs args2, ArrayList<? extends TextureStop> stops) {
        TextureStop textureStop = this.evalTextureStop(args2, stops);
        return textureStop == null ? null : textureStop.texture;
    }

    protected TextureScaling evalTextureScaling(RenderArgs args2, ArrayList<? extends TextureStop> stops, TextureScaling defaultValue) {
        TextureStop textureStop = this.evalTextureStop(args2, stops);
        return textureStop == null ? defaultValue : textureStop.scaling;
    }

    protected TextureStop evalTextureStop(RenderArgs args2, ArrayList<? extends TextureStop> stops) {
        TextureStop stop2;
        if (stops.isEmpty()) {
            return null;
        }
        float zoomF = args2.drawer.zoomF;
        int stopIndex = WorldMapStyleLayer.findStop(zoomF, stops);
        int stopIndex1 = stopIndex == -1 ? 0 : stopIndex;
        int stopIndex2 = PZMath.min(stopIndex + 1, stops.size() - 1);
        TextureStop stop1 = stops.get(stopIndex1);
        if (stop1 == (stop2 = stops.get(stopIndex2))) {
            return zoomF < stop1.zoom ? null : stop1;
        }
        if (zoomF < stop1.zoom || zoomF > stop2.zoom) {
            return null;
        }
        float zoomAlpha = stopIndex1 == stopIndex2 ? 1.0f : (PZMath.clamp(zoomF, stop1.zoom, stop2.zoom) - stop1.zoom) / (stop2.zoom - stop1.zoom);
        return zoomAlpha < 0.5f ? stop1 : stop2;
    }

    public boolean filter(WorldMapFeature feature, FilterArgs args2) {
        if (this.filter == null) {
            return false;
        }
        return this.filter.filter(feature, args2);
    }

    public abstract void render(WorldMapFeature var1, RenderArgs var2);

    public void renderCell(RenderArgs args2) {
    }

    public abstract void renderVisibleCells(RenderArgs var1);

    public static class Stop {
        public float zoom;

        Stop(float zoom) {
            this.zoom = zoom;
        }
    }

    public static final class RenderArgs {
        public WorldMapRenderer renderer;
        public WorldMapRenderer.Drawer drawer;
        public int cellX;
        public int cellY;
    }

    public static final class RGBAf {
        public float r = 1.0f;
        public float g = 1.0f;
        public float b = 1.0f;
        public float a = 1.0f;
        public static final ObjectPool<RGBAf> s_pool = new ObjectPool<RGBAf>(RGBAf::new);

        public RGBAf init(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }
    }

    public static class ColorStop
    extends Stop {
        public int r;
        public int g;
        public int b;
        public int a;

        public ColorStop(float zoom, int r, int g, int b, int a) {
            super(zoom);
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    public static class FloatStop
    extends Stop {
        public float f;

        public FloatStop(float zoom, float f) {
            super(zoom);
            this.f = f;
        }
    }

    public static class TextureStop
    extends Stop {
        public String texturePath;
        public Texture texture;
        public TextureScaling scaling = TextureScaling.IsoGridSquare;

        public TextureStop(float zoom, String texturePath) {
            super(zoom);
            this.texturePath = texturePath;
            this.texture = Texture.getTexture(texturePath);
        }

        public TextureStop(float zoom, String texturePath, TextureScaling scaling) {
            super(zoom);
            this.texturePath = texturePath;
            this.texture = Texture.getTexture(texturePath);
            this.scaling = scaling == null ? TextureScaling.IsoGridSquare : scaling;
        }

        public TextureStop(float zoom, String texturePath, String scalingStr) {
            this(zoom, texturePath, TextureScaling.valueOf(scalingStr));
        }
    }

    public static enum TextureScaling {
        IsoGridSquare,
        ScreenPixel;

    }

    public static interface IWorldMapStyleFilter {
        public boolean filter(WorldMapFeature var1, FilterArgs var2);
    }

    public static final class FilterArgs {
        public WorldMapRenderer renderer;
    }
}

