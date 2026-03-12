/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.WorldMarkers;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;
import zombie.util.StringUtils;

public class FBORenderWorldMarkers {
    private static FBORenderWorldMarkers instance;
    private final ArrayList<Marker> highlights = new ArrayList();
    private final ObjectPool<Marker> highlightPool = new ObjectPool<Marker>(Marker::new);
    private final ObjectPool<Drawer> drawerPool = new ObjectPool<Drawer>(Drawer::new);
    private final boolean outline = false;
    private final float outlineR = 1.0f;
    private final float outlineG = 1.0f;
    private final float outlineB = 1.0f;
    private final boolean useGroundDepth = true;

    public static FBORenderWorldMarkers getInstance() {
        if (instance == null) {
            instance = new FBORenderWorldMarkers();
        }
        return instance;
    }

    public void render(int z, List<WorldMarkers.GridSquareMarker> markerList) {
        int i;
        for (int i2 = 0; i2 < this.highlights.size(); ++i2) {
            Marker ah = this.highlights.get(i2);
            this.highlights.remove(i2--);
            this.highlightPool.release(ah);
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        Drawer drawer = this.drawerPool.alloc();
        drawer.playerIndex = playerIndex;
        this.highlightPool.releaseAll((List<Marker>)drawer.highlights);
        drawer.highlights.clear();
        for (i = 0; i < markerList.size(); ++i) {
            WorldMarkers.GridSquareMarker gsm = markerList.get(i);
            if (!gsm.isActive() || gsm.getZ() != (float)z) continue;
            float centerX = gsm.getOriginalX() + 0.5f;
            float centerY = gsm.getOriginalY() + 0.5f;
            float radius = gsm.getSize() * 0.69f;
            Marker mkr = this.highlightPool.alloc().set(centerX - radius, centerY - radius, centerX + radius, centerY + radius, gsm.getOriginalZ(), gsm.getR(), gsm.getG(), gsm.getB(), gsm.getAlpha());
            String textureName = gsm.getTextureName();
            String overlayTextureName = gsm.getOverlayTextureName();
            if (StringUtils.equals(textureName, "circle_center") || StringUtils.equals(textureName, "circle_highlight_2")) {
                mkr.texture1 = Texture.getSharedTexture("media/textures/worldMap/circle_center.png");
            } else if (StringUtils.equals(textureName, "circle_highlight")) {
                mkr.texture1 = Texture.getSharedTexture("media/textures/worldMap/circle_center.png");
                mkr.texture2 = Texture.getSharedTexture("media/textures/worldMap/circle_only_highlight.png");
            }
            if (StringUtils.equals(overlayTextureName, "circle_only_highlight") || StringUtils.equals(overlayTextureName, "circle_only_highlight_2")) {
                mkr.texture2 = Texture.getSharedTexture("media/textures/worldMap/circle_only_highlight.png");
            }
            this.highlights.add(mkr);
        }
        for (i = 0; i < this.highlights.size(); ++i) {
            Marker ah1 = this.highlights.get(i);
            if (!ah1.isOnScreen(playerIndex)) continue;
            Marker ah2 = this.highlightPool.alloc().set(ah1);
            this.renderOutline(ah2);
            drawer.highlights.add(ah2);
        }
        if (drawer.highlights.isEmpty()) {
            this.drawerPool.release(drawer);
        } else {
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    private void renderOutline(Marker ah) {
    }

    private static final class Marker {
        float x1;
        float y1;
        float x2;
        float y2;
        float z;
        float r;
        float g;
        float b;
        float a;
        long renderTimeMs;
        Texture texture1;
        Texture texture2;

        private Marker() {
        }

        Marker set(float x1, float y1, float x2, float y2, float z, float r, float g, float b, float a) {
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
            this.texture1 = null;
            this.texture2 = null;
            return this;
        }

        Marker set(Marker other) {
            this.set(other.x1, other.y1, other.x2, other.y2, other.z, other.r, other.g, other.b, other.a);
            this.texture1 = other.texture1;
            this.texture2 = other.texture2;
            return this;
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
            return this.x1 < (float)xMax && this.x2 > (float)xMin && this.y1 < (float)yMax && this.y2 > (float)yMin;
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
            float ox = (this.x1 + this.x2) / 2.0f;
            float oy = (this.y1 + this.y2) / 2.0f;
            float depthAdjust = -1.0E-4f;
            float depth0 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)this.x1, (float)this.y1, (float)this.z).depthStart + (depthAdjust -= 5.0E-5f);
            float depth1 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)this.x2, (float)this.y1, (float)this.z).depthStart + depthAdjust;
            float depth2 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)this.x2, (float)this.y2, (float)this.z).depthStart + depthAdjust;
            float depth3 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)this.x1, (float)this.y2, (float)this.z).depthStart + depthAdjust;
            Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.scaling(Core.scale);
            modelView.scale((float)Core.tileScale / 2.0f);
            modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
            modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
            double difX = ox - cx;
            double difY = oy - cy;
            double difZ = (this.z - playerZ) * 2.44949f;
            modelView.translate(-((float)difX), (float)difZ, -((float)difY));
            modelView.scale(-1.0f, 1.0f, -1.0f);
            modelView.translate(0.0f, -0.71999997f, 0.0f);
            vbor.cmdPushAndLoadMatrix(5888, modelView);
            vbor.startRun(vbor.formatPositionColorUvDepth);
            vbor.setMode(7);
            vbor.setDepthTest(FBORenderWorldMarkers.getInstance().useGroundDepth);
            if (this.texture1 != null) {
                vbor.setTextureID(this.texture1.getTextureId());
            }
            float z = 0.0f;
            vbor.addQuadDepth(this.x1 - ox, 0.0f, this.y1 - oy, 0.0f, 0.0f, depth0, this.x2 - ox, 0.0f, this.y1 - oy, 1.0f, 0.0f, depth1, this.x2 - ox, 0.0f, this.y2 - oy, 1.0f, 1.0f, depth2, this.x1 - ox, 0.0f, this.y2 - oy, 0.0f, 1.0f, depth3, this.r, this.g, this.b, this.a);
            vbor.endRun();
            vbor.startRun(vbor.formatPositionColorUvDepth);
            vbor.setMode(7);
            vbor.setDepthTest(FBORenderWorldMarkers.getInstance().useGroundDepth);
            if (this.texture2 != null) {
                vbor.setTextureID(this.texture2.getTextureId());
            }
            vbor.addQuadDepth(this.x1 - ox, 0.0f, this.y1 - oy, 0.0f, 0.0f, depth0, this.x2 - ox, 0.0f, this.y1 - oy, 1.0f, 0.0f, depth1, this.x2 - ox, 0.0f, this.y2 - oy, 1.0f, 1.0f, depth2, this.x1 - ox, 0.0f, this.y2 - oy, 0.0f, 1.0f, depth3, this.r, this.g, this.b, this.a);
            vbor.endRun();
            vbor.cmdPopMatrix(5888);
        }
    }

    private static final class Drawer
    extends TextureDraw.GenericDrawer {
        final ArrayList<Marker> highlights = new ArrayList();
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
                Marker ah = this.highlights.get(i);
                ah.render(playerX, playerY, playerZ, cx, cy);
            }
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.flush();
            Core.getInstance().projectionMatrixStack.pop();
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            FBORenderWorldMarkers.getInstance().drawerPool.release(this);
        }
    }
}

