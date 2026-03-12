/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector2;
import zombie.popman.ObjectPool;

public final class FBORenderShadows {
    private static FBORenderShadows instance;
    private final ObjectPool<Drawer> drawerPool = new ObjectPool<Drawer>(Drawer::new);
    private final ObjectPool<Shadow> shadowPool = new ObjectPool<Shadow>(Shadow::new);
    private final ArrayList<Shadow> shadows = new ArrayList();
    private final Vector3f tempVector3f1 = new Vector3f();
    private final Vector3f tempVector3f2 = new Vector3f();
    private Texture dropShadow;

    public static FBORenderShadows getInstance() {
        if (instance == null) {
            instance = new FBORenderShadows();
        }
        return instance;
    }

    private FBORenderShadows() {
    }

    public void clear() {
        this.shadowPool.releaseAll((List<Shadow>)this.shadows);
        this.shadows.clear();
        this.dropShadow = Texture.getSharedTexture("media/textures/NewShadow.png");
    }

    public void addShadow(float x, float y, float z, Vector3f forward, float w, float fm, float bm, float r, float g, float b, float a, boolean bAnimal) {
        if (this.dropShadow == null || !this.dropShadow.isReady()) {
            return;
        }
        if (!bAnimal) {
            w = Math.max(w, 0.65f);
            fm = Math.max(fm, 0.65f);
            bm = Math.max(bm, 0.65f);
        }
        Vector3f forwardN = this.tempVector3f1.set(forward);
        forwardN.normalize();
        Vector3f perp = forwardN.cross(0.0f, 0.0f, 1.0f, this.tempVector3f2);
        perp.x *= w;
        perp.y *= w;
        float fx = x + forward.x * fm;
        float fy = y + forward.y * fm;
        float bx = x - forward.x * bm;
        float by = y - forward.y * bm;
        float fx1 = fx - perp.x;
        float fx2 = fx + perp.x;
        float bx1 = bx - perp.x;
        float bx2 = bx + perp.x;
        float by1 = by - perp.y;
        float by2 = by + perp.y;
        float fy1 = fy - perp.y;
        float fy2 = fy + perp.y;
        float shadowAlpha = a;
        shadowAlpha *= (r + g + b) / 3.0f;
        this.addShadow(x, y, z, fx1, fy1, fx2, fy2, bx2, by2, bx1, by1, r, g, b, shadowAlpha *= 0.66f, this.dropShadow, false);
        if (DebugOptions.instance.isoSprite.dropShadowEdges.getValue()) {
            LineDrawer.addLine(fx1, fy1, z, fx2, fy2, z, 1, 1, 1, null);
            LineDrawer.addLine(fx2, fy2, z, bx2, by2, z, 1, 1, 1, null);
            LineDrawer.addLine(bx2, by2, z, bx1, by1, z, 1, 1, 1, null);
            LineDrawer.addLine(bx1, by1, z, fx1, fy1, z, 1, 1, 1, null);
        }
    }

    public void addShadow(float ox, float oy, float oz, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float r, float g, float b, float a, Texture texture, boolean bVehicle) {
        if (a <= 0.0f) {
            return;
        }
        Shadow shadow = this.shadowPool.alloc();
        shadow.ox = ox;
        shadow.oy = oy;
        shadow.oz = oz;
        shadow.x0 = x0;
        shadow.y0 = y0;
        shadow.x1 = x1;
        shadow.y1 = y1;
        shadow.x2 = x2;
        shadow.y2 = y2;
        shadow.x3 = x3;
        shadow.y3 = y3;
        this.calculateSlopeAngles(shadow);
        float playerX = IsoCamera.frameState.camCharacterX;
        float playerY = IsoCamera.frameState.camCharacterY;
        float spriteShadowOffset = 5.0E-5f;
        float depthAdjust = ox + oy < playerX + playerY ? -1.4E-4f : -1.4999999E-4f;
        shadow.depth0 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)x0, (float)y0, (float)this.calculateApparentZ((Shadow)shadow, (float)x0, (float)y0, (float)oz)).depthStart + depthAdjust;
        shadow.depth1 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)x1, (float)y1, (float)this.calculateApparentZ((Shadow)shadow, (float)x1, (float)y1, (float)oz)).depthStart + depthAdjust;
        shadow.depth2 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)x2, (float)y2, (float)this.calculateApparentZ((Shadow)shadow, (float)x2, (float)y2, (float)oz)).depthStart + depthAdjust;
        shadow.depth3 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)x3, (float)y3, (float)this.calculateApparentZ((Shadow)shadow, (float)x3, (float)y3, (float)oz)).depthStart + depthAdjust;
        shadow.r = r;
        shadow.g = g;
        shadow.b = b;
        shadow.a = a;
        shadow.texture = texture;
        shadow.vehicle = bVehicle;
        this.shadows.add(shadow);
    }

    void calculateSlopeAngles(Shadow shadow) {
        shadow.slopeAngleY = 0.0f;
        shadow.slopeAngleX = 0.0f;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(shadow.ox, shadow.oy, shadow.oz);
        if (square == null) {
            return;
        }
        IsoDirections dir = square.getSlopedSurfaceDirection();
        if (dir == null) {
            return;
        }
        float heightMin = square.getSlopedSurfaceHeightMin();
        float heightMax = square.getSlopedSurfaceHeightMax();
        switch (dir) {
            case N: {
                shadow.slopeAngleX = Vector2.getDirection(1.0f, (heightMax - heightMin) * 2.44949f);
                break;
            }
            case S: {
                shadow.slopeAngleX = Vector2.getDirection(1.0f, (heightMin - heightMax) * 2.44949f);
                break;
            }
            case W: {
                shadow.slopeAngleY = Vector2.getDirection(1.0f, (heightMin - heightMax) * 2.44949f);
                break;
            }
            case E: {
                shadow.slopeAngleY = Vector2.getDirection(1.0f, (heightMax - heightMin) * 2.44949f);
            }
        }
    }

    float calculateApparentZ(Shadow shadow, float x, float y, float z) {
        float z2;
        if (shadow.slopeAngleX == 0.0f && shadow.slopeAngleY == 0.0f) {
            return z;
        }
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(shadow.ox, shadow.oy, shadow.oz);
        float slopeHeightMin = square.getSlopedSurfaceHeightMin();
        float slopeHeightMax = square.getSlopedSurfaceHeightMax();
        float slopeDelta = slopeHeightMax - slopeHeightMin;
        float dx = (x - (float)(square.x - 5)) % 10.0f;
        float dy = (y - (float)(square.y - 5)) % 10.0f;
        switch (square.getSlopedSurfaceDirection()) {
            case N: {
                float f = PZMath.lerp(slopeHeightMin - 4.0f * slopeDelta, slopeHeightMax + 5.0f * slopeDelta, 1.0f - dy / 10.0f);
                break;
            }
            case S: {
                float f = PZMath.lerp(slopeHeightMin - 5.0f * slopeDelta, slopeHeightMax + 4.0f * slopeDelta, dy / 10.0f);
                break;
            }
            case W: {
                float f = PZMath.lerp(slopeHeightMin - 4.0f * slopeDelta, slopeHeightMax + 5.0f * slopeDelta, 1.0f - dx / 10.0f);
                break;
            }
            case E: {
                float f = PZMath.lerp(slopeHeightMin - 5.0f * slopeDelta, slopeHeightMax + 4.0f * slopeDelta, dx / 10.0f);
                break;
            }
            default: {
                float f = z2 = -1.0f;
            }
        }
        if (z2 == -1.0f) {
            return z;
        }
        return (float)PZMath.fastfloor(z) + z2;
    }

    public void renderMain(int z) {
        Drawer drawer = this.drawerPool.alloc();
        drawer.shadows.clear();
        for (int i = 0; i < this.shadows.size(); ++i) {
            Shadow shadow = this.shadows.get(i);
            if (PZMath.fastfloor(shadow.oz) != z) continue;
            drawer.shadows.add(shadow);
        }
        if (drawer.shadows.isEmpty()) {
            this.drawerPool.release(drawer);
            return;
        }
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    public void endRender() {
        this.shadows.clear();
    }

    private void render(ArrayList<Shadow> shadows) {
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
        VBORenderer vbor = VBORenderer.getInstance();
        GL11.glEnable(2929);
        GL11.glDepthFunc(515);
        GL11.glDepthMask(false);
        IndieGL.glDefaultBlendFuncA();
        for (int i = 0; i < shadows.size(); ++i) {
            Shadow shadow = shadows.get(i);
            float u0 = 0.0f;
            float v0 = 0.0f;
            float u1 = 1.0f;
            float v1 = 0.0f;
            float u2 = 1.0f;
            float v2 = 1.0f;
            float u3 = 0.0f;
            float v3 = 1.0f;
            Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.scaling(Core.scale);
            modelView.scale((float)Core.tileScale / 2.0f);
            modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
            modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
            double difX = shadow.ox - cx;
            double difY = shadow.oy - cy;
            double difZ = (shadow.oz - playerZ) * 2.44949f;
            modelView.translate(-((float)difX), (float)difZ, -((float)difY));
            modelView.scale(-1.0f, 1.0f, -1.0f);
            modelView.translate(0.0f, -0.71999997f, 0.0f);
            if (shadow.slopeAngleX != 0.0f) {
                modelView.rotate(shadow.slopeAngleX, 1.0f, 0.0f, 0.0f);
            }
            if (shadow.slopeAngleY != 0.0f) {
                modelView.rotate(shadow.slopeAngleY, 0.0f, 0.0f, 1.0f);
            }
            vbor.cmdPushAndLoadMatrix(5888, modelView);
            Core.getInstance().modelViewMatrixStack.push(modelView);
            float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
            Core.getInstance().modelViewMatrixStack.pop();
            vbor.startRun(VBORenderer.getInstance().formatPositionColorUvDepth);
            vbor.setMode(7);
            vbor.setTextureID(shadow.texture.getTextureId());
            vbor.setDepthTest(true);
            vbor.addQuadDepth(shadow.x0 - shadow.ox, 0.0f, shadow.y0 - shadow.oy, 0.0f, 0.0f, shadow.depth0, shadow.x1 - shadow.ox, 0.0f, shadow.y1 - shadow.oy, 1.0f, 0.0f, shadow.depth1, shadow.x2 - shadow.ox, 0.0f, shadow.y2 - shadow.oy, 1.0f, 1.0f, shadow.depth2, shadow.x3 - shadow.ox, 0.0f, shadow.y3 - shadow.oy, 0.0f, 1.0f, shadow.depth3, shadow.r, shadow.g, shadow.b, shadow.a);
            vbor.endRun();
            vbor.cmdPopMatrix(5888);
        }
        vbor.flush();
        Core.getInstance().projectionMatrixStack.pop();
        GLStateRenderThread.restore();
    }

    static final class Shadow {
        float ox;
        float oy;
        float oz;
        float x0;
        float y0;
        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float slopeAngleX;
        float slopeAngleY;
        float depth0;
        float depth1;
        float depth2;
        float depth3;
        float r;
        float g;
        float b;
        float a;
        Texture texture;
        boolean vehicle;

        Shadow() {
        }
    }

    static final class Drawer
    extends TextureDraw.GenericDrawer {
        private final ArrayList<Shadow> shadows = new ArrayList();

        Drawer() {
        }

        @Override
        public void render() {
            FBORenderShadows.getInstance().render(this.shadows);
        }

        @Override
        public void postRender() {
            FBORenderShadows.instance.shadowPool.releaseAll((List<Shadow>)this.shadows);
            this.shadows.clear();
            FBORenderShadows.instance.drawerPool.release(this);
        }
    }
}

