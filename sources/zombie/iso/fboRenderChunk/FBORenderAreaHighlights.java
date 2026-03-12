/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;

public class FBORenderAreaHighlights {
    private static FBORenderAreaHighlights instance;
    private final ArrayList<AreaHighlight> highlights = new ArrayList();
    private final ObjectPool<AreaHighlight> highlightPool = new ObjectPool<AreaHighlight>(AreaHighlight::new);
    private final ObjectPool<Drawer> drawerPool = new ObjectPool<Drawer>(Drawer::new);
    private final boolean outline = true;
    private final float outlineR = 1.0f;
    private final float outlineG = 1.0f;
    private final float outlineB = 1.0f;
    private final boolean useGroundDepth = true;

    public static FBORenderAreaHighlights getInstance() {
        if (instance == null) {
            instance = new FBORenderAreaHighlights();
        }
        return instance;
    }

    public void addHighlight(int x1, int y1, int x2, int y2, int z, float r, float g, float b, float a) {
        AreaHighlight ah = this.highlightPool.alloc().set(-1, x1, y1, x2, y2, z, r, g, b, a);
        this.highlights.add(ah);
    }

    public void addHighlightForPlayer(int playerIndex, int x1, int y1, int x2, int y2, int z, float r, float g, float b, float a) {
        AreaHighlight ah = this.highlightPool.alloc().set(playerIndex, x1, y1, x2, y2, z, r, g, b, a);
        this.highlights.add(ah);
    }

    public void render() {
        for (int i = 0; i < this.highlights.size(); ++i) {
            AreaHighlight ah = this.highlights.get(i);
            if (UIManager.uiRenderTimeMS == ah.renderTimeMs) continue;
            this.highlights.remove(i--);
            this.highlightPool.release(ah);
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        Drawer drawer = this.drawerPool.alloc();
        drawer.playerIndex = playerIndex;
        this.highlightPool.releaseAll((List<AreaHighlight>)drawer.highlights);
        drawer.highlights.clear();
        this.renderUserDefinedAreas(drawer);
        this.renderNonPVPZones(drawer);
        this.renderSafehouses(drawer);
        this.renderAnimalDesigationZones(drawer);
        if (drawer.highlights.isEmpty()) {
            this.drawerPool.release(drawer);
        } else {
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    private void renderUserDefinedAreas(Drawer drawer) {
        int playerIndex = drawer.playerIndex;
        for (int i = 0; i < this.highlights.size(); ++i) {
            AreaHighlight ah1 = this.highlights.get(i);
            if (ah1.playerIndex != -1 && ah1.playerIndex != playerIndex || !ah1.isOnScreen(playerIndex)) continue;
            AreaHighlight ah2 = this.highlightPool.alloc().set(ah1);
            this.renderOutline(ah2);
            ah2.clampToChunkMap(playerIndex);
            drawer.highlights.add(ah2);
        }
    }

    private void renderNonPVPZones(Drawer drawer) {
        int playerIndex = drawer.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (GameClient.client && player != null && player.isSeeNonPvpZone()) {
            ArrayList<NonPvpZone> nonPvpZones = NonPvpZone.getAllZones();
            for (int i = 0; i < nonPvpZones.size(); ++i) {
                NonPvpZone npz = nonPvpZones.get(i);
                float r = 0.0f;
                float g = 0.0f;
                float b = 1.0f;
                float a = 0.25f;
                AreaHighlight ah = this.highlightPool.alloc().set(playerIndex, npz.getX(), npz.getY(), npz.getX2(), npz.getY2(), 0, 0.0f, 0.0f, 1.0f, 0.25f);
                this.tryRenderArea(drawer, ah);
            }
        }
    }

    private void renderSafehouses(Drawer drawer) {
        int playerIndex = drawer.playerIndex;
        if (GameClient.client && Core.debug) {
            ArrayList<SafeHouse> safeHouses = SafeHouse.getSafehouseList();
            for (int i = 0; i < safeHouses.size(); ++i) {
                SafeHouse sh = safeHouses.get(i);
                float r = 1.0f;
                float g = 0.0f;
                float b = 0.0f;
                float a = 0.25f;
                AreaHighlight ah = this.highlightPool.alloc().set(playerIndex, sh.getX(), sh.getY(), sh.getX2(), sh.getY2(), 0, 1.0f, 0.0f, 0.0f, 0.25f);
                this.tryRenderArea(drawer, ah);
            }
        }
    }

    private void renderAnimalDesigationZones(Drawer drawer) {
        int playerIndex = drawer.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player != null && player.isSeeDesignationZone()) {
            ArrayList<Double> selectedMetaAnimalZones = player.getSelectedZonesForHighlight();
            ArrayList<DesignationZone> zones = DesignationZone.allZones;
            for (int i = 0; i < zones.size(); ++i) {
                DesignationZone dz = zones.get(i);
                float r = 0.2f;
                float g = 0.2f;
                float b = 0.9f;
                float a = 0.4f;
                if (selectedMetaAnimalZones.contains(dz.getId())) {
                    r = 0.2f;
                    g = 0.8f;
                    b = 0.9f;
                    a = 0.4f;
                }
                AreaHighlight ah = this.highlightPool.alloc().set(playerIndex, dz.x, dz.y, dz.x + dz.w, dz.y + dz.h, dz.z, r, g, b, a);
                this.tryRenderArea(drawer, ah);
            }
        }
    }

    private void tryRenderArea(Drawer drawer, AreaHighlight ah) {
        int playerIndex = drawer.playerIndex;
        if (ah.isOnScreen(playerIndex)) {
            this.renderOutline(ah);
            ah.clampToChunkMap(playerIndex);
            drawer.highlights.add(ah);
        } else {
            this.highlightPool.release(ah);
        }
    }

    private void renderOutline(AreaHighlight ah) {
        LineDrawer.addRect(ah.x1, ah.y1, ah.z, ah.x2 - ah.x1, ah.y2 - ah.y1, ah.r, ah.g, ah.b);
    }

    private static final class AreaHighlight {
        int playerIndex = -1;
        int x1;
        int y1;
        int x2;
        int y2;
        int z;
        float r;
        float g;
        float b;
        float a;
        long renderTimeMs;

        private AreaHighlight() {
        }

        AreaHighlight set(int playerIndex, int x1, int y1, int x2, int y2, int z, float r, float g, float b, float a) {
            this.playerIndex = playerIndex;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.renderTimeMs = UIManager.uiRenderTimeMS;
            return this;
        }

        AreaHighlight set(AreaHighlight other) {
            return this.set(other.playerIndex, other.x1, other.y1, other.x2, other.y2, other.z, other.r, other.g, other.b, other.a);
        }

        boolean isOnScreen(int playerIndex) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (chunkMap.ignore) {
                return false;
            }
            int xMin = chunkMap.getWorldXMinTiles();
            int yMin = chunkMap.getWorldYMinTiles();
            int xMax = chunkMap.getWorldXMaxTiles();
            int yMax = chunkMap.getWorldYMaxTiles();
            return this.x1 < xMax && this.x2 > xMin && this.y1 < yMax && this.y2 > yMin;
        }

        void clampToChunkMap(int playerIndex) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (chunkMap.ignore) {
                return;
            }
            int xMin = chunkMap.getWorldXMinTiles();
            int yMin = chunkMap.getWorldYMinTiles();
            int xMax = chunkMap.getWorldXMaxTiles();
            int yMax = chunkMap.getWorldYMaxTiles();
            this.x1 = PZMath.max(this.x1, xMin);
            this.y1 = PZMath.max(this.y1, yMin);
            this.x2 = PZMath.min(this.x2, xMax);
            this.y2 = PZMath.min(this.y2, yMax);
        }

        void render(float playerX, float playerY, float playerZ, float cx, float cy) {
            VBORenderer vbor = VBORenderer.getInstance();
            float u0 = 0.0f;
            float v0 = 0.0f;
            float u1 = 1.0f;
            float v1 = 0.0f;
            float u2 = 1.0f;
            float v2 = 1.0f;
            float u3 = 0.0f;
            float v3 = 1.0f;
            float ox = (float)(this.x1 + this.x2) / 2.0f;
            float oy = (float)(this.y1 + this.y2) / 2.0f;
            float depthAdjust = ox + oy < playerX + playerY ? -1.4E-4f : -1.0E-4f;
            float depth0 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)((float)this.x1), (float)((float)this.y1), (float)((float)this.z)).depthStart + depthAdjust;
            float depth1 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)((float)this.x2), (float)((float)this.y1), (float)((float)this.z)).depthStart + depthAdjust;
            float depth2 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)((float)this.x2), (float)((float)this.y2), (float)((float)this.z)).depthStart + depthAdjust;
            float depth3 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)((float)this.x1), (float)((float)this.y2), (float)((float)this.z)).depthStart + depthAdjust;
            Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.scaling(Core.scale);
            modelView.scale((float)Core.tileScale / 2.0f);
            modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
            modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
            double difX = ox - cx;
            double difY = oy - cy;
            double difZ = ((float)this.z - playerZ) * 2.44949f;
            modelView.translate(-((float)difX), (float)difZ, -((float)difY));
            modelView.scale(-1.0f, 1.0f, -1.0f);
            modelView.translate(0.0f, -0.71999997f, 0.0f);
            vbor.cmdPushAndLoadMatrix(5888, modelView);
            vbor.startRun(vbor.formatPositionColorUvDepth);
            vbor.setMode(7);
            Objects.requireNonNull(FBORenderAreaHighlights.getInstance());
            vbor.setDepthTest(true);
            vbor.setTextureID(Texture.getWhite().getTextureId());
            float z = 0.0f;
            vbor.addQuadDepth((float)this.x1 - ox, 0.0f, (float)this.y1 - oy, 0.0f, 0.0f, depth0, (float)this.x2 - ox, 0.0f, (float)this.y1 - oy, 1.0f, 0.0f, depth1, (float)this.x2 - ox, 0.0f, (float)this.y2 - oy, 1.0f, 1.0f, depth2, (float)this.x1 - ox, 0.0f, (float)this.y2 - oy, 0.0f, 1.0f, depth3, this.r, this.g, this.b, this.a);
            vbor.endRun();
            vbor.cmdPopMatrix(5888);
        }
    }

    private static final class Drawer
    extends TextureDraw.GenericDrawer {
        final ArrayList<AreaHighlight> highlights = new ArrayList();
        int playerIndex;

        private Drawer() {
        }

        @Override
        public void render() {
            SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
            PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
            float rcx = camera.rightClickX;
            float rcy = camera.rightClickY;
            float tox = camera.getTOffX();
            float toy = camera.getTOffY();
            float defx = camera.deferedX;
            float defy = camera.deferedY;
            float playerX = Core.getInstance().floatParamMap.get(0).floatValue();
            float playerY = Core.getInstance().floatParamMap.get(1).floatValue();
            float playerZ = Core.getInstance().floatParamMap.get(2).floatValue();
            float cx = playerX - camera.XToIso(-tox - rcx, -toy - rcy, 0.0f);
            float cy = playerY - camera.YToIso(-tox - rcx, -toy - rcy, 0.0f);
            cx += defx;
            cy += defy;
            double screenWidth = (float)camera.offscreenWidth / 1920.0f;
            double screenHeight = (float)camera.offscreenHeight / 1920.0f;
            Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
            projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, -((float)screenHeight) / 2.0f, (float)screenHeight / 2.0f, -10.0f, 10.0f);
            Core.getInstance().projectionMatrixStack.push(projection);
            GL11.glEnable(2929);
            GL11.glDepthFunc(515);
            GL11.glDepthMask(false);
            GL11.glBlendFunc(770, 771);
            for (int i = 0; i < this.highlights.size(); ++i) {
                AreaHighlight ah = this.highlights.get(i);
                ah.render(playerX, playerY, playerZ, cx, cy);
            }
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.flush();
            Core.getInstance().projectionMatrixStack.pop();
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            FBORenderAreaHighlights.getInstance().drawerPool.release(this);
        }
    }
}

