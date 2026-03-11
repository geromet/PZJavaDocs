/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureID;
import zombie.iso.IsoCamera;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.MapFiles;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.zones.Zone;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;
import zombie.worldMap.ImagePyramid;
import zombie.worldMap.MapProjection;
import zombie.worldMap.Rasterize;
import zombie.worldMap.StrokeGeometry;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMap;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapData;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapGeometry;
import zombie.worldMap.WorldMapImages;
import zombie.worldMap.WorldMapPoints;
import zombie.worldMap.WorldMapRenderCell;
import zombie.worldMap.WorldMapRenderLayer;
import zombie.worldMap.WorldMapVBOs;
import zombie.worldMap.WorldMapVisited;
import zombie.worldMap.streets.StreetRenderData;
import zombie.worldMap.styles.WorldMapPyramidStyleLayer;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapTextureStyleLayer;
import zombie.worldMap.symbols.SymbolsLayoutData;
import zombie.worldMap.symbols.SymbolsRenderData;

public final class WorldMapRenderer {
    private WorldMap worldMap;
    private int x;
    private int y;
    private int width;
    private int height;
    private int zoom;
    private float zoomF;
    private float displayZoomF;
    private float centerWorldX;
    private float centerWorldY;
    private float zoomUiX;
    private float zoomUiY;
    private float zoomWorldX;
    private float zoomWorldY;
    private boolean transitionTo;
    private float transitionFromWorldX;
    private float transitionFromWorldY;
    private float transitionFromZoomF;
    private float transitionToWorldX;
    private float transitionToWorldY;
    private float transitionToZoomF;
    private long transitionToStartMs;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f modelViewProjection = new Matrix4f();
    private final Quaternionf modelViewChange = new Quaternionf();
    private long viewChangeTime;
    private static final long VIEW_CHANGE_TIME = 175L;
    private boolean isIsometric;
    private boolean firstUpdate;
    private WorldMapVisited visited;
    private final Drawer[] drawer = new Drawer[3];
    private final CharacterModelCamera characterModelCamera = new CharacterModelCamera();
    private int dropShadowWidth = 12;
    public WorldMapStyle style;
    protected static VBORenderer vboLines;
    private float maxZoom = 18.0f;
    private final int[] viewport = new int[]{0, 0, 0, 0};
    private static final ThreadLocal<ObjectPool<UI3DScene.Plane>> TL_Plane_pool;
    private static final ThreadLocal<ObjectPool<UI3DScene.Ray>> TL_Ray_pool;
    static final float SMALL_NUM = 1.0E-8f;
    private final ArrayList<ConfigOption> options = new ArrayList();
    private final WorldMapBooleanOption allPrintMedia = new WorldMapBooleanOption(this, "AllPrintMedia", false);
    private final WorldMapBooleanOption allStashMaps = new WorldMapBooleanOption(this, "AllStashMaps", false);
    private final WorldMapBooleanOption animals = new WorldMapBooleanOption(this, "Animals", false);
    private final WorldMapBooleanOption animalTracks = new WorldMapBooleanOption(this, "AnimalTracks", false);
    private final WorldMapBooleanOption basements = new WorldMapBooleanOption(this, "Basements", false);
    private final WorldMapBooleanOption blurUnvisited = new WorldMapBooleanOption(this, "BlurUnvisited", true);
    private final WorldMapBooleanOption buildingsWithoutFeatures = new WorldMapBooleanOption(this, "BuildingsWithoutFeatures", false);
    private final WorldMapBooleanOption cellGrid = new WorldMapBooleanOption(this, "CellGrid", false);
    private final WorldMapBooleanOption clampBaseZoomToPoint5 = new WorldMapBooleanOption(this, "ClampBaseZoomToPoint5", true);
    private final WorldMapBooleanOption colorblindPatterns = new WorldMapBooleanOption(this, "ColorblindPatterns", false);
    private final WorldMapBooleanOption debugInfo = new WorldMapBooleanOption(this, "DebugInfo", false);
    private final WorldMapBooleanOption placeNames = new WorldMapBooleanOption(this, "PlaceNames", true);
    private final WorldMapBooleanOption tileGrid = new WorldMapBooleanOption(this, "TileGrid", false);
    private final WorldMapBooleanOption unvisitedGrid = new WorldMapBooleanOption(this, "UnvisitedGrid", true);
    private final WorldMapBooleanOption features = new WorldMapBooleanOption(this, "Features", true);
    private final WorldMapBooleanOption otherZones = new WorldMapBooleanOption(this, "OtherZones", false);
    private final WorldMapBooleanOption forestZones = new WorldMapBooleanOption(this, "ForagingZones", false);
    private final WorldMapBooleanOption zombieIntensity = new WorldMapBooleanOption(this, "ZombieIntensity", false);
    private final WorldMapBooleanOption zombieVoronoi = new WorldMapBooleanOption(this, "ZombieVoronoi", false);
    private final WorldMapBooleanOption zombieCutoff = new WorldMapBooleanOption(this, "ZombieCutoff", false);
    private final WorldMapBooleanOption hideUnvisited = new WorldMapBooleanOption(this, "HideUnvisited", false);
    private final WorldMapBooleanOption hitTest = new WorldMapBooleanOption(this, "HitTest", false);
    private final WorldMapBooleanOption highlightStreet = new WorldMapBooleanOption(this, "HighlightStreet", true);
    private final WorldMapBooleanOption largeStreetLabel = new WorldMapBooleanOption(this, "LargeStreetLabel", true);
    private final WorldMapBooleanOption outlineStreet = new WorldMapBooleanOption(this, "OutlineStreets", false);
    private final WorldMapBooleanOption showStreetNames = new WorldMapBooleanOption(this, "ShowStreetNames", true);
    private final WorldMapBooleanOption imagePyramid = new WorldMapBooleanOption(this, "ImagePyramid", true);
    private final WorldMapBooleanOption terrainImage = new WorldMapBooleanOption(this, "TerrainImage", false);
    private final WorldMapBooleanOption isometric = new WorldMapBooleanOption(this, "Isometric", true);
    private final WorldMapBooleanOption lineString = new WorldMapBooleanOption(this, "LineString", true);
    private final WorldMapBooleanOption players = new WorldMapBooleanOption(this, "Players", false);
    private final WorldMapBooleanOption playerModel = new WorldMapBooleanOption(this, "PlayerModel", false);
    private final WorldMapBooleanOption remotePlayers = new WorldMapBooleanOption(this, "RemotePlayers", false);
    private final WorldMapBooleanOption playerNames = new WorldMapBooleanOption(this, "PlayerNames", false);
    private final WorldMapBooleanOption symbols = new WorldMapBooleanOption(this, "Symbols", true);
    private final WorldMapBooleanOption wireframe = new WorldMapBooleanOption(this, "Wireframe", false);
    private final WorldMapBooleanOption worldBounds = new WorldMapBooleanOption(this, "WorldBounds", true);
    private final WorldMapBooleanOption miniMapSymbols = new WorldMapBooleanOption(this, "MiniMapSymbols", false);
    private final WorldMapBooleanOption visibleCells = new WorldMapBooleanOption(this, "VisibleCells", false);
    private final WorldMapBooleanOption visibleTiles = new WorldMapBooleanOption(this, "VisibleTiles", false);
    private final WorldMapBooleanOption dimUnsharedSymbols = new WorldMapBooleanOption(this, "DimUnsharedSymbols", false);
    private final WorldMapBooleanOption storyZones = new WorldMapBooleanOption(this, "StoryZones", false);
    private final WorldMapBooleanOption wgRoads = new WorldMapBooleanOption(this, "WGRoads", false);
    private final WorldMapBooleanOption infiniteZoom = new WorldMapBooleanOption(this, "InfiniteZoom", false);

    public WorldMapRenderer() {
        PZArrayUtil.arrayPopulate(this.drawer, Drawer::new);
    }

    public int getAbsoluteX() {
        return this.x;
    }

    public int getAbsoluteY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    private void calcMatrices(float centerWorldX, float centerWorldY, float zoomF, Matrix4f projection, Matrix4f modelView) {
        int w = this.getWidth();
        int h = this.getHeight();
        projection.setOrtho((float)(-w) / 2.0f, (float)w / 2.0f, (float)h / 2.0f, (float)(-h) / 2.0f, -2000.0f, 2000.0f);
        modelView.identity();
        if (this.isometric.getValue()) {
            modelView.rotateXYZ(1.0471976f, 0.0f, 0.7853982f);
        }
    }

    public Vector3f uiToScene(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, Vector3f out) {
        UI3DScene.Plane plane = WorldMapRenderer.allocPlane();
        plane.point.set(0.0f);
        plane.normal.set(0.0f, 0.0f, 1.0f);
        UI3DScene.Ray cameraRay = this.getCameraRay(uiX, (float)this.getHeight() - uiY, projection, modelView, WorldMapRenderer.allocRay());
        if (this.intersect_ray_plane(plane, cameraRay, out) != 1) {
            out.set(0.0f);
        }
        WorldMapRenderer.releasePlane(plane);
        WorldMapRenderer.releaseRay(cameraRay);
        return out;
    }

    public Vector3f uiToScene(float uiX, float uiY, Matrix4f modelViewProjection, Vector3f out) {
        UI3DScene.Plane plane = WorldMapRenderer.allocPlane();
        plane.point.set(0.0f);
        plane.normal.set(0.0f, 0.0f, 1.0f);
        UI3DScene.Ray cameraRay = this.getCameraRay(uiX, (float)this.getHeight() - uiY, modelViewProjection, WorldMapRenderer.allocRay());
        if (this.intersect_ray_plane(plane, cameraRay, out) != 1) {
            out.set(0.0f);
        }
        WorldMapRenderer.releasePlane(plane);
        WorldMapRenderer.releaseRay(cameraRay);
        return out;
    }

    public Vector3f sceneToUI(float sceneX, float sceneY, float sceneZ, Matrix4f modelViewProjection, Vector3f out) {
        this.viewport[0] = 0;
        this.viewport[1] = 0;
        this.viewport[2] = this.getWidth();
        this.viewport[3] = this.getHeight();
        modelViewProjection.project(sceneX, sceneY, sceneZ, this.viewport, out);
        return out;
    }

    public Vector3f sceneToUI(float sceneX, float sceneY, float sceneZ, Matrix4f projection, Matrix4f modelView, Vector3f out) {
        Matrix4f matrix4f = WorldMapRenderer.allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        this.sceneToUI(sceneX, sceneY, sceneZ, matrix4f, out);
        WorldMapRenderer.releaseMatrix4f(matrix4f);
        return out;
    }

    public float uiToWorldX(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY) {
        Matrix4f projection = WorldMapRenderer.allocMatrix4f();
        Matrix4f modelView = WorldMapRenderer.allocMatrix4f();
        this.calcMatrices(centerWorldX, centerWorldY, zoomF, projection, modelView);
        float x = this.uiToWorldX(uiX, uiY, zoomF, centerWorldX, centerWorldY, projection, modelView);
        WorldMapRenderer.releaseMatrix4f(projection);
        WorldMapRenderer.releaseMatrix4f(modelView);
        return x;
    }

    public float uiToWorldY(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY) {
        Matrix4f projection = WorldMapRenderer.allocMatrix4f();
        Matrix4f modelView = WorldMapRenderer.allocMatrix4f();
        this.calcMatrices(centerWorldX, centerWorldY, zoomF, projection, modelView);
        float y = this.uiToWorldY(uiX, uiY, zoomF, centerWorldX, centerWorldY, projection, modelView);
        WorldMapRenderer.releaseMatrix4f(projection);
        WorldMapRenderer.releaseMatrix4f(modelView);
        return y;
    }

    public float uiToWorldX(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, projection, modelView, WorldMapRenderer.allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0f / worldScale);
        float worldX = worldPos.x() + centerWorldX;
        WorldMapRenderer.releaseVector3f(worldPos);
        return worldX;
    }

    public float uiToWorldY(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, projection, modelView, WorldMapRenderer.allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0f / worldScale);
        float worldY = worldPos.y() + centerWorldY;
        WorldMapRenderer.releaseVector3f(worldPos);
        return worldY;
    }

    public float uiToWorldX(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, modelViewProjection, WorldMapRenderer.allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0f / worldScale);
        float worldX = worldPos.x() + centerWorldX;
        WorldMapRenderer.releaseVector3f(worldPos);
        return worldX;
    }

    public float uiToWorldY(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, modelViewProjection, WorldMapRenderer.allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0f / worldScale);
        float worldY = worldPos.y() + centerWorldY;
        WorldMapRenderer.releaseVector3f(worldPos);
        return worldY;
    }

    public float worldToUIX(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI((worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0f, projection, modelView, WorldMapRenderer.allocVector3f());
        float uiX = uiPos.x();
        WorldMapRenderer.releaseVector3f(uiPos);
        return uiX;
    }

    public float worldToUIY(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI((worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0f, projection, modelView, WorldMapRenderer.allocVector3f());
        float uiY = (float)this.getHeight() - uiPos.y();
        WorldMapRenderer.releaseVector3f(uiPos);
        return uiY;
    }

    public float worldToUIX(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI((worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0f, modelViewProjection, WorldMapRenderer.allocVector3f());
        float uiX = uiPos.x();
        WorldMapRenderer.releaseVector3f(uiPos);
        return uiX;
    }

    public float worldToUIY(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI((worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0f, modelViewProjection, WorldMapRenderer.allocVector3f());
        float uiY = (float)this.getHeight() - uiPos.y();
        WorldMapRenderer.releaseVector3f(uiPos);
        return uiY;
    }

    public float worldOriginUIX(float zoomF, float centerWorldX) {
        return this.worldToUIX(0.0f, 0.0f, zoomF, centerWorldX, this.centerWorldY, this.modelViewProjection);
    }

    public float worldOriginUIY(float zoomF, float centerWorldY) {
        return this.worldToUIY(0.0f, 0.0f, zoomF, this.centerWorldX, centerWorldY, this.modelViewProjection);
    }

    public int getZoom() {
        return this.zoom;
    }

    public float getZoomF() {
        return this.zoomF;
    }

    public float getDisplayZoomF() {
        return this.displayZoomF;
    }

    public float zoomMult() {
        return this.zoomMult(this.zoomF);
    }

    public float zoomMult(float zoomF) {
        return (float)Math.pow(2.0, zoomF);
    }

    public float getWorldScale(float zoomF) {
        int tileSize = this.getHeight();
        double metersPerPixel = MapProjection.metersPerPixelAtZoom(zoomF, tileSize);
        return (float)(1.0 / metersPerPixel);
    }

    public void zoomAt(int mouseX, int mouseY, int delta) {
        float worldX = this.uiToWorldX(mouseX, mouseY, this.displayZoomF, this.centerWorldX, this.centerWorldY);
        float worldY = this.uiToWorldY(mouseX, mouseY, this.displayZoomF, this.centerWorldX, this.centerWorldY);
        this.zoomF = PZMath.clamp(this.zoomF + (float)delta / 2.0f, this.getBaseZoom(), this.getMaxZoom());
        this.zoom = (int)this.zoomF;
        this.zoomWorldX = worldX;
        this.zoomWorldY = worldY;
        this.zoomUiX = mouseX;
        this.zoomUiY = mouseY;
    }

    public void transitionTo(float worldX, float worldY, float zoomF) {
        this.transitionFromWorldX = this.centerWorldX;
        this.transitionFromWorldY = this.centerWorldY;
        this.transitionFromZoomF = this.displayZoomF;
        this.transitionToWorldX = worldX;
        this.transitionToWorldY = worldY;
        this.transitionToZoomF = PZMath.clamp(zoomF, this.getBaseZoom(), this.getMaxZoom());
        this.transitionTo = true;
        this.transitionToStartMs = System.currentTimeMillis();
    }

    public float getCenterWorldX() {
        return this.centerWorldX;
    }

    public float getCenterWorldY() {
        return this.centerWorldY;
    }

    public void centerOn(float worldX, float worldY) {
        this.centerWorldX = worldX;
        this.centerWorldY = worldY;
        if (this.displayZoomF != this.zoomF) {
            this.zoomWorldX = worldX;
            this.zoomWorldY = worldY;
            this.zoomUiX = (float)this.width / 2.0f;
            this.zoomUiY = (float)this.height / 2.0f;
        }
    }

    public void moveView(int dx, int dy) {
        this.centerOn(this.centerWorldX + (float)dx, this.centerWorldY + (float)dy);
    }

    public double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    public float getBaseZoom() {
        double zoom = MapProjection.zoomAtMetersPerPixel((double)this.worldMap.getHeightInSquares() / (double)this.getHeight(), this.getHeight());
        if ((float)this.worldMap.getWidthInSquares() * this.getWorldScale((float)zoom) > (float)this.getWidth()) {
            zoom = MapProjection.zoomAtMetersPerPixel((double)this.worldMap.getWidthInSquares() / (double)this.getWidth(), this.getHeight());
        }
        if (this.clampBaseZoomToPoint5.getValue()) {
            zoom = (double)((int)(zoom * 2.0)) / 2.0;
        }
        return PZMath.max((float)zoom, this.infiniteZoom.getValue() ? (float)zoom : 12.0f);
    }

    public void setZoom(float zoom) {
        this.zoomF = PZMath.clamp(zoom, this.getBaseZoom(), this.getMaxZoom());
        this.zoom = (int)this.zoomF;
        this.displayZoomF = this.zoomF;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = PZMath.clamp(maxZoom, 0.0f, 24.0f);
    }

    public float getMaxZoom() {
        return this.infiniteZoom.getValue() ? 24.0f : this.maxZoom;
    }

    public void resetView() {
        this.zoomF = this.getWidth() * this.getHeight() < 1 ? 12.0f : this.getBaseZoom();
        this.zoom = (int)this.zoomF;
        this.centerWorldX = (float)this.worldMap.getMinXInSquares() + (float)this.worldMap.getWidthInSquares() / 2.0f;
        this.centerWorldY = (float)this.worldMap.getMinYInSquares() + (float)this.worldMap.getHeightInSquares() / 2.0f;
        this.zoomWorldX = this.centerWorldX;
        this.zoomWorldY = this.centerWorldY;
        this.zoomUiX = (float)this.getWidth() / 2.0f;
        this.zoomUiY = (float)this.getHeight() / 2.0f;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projection;
    }

    public Matrix4f getModelViewMatrix() {
        return this.modelView;
    }

    public Matrix4f getModelViewProjectionMatrix() {
        return this.modelViewProjection;
    }

    public void setMap(WorldMap worldMap, int x, int y, int width, int height) {
        this.worldMap = worldMap;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public WorldMap getWorldMap() {
        return this.worldMap;
    }

    public void setVisited(WorldMapVisited visited) {
        this.visited = visited;
    }

    public WorldMapVisited getVisited() {
        return this.visited;
    }

    public void updateView() {
        float f;
        if (this.worldMap == null || this.worldMap.getWidthInSquares() <= 1 || this.worldMap.getHeightInSquares() <= 1 || this.getWidth() < 1 || this.getHeight() < 1) {
            return;
        }
        if (this.displayZoomF == 0.0f) {
            this.displayZoomF = this.zoomF;
        }
        if (this.displayZoomF != this.zoomF) {
            float mult;
            float dt = (float)(UIManager.getMillisSinceLastRender() / 750.0);
            float diff = Math.abs(this.zoomF - this.displayZoomF);
            float f2 = mult = diff > 0.25f ? diff / 0.25f : 1.0f;
            if (this.displayZoomF < this.zoomF) {
                this.displayZoomF = PZMath.min(this.displayZoomF + dt * mult, this.zoomF);
            } else if (this.displayZoomF > this.zoomF) {
                this.displayZoomF = PZMath.max(this.displayZoomF - dt * mult, this.zoomF);
            }
            float worldX2 = this.uiToWorldX(this.zoomUiX, this.zoomUiY, this.displayZoomF, 0.0f, 0.0f);
            float worldY2 = this.uiToWorldY(this.zoomUiX, this.zoomUiY, this.displayZoomF, 0.0f, 0.0f);
            this.centerWorldX = this.zoomWorldX - worldX2;
            this.centerWorldY = this.zoomWorldY - worldY2;
        }
        if (!this.firstUpdate) {
            this.firstUpdate = true;
            this.isIsometric = this.isometric.getValue();
        }
        if (this.isIsometric != this.isometric.getValue()) {
            this.isIsometric = this.isometric.getValue();
            long ms = System.currentTimeMillis();
            if (this.viewChangeTime + 175L < ms) {
                this.modelViewChange.setFromUnnormalized(this.modelView);
            }
            this.viewChangeTime = ms;
        }
        if (this.transitionTo) {
            long elapsed = System.currentTimeMillis() - this.transitionToStartMs;
            if (elapsed >= 600L) {
                this.transitionTo = false;
                this.centerWorldX = this.transitionToWorldX;
                this.centerWorldY = this.transitionToWorldY;
                this.zoomF = this.transitionToZoomF;
                this.zoom = (int)PZMath.floor(this.zoomF);
                this.displayZoomF = this.zoomF;
            } else {
                f = (float)elapsed / 600.0f;
                f = PZMath.lerpFunc_EaseOutInQuad(f);
                this.centerWorldX = PZMath.lerp(this.transitionFromWorldX, this.transitionToWorldX, f);
                this.centerWorldY = PZMath.lerp(this.transitionFromWorldY, this.transitionToWorldY, f);
                this.zoomF = PZMath.lerp(this.transitionFromZoomF, this.transitionToZoomF, f);
                this.zoom = (int)PZMath.floor(this.zoomF);
                this.displayZoomF = this.zoomF;
            }
        }
        this.calcMatrices(this.centerWorldX, this.centerWorldY, this.displayZoomF, this.projection, this.modelView);
        long ms = System.currentTimeMillis();
        if (this.viewChangeTime + 175L > ms) {
            f = (float)(this.viewChangeTime + 175L - ms) / 175.0f;
            Quaternionf q1 = WorldMapRenderer.allocQuaternionf().set(this.modelViewChange);
            Quaternionf q2 = WorldMapRenderer.allocQuaternionf().setFromUnnormalized(this.modelView);
            this.modelView.set(q1.slerp(q2, 1.0f - f));
            WorldMapRenderer.releaseQuaternionf(q1);
            WorldMapRenderer.releaseQuaternionf(q2);
        }
        this.modelViewProjection.set(this.projection).mul(this.modelView);
    }

    public void render(UIWorldMap ui) {
        vboLines = VBORenderer.getInstance();
        this.style = ui.getAPI().getStyle();
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.drawer[stateIndex].init(this, ui);
        SpriteRenderer.instance.drawGeneric(this.drawer[stateIndex]);
    }

    public void setDropShadowWidth(int width) {
        this.dropShadowWidth = width;
    }

    private static Matrix4f allocMatrix4f() {
        return (Matrix4f)BaseVehicle.TL_matrix4f_pool.get().alloc();
    }

    private static void releaseMatrix4f(Matrix4f matrix) {
        BaseVehicle.TL_matrix4f_pool.get().release(matrix);
    }

    private static Quaternionf allocQuaternionf() {
        return (Quaternionf)BaseVehicle.TL_quaternionf_pool.get().alloc();
    }

    private static void releaseQuaternionf(Quaternionf q) {
        BaseVehicle.TL_quaternionf_pool.get().release(q);
    }

    private static UI3DScene.Ray allocRay() {
        return TL_Ray_pool.get().alloc();
    }

    private static void releaseRay(UI3DScene.Ray ray) {
        TL_Ray_pool.get().release(ray);
    }

    private static UI3DScene.Plane allocPlane() {
        return TL_Plane_pool.get().alloc();
    }

    private static void releasePlane(UI3DScene.Plane plane) {
        TL_Plane_pool.get().release(plane);
    }

    private static Vector2 allocVector2() {
        return (Vector2)Vector2ObjectPool.get().alloc();
    }

    private static void releaseVector2(Vector2 vector2) {
        Vector2ObjectPool.get().release(vector2);
    }

    private static Vector3f allocVector3f() {
        return (Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc();
    }

    private static void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.TL_vector3f_pool.get().release(vector3f);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, UI3DScene.Ray cameraRay) {
        return this.getCameraRay(uiX, uiY, this.modelViewProjection, cameraRay);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, UI3DScene.Ray cameraRay) {
        Matrix4f matrix4f = WorldMapRenderer.allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        this.getCameraRay(uiX, uiY, matrix4f, cameraRay);
        WorldMapRenderer.releaseMatrix4f(matrix4f);
        return cameraRay;
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f modelViewProjection, UI3DScene.Ray cameraRay) {
        Matrix4f matrix4f = WorldMapRenderer.allocMatrix4f().set(modelViewProjection);
        matrix4f.invert();
        this.viewport[0] = 0;
        this.viewport[1] = 0;
        this.viewport[2] = this.getWidth();
        this.viewport[3] = this.getHeight();
        Vector3f rayStart = matrix4f.unprojectInv(uiX, uiY, 0.0f, this.viewport, WorldMapRenderer.allocVector3f());
        Vector3f rayEnd = matrix4f.unprojectInv(uiX, uiY, 1.0f, this.viewport, WorldMapRenderer.allocVector3f());
        cameraRay.origin.set(rayStart);
        cameraRay.direction.set(rayEnd.sub(rayStart).normalize());
        WorldMapRenderer.releaseVector3f(rayEnd);
        WorldMapRenderer.releaseVector3f(rayStart);
        WorldMapRenderer.releaseMatrix4f(matrix4f);
        return cameraRay;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    int intersect_ray_plane(UI3DScene.Plane pn, UI3DScene.Ray s, Vector3f out) {
        Vector3f u = WorldMapRenderer.allocVector3f().set(s.direction).mul(10000.0f);
        Vector3f w = WorldMapRenderer.allocVector3f().set(s.origin).sub(pn.point);
        try {
            float d = pn.normal.dot(u);
            float n = -pn.normal.dot(w);
            if (Math.abs(d) < 1.0E-8f) {
                if (n == 0.0f) {
                    int n2 = 2;
                    return n2;
                }
                int n3 = 0;
                return n3;
            }
            float sI = n / d;
            if (sI < 0.0f || sI > 1.0f) {
                int n4 = 0;
                return n4;
            }
            out.set(s.origin).add(u.mul(sI));
            int n5 = 1;
            return n5;
        }
        finally {
            WorldMapRenderer.releaseVector3f(u);
            WorldMapRenderer.releaseVector3f(w);
        }
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); ++i) {
            ConfigOption setting = this.options.get(i);
            if (!setting.getName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            return booleanConfigOption.getValue();
        }
        return false;
    }

    public void setDouble(String name, double value) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof DoubleConfigOption) {
            DoubleConfigOption doubleConfigOption = (DoubleConfigOption)setting;
            doubleConfigOption.setValue(value);
        }
    }

    public double getDouble(String name, double defaultValue) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof DoubleConfigOption) {
            DoubleConfigOption doubleConfigOption = (DoubleConfigOption)setting;
            return doubleConfigOption.getValue();
        }
        return defaultValue;
    }

    public boolean isDimUnsharedSymbols() {
        return this.dimUnsharedSymbols.getValue();
    }

    static {
        TL_Plane_pool = ThreadLocal.withInitial(UI3DScene.PlaneObjectPool::new);
        TL_Ray_pool = ThreadLocal.withInitial(UI3DScene.RayObjectPool::new);
    }

    public static final class Drawer
    extends TextureDraw.GenericDrawer {
        public WorldMapRenderer renderer;
        public final WorldMapStyle style = new WorldMapStyle();
        public WorldMap worldMap;
        public int x;
        public int y;
        public int width;
        public int height;
        float centerWorldX;
        float centerWorldY;
        int zoom;
        public float zoomF;
        public float worldScale;
        float renderOriginX;
        float renderOriginY;
        public float worldOriginUiX;
        public float worldOriginUiY;
        float renderCellX;
        float renderCellY;
        private final Matrix4f projection = new Matrix4f();
        private final Matrix4f modelView = new Matrix4f();
        private final Matrix4f modelViewProjection = new Matrix4f();
        private final PlayerRenderData[] playerRenderData = new PlayerRenderData[4];
        final WorldMapStyleLayer.FilterArgs filterArgs = new WorldMapStyleLayer.FilterArgs();
        final WorldMapStyleLayer.RenderArgs renderArgs = new WorldMapStyleLayer.RenderArgs();
        final TLongObjectHashMap<WorldMapRenderCell> renderCellLookup = new TLongObjectHashMap();
        ListOfRenderLayers[] renderLayersForStyleLayer;
        final ArrayList<WorldMapRenderCell> renderCells = new ArrayList();
        final ArrayList<WorldMapFeature> features = new ArrayList();
        final ArrayList<Zone> zones = new ArrayList();
        final HashSet<Zone> zoneSet = new HashSet();
        WorldMapStyleLayer.RGBAf fill;
        int triangulationsThisFrame;
        public final SymbolsRenderData symbolsRenderData = new SymbolsRenderData();
        public final SymbolsLayoutData symbolsLayoutData = new SymbolsLayoutData();
        final StreetRenderData streetRenderData = new StreetRenderData();
        float[] floatArray;
        final Vector2f vector2f = new Vector2f();
        final TIntArrayList rasterizeXy = new TIntArrayList();
        final TIntArrayList rasterizePyramidXy = new TIntArrayList();
        final TIntSet rasterizeSet = new TIntHashSet();
        float rasterizeMinTileX;
        float rasterizeMinTileY;
        float rasterizeMaxTileX;
        float rasterizeMaxTileY;
        final Rasterize rasterize = new Rasterize();
        int[] rasterizeXyInts;
        int rasterizeMult = 1;
        final int[] rasterizeTileBounds = new int[4];
        static final int PRT_REQUIRED = 1;
        static final int PRT_RENDERED = 2;
        byte[] renderedTiles;
        static final int[] s_tilesCoveringCell = new int[4];

        Drawer() {
            PZArrayUtil.arrayPopulate(this.playerRenderData, PlayerRenderData::new);
        }

        void init(WorldMapRenderer renderer, UIWorldMap ui) {
            this.renderer = renderer;
            this.style.copyFrom(this.renderer.style);
            this.worldMap = renderer.worldMap;
            this.x = renderer.x;
            this.y = renderer.y;
            this.width = renderer.width;
            this.height = renderer.height;
            this.centerWorldX = renderer.centerWorldX;
            this.centerWorldY = renderer.centerWorldY;
            this.zoomF = renderer.displayZoomF;
            this.zoom = (int)this.zoomF;
            this.worldScale = this.getWorldScale();
            this.renderOriginX = ((float)this.renderer.worldMap.getMinXInSquares() - this.centerWorldX) * this.worldScale;
            this.renderOriginY = ((float)this.renderer.worldMap.getMinYInSquares() - this.centerWorldY) * this.worldScale;
            this.projection.set(renderer.projection);
            this.modelView.set(renderer.modelView);
            this.modelViewProjection.set(renderer.modelViewProjection);
            this.fill = ui.color;
            this.triangulationsThisFrame = 0;
            this.streetRenderData.init(ui, renderer);
            this.worldOriginUiX = this.worldOriginUIX(this.centerWorldX);
            this.worldOriginUiY = this.worldOriginUIY(this.centerWorldY);
            this.symbolsRenderData.renderMain(ui, ui.symbols, this.symbolsLayoutData, this.style);
            if (this.renderer.visited != null) {
                this.renderer.visited.renderMain();
            }
            for (int i = 0; i < 4; ++i) {
                this.playerRenderData[i].modelSlotRenderData = null;
            }
            if (this.renderer.players.getValue() && this.zoomF >= 20.0f && this.renderer.playerModel.getValue()) {
                for (int pn = 0; pn < 4; ++pn) {
                    IsoPlayer player = IsoPlayer.players[pn];
                    if (player == null || player.isDead() || !player.legsSprite.hasActiveModel()) continue;
                    float playerX = player.getX();
                    float playerY = player.getY();
                    if (player.getVehicle() != null) {
                        playerX = player.getVehicle().getX();
                        playerY = player.getVehicle().getY();
                    }
                    float uiX = this.renderer.worldToUIX(playerX, playerY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
                    float uiY = this.renderer.worldToUIY(playerX, playerY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
                    if (uiX < -100.0f || uiX > (float)(this.width + 100) || uiY < -100.0f || uiY > (float)(this.height + 100)) continue;
                    this.playerRenderData[pn].angle = player.getVehicle() == null ? player.getAnimationPlayer().getAngle() : 4.712389f;
                    this.playerRenderData[pn].x = playerX - this.centerWorldX;
                    this.playerRenderData[pn].y = playerY - this.centerWorldY;
                    player.legsSprite.modelSlot.model.updateLights();
                    int cameraIndex = IsoCamera.frameState.playerIndex;
                    IsoCamera.frameState.playerIndex = pn;
                    player.checkUpdateModelTextures();
                    this.playerRenderData[pn].modelSlotRenderData = ModelSlotRenderData.alloc();
                    this.playerRenderData[pn].modelSlotRenderData.initModel(player.legsSprite.modelSlot);
                    this.playerRenderData[pn].modelSlotRenderData.init(player.legsSprite.modelSlot);
                    this.playerRenderData[pn].modelSlotRenderData.centerOfMassY = 0.0f;
                    IsoCamera.frameState.playerIndex = cameraIndex;
                    ++player.legsSprite.modelSlot.renderRefCount;
                }
            }
        }

        public int getAbsoluteX() {
            return this.x;
        }

        public int getAbsoluteY() {
            return this.y;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public float getWorldScale() {
            return this.renderer.getWorldScale(this.zoomF);
        }

        public float uiToWorldX(float uiX, float uiY) {
            return this.renderer.uiToWorldX(uiX, uiY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float uiToWorldY(float uiX, float uiY) {
            return this.renderer.uiToWorldY(uiX, uiY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float worldToUIX(float worldX, float worldY) {
            return this.renderer.worldToUIX(worldX, worldY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float worldToUIY(float worldX, float worldY) {
            return this.renderer.worldToUIY(worldX, worldY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float worldOriginUIX(float centerWorldX) {
            return this.renderer.worldOriginUIX(this.zoomF, centerWorldX);
        }

        public float worldOriginUIY(float centerWorldY) {
            return this.renderer.worldOriginUIY(this.zoomF, centerWorldY);
        }

        public void renderVisibleCells(WorldMapStyleLayer styleLayer) {
            int layerIndex = this.style.indexOf(styleLayer);
            ListOfRenderLayers renderLayers = this.renderLayersForStyleLayer[layerIndex];
            for (int i = 0; i < renderLayers.size(); ++i) {
                WorldMapRenderLayer renderLayer = (WorldMapRenderLayer)renderLayers.get(i);
                WorldMapRenderCell renderCell = renderLayer.renderCell;
                int cellX = renderCell.cellX;
                int cellY = renderCell.cellY;
                this.renderCell(styleLayer, cellX, cellY, renderLayer.features);
            }
        }

        private void renderCellFeatures() {
            this.renderArgs.renderer = this.renderer;
            this.renderArgs.drawer = this;
            for (int i = 0; i < this.style.getLayerCount(); ++i) {
                WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(i);
                if (styleLayer.minZoom > this.zoomF) continue;
                styleLayer.renderVisibleCells(this.renderArgs);
            }
        }

        private void renderNonCellFeatures() {
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY)) continue;
                this.renderArgs.renderer = this.renderer;
                this.renderArgs.drawer = this;
                this.renderArgs.cellX = cellX;
                this.renderArgs.cellY = cellY;
                this.renderCellX = this.renderOriginX + (float)(cellX * 256 - this.worldMap.getMinXInSquares()) * this.worldScale;
                this.renderCellY = this.renderOriginY + (float)(cellY * 256 - this.worldMap.getMinYInSquares()) * this.worldScale;
                for (int j = 0; j < this.style.getLayerCount(); ++j) {
                    WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(j);
                    if (styleLayer.minZoom > this.zoomF || !(styleLayer instanceof WorldMapPyramidStyleLayer) && !(styleLayer instanceof WorldMapTextureStyleLayer)) continue;
                    styleLayer.renderCell(this.renderArgs);
                }
            }
        }

        private void renderCell(WorldMapStyleLayer styleLayer, int cellX, int cellY, ArrayList<WorldMapFeature> features) {
            this.renderCellX = this.renderOriginX + (float)(cellX * 256 - this.worldMap.getMinXInSquares()) * this.worldScale;
            this.renderCellY = this.renderOriginY + (float)(cellY * 256 - this.worldMap.getMinYInSquares()) * this.worldScale;
            this.renderArgs.renderer = this.renderer;
            this.renderArgs.drawer = this;
            this.renderArgs.cellX = cellX;
            this.renderArgs.cellY = cellY;
            styleLayer.renderCell(this.renderArgs);
            for (int j = 0; j < features.size(); ++j) {
                WorldMapFeature feature = features.get(j);
                styleLayer.render(feature, this.renderArgs);
            }
        }

        void filterFeatures() {
            int i;
            for (i = 0; i < this.renderCells.size(); ++i) {
                WorldMapRenderCell renderCell = this.renderCells.get(i);
                WorldMapRenderLayer.pool.releaseAll((List<WorldMapRenderLayer>)renderCell.renderLayers);
                renderCell.renderLayers.clear();
                WorldMapRenderCell.pool.release(renderCell);
            }
            this.renderCells.clear();
            this.renderCellLookup.clear();
            if (this.renderLayersForStyleLayer == null || this.renderLayersForStyleLayer.length < this.style.getLayerCount()) {
                this.renderLayersForStyleLayer = new ListOfRenderLayers[this.style.getLayerCount()];
            }
            for (i = 0; i < this.style.getLayerCount(); ++i) {
                if (this.renderLayersForStyleLayer[i] == null) {
                    this.renderLayersForStyleLayer[i] = new ListOfRenderLayers();
                }
                this.renderLayersForStyleLayer[i].clear();
            }
            this.filterArgs.renderer = this.renderer;
            for (i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY)) continue;
                this.features.clear();
                for (int k = 0; k < this.worldMap.data.size(); ++k) {
                    WorldMapCell cell;
                    WorldMapData data = this.worldMap.data.get(k);
                    if (!data.isReady() || (cell = data.getCell(cellX, cellY)) == null || cell.features.isEmpty()) continue;
                    if (cell.priority == -1) {
                        ArrayList<MapFiles> mapFiles = MapFiles.getCurrentMapFiles();
                        for (int m = 0; m < mapFiles.size(); ++m) {
                            MapFiles mapFiles1 = mapFiles.get(m);
                            if (!data.relativeFileName.startsWith("media/maps/" + mapFiles1.mapDirectoryName)) continue;
                            cell.priority = m;
                            break;
                        }
                    }
                    PZArrayUtil.addAll(this.features, cell.features);
                }
                if (this.features.isEmpty()) continue;
                WorldMapRenderCell renderCell = WorldMapRenderCell.alloc();
                renderCell.cellX = cellX;
                renderCell.cellY = cellY;
                this.filterFeatures(this.features, this.filterArgs, renderCell);
                if (renderCell.renderLayers.isEmpty()) {
                    WorldMapRenderCell.pool.release(renderCell);
                    continue;
                }
                this.renderCells.add(renderCell);
                this.renderCellLookup.put((long)cellX | (long)cellY << 32, renderCell);
            }
        }

        void filterFeatures(ArrayList<WorldMapFeature> features, WorldMapStyleLayer.FilterArgs args2, WorldMapRenderCell renderCell) {
            for (int i = 0; i < this.style.getLayerCount(); ++i) {
                WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(i);
                if (styleLayer.ignoreFeatures() || styleLayer.minZoom > this.zoomF) continue;
                WorldMapRenderLayer renderLayer = null;
                for (int j = 0; j < features.size(); ++j) {
                    WorldMapFeature feature = features.get(j);
                    if (!styleLayer.filter(feature, args2)) continue;
                    if (renderLayer == null) {
                        renderLayer = WorldMapRenderLayer.pool.alloc();
                        renderLayer.renderCell = renderCell;
                        renderLayer.styleLayer = styleLayer;
                        renderLayer.features.clear();
                        renderCell.renderLayers.add(renderLayer);
                    }
                    renderLayer.features.add(feature);
                }
                if (renderLayer == null) continue;
                this.renderLayersForStyleLayer[i].add(renderLayer);
            }
        }

        void renderCellGrid(int minX, int minY, int maxX, int maxY) {
            float minXui = this.renderOriginX + (float)(minX * 256 - this.worldMap.getMinXInSquares()) * this.worldScale;
            float minYui = this.renderOriginY + (float)(minY * 256 - this.worldMap.getMinYInSquares()) * this.worldScale;
            float maxXui = minXui + (float)((maxX - minX + 1) * 256) * this.worldScale;
            float maxYui = minYui + (float)((maxY - minY + 1) * 256) * this.worldScale;
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(1);
            vboLines.setLineWidth(1.0f);
            for (int x = minX; x <= maxX + 1; ++x) {
                vboLines.addLine(this.renderOriginX + (float)(x * 256 - this.worldMap.getMinXInSquares()) * this.worldScale, minYui, 0.0f, this.renderOriginX + (float)(x * 256 - this.worldMap.getMinXInSquares()) * this.worldScale, maxYui, 0.0f, 0.25f, 0.25f, 0.25f, 1.0f);
            }
            for (int y = minY; y <= maxY + 1; ++y) {
                vboLines.addLine(minXui, this.renderOriginY + (float)(y * 256 - this.worldMap.getMinYInSquares()) * this.worldScale, 0.0f, maxXui, this.renderOriginY + (float)(y * 256 - this.worldMap.getMinYInSquares()) * this.worldScale, 0.0f, 0.25f, 0.25f, 0.25f, 1.0f);
            }
            vboLines.endRun();
            vboLines.flush();
        }

        void renderPlayers() {
            boolean bClearDepth = true;
            for (int i = 0; i < this.playerRenderData.length; ++i) {
                PlayerRenderData playerRenderData = this.playerRenderData[i];
                if (playerRenderData.modelSlotRenderData == null) continue;
                if (bClearDepth) {
                    GL11.glClear(256);
                    bClearDepth = false;
                }
                this.renderer.characterModelCamera.worldScale = this.worldScale;
                this.renderer.characterModelCamera.useWorldIso = true;
                this.renderer.characterModelCamera.angle = playerRenderData.angle;
                this.renderer.characterModelCamera.playerX = playerRenderData.x;
                this.renderer.characterModelCamera.playerY = playerRenderData.y;
                this.renderer.characterModelCamera.vehicle = playerRenderData.modelSlotRenderData.inVehicle;
                ModelCamera.instance = this.renderer.characterModelCamera;
                playerRenderData.modelSlotRenderData.render();
            }
            if (UIManager.useUiFbo) {
                GL14.glBlendFuncSeparate(770, 771, 1, 771);
            }
        }

        public void drawLineStringXXX(WorldMapStyleLayer.RenderArgs args2, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.LineString) {
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                vboLines.setMode(1);
                vboLines.setLineWidth(lineWidth);
                for (int k = 0; k < geometry.points.size(); ++k) {
                    WorldMapPoints points = geometry.points.get(k);
                    for (int j = 0; j < points.numPoints() - 1; ++j) {
                        float x1 = points.getX(j);
                        float y1 = points.getY(j);
                        float x2 = points.getX(j + 1);
                        float y2 = points.getY(j + 1);
                        vboLines.addLine(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0f, renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0f, r, g, b, a);
                    }
                }
                vboLines.endRun();
            }
        }

        public void drawLineStringYYY(WorldMapStyleLayer.RenderArgs args2, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.LineString) {
                StrokeGeometry.Point[] points = new StrokeGeometry.Point[geometry.points.size()];
                WorldMapPoints gpoints = geometry.points.get(0);
                for (int j = 0; j < gpoints.numPoints(); ++j) {
                    float x1 = gpoints.getX(j);
                    float y1 = gpoints.getY(j);
                    points[j] = StrokeGeometry.newPoint(renderOriginX + x1 * scale, renderOriginY + y1 * scale);
                }
                StrokeGeometry.Attrs attrs = new StrokeGeometry.Attrs();
                attrs.join = "miter";
                attrs.width = lineWidth;
                ArrayList<StrokeGeometry.Point> vertices = StrokeGeometry.getStrokeGeometry(points, attrs);
                if (vertices == null) {
                    return;
                }
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                vboLines.setMode(4);
                for (int j = 0; j < vertices.size(); ++j) {
                    float x1 = (float)vertices.get((int)j).x;
                    float y1 = (float)vertices.get((int)j).y;
                    vboLines.addElement(x1, y1, 0.0f, r, g, b, a);
                }
                vboLines.endRun();
                StrokeGeometry.release(vertices);
            }
        }

        public void drawLineString(WorldMapStyleLayer.RenderArgs args2, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth) {
            if (!this.renderer.lineString.getValue()) {
                return;
            }
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            vboLines.flush();
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.LineString) {
                WorldMapPoints points = geometry.points.get(0);
                if (this.floatArray == null || this.floatArray.length < points.numPoints() * 2) {
                    this.floatArray = new float[points.numPoints() * 2];
                }
                for (int j = 0; j < points.numPoints(); ++j) {
                    float x1 = points.getX(j);
                    float y1 = points.getY(j);
                    this.floatArray[j * 2] = renderOriginX + x1 * scale;
                    this.floatArray[j * 2 + 1] = renderOriginY + y1 * scale;
                }
                GL13.glActiveTexture(33984);
                GL11.glDisable(3553);
                GL11.glEnable(3042);
            }
        }

        public void drawLineStringTexture(WorldMapStyleLayer.RenderArgs args2, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth, Texture texture) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            if (texture == null || !texture.isReady()) {
                return;
            }
            if (texture.getID() == -1) {
                texture.bind();
            }
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.LineString) {
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                vboLines.setMode(7);
                vboLines.setTextureID(texture.getTextureId());
                WorldMapPoints points = geometry.points.get(0);
                for (int j = 0; j < points.numPoints() - 1; ++j) {
                    float x1 = renderOriginX + (float)points.getX(j) * scale;
                    float y1 = renderOriginY + (float)points.getY(j) * scale;
                    float x2 = renderOriginX + (float)points.getX(j + 1) * scale;
                    float y2 = renderOriginY + (float)points.getY(j + 1) * scale;
                    float perpX = y2 - y1;
                    float perpY = -(x2 - x1);
                    Vector2f perp = this.vector2f.set(perpX, perpY);
                    perp.normalize();
                    float px1 = x1 + perp.x * lineWidth / 2.0f;
                    float py1 = y1 + perp.y * lineWidth / 2.0f;
                    float px2 = x2 + perp.x * lineWidth / 2.0f;
                    float py2 = y2 + perp.y * lineWidth / 2.0f;
                    float px3 = x2 - perp.x * lineWidth / 2.0f;
                    float py3 = y2 - perp.y * lineWidth / 2.0f;
                    float px4 = x1 - perp.x * lineWidth / 2.0f;
                    float py4 = y1 - perp.y * lineWidth / 2.0f;
                    float length = Vector2f.length(x2 - x1, y2 - y1);
                    float u1 = 0.0f;
                    float v1 = length / (lineWidth * ((float)texture.getHeight() / (float)texture.getWidth()));
                    float u2 = 0.0f;
                    float v2 = 0.0f;
                    float u3 = 1.0f;
                    float v3 = 0.0f;
                    float u4 = 1.0f;
                    float v4 = length / (lineWidth * ((float)texture.getHeight() / (float)texture.getWidth()));
                    vboLines.addQuad(px1, py1, 0.0f, v1, px2, py2, 0.0f, 0.0f, px3, py3, 1.0f, 0.0f, px4, py4, 1.0f, v4, 0.0f, color.r, color.g, color.b, color.a);
                }
                vboLines.endRun();
            }
        }

        public void fillPolygon(WorldMapStyleLayer.RenderArgs args2, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.Polygon) {
                WorldMapGeometry.TrianglesPerZoom tpz;
                boolean useWorldMapVbo = false;
                if (geometry.failedToTriangulate) {
                    if (Core.debug) {
                        // empty if block
                    }
                    return;
                }
                if (geometry.firstIndex == -1) {
                    double[] dArray;
                    if (this.triangulationsThisFrame > 500) {
                        return;
                    }
                    ++this.triangulationsThisFrame;
                    if (feature.properties.containsKey("highway")) {
                        double[] dArray2 = new double[6];
                        dArray2[0] = 1.0;
                        dArray2[1] = 2.0;
                        dArray2[2] = 4.0;
                        dArray2[3] = 8.0;
                        dArray2[4] = 12.0;
                        dArray = dArray2;
                        dArray2[5] = 18.0;
                    } else {
                        dArray = null;
                    }
                    double[] delta = dArray;
                    geometry.triangulate(feature.cell, delta);
                    if (geometry.indexCount <= 0) {
                        geometry.failedToTriangulate = true;
                        return;
                    }
                }
                ShortBuffer indices = geometry.cell.indexBuffer;
                FloatBuffer tris = geometry.cell.triangleBuffer;
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                vboLines.setMode(4);
                double delta = 0.0;
                if ((double)this.zoomF <= 11.5) {
                    delta = 18.0;
                } else if ((double)this.zoomF <= 12.0) {
                    delta = 12.0;
                } else if ((double)this.zoomF <= 12.5) {
                    delta = 8.0;
                } else if ((double)this.zoomF <= 13.0) {
                    delta = 4.0;
                } else if ((double)this.zoomF <= 13.5) {
                    delta = 2.0;
                } else if ((double)this.zoomF <= 14.0) {
                    delta = 1.0;
                }
                WorldMapGeometry.TrianglesPerZoom trianglesPerZoom = tpz = delta == 0.0 ? null : geometry.findTriangles(delta);
                if (tpz != null) {
                    for (int j = 0; j < tpz.indexCount; j += 3) {
                        int i1 = indices.get(tpz.firstIndex + j) & 0xFFFF;
                        int i2 = indices.get(tpz.firstIndex + j + 1) & 0xFFFF;
                        int i3 = indices.get(tpz.firstIndex + j + 2) & 0xFFFF;
                        float x1 = tris.get(i1 * 2);
                        float y1 = tris.get(i1 * 2 + 1);
                        float x2 = tris.get(i2 * 2);
                        float y2 = tris.get(i2 * 2 + 1);
                        float x3 = tris.get(i3 * 2);
                        float y3 = tris.get(i3 * 2 + 1);
                        vboLines.reserve(3);
                        float m = 1.0f;
                        vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0f, r * 1.0f, g * 1.0f, b * 1.0f, a);
                        vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0f, r * 1.0f, g * 1.0f, b * 1.0f, a);
                        vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0f, r * 1.0f, g * 1.0f, b * 1.0f, a);
                    }
                    vboLines.endRun();
                    return;
                }
                for (int j = 0; j < geometry.indexCount; j += 3) {
                    int i1 = indices.get(geometry.firstIndex + j) & 0xFFFF;
                    int i2 = indices.get(geometry.firstIndex + j + 1) & 0xFFFF;
                    int i3 = indices.get(geometry.firstIndex + j + 2) & 0xFFFF;
                    float x1 = tris.get(i1 * 2);
                    float y1 = tris.get(i1 * 2 + 1);
                    float x2 = tris.get(i2 * 2);
                    float y2 = tris.get(i2 * 2 + 1);
                    float x3 = tris.get(i3 * 2);
                    float y3 = tris.get(i3 * 2 + 1);
                    vboLines.reserve(3);
                    vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0f, r, g, b, a);
                    vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0f, r, g, b, a);
                    vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0f, r, g, b, a);
                }
                vboLines.endRun();
            }
        }

        public void fillPolygon(WorldMapStyleLayer.RenderArgs args2, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, Texture texture, float textureScale, WorldMapStyleLayer.TextureScaling scaling) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float worldScale = this.worldScale;
            boolean bOnePixelPerSquare = scaling == WorldMapStyleLayer.TextureScaling.IsoGridSquare;
            float textureCoordScale = bOnePixelPerSquare ? 1.0f / textureScale : worldScale / textureScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.Polygon) {
                if (geometry.failedToTriangulate) {
                    return;
                }
                if (geometry.firstIndex == -1) {
                    if (this.triangulationsThisFrame > 500) {
                        return;
                    }
                    ++this.triangulationsThisFrame;
                    geometry.triangulate(feature.cell, null);
                    if (geometry.indexCount <= 0) {
                        geometry.failedToTriangulate = true;
                        return;
                    }
                }
                GL11.glEnable(3553);
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                vboLines.setMode(4);
                vboLines.setTextureID(texture.getTextureId());
                vboLines.setMinMagFilters(9728, 9728);
                float minX = args2.cellX * 256 + geometry.minX;
                float minY = args2.cellY * 256 + geometry.minY;
                float texW = (float)texture.getWidth() * textureScale;
                float texH = (float)texture.getHeight() * textureScale;
                float texWhw = texture.getWidthHW();
                float texHhw = texture.getHeightHW();
                float textureOriginX = PZMath.floor(minX / texW) * texW;
                float textureOriginY = PZMath.floor(minY / texH) * texH;
                ShortBuffer indices = geometry.cell.indexBuffer;
                FloatBuffer tris = geometry.cell.triangleBuffer;
                for (int j = 0; j < geometry.indexCount; j += 3) {
                    int i1 = indices.get(geometry.firstIndex + j) & 0xFFFF;
                    int i2 = indices.get(geometry.firstIndex + j + 1) & 0xFFFF;
                    int i3 = indices.get(geometry.firstIndex + j + 2) & 0xFFFF;
                    float x1 = tris.get(i1 * 2);
                    float y1 = tris.get(i1 * 2 + 1);
                    float x2 = tris.get(i2 * 2);
                    float y2 = tris.get(i2 * 2 + 1);
                    float x3 = tris.get(i3 * 2);
                    float y3 = tris.get(i3 * 2 + 1);
                    float tx1 = (x1 + (float)(args2.cellX * 256) - textureOriginX) * textureCoordScale;
                    float ty1 = (y1 + (float)(args2.cellY * 256) - textureOriginY) * textureCoordScale;
                    float tx2 = (x2 + (float)(args2.cellX * 256) - textureOriginX) * textureCoordScale;
                    float ty2 = (y2 + (float)(args2.cellY * 256) - textureOriginY) * textureCoordScale;
                    float tx3 = (x3 + (float)(args2.cellX * 256) - textureOriginX) * textureCoordScale;
                    float ty3 = (y3 + (float)(args2.cellY * 256) - textureOriginY) * textureCoordScale;
                    x1 = renderOriginX + x1 * worldScale;
                    y1 = renderOriginY + y1 * worldScale;
                    x2 = renderOriginX + x2 * worldScale;
                    y2 = renderOriginY + y2 * worldScale;
                    x3 = renderOriginX + x3 * worldScale;
                    y3 = renderOriginY + y3 * worldScale;
                    float u1 = tx1 / texWhw;
                    float v1 = ty1 / texHhw;
                    float u2 = tx2 / texWhw;
                    float v2 = ty2 / texHhw;
                    float u3 = tx3 / texWhw;
                    float v3 = ty3 / texHhw;
                    vboLines.reserve(3);
                    vboLines.addElement(x1, y1, 0.0f, u1, v1, r, g, b, a);
                    vboLines.addElement(x2, y2, 0.0f, u2, v2, r, g, b, a);
                    vboLines.addElement(x3, y3, 0.0f, u3, v3, r, g, b, a);
                }
                vboLines.endRun();
                GL11.glDisable(3553);
            }
        }

        void uploadTrianglesToVBO(WorldMapGeometry geometry) {
            int[] indices1 = new int[2];
            ShortBuffer indices = geometry.cell.indexBuffer;
            FloatBuffer tris = geometry.cell.triangleBuffer;
            int numElements = geometry.indexCount / 3;
            if (numElements > 2340) {
                int doneTris = 0;
                while (numElements > 0) {
                    int numTris = PZMath.min(numElements / 3, 780);
                    WorldMapVBOs.getInstance().reserveVertices(numTris * 3, indices1);
                    if (geometry.vboIndex1 == -1) {
                        geometry.vboIndex1 = indices1[0];
                        geometry.vboIndex2 = indices1[1];
                    } else {
                        geometry.vboIndex3 = indices1[0];
                        geometry.vboIndex4 = indices1[1];
                    }
                    int nj = doneTris + numTris;
                    for (int j = doneTris; j < nj; ++j) {
                        int i1 = indices.get(geometry.firstIndex + j) & 0xFFFF;
                        int i2 = indices.get(geometry.firstIndex + j + 1) & 0xFFFF;
                        int i3 = indices.get(geometry.firstIndex + j + 2) & 0xFFFF;
                        float x1 = tris.get(i1 * 2);
                        float y1 = tris.get(i1 * 2 + 1);
                        float x2 = tris.get(i2 * 2);
                        float y2 = tris.get(i2 * 2 + 1);
                        float x3 = tris.get(i3 * 2);
                        float y3 = tris.get(i3 * 2 + 1);
                        WorldMapVBOs.getInstance().addElement(x1, y1, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                        WorldMapVBOs.getInstance().addElement(x2, y2, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                        WorldMapVBOs.getInstance().addElement(x3, y3, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                    }
                    doneTris += numTris;
                    numElements -= numTris * 3;
                }
            } else {
                WorldMapVBOs.getInstance().reserveVertices(numElements, indices1);
                geometry.vboIndex1 = indices1[0];
                geometry.vboIndex2 = indices1[1];
                for (int j = 0; j < geometry.indexCount; j += 3) {
                    short i1 = indices.get(geometry.firstIndex + j);
                    short i2 = indices.get(geometry.firstIndex + j + 1);
                    short i3 = indices.get(geometry.firstIndex + j + 2);
                    float x1 = tris.get(i1 * 2);
                    float y1 = tris.get(i1 * 2 + 1);
                    float x2 = tris.get(i2 * 2);
                    float y2 = tris.get(i2 * 2 + 1);
                    float x3 = tris.get(i3 * 2);
                    float y3 = tris.get(i3 * 2 + 1);
                    WorldMapVBOs.getInstance().addElement(x1, y1, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                    WorldMapVBOs.getInstance().addElement(x2, y2, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                    WorldMapVBOs.getInstance().addElement(x3, y3, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
        }

        void outlineTriangles(WorldMapGeometry geometry, float renderOriginX, float renderOriginY, float scale) {
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(1);
            float a = 1.0f;
            float r = 1.0f;
            float b = 0.0f;
            float g = 0.0f;
            ShortBuffer indices = geometry.cell.indexBuffer;
            FloatBuffer tris = geometry.cell.triangleBuffer;
            for (int j = 0; j < geometry.indexCount; j += 3) {
                int i1 = indices.get(geometry.firstIndex + j) & 0xFFFF;
                int i2 = indices.get(geometry.firstIndex + j + 1) & 0xFFFF;
                int i3 = indices.get(geometry.firstIndex + j + 2) & 0xFFFF;
                float x1 = tris.get(i1 * 2);
                float y1 = tris.get(i1 * 2 + 1);
                float x2 = tris.get(i2 * 2);
                float y2 = tris.get(i2 * 2 + 1);
                float x3 = tris.get(i3 * 2);
                float y3 = tris.get(i3 * 2 + 1);
                vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0f, r, g, b, 1.0f);
                vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0f, r, g, b, 1.0f);
                vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0f, r, g, b, 1.0f);
                vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0f, r, g, b, 1.0f);
                vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0f, r, g, b, 1.0f);
                vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0f, r, g, b, 1.0f);
            }
            vboLines.endRun();
        }

        void outlinePolygon(WorldMapGeometry geometry, float renderOriginX, float renderOriginY, float scale) {
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(1);
            float a = 1.0f;
            float b = 0.8f;
            float g = 0.8f;
            float r = 0.8f;
            vboLines.setLineWidth(4.0f);
            for (int k = 0; k < geometry.points.size(); ++k) {
                WorldMapPoints points = geometry.points.get(k);
                for (int j = 0; j < points.numPoints(); ++j) {
                    int p1x = points.getX(j);
                    int p1y = points.getY(j);
                    int p2x = points.getX((j + 1) % points.numPoints());
                    int p2y = points.getY((j + 1) % points.numPoints());
                    vboLines.addElement(renderOriginX + (float)p1x * scale, renderOriginY + (float)p1y * scale, 0.0f, r, g, b, 1.0f);
                    vboLines.addElement(renderOriginX + (float)p2x * scale, renderOriginY + (float)p2y * scale, 0.0f, r, g, b, 1.0f);
                }
            }
            vboLines.endRun();
        }

        public void drawTexture(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2) {
            if (texture == null || !texture.isReady()) {
                return;
            }
            float worldScale = this.worldScale;
            float x1 = ((float)worldX1 - this.centerWorldX) * worldScale;
            float y1 = ((float)worldY1 - this.centerWorldY) * worldScale;
            float x2 = x1 + (float)(worldX2 - worldX1) * worldScale;
            float y2 = y1 + (float)(worldY2 - worldY1) * worldScale;
            GL11.glEnable(3553);
            GL11.glEnable(3042);
            GL11.glDisable(2929);
            if (texture.getID() == -1) {
                texture.bind();
            }
            float u0 = texture.getXStart();
            float v0 = texture.getYStart();
            float u1 = texture.getXEnd();
            float v1 = texture.getYEnd();
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
            vboLines.setMode(7);
            vboLines.setTextureID(texture.getTextureId());
            vboLines.setMinMagFilters(9728, 9728);
            vboLines.addQuad(x1, y1, u0, v0, x2, y2, u1, v1, 0.0f, fill.r, fill.g, fill.b, fill.a);
            vboLines.endRun();
        }

        public void drawTexture(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2, int cellX, int cellY) {
            if (texture == null || !texture.isReady()) {
                return;
            }
            float worldScale = this.worldScale;
            float x1 = ((float)worldX1 - this.centerWorldX) * worldScale;
            float y1 = ((float)worldY1 - this.centerWorldY) * worldScale;
            float x2 = x1 + (float)(worldX2 - worldX1) * worldScale;
            float y2 = y1 + (float)(worldY2 - worldY1) * worldScale;
            float x1c = PZMath.clamp(x1, (float)cellX, (float)cellX + 256.0f * worldScale);
            float y1c = PZMath.clamp(y1, (float)cellY, (float)cellY + 256.0f * worldScale);
            float x2c = PZMath.clamp(x2, (float)cellX, (float)cellX + 256.0f * worldScale);
            float y2c = PZMath.clamp(y2, (float)cellY, (float)cellY + 256.0f * worldScale);
            if (x1c >= x2c || y1c >= y2c) {
                return;
            }
            float xScale = (float)texture.getWidth() / (float)(worldX2 - worldX1);
            float yScale = (float)texture.getHeight() / (float)(worldY2 - worldY1);
            GL11.glEnable(3553);
            GL11.glEnable(3042);
            GL11.glDisable(2929);
            if (texture.getID() == -1) {
                texture.bind();
            }
            float u0 = (x1c - x1) / ((float)texture.getWidthHW() * worldScale) * xScale;
            float v0 = (y1c - y1) / ((float)texture.getHeightHW() * worldScale) * yScale;
            float u1 = (x2c - x1) / ((float)texture.getWidthHW() * worldScale) * xScale;
            float v1 = (y2c - y1) / ((float)texture.getHeightHW() * worldScale) * yScale;
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
            vboLines.setMode(7);
            vboLines.setTextureID(texture.getTextureId());
            vboLines.setMinMagFilters(9728, 9728);
            vboLines.addQuad(x1c, y1c, u0, v0, x2c, y2c, u1, v1, 0.0f, fill.r, fill.g, fill.b, fill.a);
            vboLines.endRun();
        }

        public void drawTextureTiled(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2, int cellX, int cellY) {
            if (texture == null || !texture.isReady()) {
                return;
            }
            if (cellX * 256 >= worldX2 || (cellX + 1) * 256 <= worldX1) {
                return;
            }
            if (cellY * 256 >= worldY2 || (cellY + 1) * 256 <= worldY1) {
                return;
            }
            float worldScale = this.worldScale;
            int tileW = texture.getWidth();
            int tileH = texture.getHeight();
            int x1 = (int)(PZMath.floor((float)cellX * 256.0f / (float)tileW) * (float)tileW);
            int y1 = (int)(PZMath.floor((float)cellY * 256.0f / (float)tileH) * (float)tileH);
            int x2 = x1 + (int)Math.ceil(((float)(cellX + 1) * 256.0f - (float)x1) / (float)tileW) * tileW;
            int y2 = y1 + (int)Math.ceil(((float)(cellY + 1) * 256.0f - (float)y1) / (float)tileH) * tileH;
            float x1c = PZMath.clamp(x1, cellX * 256, (cellX + 1) * 256);
            float y1c = PZMath.clamp(y1, cellY * 256, (cellY + 1) * 256);
            float x2c = PZMath.clamp(x2, cellX * 256, (cellX + 1) * 256);
            float y2c = PZMath.clamp(y2, cellY * 256, (cellY + 1) * 256);
            x1c = PZMath.clamp(x1c, (float)worldX1, (float)worldX2);
            y1c = PZMath.clamp(y1c, (float)worldY1, (float)worldY2);
            x2c = PZMath.clamp(x2c, (float)worldX1, (float)worldX2);
            y2c = PZMath.clamp(y2c, (float)worldY1, (float)worldY2);
            float tileX1 = (x1c - (float)worldX1) / (float)tileW;
            float tileY1 = (y1c - (float)worldY1) / (float)tileH;
            float tileX2 = (x2c - (float)worldX1) / (float)tileW;
            float tileY2 = (y2c - (float)worldY1) / (float)tileH;
            x1c = (x1c - this.centerWorldX) * worldScale;
            y1c = (y1c - this.centerWorldY) * worldScale;
            x2c = (x2c - this.centerWorldX) * worldScale;
            y2c = (y2c - this.centerWorldY) * worldScale;
            float u0 = tileX1 * texture.xEnd;
            float v0 = tileY1 * texture.yEnd;
            float u1 = (float)((int)tileX2) + (tileX2 - (float)((int)tileX2)) * texture.xEnd;
            float v1 = (float)((int)tileY2) + (tileY2 - (float)((int)tileY2)) * texture.yEnd;
            GL11.glEnable(3553);
            if (texture.getID() == -1) {
                texture.bind();
            } else {
                Texture.lastTextureID = texture.getID();
                GL11.glBindTexture(3553, Texture.lastTextureID);
                GL11.glTexParameteri(3553, 10242, 10497);
                GL11.glTexParameteri(3553, 10243, 10497);
            }
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
            vboLines.setMode(7);
            vboLines.setTextureID(texture.getTextureId());
            vboLines.setMinMagFilters(9728, 9728);
            vboLines.addQuad(x1c, y1c, u0, v0, x2c, y2c, u1, v1, 0.0f, fill.r, fill.g, fill.b, fill.a);
            vboLines.endRun();
            GL11.glDisable(3553);
        }

        public void drawTextureTiled(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2) {
            if (texture == null || !texture.isReady()) {
                return;
            }
            float worldScale = this.worldScale;
            int tileW = texture.getWidth();
            int tileH = texture.getHeight();
            float cellX = PZMath.floor((float)worldX1 / 256.0f);
            float cellY = PZMath.floor((float)worldY1 / 256.0f);
            float cellX2 = PZMath.floor((float)worldX2 / 256.0f);
            float cellY2 = PZMath.floor((float)worldY2 / 256.0f);
            int x1 = (int)(PZMath.floor(cellX * 256.0f / (float)tileW) * (float)tileW);
            int y1 = (int)(PZMath.floor(cellY * 256.0f / (float)tileH) * (float)tileH);
            int x2 = x1 + (int)Math.ceil(((cellX2 + 1.0f) * 256.0f - (float)x1) / (float)tileW) * tileW;
            int y2 = y1 + (int)Math.ceil(((cellY2 + 1.0f) * 256.0f - (float)y1) / (float)tileH) * tileH;
            float x1c = PZMath.clamp((float)x1, cellX * 256.0f, (cellX2 + 1.0f) * 256.0f);
            float y1c = PZMath.clamp((float)y1, cellY * 256.0f, (cellY2 + 1.0f) * 256.0f);
            float x2c = PZMath.clamp((float)x2, cellX * 256.0f, (cellX2 + 1.0f) * 256.0f);
            float y2c = PZMath.clamp((float)y2, cellY * 256.0f, (cellY2 + 1.0f) * 256.0f);
            x1c = PZMath.clamp(x1c, (float)worldX1, (float)worldX2);
            y1c = PZMath.clamp(y1c, (float)worldY1, (float)worldY2);
            x2c = PZMath.clamp(x2c, (float)worldX1, (float)worldX2);
            y2c = PZMath.clamp(y2c, (float)worldY1, (float)worldY2);
            float tileX1 = (x1c - (float)worldX1) / (float)tileW;
            float tileY1 = (y1c - (float)worldY1) / (float)tileH;
            float tileX2 = (x2c - (float)worldX1) / (float)tileW;
            float tileY2 = (y2c - (float)worldY1) / (float)tileH;
            x1c = (x1c - this.centerWorldX) * worldScale;
            y1c = (y1c - this.centerWorldY) * worldScale;
            x2c = (x2c - this.centerWorldX) * worldScale;
            y2c = (y2c - this.centerWorldY) * worldScale;
            float u0 = tileX1 * texture.xEnd;
            float v0 = tileY1 * texture.yEnd;
            float u1 = (float)((int)tileX2) + (tileX2 - (float)((int)tileX2)) * texture.xEnd;
            float v1 = (float)((int)tileY2) + (tileY2 - (float)((int)tileY2)) * texture.yEnd;
            GL11.glEnable(3553);
            if (texture.getID() == -1) {
                texture.bind();
            } else {
                Texture.lastTextureID = texture.getID();
                GL11.glBindTexture(3553, Texture.lastTextureID);
                GL11.glTexParameteri(3553, 10242, 10497);
                GL11.glTexParameteri(3553, 10243, 10497);
            }
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
            vboLines.setMode(7);
            vboLines.setTextureID(texture.getTextureId());
            vboLines.setMinMagFilters(9728, 9728);
            vboLines.addQuad(x1c, y1c, u0, v0, x2c, y2c, u1, v1, 0.0f, fill.r, fill.g, fill.b, fill.a);
            vboLines.endRun();
            GL11.glDisable(3553);
        }

        void renderZones() {
            this.zoneSet.clear();
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                IsoMetaCell metaCell;
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY) || (metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY)) == null) continue;
                metaCell.getZonesUnique(this.zoneSet);
            }
            this.zones.clear();
            this.zones.addAll(this.zoneSet);
            this.renderZones(this.zones, "Water", 0.0f, 0.0f, 1.0f, 0.25f);
            this.renderZones(this.zones, "WaterNoFish", 0.0f, 0.2f, 1.0f, 0.25f);
            this.renderZones(this.zones, "Forest", 0.0f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "DeepForest", 0.0f, 0.5f, 0.0f, 0.25f);
            this.renderZones(this.zones, "ForagingNav", 0.5f, 0.0f, 1.0f, 0.25f);
            this.renderZones(this.zones, "Vegitation", 1.0f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "TrailerPark", 0.0f, 0.5f, 1.0f, 0.25f);
            this.renderZones(this.zones, "TownZone", 0.0f, 0.5f, 1.0f, 0.25f);
            this.renderZones(this.zones, "Farm", 0.5f, 0.5f, 1.0f, 0.25f);
            this.renderZones(this.zones, "FarmLand", 0.5f, 0.5f, 1.0f, 0.25f);
            this.renderZones(this.zones, "PHForest", 0.1f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "PHMixForest", 0.2f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "PRForest", 0.3f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "FarmMixForest", 0.4f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "FarmForest", 0.5f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "BirchForest", 0.6f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "BirchMixForest", 0.7f, 1.0f, 0.0f, 0.25f);
            this.renderZones(this.zones, "OrganicForest", 0.8f, 1.0f, 0.0f, 0.25f);
        }

        void renderOtherZones() {
            this.zoneSet.clear();
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                IsoMetaCell isoMetaCell;
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY) || (isoMetaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY)) == null) continue;
                isoMetaCell.getZonesUnique(this.zoneSet);
            }
            HashMap<String, ColorInfo> zoneTypeToColor = new HashMap<String, ColorInfo>();
            HashMap zoneTypeToZoneList = new HashMap();
            for (Zone zone : this.zoneSet) {
                if (zone.type == null || Objects.equals(zone.type, "DeepForest") || Objects.equals(zone.type, "Farm") || Objects.equals(zone.type, "FarmLand") || Objects.equals(zone.type, "Forest") || Objects.equals(zone.type, "ForagingNav") || Objects.equals(zone.type, "TownZone") || Objects.equals(zone.type, "TrailerPark") || Objects.equals(zone.type, "Vegitation") || Objects.equals(zone.type, "Foraging_None") || Objects.equals(zone.type, "PHForest") || Objects.equals(zone.type, "PHMixForest") || Objects.equals(zone.type, "PRForest") || Objects.equals(zone.type, "FarmMixForest") || Objects.equals(zone.type, "FarmForest") || Objects.equals(zone.type, "BirchForest") || Objects.equals(zone.type, "BirchMixForest") || Objects.equals(zone.type, "OrganicForest")) continue;
                if (!zoneTypeToColor.containsKey(zone.type)) {
                    StringBuilder hash = new StringBuilder(Integer.toHexString(zone.type.hashCode()));
                    hash.append("0".repeat(Math.max(0, 6 - hash.length())));
                    float r = (float)Integer.parseInt(hash.substring(0, 2), 16) / 255.0f;
                    float g = (float)Integer.parseInt(hash.substring(2, 4), 16) / 255.0f;
                    float b = (float)Integer.parseInt(hash.substring(4, 6), 16) / 255.0f;
                    ColorInfo colorInfo = new ColorInfo(r, g, b, 1.0f);
                    zoneTypeToColor.put(zone.type, colorInfo);
                }
                if (!zoneTypeToZoneList.containsKey(zone.type)) {
                    zoneTypeToZoneList.put(zone.type, new ArrayList());
                }
                ((ArrayList)zoneTypeToZoneList.get(zone.type)).add(zone);
            }
            for (Map.Entry entry : zoneTypeToZoneList.entrySet()) {
                ColorInfo colorInfo = (ColorInfo)zoneTypeToColor.get(entry.getKey());
                this.renderZones((ArrayList)entry.getValue(), null, colorInfo.r, colorInfo.g, colorInfo.b, 0.05f);
            }
        }

        void renderZombieIntensity() {
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
            float scale = this.worldScale;
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                IsoMetaCell metaCell;
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY) || (metaCell = metaGrid.getCellData(cellX, cellY)) == null) continue;
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                vboLines.setMode(4);
                for (int x = 0; x < 32; ++x) {
                    for (int y = 0; y < 32; ++y) {
                        byte value = metaCell.info.getZombieIntensity(x + y * 32);
                        vboLines.addQuad(((float)(metaCell.getX() * 256 + x * 8) - this.centerWorldX) * scale, ((float)(metaCell.getY() * 256 + y * 8) - this.centerWorldY) * scale, ((float)(metaCell.getX() * 256 + (x + 1) * 8) - this.centerWorldX) * scale, ((float)(metaCell.getY() * 256 + (y + 1) * 8) - this.centerWorldY) * scale, 0.0f, (float)value / 255.0f * 40.0f, (float)value / 255.0f * 40.0f, (float)value / 255.0f * 40.0f, 0.75f);
                    }
                }
                vboLines.endRun();
            }
        }

        void renderZombieVoronoi(boolean cutoff) {
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
            float scale = this.worldScale;
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                List<double[]> values2;
                IsoMetaCell metaCell;
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY) || (metaCell = metaGrid.getCellData(cellX, cellY)) == null) continue;
                List<double[]> list = values2 = cutoff ? IsoWorld.instance.getZombieVoronois().stream().map(v -> v.evaluateCellCutoff(metaCell.getX(), metaCell.getY())).toList() : IsoWorld.instance.getZombieVoronois().stream().map(v -> v.evaluateCellNoise(metaCell.getX(), metaCell.getY())).toList();
                if (values2.isEmpty()) continue;
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                vboLines.setMode(4);
                for (int x = 0; x < 32; ++x) {
                    for (int y = 0; y < 32; ++y) {
                        float squareX = metaCell.getX() * 256 + x * 8;
                        float squareY = metaCell.getY() * 256 + y * 8;
                        float value = 1.0f;
                        for (double[] v2 : values2) {
                            value *= (float)v2[x + y * 32];
                        }
                        vboLines.addQuad((squareX - this.centerWorldX) * scale, (squareY - this.centerWorldY) * scale, (squareX + 8.0f - this.centerWorldX) * scale, (squareY + 8.0f - this.centerWorldY) * scale, 0.0f, value, value, value, cutoff ? 0.25f : 0.75f);
                    }
                }
                vboLines.endRun();
            }
        }

        void renderAnimalZones() {
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
            this.zoneSet.clear();
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                IsoMetaCell metaCell;
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY) || (metaCell = metaGrid.getCellData(cellX, cellY)) == null) continue;
                for (int index = 0; index < metaCell.getAnimalZonesSize(); ++index) {
                    this.zoneSet.add(metaCell.getAnimalZone(index));
                }
            }
            this.zones.clear();
            this.zones.addAll(this.zoneSet);
            this.renderZones(this.zones, "Animal", 0.0f, 0.5f, 1.0f, 1.0f);
        }

        void renderStoryZones() {
            this.zoneSet.clear();
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                IsoMetaCell metaCell;
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited != null && !this.renderer.visited.isCellVisible(cellX, cellY) || (metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY)) == null) continue;
                metaCell.getZonesUnique(this.zoneSet);
            }
            this.zones.clear();
            this.zones.addAll(this.zoneSet);
            this.renderZones(this.zones, "ZoneStory", 1.0f, 0.5f, 0.5f, 1.0f);
        }

        void renderZones(ArrayList<Zone> zones, String zoneType, float r, float g, float b, float a) {
            float y2;
            float x2;
            float y1;
            float x1;
            float scale = this.worldScale;
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(4);
            for (Zone zone : zones) {
                float y3;
                float x3;
                int i;
                float[] triangles;
                if (zoneType != null && !zoneType.equals(zone.type)) continue;
                if (zone.isRectangle()) {
                    vboLines.addQuad(((float)zone.x - this.centerWorldX) * scale, ((float)zone.y - this.centerWorldY) * scale, ((float)(zone.x + zone.w) - this.centerWorldX) * scale, ((float)(zone.y + zone.h) - this.centerWorldY) * scale, 0.0f, r, g, b, a);
                }
                if (zone.isPolygon()) {
                    triangles = zone.getPolygonTriangles();
                    if (triangles == null) continue;
                    for (i = 0; i < triangles.length; i += 6) {
                        x1 = (triangles[i] - this.centerWorldX) * scale;
                        y1 = (triangles[i + 1] - this.centerWorldY) * scale;
                        x2 = (triangles[i + 2] - this.centerWorldX) * scale;
                        y2 = (triangles[i + 3] - this.centerWorldY) * scale;
                        x3 = (triangles[i + 4] - this.centerWorldX) * scale;
                        y3 = (triangles[i + 5] - this.centerWorldY) * scale;
                        vboLines.addTriangle(x1, y1, 0.0f, x2, y2, 0.0f, x3, y3, 0.0f, r, g, b, a);
                    }
                }
                if (!zone.isPolyline() || (triangles = zone.getPolylineOutlineTriangles()) == null) continue;
                for (i = 0; i < triangles.length; i += 6) {
                    x1 = (triangles[i] - this.centerWorldX) * scale;
                    y1 = (triangles[i + 1] - this.centerWorldY) * scale;
                    x2 = (triangles[i + 2] - this.centerWorldX) * scale;
                    y2 = (triangles[i + 3] - this.centerWorldY) * scale;
                    x3 = (triangles[i + 4] - this.centerWorldX) * scale;
                    y3 = (triangles[i + 5] - this.centerWorldY) * scale;
                    vboLines.addTriangle(x1, y1, 0.0f, x2, y2, 0.0f, x3, y3, 0.0f, r, g, b, a);
                }
            }
            vboLines.endRun();
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(1);
            vboLines.setLineWidth(2.0f);
            for (Zone zone : zones) {
                if (zoneType != null && !zoneType.equals(zone.type)) continue;
                if (zone.isRectangle()) {
                    float x12 = ((float)zone.x - this.centerWorldX) * scale;
                    float y12 = ((float)zone.y - this.centerWorldY) * scale;
                    float x22 = ((float)(zone.x + zone.w) - this.centerWorldX) * scale;
                    float y22 = ((float)(zone.y + zone.h) - this.centerWorldY) * scale;
                    vboLines.addLine(x12, y12, 0.0f, x22, y12, 0.0f, r, g, b, 1.0f);
                    vboLines.addLine(x22, y12, 0.0f, x22, y22, 0.0f, r, g, b, 1.0f);
                    vboLines.addLine(x22, y22, 0.0f, x12, y22, 0.0f, r, g, b, 1.0f);
                    vboLines.addLine(x12, y22, 0.0f, x12, y12, 0.0f, r, g, b, 1.0f);
                }
                if (zone.isPolygon()) {
                    for (int i = 0; i < zone.points.size(); i += 2) {
                        float x13 = ((float)zone.points.getQuick(i) - this.centerWorldX) * scale;
                        float y13 = ((float)zone.points.getQuick(i + 1) - this.centerWorldY) * scale;
                        float x23 = ((float)zone.points.getQuick((i + 2) % zone.points.size()) - this.centerWorldX) * scale;
                        float y23 = ((float)zone.points.getQuick((i + 3) % zone.points.size()) - this.centerWorldY) * scale;
                        vboLines.addLine(x13, y13, 0.0f, x23, y23, 0.0f, r, g, b, 1.0f);
                    }
                }
                if (!zone.isPolyline()) continue;
                float[] points = zone.polylineOutlinePoints;
                if (points == null) {
                    for (int i = 0; i < zone.points.size() - 2; i += 2) {
                        x1 = ((float)zone.points.getQuick(i) + 0.5f - this.centerWorldX) * scale;
                        y1 = ((float)zone.points.getQuick(i + 1) + 0.5f - this.centerWorldY) * scale;
                        x2 = ((float)zone.points.getQuick((i + 2) % zone.points.size()) + 0.5f - this.centerWorldX) * scale;
                        y2 = ((float)zone.points.getQuick((i + 3) % zone.points.size()) + 0.5f - this.centerWorldY) * scale;
                        vboLines.addLine(x1, y1, 0.0f, x2, y2, 0.0f, r, g, b, 1.0f);
                    }
                    continue;
                }
                for (int i = 0; i < points.length; i += 2) {
                    x1 = (points[i] - this.centerWorldX) * scale;
                    y1 = (points[i + 1] - this.centerWorldY) * scale;
                    x2 = (points[(i + 2) % points.length] - this.centerWorldX) * scale;
                    y2 = (points[(i + 3) % points.length] - this.centerWorldY) * scale;
                    vboLines.addLine(x1, y1, 0.0f, x2, y2, 0.0f, r, g, b, 1.0f);
                }
            }
            vboLines.endRun();
        }

        @Override
        public void render() {
            try {
                PZGLUtil.pushAndLoadMatrix(5889, this.projection);
                PZGLUtil.pushAndLoadMatrix(5888, this.modelView);
                DefaultShader.isActive = false;
                ShaderHelper.forgetCurrentlyBound();
                GL20.glUseProgram(0);
                this.renderInternal();
                ShaderHelper.glUseProgramObjectARB(0);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
            finally {
                PZGLUtil.popMatrix(5889);
                PZGLUtil.popMatrix(5888);
            }
        }

        private void renderInternal() {
            float worldScale = this.worldScale;
            int minX = this.worldMap.getMinXInSquares();
            int minY = this.worldMap.getMinYInSquares();
            int maxX = this.worldMap.getMaxXInSquares();
            int maxY = this.worldMap.getMaxYInSquares();
            GL11.glViewport(this.x, Core.height - this.height - this.y, this.width, this.height);
            GLVertexBufferObject.funcs.glBindBuffer(GLVertexBufferObject.funcs.GL_ARRAY_BUFFER(), 0);
            GLVertexBufferObject.funcs.glBindBuffer(GLVertexBufferObject.funcs.GL_ELEMENT_ARRAY_BUFFER(), 0);
            GL11.glPolygonMode(1032, this.renderer.wireframe.getValue() ? 6913 : 6914);
            if (this.renderer.imagePyramid.getValue()) {
                this.renderImagePyramids();
            }
            this.calculateVisibleCells();
            if (this.renderer.features.getValue()) {
                this.filterFeatures();
                this.renderCellFeatures();
            } else {
                this.renderNonCellFeatures();
            }
            this.streetRenderData.render(this.renderer, vboLines);
            if (this.renderer.getBoolean("Symbols")) {
                this.symbolsRenderData.render(this, false);
            }
            if (this.renderer.otherZones.getValue()) {
                this.renderOtherZones();
            }
            if (this.renderer.forestZones.getValue()) {
                this.renderZones();
            }
            if (this.renderer.zombieIntensity.getValue()) {
                this.renderZombieIntensity();
            }
            if (this.renderer.zombieVoronoi.getValue()) {
                this.renderZombieVoronoi(false);
            }
            if (this.renderer.zombieCutoff.getValue()) {
                this.renderZombieVoronoi(true);
            }
            if (this.renderer.animals.getValue() || this.renderer.animalTracks.getValue()) {
                this.renderAnimalZones();
            }
            if (this.renderer.storyZones.getValue()) {
                this.renderStoryZones();
            }
            if (this.renderer.visibleCells.getValue()) {
                this.renderVisibleCells();
            }
            vboLines.flush();
            GL13.glActiveTexture(33984);
            GL11.glTexEnvi(8960, 8704, 8448);
            GL11.glPolygonMode(1032, 6914);
            GL11.glEnable(3042);
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            SpriteRenderer.ringBuffer.restoreVbos = true;
            if (this.renderer.visited != null) {
                this.renderer.visited.render(this.renderOriginX - (float)(this.worldMap.getMinXInSquares() - this.renderer.visited.getMinX() * 256) * worldScale, this.renderOriginY - (float)(this.worldMap.getMinYInSquares() - this.renderer.visited.getMinY() * 256) * worldScale, minX / 256, minY / 256, maxX / 256, maxY / 256, worldScale, this.renderer.blurUnvisited.getValue());
                if (this.renderer.unvisitedGrid.getValue()) {
                    this.renderer.visited.renderGrid(this.renderOriginX - (float)(this.worldMap.getMinXInSquares() - this.renderer.visited.getMinX() * 256) * worldScale, this.renderOriginY - (float)(this.worldMap.getMinYInSquares() - this.renderer.visited.getMinY() * 256) * worldScale, minX / 256, minY / 256, maxX / 256, maxY / 256, worldScale, this.zoomF);
                }
            }
            this.renderPlayers();
            if (this.renderer.cellGrid.getValue()) {
                this.renderCellGrid(minX / 256, minY / 256, maxX / 256, maxY / 256);
            }
            if (Core.debug) {
                // empty if block
            }
            this.paintAreasOutsideBounds(minX, minY, maxX, maxY, worldScale);
            if (this.renderer.worldBounds.getValue()) {
                this.renderWorldBounds();
            }
            if (this.renderer.getBoolean("Symbols")) {
                this.symbolsRenderData.render(this, true);
            }
            vboLines.flush();
            GL11.glViewport(0, 0, Core.width, Core.height);
        }

        private void rasterizeCellsCallback(int startX, int startY) {
            int setId = startX + startY * this.worldMap.getWidthInCells();
            if (this.rasterizeSet.contains(setId)) {
                return;
            }
            for (int y = startY * this.rasterizeMult; y < startY * this.rasterizeMult + this.rasterizeMult; ++y) {
                for (int x = startX * this.rasterizeMult; x < startX * this.rasterizeMult + this.rasterizeMult; ++x) {
                    if (x < this.worldMap.getMinXInCells() || x > this.worldMap.getMaxXInCells() || y < this.worldMap.getMinYInCells() || y > this.worldMap.getMaxYInCells()) continue;
                    this.rasterizeSet.add(setId);
                    this.rasterizeXy.add(x);
                    this.rasterizeXy.add(y);
                }
            }
        }

        private void rasterizeTilesCallback(int x, int y) {
            int setId = x + y * 1000;
            if (this.rasterizeSet.contains(setId)) {
                return;
            }
            if ((float)x < this.rasterizeMinTileX || (float)x > this.rasterizeMaxTileX || (float)y < this.rasterizeMinTileY || (float)y > this.rasterizeMaxTileY) {
                return;
            }
            this.rasterizeSet.add(setId);
            this.rasterizePyramidXy.add(x);
            this.rasterizePyramidXy.add(y);
        }

        private void calculateVisibleCells() {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleCells.getValue();
            int pad = bDebugRasterize ? 200 : 0;
            float worldScale = this.worldScale;
            if (1.0f / worldScale > 100.0f) {
                this.rasterizeXy.resetQuick();
                for (int y = this.worldMap.getMinYInCells(); y <= this.worldMap.getMaxYInCells(); ++y) {
                    for (int x = this.worldMap.getMinXInCells(); x <= this.worldMap.getMaxXInCells(); ++x) {
                        this.rasterizeXy.add(x);
                        this.rasterizeXy.add(y);
                    }
                }
                if (this.rasterizeXyInts == null || this.rasterizeXyInts.length < this.rasterizeXy.size()) {
                    this.rasterizeXyInts = new int[this.rasterizeXy.size()];
                }
                this.rasterizeXyInts = this.rasterizeXy.toArray(this.rasterizeXyInts);
                return;
            }
            float xTL = this.uiToWorldX((float)pad + 0.0f, (float)pad + 0.0f) / 256.0f;
            float yTL = this.uiToWorldY((float)pad + 0.0f, (float)pad + 0.0f) / 256.0f;
            float xTR = this.uiToWorldX(this.getWidth() - pad, 0.0f + (float)pad) / 256.0f;
            float yTR = this.uiToWorldY(this.getWidth() - pad, 0.0f + (float)pad) / 256.0f;
            float xBR = this.uiToWorldX(this.getWidth() - pad, this.getHeight() - pad) / 256.0f;
            float yBR = this.uiToWorldY(this.getWidth() - pad, this.getHeight() - pad) / 256.0f;
            float xBL = this.uiToWorldX(0.0f + (float)pad, this.getHeight() - pad) / 256.0f;
            float yBL = this.uiToWorldY(0.0f + (float)pad, this.getHeight() - pad) / 256.0f;
            int mult = 1;
            while (this.triangleArea(xBL / (float)mult, yBL / (float)mult, xBR / (float)mult, yBR / (float)mult, xTR / (float)mult, yTR / (float)mult) + this.triangleArea(xTR / (float)mult, yTR / (float)mult, xTL / (float)mult, yTL / (float)mult, xBL / (float)mult, yBL / (float)mult) > 80.0f) {
                ++mult;
            }
            this.rasterizeMult = mult;
            this.rasterizeXy.resetQuick();
            this.rasterizeSet.clear();
            this.rasterize.scanTriangle(xBL / (float)mult, yBL / (float)mult, xBR / (float)mult, yBR / (float)mult, xTR / (float)mult, yTR / (float)mult, -1000, 1000, this::rasterizeCellsCallback);
            this.rasterize.scanTriangle(xTR / (float)mult, yTR / (float)mult, xTL / (float)mult, yTL / (float)mult, xBL / (float)mult, yBL / (float)mult, -1000, 1000, this::rasterizeCellsCallback);
            if (this.rasterizeXyInts == null || this.rasterizeXyInts.length < this.rasterizeXy.size()) {
                this.rasterizeXyInts = new int[this.rasterizeXy.size()];
            }
            this.rasterizeXyInts = this.rasterizeXy.toArray(this.rasterizeXyInts);
        }

        void renderVisibleCells() {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleCells.getValue();
            int pad = bDebugRasterize ? 200 : 0;
            float worldScale = this.worldScale;
            if (1.0f / worldScale > 100.0f) {
                return;
            }
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(4);
            for (int i = 0; i < this.rasterizeXy.size(); i += 2) {
                int x = this.rasterizeXy.get(i);
                int y = this.rasterizeXy.get(i + 1);
                float x0 = this.renderOriginX + (float)(x * 256 - this.worldMap.getMinXInSquares()) * worldScale;
                float y0 = this.renderOriginY + (float)(y * 256 - this.worldMap.getMinYInSquares()) * worldScale;
                float x1 = this.renderOriginX + (float)((x + 1) * 256 - this.worldMap.getMinXInSquares()) * worldScale;
                float y1 = this.renderOriginY + (float)((y + 1) * 256 - this.worldMap.getMinYInSquares()) * worldScale;
                vboLines.reserve(3);
                vboLines.addElement(x0, y0, 0.0f, 0.0f, 1.0f, 0.0f, 0.2f);
                vboLines.addElement(x1, y0, 0.0f, 0.0f, 1.0f, 0.0f, 0.2f);
                vboLines.addElement(x0, y1, 0.0f, 0.0f, 1.0f, 0.0f, 0.2f);
                vboLines.reserve(3);
                vboLines.addElement(x1, y0, 0.0f, 0.0f, 0.0f, 1.0f, 0.2f);
                vboLines.addElement(x1, y1, 0.0f, 0.0f, 0.0f, 1.0f, 0.2f);
                vboLines.addElement(x0, y1, 0.0f, 0.0f, 0.0f, 1.0f, 0.2f);
            }
            vboLines.endRun();
            float xTL = this.uiToWorldX((float)pad + 0.0f, (float)pad + 0.0f) / 256.0f;
            float yTL = this.uiToWorldY((float)pad + 0.0f, (float)pad + 0.0f) / 256.0f;
            float xTR = this.uiToWorldX(this.getWidth() - pad, 0.0f + (float)pad) / 256.0f;
            float yTR = this.uiToWorldY(this.getWidth() - pad, 0.0f + (float)pad) / 256.0f;
            float xBR = this.uiToWorldX(this.getWidth() - pad, this.getHeight() - pad) / 256.0f;
            float yBR = this.uiToWorldY(this.getWidth() - pad, this.getHeight() - pad) / 256.0f;
            float xBL = this.uiToWorldX(0.0f + (float)pad, this.getHeight() - pad) / 256.0f;
            float yBL = this.uiToWorldY(0.0f + (float)pad, this.getHeight() - pad) / 256.0f;
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(1);
            vboLines.setLineWidth(4.0f);
            vboLines.addLine(this.renderOriginX + (xBL * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBL * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xBR * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBR * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
            vboLines.addLine(this.renderOriginX + (xBR * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBR * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xTR * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTR * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
            vboLines.addLine(this.renderOriginX + (xTR * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTR * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xBL * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBL * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f);
            vboLines.addLine(this.renderOriginX + (xTR * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTR * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xTL * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTL * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
            vboLines.addLine(this.renderOriginX + (xTL * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTL * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xBL * 256.0f - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBL * 256.0f - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
            vboLines.endRun();
        }

        double getZoomAdjustedForPyramidScale(float zoomF, WorldMapImages images) {
            double mppaz = MapProjection.metersPerPixelAtZoom(zoomF, (float)this.getHeight() * images.getResolution() * 2.0f);
            return MapProjection.zoomAtMetersPerPixel(mppaz, this.getHeight());
        }

        void calcVisiblePyramidTiles(WorldMapImages images, int ptz) {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleTiles.getValue();
            int pad = bDebugRasterize ? PZMath.min(this.width / 20, 200) : 0;
            float worldScale = this.worldScale;
            int tileSize = 256;
            float metersPerTile = (float)images.getPyramid().calculateMetersPerTile(ptz);
            int tileOriginWorldX = images.getMinX();
            int tileOriginWorldY = images.getMinY();
            float xTL = (this.uiToWorldX((float)pad + 0.0f, (float)pad + 0.0f) - (float)tileOriginWorldX) / metersPerTile;
            float yTL = (this.uiToWorldY((float)pad + 0.0f, (float)pad + 0.0f) - (float)tileOriginWorldY) / metersPerTile;
            float xTR = (this.uiToWorldX(this.getWidth() - pad, 0.0f + (float)pad) - (float)tileOriginWorldX) / metersPerTile;
            float yTR = (this.uiToWorldY(this.getWidth() - pad, 0.0f + (float)pad) - (float)tileOriginWorldY) / metersPerTile;
            float xBR = (this.uiToWorldX(this.getWidth() - pad, this.getHeight() - pad) - (float)tileOriginWorldX) / metersPerTile;
            float yBR = (this.uiToWorldY(this.getWidth() - pad, this.getHeight() - pad) - (float)tileOriginWorldY) / metersPerTile;
            float xBL = (this.uiToWorldX(0.0f + (float)pad, this.getHeight() - pad) - (float)tileOriginWorldX) / metersPerTile;
            float yBL = (this.uiToWorldY(0.0f + (float)pad, this.getHeight() - pad) - (float)tileOriginWorldY) / metersPerTile;
            this.rasterizePyramidXy.resetQuick();
            this.rasterizeSet.clear();
            this.rasterizeMinTileX = (int)((float)(this.worldMap.getMinXInSquares() - images.getMinX()) / metersPerTile);
            this.rasterizeMinTileY = (int)((float)(this.worldMap.getMinYInSquares() - images.getMinY()) / metersPerTile);
            this.rasterizeMaxTileX = (float)(this.worldMap.getMaxXInSquares() - images.getMinX()) / metersPerTile;
            this.rasterizeMaxTileY = (float)(this.worldMap.getMaxYInSquares() - images.getMinY()) / metersPerTile;
            this.rasterize.scanTriangle(xBL, yBL, xBR, yBR, xTR, yTR, -1000, 1000, this::rasterizeTilesCallback);
            this.rasterize.scanTriangle(xTR, yTR, xTL, yTL, xBL, yBL, -1000, 1000, this::rasterizeTilesCallback);
            this.rasterizeTileBounds[0] = Integer.MAX_VALUE;
            this.rasterizeTileBounds[1] = Integer.MAX_VALUE;
            this.rasterizeTileBounds[2] = Integer.MIN_VALUE;
            this.rasterizeTileBounds[3] = Integer.MIN_VALUE;
            for (int i = 0; i < this.rasterizePyramidXy.size() - 1; i += 2) {
                int ptx = this.rasterizePyramidXy.getQuick(i);
                int pty = this.rasterizePyramidXy.getQuick(i + 1);
                this.rasterizeTileBounds[0] = PZMath.min(this.rasterizeTileBounds[0], ptx);
                this.rasterizeTileBounds[1] = PZMath.min(this.rasterizeTileBounds[1], pty);
                this.rasterizeTileBounds[2] = PZMath.max(this.rasterizeTileBounds[2], ptx);
                this.rasterizeTileBounds[3] = PZMath.max(this.rasterizeTileBounds[3], pty);
            }
        }

        void renderVisiblePyramidTiles(WorldMapImages images, int ptz) {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleTiles.getValue();
            int pad = bDebugRasterize ? PZMath.min(this.width / 20, 200) : 0;
            float worldScale = this.worldScale;
            float metersPerTile = (float)images.getPyramid().calculateMetersPerTile(ptz);
            int tileOriginWorldX = images.getMinX();
            int tileOriginWorldY = images.getMinY();
            float xTL = (this.uiToWorldX((float)pad + 0.0f, (float)pad + 0.0f) - (float)tileOriginWorldX) / metersPerTile;
            float yTL = (this.uiToWorldY((float)pad + 0.0f, (float)pad + 0.0f) - (float)tileOriginWorldY) / metersPerTile;
            float xTR = (this.uiToWorldX(this.getWidth() - pad, 0.0f + (float)pad) - (float)tileOriginWorldX) / metersPerTile;
            float yTR = (this.uiToWorldY(this.getWidth() - pad, 0.0f + (float)pad) - (float)tileOriginWorldY) / metersPerTile;
            float xBR = (this.uiToWorldX(this.getWidth() - pad, this.getHeight() - pad) - (float)tileOriginWorldX) / metersPerTile;
            float yBR = (this.uiToWorldY(this.getWidth() - pad, this.getHeight() - pad) - (float)tileOriginWorldY) / metersPerTile;
            float xBL = (this.uiToWorldX(0.0f + (float)pad, this.getHeight() - pad) - (float)tileOriginWorldX) / metersPerTile;
            float yBL = (this.uiToWorldY(0.0f + (float)pad, this.getHeight() - pad) - (float)tileOriginWorldY) / metersPerTile;
            if (bDebugRasterize) {
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                vboLines.setMode(1);
                vboLines.setLineWidth(4.0f);
                vboLines.addLine(this.renderOriginX + (xBL * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBL * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xBR * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBR * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                vboLines.addLine(this.renderOriginX + (xBR * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBR * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xTR * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTR * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                vboLines.addLine(this.renderOriginX + (xTR * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTR * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xBL * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBL * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f);
                vboLines.addLine(this.renderOriginX + (xTR * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTR * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xTL * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTL * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
                vboLines.addLine(this.renderOriginX + (xTL * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yTL * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, this.renderOriginX + (xBL * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale, this.renderOriginY + (yBL * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
                vboLines.endRun();
            }
            if (bDebugRasterize) {
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                vboLines.setMode(4);
                for (int i = 0; i < this.rasterizePyramidXy.size(); i += 2) {
                    int x = this.rasterizePyramidXy.get(i);
                    int y = this.rasterizePyramidXy.get(i + 1);
                    float x0 = this.renderOriginX + ((float)x * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale;
                    float y0 = this.renderOriginY + ((float)y * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale;
                    float x1 = this.renderOriginX + ((float)(x + 1) * metersPerTile - (float)this.worldMap.getMinXInSquares()) * worldScale;
                    float y1 = this.renderOriginY + ((float)(y + 1) * metersPerTile - (float)this.worldMap.getMinYInSquares()) * worldScale;
                    vboLines.addElement(x0, y0, 0.0f, 0.0f, 1.0f, 0.0f, 0.2f);
                    vboLines.addElement(x1, y0, 0.0f, 0.0f, 1.0f, 0.0f, 0.2f);
                    vboLines.addElement(x0, y1, 0.0f, 0.0f, 1.0f, 0.0f, 0.2f);
                    vboLines.addElement(x1, y0, 0.0f, 0.0f, 0.0f, 1.0f, 0.2f);
                    vboLines.addElement(x1, y1, 0.0f, 0.0f, 0.0f, 1.0f, 0.2f);
                    vboLines.addElement(x0, y1, 0.0f, 0.0f, 0.0f, 1.0f, 0.2f);
                }
                vboLines.endRun();
            }
        }

        boolean hasAnyImagePyramidStyleLayers() {
            for (int i = 0; i < this.style.getLayerCount(); ++i) {
                WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(i);
                if (!(styleLayer instanceof WorldMapPyramidStyleLayer)) continue;
                return true;
            }
            return false;
        }

        void renderImagePyramids() {
            if (this.hasAnyImagePyramidStyleLayers()) {
                for (int i = 0; i < this.worldMap.getImagesCount(); ++i) {
                    WorldMapImages images = this.worldMap.getImagesByIndex(i);
                    if (this.findPyramidStyleLayer(images) == null) continue;
                    this.calculateVisibleCells();
                    int ptz = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images));
                    images.getPyramid().calculateRequiredTilesForCells(this.rasterizeXy, ptz);
                }
                return;
            }
            for (int i = this.worldMap.getImagesCount() - 1; i >= 0; --i) {
                WorldMapImages images = this.worldMap.getImagesByIndex(i);
                this.renderImagePyramid(images);
                GL11.glDisable(3553);
            }
        }

        WorldMapPyramidStyleLayer findPyramidStyleLayer(WorldMapImages images) {
            this.renderArgs.renderer = this.renderer;
            this.renderArgs.drawer = this;
            this.renderArgs.cellX = Integer.MIN_VALUE;
            this.renderArgs.cellY = Integer.MIN_VALUE;
            WorldMapPyramidStyleLayer pyramidStyleLayer = null;
            for (int i = 0; i < this.style.getLayerCount(); ++i) {
                WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(i);
                if (!(styleLayer instanceof WorldMapPyramidStyleLayer)) continue;
                WorldMapPyramidStyleLayer layer = (WorldMapPyramidStyleLayer)styleLayer;
                if (!images.getAbsolutePath().endsWith(File.separator + layer.fileName)) continue;
                pyramidStyleLayer = layer;
                if (!layer.isVisible(this.renderArgs)) continue;
                return layer;
            }
            return pyramidStyleLayer;
        }

        public void renderImagePyramid(WorldMapImages images) {
            ImagePyramid.PyramidTexture pyramidTexture;
            if (!this.renderer.imagePyramid.getValue()) {
                return;
            }
            ImagePyramid pyramid = images.getPyramid();
            float worldScale = this.worldScale;
            int ptz1 = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images));
            double metersPerTile = pyramid.calculateMetersPerTile(ptz1);
            this.calcVisiblePyramidTiles(images, ptz1);
            pyramid.calculateRequiredTiles(this.rasterizePyramidXy, ptz1);
            int tileSpanX = this.rasterizeTileBounds[2] - this.rasterizeTileBounds[0] + 1;
            int tileSpanY = this.rasterizeTileBounds[3] - this.rasterizeTileBounds[1] + 1;
            if (this.renderedTiles == null || this.renderedTiles.length < tileSpanX * tileSpanY) {
                this.renderedTiles = new byte[tileSpanX * tileSpanY];
            }
            Arrays.fill(this.renderedTiles, (byte)0);
            GL11.glEnable(3553);
            GL11.glEnable(3042);
            int clipX1 = PZMath.clamp(images.getMinX(), this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares());
            int clipY1 = PZMath.clamp(images.getMinY(), this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares());
            int clipX2 = PZMath.clamp(images.getMaxX(), this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares() + 1);
            int clipY2 = PZMath.clamp(images.getMaxY(), this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares() + 1);
            WorldMapPyramidStyleLayer pyramidStyleLayer = this.findPyramidStyleLayer(images);
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            float a = 1.0f;
            if (pyramidStyleLayer != null) {
                if (!pyramidStyleLayer.isVisible(this.renderArgs)) {
                    return;
                }
                WorldMapStyleLayer.RGBAf fill = pyramidStyleLayer.getFill(this.renderArgs);
                r = fill.r;
                g = fill.g;
                b = fill.b;
                a = fill.a;
                WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            }
            for (int i = 0; i < this.rasterizePyramidXy.size() - 1; i += 2) {
                int ptx = this.rasterizePyramidXy.getQuick(i);
                int pty = this.rasterizePyramidXy.getQuick(i + 1);
                int lx = ptx - this.rasterizeTileBounds[0];
                int ly = pty - this.rasterizeTileBounds[1];
                if (!pyramid.isValidTile(ptx, pty, ptz1)) continue;
                int n = lx + ly * tileSpanX;
                this.renderedTiles[n] = (byte)(this.renderedTiles[n] | 1);
                pyramidTexture = pyramid.getTexture(ptx, pty, ptz1);
                if (pyramidTexture == null || !pyramidTexture.isReady()) {
                    if (pyramidTexture == null) continue;
                    boolean bl = true;
                    continue;
                }
                TextureID textureID = pyramidTexture.getTextureID();
                if (textureID == null || !textureID.isReady()) continue;
                double worldX1 = (double)images.getMinX() + (double)ptx * metersPerTile;
                double worldY1 = (double)images.getMinY() + (double)pty * metersPerTile;
                double worldX2 = worldX1 + metersPerTile;
                double worldY2 = worldY1 + metersPerTile;
                double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                double x1 = (worldX1c - (double)this.centerWorldX) * (double)worldScale;
                double y1 = (worldY1c - (double)this.centerWorldY) * (double)worldScale;
                double x2 = (worldX2c - (double)this.centerWorldX) * (double)worldScale;
                double y2 = (worldY1c - (double)this.centerWorldY) * (double)worldScale;
                double x3 = (worldX2c - (double)this.centerWorldX) * (double)worldScale;
                double y3 = (worldY2c - (double)this.centerWorldY) * (double)worldScale;
                double x4 = (worldX1c - (double)this.centerWorldX) * (double)worldScale;
                double y4 = (worldY2c - (double)this.centerWorldY) * (double)worldScale;
                double u1 = (worldX1c - worldX1) / metersPerTile;
                double v1 = (worldY1c - worldY1) / metersPerTile;
                double u2 = (worldX2c - worldX1) / metersPerTile;
                double v2 = (worldY1c - worldY1) / metersPerTile;
                double u3 = (worldX2c - worldX1) / metersPerTile;
                double v3 = (worldY2c - worldY1) / metersPerTile;
                double u4 = (worldX1c - worldX1) / metersPerTile;
                double v4 = (worldY2c - worldY1) / metersPerTile;
                vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                vboLines.setMode(7);
                vboLines.setTextureID(textureID);
                vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                vboLines.addQuad((float)x1, (float)y1, 0.0f, (float)u1, (float)v1, (float)x2, (float)y2, 0.0f, (float)u2, (float)v2, (float)x3, (float)y3, 0.0f, (float)u3, (float)v3, (float)x4, (float)y4, 0.0f, (float)u4, (float)v4, r, g, b, a);
                vboLines.endRun();
                if (this.renderer.tileGrid.getValue()) {
                    vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                    vboLines.setMode(1);
                    vboLines.setLineWidth(2.0f);
                    vboLines.addRectOutline((float)(worldX1 - (double)this.centerWorldX) * worldScale, (float)(worldY1 - (double)this.centerWorldY) * worldScale, (float)(worldX2 - (double)this.centerWorldX) * worldScale, (float)(worldY2 - (double)this.centerWorldY) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                    vboLines.endRun();
                }
                int n2 = lx + ly * tileSpanX;
                this.renderedTiles[n2] = (byte)(this.renderedTiles[n2] | 2);
            }
            int ptz = ptz1;
            for (int ly = 0; ly < tileSpanY; ++ly) {
                for (int lx = 0; lx < tileSpanX; ++lx) {
                    if ((this.renderedTiles[lx + ly * tileSpanX] & 1) == 0 || (this.renderedTiles[lx + ly * tileSpanX] & 2) != 0) continue;
                    int ptx = this.rasterizeTileBounds[0] + lx;
                    int pty = this.rasterizeTileBounds[1] + ly;
                    pyramidTexture = pyramid.getLowerResTexture(ptx, pty, ptz);
                    if (pyramidTexture == null || !pyramidTexture.isReady()) {
                        if (pyramidTexture == null) continue;
                        boolean textureID = true;
                        continue;
                    }
                    TextureID textureID = pyramidTexture.getTextureID();
                    if (textureID == null || !textureID.isReady()) {
                        boolean dbg = true;
                        continue;
                    }
                    int ptx1 = pyramidTexture.x;
                    int pty1 = pyramidTexture.y;
                    ptz1 = pyramidTexture.z;
                    double metersPerTile1 = pyramid.calculateMetersPerTile(ptz1);
                    double worldX1 = (double)images.getMinX() + (double)ptx1 * metersPerTile1;
                    double worldY1 = (double)images.getMinY() + (double)pty1 * metersPerTile1;
                    double worldX2 = worldX1 + metersPerTile1;
                    double worldY2 = worldY1 + metersPerTile1;
                    double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                    double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                    double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                    double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                    double hx1 = (double)images.getMinX() + (double)ptx * metersPerTile;
                    double hy1 = (double)images.getMinY() + (double)pty * metersPerTile;
                    double hx2 = hx1 + metersPerTile;
                    double hy2 = hy1 + metersPerTile;
                    worldX1c = PZMath.clamp(worldX1c, hx1, hx2);
                    worldY1c = PZMath.clamp(worldY1c, hy1, hy2);
                    worldX2c = PZMath.clamp(worldX2c, hx1, hx2);
                    worldY2c = PZMath.clamp(worldY2c, hy1, hy2);
                    double x1 = (worldX1c - (double)this.centerWorldX) * (double)worldScale;
                    double y1 = (worldY1c - (double)this.centerWorldY) * (double)worldScale;
                    double x2 = (worldX2c - (double)this.centerWorldX) * (double)worldScale;
                    double y2 = (worldY2c - (double)this.centerWorldY) * (double)worldScale;
                    double u1 = (worldX1c - worldX1) / metersPerTile1;
                    double v1 = (worldY1c - worldY1) / metersPerTile1;
                    double u2 = (worldX2c - worldX1) / metersPerTile1;
                    double v2 = (worldY2c - worldY1) / metersPerTile1;
                    vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                    vboLines.setMode(7);
                    vboLines.setTextureID(textureID);
                    vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                    vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                    vboLines.addQuad((float)x1, (float)y1, (float)u1, (float)v1, (float)x2, (float)y2, (float)u2, (float)v2, 0.0f, r, g, b, a);
                    vboLines.endRun();
                    if (this.renderer.tileGrid.getValue()) {
                        vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                        vboLines.setMode(1);
                        vboLines.setLineWidth(2.0f);
                        vboLines.addRectOutline((float)(worldX1 - (double)this.centerWorldX) * worldScale, (float)(worldY1 - (double)this.centerWorldY) * worldScale, (float)(worldX2 - (double)this.centerWorldX) * worldScale, (float)(worldY2 - (double)this.centerWorldY) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                        vboLines.endRun();
                    }
                    if (!Core.debug) continue;
                }
            }
            if (Core.debug && this.renderer.visibleTiles.getValue()) {
                this.renderVisiblePyramidTiles(images, ptz);
            }
        }

        public void drawImagePyramid(int cellX, int cellY, String fileName, WorldMapStyleLayer.RGBAf fill) {
            for (int i = 0; i < this.worldMap.getImagesCount(); ++i) {
                WorldMapImages images = this.worldMap.getImagesByIndex(i);
                if (!images.getAbsolutePath().endsWith(File.separator + fileName)) continue;
                this.drawImagePyramid(cellX, cellY, images, fill);
            }
        }

        public void drawImagePyramid(int cellX, int cellY, WorldMapImages images, WorldMapStyleLayer.RGBAf fill) {
            int ptz1;
            if (!this.renderer.imagePyramid.getValue()) {
                return;
            }
            ImagePyramid pyramid = images.getPyramid();
            if (!pyramid.calculateTilesCoveringCell(cellX, cellY, ptz1 = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images)), s_tilesCoveringCell)) {
                return;
            }
            int clipX1 = PZMath.clamp(images.getMinX(), cellX * 256, (cellX + 1) * 256);
            int clipY1 = PZMath.clamp(images.getMinY(), cellY * 256, (cellY + 1) * 256);
            int clipX2 = PZMath.clamp(images.getMaxX(), cellX * 256, (cellX + 1) * 256);
            int clipY2 = PZMath.clamp(images.getMaxY(), cellY * 256, (cellY + 1) * 256);
            if (clipX1 == clipX2 || clipY1 == clipY2) {
                return;
            }
            float worldScale = this.worldScale;
            double metersPerTile = pyramid.calculateMetersPerTile(ptz1);
            clipX1 = PZMath.clamp(clipX1, this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares());
            clipY1 = PZMath.clamp(clipY1, this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares());
            clipX2 = PZMath.clamp(clipX2, this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares() + 1);
            clipY2 = PZMath.clamp(clipY2, this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares() + 1);
            int tileMinX = s_tilesCoveringCell[0];
            int tileMinY = s_tilesCoveringCell[1];
            int tileMaxX = s_tilesCoveringCell[2];
            int tileMaxY = s_tilesCoveringCell[3];
            int tileSpanX = tileMaxX - tileMinX + 1;
            int tileSpanY = tileMaxY - tileMinY + 1;
            if (this.renderedTiles == null || this.renderedTiles.length < tileSpanX * tileSpanY) {
                this.renderedTiles = new byte[tileSpanX * tileSpanY];
            }
            Arrays.fill(this.renderedTiles, (byte)0);
            boolean bGLinit = false;
            for (int pty = tileMinY; pty <= tileMaxY; ++pty) {
                for (int ptx = tileMinX; ptx <= tileMaxX; ++ptx) {
                    if (!pyramid.isValidTile(ptx, pty, ptz1)) continue;
                    int n = ptx - tileMinX + (pty - tileMinY) * tileSpanX;
                    this.renderedTiles[n] = (byte)(this.renderedTiles[n] | 1);
                    ImagePyramid.PyramidTexture pyramidTexture = pyramid.getTexture(ptx, pty, ptz1);
                    if (pyramidTexture == null || !pyramidTexture.isReady()) {
                        if (pyramidTexture == null) continue;
                        boolean bl = true;
                        continue;
                    }
                    TextureID textureID = pyramidTexture.getTextureID();
                    if (textureID == null || !textureID.isReady()) continue;
                    double worldX1 = (double)images.getMinX() + (double)ptx * metersPerTile;
                    double worldY1 = (double)images.getMinY() + (double)pty * metersPerTile;
                    double worldX2 = worldX1 + metersPerTile;
                    double worldY2 = worldY1 + metersPerTile;
                    double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                    double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                    double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                    double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                    if (worldX2c <= worldX1c || worldY2c <= worldY1c) {
                        int n2 = ptx - tileMinX + (pty - tileMinY) * tileSpanX;
                        this.renderedTiles[n2] = (byte)(this.renderedTiles[n2] | 2);
                        continue;
                    }
                    double x1 = (worldX1c - (double)this.centerWorldX) * (double)worldScale;
                    double y1 = (worldY1c - (double)this.centerWorldY) * (double)worldScale;
                    double x2 = (worldX2c - (double)this.centerWorldX) * (double)worldScale;
                    double y2 = (worldY1c - (double)this.centerWorldY) * (double)worldScale;
                    double x3 = (worldX2c - (double)this.centerWorldX) * (double)worldScale;
                    double y3 = (worldY2c - (double)this.centerWorldY) * (double)worldScale;
                    double x4 = (worldX1c - (double)this.centerWorldX) * (double)worldScale;
                    double y4 = (worldY2c - (double)this.centerWorldY) * (double)worldScale;
                    double u1 = (worldX1c - worldX1) / metersPerTile;
                    double v1 = (worldY1c - worldY1) / metersPerTile;
                    double u2 = (worldX2c - worldX1) / metersPerTile;
                    double v2 = (worldY1c - worldY1) / metersPerTile;
                    double u3 = (worldX2c - worldX1) / metersPerTile;
                    double v3 = (worldY2c - worldY1) / metersPerTile;
                    double u4 = (worldX1c - worldX1) / metersPerTile;
                    double v4 = (worldY2c - worldY1) / metersPerTile;
                    if (!bGLinit) {
                        bGLinit = true;
                        GL11.glEnable(3553);
                        GL11.glEnable(3042);
                    }
                    vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                    vboLines.setMode(7);
                    vboLines.setTextureID(textureID);
                    vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                    vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                    vboLines.addQuad((float)x1, (float)y1, 0.0f, (float)u1, (float)v1, (float)x2, (float)y2, 0.0f, (float)u2, (float)v2, (float)x3, (float)y3, 0.0f, (float)u3, (float)v3, (float)x4, (float)y4, 0.0f, (float)u4, (float)v4, fill.r, fill.g, fill.b, fill.a);
                    vboLines.endRun();
                    if (this.renderer.tileGrid.getValue()) {
                        vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                        vboLines.setMode(1);
                        vboLines.setLineWidth(2.0f);
                        vboLines.addRectOutline((float)(worldX1 - (double)this.centerWorldX) * worldScale, (float)(worldY1 - (double)this.centerWorldY) * worldScale, (float)(worldX2 - (double)this.centerWorldX) * worldScale, (float)(worldY2 - (double)this.centerWorldY) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                        vboLines.endRun();
                    }
                    int n3 = ptx - tileMinX + (pty - tileMinY) * tileSpanX;
                    this.renderedTiles[n3] = (byte)(this.renderedTiles[n3] | 2);
                }
            }
            int ptz = ptz1;
            for (int ly = 0; ly < tileSpanY; ++ly) {
                for (int lx = 0; lx < tileSpanX; ++lx) {
                    if ((this.renderedTiles[lx + ly * tileSpanX] & 1) == 0 || (this.renderedTiles[lx + ly * tileSpanX] & 2) != 0) continue;
                    int ptx = tileMinX + lx;
                    int pty = tileMinY + ly;
                    ImagePyramid.PyramidTexture pyramidTexture = pyramid.getLowerResTexture(ptx, pty, ptz);
                    if (pyramidTexture == null || !pyramidTexture.isReady()) {
                        if (pyramidTexture == null) continue;
                        boolean worldY1 = true;
                        continue;
                    }
                    TextureID textureID = pyramidTexture.getTextureID();
                    if (textureID == null || !textureID.isReady()) {
                        boolean dbg = true;
                        continue;
                    }
                    int ptx1 = pyramidTexture.x;
                    int pty1 = pyramidTexture.y;
                    ptz1 = pyramidTexture.z;
                    double metersPerTile1 = pyramid.calculateMetersPerTile(ptz1);
                    double worldX1 = (double)images.getMinX() + (double)ptx1 * metersPerTile1;
                    double worldY1 = (double)images.getMinY() + (double)pty1 * metersPerTile1;
                    double worldX2 = worldX1 + metersPerTile1;
                    double worldY2 = worldY1 + metersPerTile1;
                    double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                    double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                    double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                    double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                    double hx1 = (double)images.getMinX() + (double)ptx * metersPerTile;
                    double hy1 = (double)images.getMinY() + (double)pty * metersPerTile;
                    double hx2 = hx1 + metersPerTile;
                    double hy2 = hy1 + metersPerTile;
                    worldX1c = PZMath.clamp(worldX1c, hx1, hx2);
                    worldY1c = PZMath.clamp(worldY1c, hy1, hy2);
                    worldX2c = PZMath.clamp(worldX2c, hx1, hx2);
                    worldY2c = PZMath.clamp(worldY2c, hy1, hy2);
                    double x1 = (worldX1c - (double)this.centerWorldX) * (double)worldScale;
                    double y1 = (worldY1c - (double)this.centerWorldY) * (double)worldScale;
                    double x2 = (worldX2c - (double)this.centerWorldX) * (double)worldScale;
                    double y2 = (worldY2c - (double)this.centerWorldY) * (double)worldScale;
                    double u1 = (worldX1c - worldX1) / metersPerTile1;
                    double v1 = (worldY1c - worldY1) / metersPerTile1;
                    double u2 = (worldX2c - worldX1) / metersPerTile1;
                    double v2 = (worldY2c - worldY1) / metersPerTile1;
                    if (!bGLinit) {
                        bGLinit = true;
                        GL11.glEnable(3553);
                        GL11.glEnable(3042);
                    }
                    vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                    vboLines.setMode(7);
                    vboLines.setTextureID(textureID);
                    vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                    vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                    vboLines.addQuad((float)x1, (float)y1, (float)u1, (float)v1, (float)x2, (float)y2, (float)u2, (float)v2, 0.0f, fill.r, fill.g, fill.b, fill.a);
                    vboLines.endRun();
                    if (this.renderer.tileGrid.getValue()) {
                        vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                        vboLines.setMode(1);
                        vboLines.setLineWidth(2.0f);
                        vboLines.addRectOutline((float)(worldX1 - (double)this.centerWorldX) * worldScale, (float)(worldY1 - (double)this.centerWorldY) * worldScale, (float)(worldX2 - (double)this.centerWorldX) * worldScale, (float)(worldY2 - (double)this.centerWorldY) * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                        vboLines.endRun();
                    }
                    if (!Core.debug) continue;
                }
            }
        }

        void renderImagePyramidGrid(WorldMapImages images) {
            float worldScale = this.worldScale;
            int tileSize = 256;
            int ptz1 = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images));
            float metersPerTile = 256 * (1 << ptz1);
            float renderOriginX = ((float)images.getMinX() - this.centerWorldX) * worldScale;
            float renderOriginY = ((float)images.getMinY() - this.centerWorldY) * worldScale;
            int numTilesX = (int)Math.ceil((float)(images.getMaxX() - images.getMinX()) / (metersPerTile *= images.getResolution()));
            int numTilesY = (int)Math.ceil((float)(images.getMaxY() - images.getMinY()) / metersPerTile);
            float minXui = renderOriginX;
            float minYui = renderOriginY;
            float maxXui = minXui + (float)numTilesX * metersPerTile * worldScale;
            float maxYui = minYui + (float)numTilesY * metersPerTile * worldScale;
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(1);
            vboLines.setLineWidth(2.0f);
            for (int x = 0; x < numTilesX + 1; ++x) {
                vboLines.addLine(renderOriginX + (float)x * metersPerTile * worldScale, minYui, 0.0f, renderOriginX + (float)x * metersPerTile * worldScale, maxYui, 0.0f, 1.0f, 0.0f, 0.0f, 0.5f);
            }
            for (int y = 0; y < numTilesY + 1; ++y) {
                vboLines.addLine(minXui, renderOriginY + (float)y * metersPerTile * worldScale, 0.0f, maxXui, renderOriginY + (float)y * metersPerTile * worldScale, 0.0f, 1.0f, 0.0f, 0.0f, 0.5f);
            }
            vboLines.endRun();
        }

        float triangleArea(float x0, float y0, float x1, float y1, float x2, float y2) {
            float a = Vector2f.length(x1 - x0, y1 - y0);
            float b = Vector2f.length(x2 - x1, y2 - y1);
            float c = Vector2f.length(x0 - x2, y0 - y2);
            float s = (a + b + c) / 2.0f;
            return (float)Math.sqrt(s * (s - a) * (s - b) * (s - c));
        }

        void paintAreasOutsideBounds(int minX, int minY, int maxX, int maxY, float worldScale) {
            float y2;
            float x2;
            float y1;
            float x1;
            float x1cell = this.renderOriginX - (float)(minX % 256) * worldScale;
            float y1cell = this.renderOriginY - (float)(minY % 256) * worldScale;
            float x2cell = this.renderOriginX + (float)((this.worldMap.getMaxXInCells() + 1) * 256 - minX) * worldScale;
            float y2cell = this.renderOriginY + (float)((this.worldMap.getMaxYInCells() + 1) * 256 - minY) * worldScale;
            float z = 0.0f;
            WorldMapStyleLayer.RGBAf fill = this.fill;
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(7);
            if (minX % 256 != 0) {
                x1 = x1cell;
                y1 = y1cell;
                x2 = this.renderOriginX;
                y2 = y2cell;
                vboLines.addQuad(x1, y1, x2, y2, 0.0f, fill.r, fill.g, fill.b, fill.a);
            }
            if (minY % 256 != 0) {
                x1 = this.renderOriginX;
                y1 = y1cell;
                x2 = x1 + (float)this.worldMap.getWidthInSquares() * this.worldScale;
                y2 = this.renderOriginY;
                vboLines.addQuad(x1, y1, x2, y2, 0.0f, fill.r, fill.g, fill.b, fill.a);
            }
            if (maxX + 1 != 0) {
                x1 = this.renderOriginX + (float)(maxX - minX + 1) * worldScale;
                y1 = y1cell;
                x2 = x2cell;
                y2 = y2cell;
                vboLines.addQuad(x1, y1, x2, y2, 0.0f, fill.r, fill.g, fill.b, fill.a);
            }
            if (maxY + 1 != 0) {
                x1 = this.renderOriginX;
                y1 = this.renderOriginY + (float)this.worldMap.getHeightInSquares() * worldScale;
                x2 = this.renderOriginX + (float)this.worldMap.getWidthInSquares() * worldScale;
                y2 = y2cell;
                vboLines.addQuad(x1, y1, x2, y2, 0.0f, fill.r, fill.g, fill.b, fill.a);
            }
            vboLines.endRun();
        }

        void renderWorldBounds() {
            float x1 = this.renderOriginX;
            float y1 = this.renderOriginY;
            float x2 = x1 + (float)this.worldMap.getWidthInSquares() * this.worldScale;
            float y2 = y1 + (float)this.worldMap.getHeightInSquares() * this.worldScale;
            this.renderDropShadow();
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(1);
            vboLines.setLineWidth(2.0f);
            float rgb = 0.5f;
            vboLines.addLine(x1, y1, 0.0f, x2, y1, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f);
            vboLines.addLine(x2, y1, 0.0f, x2, y2, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f);
            vboLines.addLine(x2, y2, 0.0f, x1, y2, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f);
            vboLines.addLine(x1, y2, 0.0f, x1, y1, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f);
            vboLines.endRun();
        }

        private void renderDropShadow() {
            float th = (float)this.renderer.dropShadowWidth * ((float)this.renderer.getHeight() / 1080.0f) * this.worldScale / this.renderer.getWorldScale(this.renderer.getBaseZoom());
            if (th < 2.0f) {
                return;
            }
            float x1 = this.renderOriginX;
            float y1 = this.renderOriginY;
            float x2 = x1 + (float)this.worldMap.getWidthInSquares() * this.worldScale;
            float y2 = y1 + (float)this.worldMap.getHeightInSquares() * this.worldScale;
            vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            vboLines.setMode(4);
            vboLines.reserve(3);
            vboLines.addElement(x1 + th, y2, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f);
            vboLines.addElement(x2, y2, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f);
            vboLines.addElement(x1 + th, y2 + th, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f);
            vboLines.reserve(3);
            vboLines.addElement(x2, y2, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f);
            vboLines.addElement(x2 + th, y2 + th, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f);
            vboLines.addElement(x1 + th, y2 + th, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f);
            vboLines.reserve(3);
            vboLines.addElement(x2, y1 + th, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f);
            vboLines.addElement(x2 + th, y1 + th, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f);
            vboLines.addElement(x2, y2, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f);
            vboLines.reserve(3);
            vboLines.addElement(x2 + th, y1 + th, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f);
            vboLines.addElement(x2 + th, y2 + th, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f);
            vboLines.addElement(x2, y2, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f);
            vboLines.endRun();
        }

        @Override
        public void postRender() {
            for (int i = 0; i < this.playerRenderData.length; ++i) {
                PlayerRenderData playerRenderData = this.playerRenderData[i];
                if (playerRenderData.modelSlotRenderData == null) continue;
                playerRenderData.modelSlotRenderData.postRender();
            }
            this.symbolsRenderData.postRender();
        }
    }

    private static final class CharacterModelCamera
    extends ModelCamera {
        float worldScale;
        float angle;
        float playerX;
        float playerY;
        boolean vehicle;

        private CharacterModelCamera() {
        }

        @Override
        public void Begin() {
            Matrix4f matrix4f = WorldMapRenderer.allocMatrix4f();
            matrix4f.identity();
            matrix4f.translate(this.playerX * this.worldScale, this.playerY * this.worldScale, 0.0f);
            matrix4f.rotateX(1.5707964f);
            matrix4f.rotateY(this.angle + 4.712389f);
            if (this.vehicle) {
                matrix4f.scale(this.worldScale);
            } else {
                matrix4f.scale(1.5f * this.worldScale);
            }
            PZGLUtil.pushAndMultMatrix(5888, matrix4f);
            WorldMapRenderer.releaseMatrix4f(matrix4f);
        }

        @Override
        public void End() {
            PZGLUtil.popMatrix(5888);
        }
    }

    @UsedFromLua
    public final class WorldMapBooleanOption
    extends BooleanConfigOption {
        public WorldMapBooleanOption(WorldMapRenderer this$0, String name, boolean defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, defaultValue);
            this$0.options.add(this);
        }
    }

    @UsedFromLua
    public final class WorldMapDoubleOption
    extends DoubleConfigOption {
        public WorldMapDoubleOption(WorldMapRenderer this$0, String name, double min, double max, double defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, min, max, defaultValue);
            this$0.options.add(this);
        }
    }

    private static final class ListOfRenderLayers
    extends ArrayList<WorldMapRenderLayer> {
        private ListOfRenderLayers() {
        }
    }

    private static final class PlayerRenderData {
        ModelSlotRenderData modelSlotRenderData;
        float angle;
        float x;
        float y;

        private PlayerRenderData() {
        }
    }
}

