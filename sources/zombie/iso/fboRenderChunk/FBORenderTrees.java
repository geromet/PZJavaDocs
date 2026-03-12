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
import zombie.core.skinnedmodel.advancedanimation.Anim2DBlendTriangle;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkCamera;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.popman.ObjectPool;

public final class FBORenderTrees
extends TextureDraw.GenericDrawer {
    static final ObjectPool<FBORenderTrees> s_pool = new ObjectPool<FBORenderTrees>(FBORenderTrees::new);
    static final ObjectPool<Tree> s_treePool = new ObjectPool<Tree>(Tree::new);
    public static FBORenderTrees current;
    static final FBORenderChunkCamera s_chunkCamera;
    static Shader shader;
    static final float SQRT2;
    static final BarycentricCompute s_barycentricCompute;
    static final Barycentric[] s_barycentric;
    final ArrayList<Tree> trees = new ArrayList();
    float playerX;
    float playerY;
    float playerZ;
    boolean playerInside;

    public static FBORenderTrees alloc() {
        return s_pool.alloc();
    }

    @Override
    public void render() {
        boolean bRenderToChunkTexture;
        boolean bl = bRenderToChunkTexture = FBORenderChunkManager.instance.renderThreadCurrent != null;
        if (bRenderToChunkTexture) {
            s_chunkCamera.set(FBORenderChunkManager.instance.renderThreadCurrent, 0.0f, 0.0f, 0.0f, 2.3561945f);
            s_chunkCamera.pushProjectionMatrix();
        } else {
            this.pushProjectionMatrix();
        }
        for (int i = 0; i < this.trees.size(); ++i) {
            Tree tree = this.trees.get(i);
            this.renderTree(tree);
        }
        Core.getInstance().projectionMatrixStack.pop();
        GLStateRenderThread.restore();
    }

    private void pushProjectionMatrix() {
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
        double screenWidth = (float)camera.offscreenWidth / 1920.0f;
        double screenHeight = (float)camera.offscreenHeight / 1920.0f;
        Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
        projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, -((float)screenHeight) / 2.0f, (float)screenHeight / 2.0f, -10.0f, 10.0f);
        Core.getInstance().projectionMatrixStack.push(projection);
    }

    private void pushModelViewMatrix(float ox, float oy, float oz, float useangle) {
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
        float rcx = camera.rightClickX;
        float rcy = camera.rightClickY;
        float tox = camera.getTOffX();
        float toy = camera.getTOffY();
        float defx = camera.deferedX;
        float defy = camera.deferedY;
        float cx = this.playerX - camera.XToIso(-tox - rcx, -toy - rcy, 0.0f);
        float cy = this.playerY - camera.YToIso(-tox - rcx, -toy - rcy, 0.0f);
        cx += defx;
        cy += defy;
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.scaling(Core.scale);
        modelView.scale((float)Core.tileScale / 2.0f);
        modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
        double difX = ox - cx;
        double difY = oy - cy;
        double difZ = (oz - this.playerZ) * 2.44949f;
        modelView.translate(-((float)difX), (float)difZ, -((float)difY));
        modelView.scale(-1.0f, 1.0f, 1.0f);
        modelView.rotate(useangle + (float)Math.PI, 0.0f, 1.0f, 0.0f);
        modelView.translate(0.0f, -0.71999997f, 0.0f);
        Core.getInstance().modelViewMatrixStack.push(modelView);
    }

    private void renderTree(Tree tree) {
        if (Core.debug) {
            // empty if block
        }
        boolean bLines = false;
        boolean bRenderToChunkTexture = FBORenderChunkManager.instance.renderThreadCurrent != null;
        VBORenderer vbor = VBORenderer.getInstance();
        if (bRenderToChunkTexture) {
            s_chunkCamera.set(FBORenderChunkManager.instance.renderThreadCurrent, tree.x, tree.y, tree.z, 2.3561945f);
            s_chunkCamera.pushModelViewMatrix(tree.x, tree.y, tree.z, 2.3561945f, false);
            Core.getInstance().modelViewMatrixStack.peek().scale(0.6666667f);
        } else {
            this.pushModelViewMatrix(tree.x, tree.y, tree.z, 2.3561945f);
        }
        GL11.glDepthMask(true);
        GL11.glDepthFunc(515);
        if (bLines) {
            GL11.glPolygonMode(1032, 6913);
        }
        if (tree.useStencil) {
            GL11.glEnable(2960);
            GL11.glStencilFunc(517, 128, 128);
            GL11.glBlendFunc(770, 771);
        } else {
            GL11.glDisable(2960);
        }
        this.renderTreeTextures(tree, false);
        vbor.flush();
        if (tree.useStencil) {
            GL11.glStencilFunc(514, 128, 128);
            float a = tree.a;
            tree.a = tree.fadeAlpha;
            this.renderTreeTextures(tree, false);
            vbor.flush();
            tree.a = a;
            this.renderTreeTextures(tree, true);
            vbor.flush();
            GL11.glStencilFunc(519, 255, 255);
        }
        if (bLines) {
            GL11.glPolygonMode(1032, 6914);
        }
        Core.getInstance().modelViewMatrixStack.pop();
    }

    private void renderTreeTextures(Tree tree, boolean bUseShader) {
        float y2;
        float y1;
        int floorHeight = 32 * Core.tileScale;
        float dyFloor = (float)floorHeight / 2.0f;
        if (tree.texture != null) {
            y1 = tree.texture.getOffsetY();
            y2 = (float)tree.texture.getHeightOrig() - dyFloor;
            float y3 = tree.texture.getOffsetY() + (float)tree.texture.getHeight();
            this.renderTexture(tree, tree.texture, y1, y2, bUseShader);
            this.renderTexture(tree, tree.texture, y2, y3, bUseShader);
        }
        if (tree.texture2 != null) {
            y1 = tree.texture2.getOffsetY();
            y2 = tree.texture2.getOffsetY() + (float)tree.texture2.getHeight();
            this.renderTexture(tree, tree.texture2, y1, y2, bUseShader);
        }
    }

    private float calculateDepth(Tree tree, boolean bTexture2, boolean bUseShader, boolean bFloorHack) {
        float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.playerX), PZMath.fastfloor(this.playerY), tree.x - 0.5f, tree.y - 0.5f, tree.z);
        float targetDepth = results.depthStart - (depthBufferValue + 1.0f) / 2.0f;
        FBORenderChunk renderChunk = FBORenderChunkManager.instance.renderThreadCurrent;
        if (renderChunk != null) {
            float chunkDepth = IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(this.playerX / 8.0f)), (int)PZMath.fastfloor((float)(this.playerY / 8.0f)), (int)renderChunk.chunk.wx, (int)renderChunk.chunk.wy, (int)PZMath.fastfloor((float)tree.z)).depthStart;
            targetDepth -= chunkDepth;
        }
        if (bTexture2) {
            targetDepth -= 1.0E-4f;
        }
        if (bUseShader) {
            targetDepth -= 2.0E-4f;
        }
        if (PZMath.fastfloor(tree.x) % 2 == 0) {
            targetDepth += 2.0E-4f;
        }
        if (bFloorHack) {
            targetDepth -= 0.0015f;
        }
        return targetDepth;
    }

    private void renderTexture(Tree tree, Texture tex, float top, float bottom, boolean bUseShader) {
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.startRun(VBORenderer.getInstance().formatPositionColorUv);
        vbor.setMode(7);
        vbor.setTextureID(tex.getTextureId());
        vbor.setMinMagFilters(9728, 9728);
        vbor.setDepthTest(true);
        if (shader == null) {
            shader = ShaderManager.instance.getOrCreateShader("vboRenderer_Tree", true, false);
        }
        if (bUseShader && shader.getShaderProgram().isCompiled()) {
            shader.Start();
            shader.getShaderProgram().setSamplerUnit("DIFFUSE", 0);
            shader.End();
            vbor.setShaderProgram(shader.getShaderProgram());
            vbor.cmdShader4f("outlineColor", 0.1f, 0.1f, 0.1f, this.playerInside && tree.fadeAlpha < 0.5f ? tree.fadeAlpha : 1.0f - tree.fadeAlpha);
            vbor.cmdShader2f("stepSize", 0.25f / (float)tex.getWidth(), 0.25f / (float)tex.getHeight());
        }
        float targetDepth = this.calculateDepth(tree, tex == tree.texture2, bUseShader, top > tex.getOffsetY());
        vbor.setUserDepth(targetDepth);
        boolean bJumbo = tex.getWidthOrig() == Core.tileScale * 64 * 3;
        int unitsX = 2;
        int unitsY = 4;
        if (bJumbo) {
            unitsX = 6;
            unitsY = 8;
        }
        float x1 = tex.getOffsetX();
        float x2 = tex.getOffsetX() + (float)tex.getWidth();
        float x3 = tex.getOffsetX() + (float)tex.getWidth();
        float x4 = tex.getOffsetX();
        float y1 = top;
        float y2 = top;
        float y3 = bottom;
        float y4 = bottom;
        if (tree.objectRenderEffects) {
            this.calculateBarycentricCoordinatesForVertex(tex, x1, y1, 0);
            this.calculateBarycentricCoordinatesForVertex(tex, x2, y2, 1);
            this.calculateBarycentricCoordinatesForVertex(tex, x3, y3, 2);
            this.calculateBarycentricCoordinatesForVertex(tex, x4, y4, 3);
            double ax = 0.0;
            double ay = 0.0;
            double bx = tex.getWidthOrig();
            double by = 0.0;
            double cx = tex.getWidthOrig();
            double cy = tex.getHeightOrig();
            double dx = 0.0;
            double dy = tex.getHeightOrig();
            int texW = 128;
            int texH = 256;
            double[] xy = s_barycentric[0].compute(ax += (double)(tree.oreX1 * 128.0f), ay += (double)(tree.oreY1 * 256.0f), bx += (double)(tree.oreX2 * 128.0f), by += (double)(tree.oreY2 * 256.0f), cx += (double)(tree.oreX3 * 128.0f), cy += (double)(tree.oreY3 * 256.0f), dx += (double)(tree.oreX4 * 128.0f), dy += (double)(tree.oreY4 * 256.0f));
            x1 = (float)xy[0];
            y1 = (float)xy[1];
            xy = s_barycentric[1].compute(ax, ay, bx, by, cx, cy, dx, dy);
            x2 = (float)xy[0];
            y2 = (float)xy[1];
            xy = s_barycentric[2].compute(ax, ay, bx, by, cx, cy, dx, dy);
            x3 = (float)xy[0];
            y3 = (float)xy[1];
            xy = s_barycentric[3].compute(ax, ay, bx, by, cx, cy, dx, dy);
            x4 = (float)xy[0];
            y4 = (float)xy[1];
        }
        float dx = (float)unitsX / SQRT2 / 2.0f;
        float xTL = this.pixelToGLX(tex, x1, unitsX) - dx;
        float xTR = this.pixelToGLX(tex, x2, unitsX) - dx;
        float xBR = this.pixelToGLX(tex, x3, unitsX) - dx;
        float xBL = this.pixelToGLX(tex, x4, unitsX) - dx;
        float yTL = (float)unitsY - this.pixelToGLY(tex, y1, unitsY);
        float yTR = (float)unitsY - this.pixelToGLY(tex, y2, unitsY);
        float yBR = this.pixelToGLY(tex, (float)tex.getHeightOrig() - y3, unitsY);
        float yBL = this.pixelToGLY(tex, (float)tex.getHeightOrig() - y4, unitsY);
        float yStart = tex.getYStart() + (top - tex.getOffsetY()) / (float)tex.getHeight() * (tex.getYEnd() - tex.getYStart());
        float yEnd = tex.getYStart() + (bottom - tex.getOffsetY()) / (float)tex.getHeight() * (tex.getYEnd() - tex.getYStart());
        float z = 0.0f;
        vbor.addQuad(xTL, yTL * 0.8164967f, 0.0f, tex.getXStart(), yStart, xTR, yTR * 0.8164967f, 0.0f, tex.getXEnd(), yStart, xBR, yBR * 0.8164967f, 0.0f, tex.getXEnd(), yEnd, xBL, yBL * 0.8164967f, 0.0f, tex.getXStart(), yEnd, tree.r, tree.g, tree.b, tree.a);
        vbor.endRun();
    }

    private void calculateBarycentricCoordinatesForVertex(Texture tex, float x, float y, int index) {
        Barycentric barycentric = s_barycentric[index];
        float x1 = 0.0f;
        float y1 = 0.0f;
        float x2 = tex.getWidthOrig();
        float y2 = 0.0f;
        float x3 = tex.getWidthOrig();
        float y3 = tex.getHeightOrig();
        float x4 = 0.0f;
        float y4 = tex.getHeightOrig();
        if (Anim2DBlendTriangle.PointInTriangle(x, y, 0.0f, 0.0f, x2, 0.0f, x3, y3)) {
            barycentric.triangle = 0;
            double[] bcc = s_barycentricCompute.compute(x, y, 0.0, 0.0, x2, 0.0, x3, y3);
            barycentric.u = bcc[0];
            barycentric.v = bcc[1];
            barycentric.w = bcc[2];
        } else {
            barycentric.triangle = 1;
            double[] bcc = s_barycentricCompute.compute(x, y, 0.0, 0.0, x3, y3, 0.0, y4);
            barycentric.u = bcc[0];
            barycentric.v = bcc[1];
            barycentric.w = bcc[2];
        }
    }

    private float pixelToGLX(Texture tex, float pixels, int unitsX) {
        float u = pixels / (float)tex.getWidthOrig();
        return u *= (float)unitsX / SQRT2;
    }

    private float pixelToGLY(Texture tex, float pixels, int unitsY) {
        float v = pixels / (float)tex.getHeightOrig();
        return v *= (float)unitsY;
    }

    @Override
    public void postRender() {
        s_treePool.releaseAll((List<Tree>)this.trees);
        this.trees.clear();
        s_pool.release(this);
    }

    public void init() {
        s_treePool.releaseAll((List<Tree>)this.trees);
        this.trees.clear();
        this.playerX = IsoCamera.frameState.camCharacterX;
        this.playerY = IsoCamera.frameState.camCharacterY;
        this.playerZ = IsoCamera.frameState.camCharacterZ;
        this.playerInside = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
    }

    public void addTree(Texture texture, Texture texture2, float x, float y, float z, float r, float g, float b, float a, ObjectRenderEffects ore, boolean bUseStencil, float fadeAlpha) {
        Tree tree = s_treePool.alloc();
        tree.texture = texture;
        tree.texture2 = texture2;
        tree.x = x + 1.0f;
        tree.y = y + 1.0f;
        tree.z = z;
        tree.r = r;
        tree.g = g;
        tree.b = b;
        tree.a = a;
        boolean bl = tree.objectRenderEffects = ore != null && (ore.x1 != 0.0 || ore.y1 != 0.0 || ore.x2 != 0.0 || ore.y2 != 0.0 || ore.x3 != 0.0 || ore.y3 != 0.0 || ore.x4 != 0.0 || ore.y4 != 0.0);
        if (tree.objectRenderEffects) {
            tree.oreX1 = (float)ore.x1;
            tree.oreY1 = (float)ore.y1;
            tree.oreX2 = (float)ore.x2;
            tree.oreY2 = (float)ore.y2;
            tree.oreX3 = (float)ore.x3;
            tree.oreY3 = (float)ore.y3;
            tree.oreX4 = (float)ore.x4;
            tree.oreY4 = (float)ore.y4;
        }
        tree.useStencil = bUseStencil;
        tree.fadeAlpha = fadeAlpha;
        this.trees.add(tree);
    }

    static {
        s_chunkCamera = new FBORenderChunkCamera();
        SQRT2 = (float)Math.sqrt(2.0);
        s_barycentricCompute = new BarycentricCompute();
        s_barycentric = new Barycentric[4];
        for (int i = 0; i < s_barycentric.length; ++i) {
            FBORenderTrees.s_barycentric[i] = new Barycentric();
        }
    }

    static final class Tree {
        Texture texture;
        Texture texture2;
        float x;
        float y;
        float z;
        float r;
        float g;
        float b;
        float a;
        boolean objectRenderEffects;
        boolean useStencil;
        float fadeAlpha;
        float oreX1;
        float oreY1;
        float oreX2;
        float oreY2;
        float oreX3;
        float oreY3;
        float oreX4;
        float oreY4;

        Tree() {
        }
    }

    static final class Barycentric {
        int triangle;
        double u;
        double v;
        double w;
        double[] pt = new double[2];

        Barycentric() {
        }

        double[] compute(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
            if (this.triangle == 0) {
                this.pt[0] = this.u * ax + this.v * bx + this.w * cx;
                this.pt[1] = this.u * ay + this.v * by + this.w * cy;
            } else {
                this.pt[0] = this.u * ax + this.v * cx + this.w * dx;
                this.pt[1] = this.u * ay + this.v * cy + this.w * dy;
            }
            return this.pt;
        }
    }

    static final class BarycentricCompute {
        double[] v0 = new double[2];
        double[] v1 = new double[2];
        double[] v2 = new double[2];
        double[] barycentric = new double[3];

        BarycentricCompute() {
        }

        double[] compute(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
            this.v0[0] = bx - ax;
            this.v0[1] = by - ay;
            this.v1[0] = cx - ax;
            this.v1[1] = cy - ay;
            this.v2[0] = px - ax;
            this.v2[1] = py - ay;
            double dot00 = this.v0[0] * this.v0[0] + this.v0[1] * this.v0[1];
            double dot01 = this.v0[0] * this.v1[0] + this.v0[1] * this.v1[1];
            double dot02 = this.v0[0] * this.v2[0] + this.v0[1] * this.v2[1];
            double dot11 = this.v1[0] * this.v1[0] + this.v1[1] * this.v1[1];
            double dot12 = this.v1[0] * this.v2[0] + this.v1[1] * this.v2[1];
            double invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01);
            this.barycentric[1] = (dot11 * dot02 - dot01 * dot12) * invDenom;
            this.barycentric[2] = (dot00 * dot12 - dot01 * dot02) * invDenom;
            this.barycentric[0] = 1.0 - this.barycentric[1] - this.barycentric[2];
            return this.barycentric;
        }
    }
}

