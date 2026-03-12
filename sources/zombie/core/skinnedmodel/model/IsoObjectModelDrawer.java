/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.nio.FloatBuffer;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjglx.BufferUtils;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelOutlines;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.vehicles.BaseVehicle;

public final class IsoObjectModelDrawer
extends TextureDraw.GenericDrawer {
    private static final ObjectPool<IsoObjectModelDrawer> s_modelDrawerPool = new ObjectPool<IsoObjectModelDrawer>(IsoObjectModelDrawer::new);
    private static final Vector3f tempVector3f = new Vector3f(0.0f, 5.0f, -2.0f);
    private static final Vector2 tempVector2_1 = new Vector2();
    private SpriteModel spriteModel;
    private Model model;
    private ModelScript modelScript;
    private Texture texture;
    private AnimationPlayer animationPlayer;
    private FloatBuffer matrixPalette;
    private float hue;
    private float tintR;
    private float tintG;
    private float tintB;
    private float x;
    private float y;
    private float z;
    private final Vector3f angle = new Vector3f();
    private final org.joml.Matrix4f transform = new org.joml.Matrix4f();
    private float ambientR;
    private float ambientG;
    private float ambientB;
    private float alpha = 1.0f;
    private boolean renderToChunkTexture;
    private float squareDepth;
    private boolean outline;
    private final ColorInfo outlineColor = new ColorInfo();
    private boolean outlineBehindPlayer = true;
    static final WorldModelCamera s_worldModelCamera = new WorldModelCamera();

    public static RenderStatus renderMain(SpriteModel spriteModel, float x, float y, float z, ColorInfo colorInfo, float renderYOffset) {
        return IsoObjectModelDrawer.renderMain(spriteModel, x, y, z, colorInfo, renderYOffset, null, null, false, true);
    }

    public static RenderStatus renderMainOutline(SpriteModel spriteModel, float x, float y, float z, ColorInfo colorInfo, float renderYOffset) {
        return IsoObjectModelDrawer.renderMain(spriteModel, x, y, z, colorInfo, renderYOffset, null, null, true, true);
    }

    public static RenderStatus renderMain(SpriteModel spriteModel, float x, float y, float z, ColorInfo colorInfo, float renderYOffset, SpriteModel parentSpriteModel, org.joml.Matrix4f attachmentWorldXfrm, boolean bOutline, boolean bApplySurfaceAlpha) {
        RenderStatus renderStatus;
        AnimationsMesh animationsMesh;
        ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.modelScriptName);
        if (modelScript == null) {
            return RenderStatus.NoModel;
        }
        String meshName = modelScript.getMeshName();
        String texName = modelScript.getTextureName();
        String shaderName = modelScript.getShaderName();
        ImmutableColor tint = ImmutableColor.white;
        float hue = 1.0f;
        boolean bStatic = modelScript.isStatic;
        Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, true);
        if (model == null && !bStatic && modelScript.animationsMesh != null && (animationsMesh = ScriptManager.instance.getAnimationsMesh(modelScript.animationsMesh)) != null && animationsMesh.modelMesh != null) {
            model = ModelManager.instance.loadModel(meshName, texName, animationsMesh.modelMesh, shaderName);
        }
        if (model == null) {
            ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
            model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
        }
        if (model == null) {
            return RenderStatus.NoModel;
        }
        if (model.isFailure()) {
            return RenderStatus.Failed;
        }
        if (!model.isReady() || model.mesh == null || !model.mesh.isReady()) {
            return RenderStatus.Loading;
        }
        IsoObjectModelDrawer modelDrawer = s_modelDrawerPool.alloc();
        modelDrawer.spriteModel = spriteModel;
        modelDrawer.model = model;
        modelDrawer.modelScript = modelScript;
        modelDrawer.texture = null;
        if (spriteModel.getTextureName() != null) {
            modelDrawer.texture = spriteModel.getTextureName().contains("media/") ? Texture.getSharedTexture(spriteModel.getTextureName()) : Texture.getSharedTexture("media/textures/" + spriteModel.getTextureName() + ".png");
            renderStatus = IsoObjectModelDrawer.checkTextureStatus(modelDrawer.texture);
            if (renderStatus != RenderStatus.Ready) {
                s_modelDrawerPool.release(modelDrawer);
                return renderStatus;
            }
        } else if (model.tex != null && (renderStatus = IsoObjectModelDrawer.checkTextureStatus(model.tex)) != RenderStatus.Ready) {
            s_modelDrawerPool.release(modelDrawer);
            return renderStatus;
        }
        modelDrawer.animationPlayer = null;
        modelDrawer.initMatrixPalette();
        modelDrawer.x = x;
        modelDrawer.y = y;
        modelDrawer.z = z;
        modelDrawer.tintR = tint.r;
        modelDrawer.tintG = tint.g;
        modelDrawer.tintB = tint.b;
        modelDrawer.hue = 1.0f;
        modelDrawer.transform.identity();
        float dy = renderYOffset / 96.0f * 2.44949f;
        if (parentSpriteModel != null) {
            modelDrawer.transform.translate(-parentSpriteModel.translate.x / 1.5f, (parentSpriteModel.translate.y + dy) / 1.5f, parentSpriteModel.translate.z / 1.5f);
            modelDrawer.transform.rotateXYZ(parentSpriteModel.rotate.x * ((float)Math.PI / 180), -parentSpriteModel.rotate.y * ((float)Math.PI / 180), parentSpriteModel.rotate.z * ((float)Math.PI / 180));
            ModelScript modelScript1 = ScriptManager.instance.getModelScript(parentSpriteModel.modelScriptName);
            if (modelScript1.scale != 1.0f) {
                modelDrawer.transform.scale(modelScript1.scale);
            }
            if (parentSpriteModel.scale != 1.0f) {
                modelDrawer.transform.scale(parentSpriteModel.scale);
            }
            if (attachmentWorldXfrm != null) {
                modelDrawer.transform.mul(attachmentWorldXfrm);
                if (modelScript.scale * spriteModel.scale * 1.5f != 1.0f) {
                    modelDrawer.transform.scale(modelScript.scale * spriteModel.scale * 1.5f);
                }
            }
        } else {
            modelDrawer.transform.translate(-spriteModel.translate.x / 1.5f, (spriteModel.translate.y + dy) / 1.5f, spriteModel.translate.z / 1.5f);
            modelDrawer.transform.rotateXYZ(spriteModel.rotate.x * ((float)Math.PI / 180), -spriteModel.rotate.y * ((float)Math.PI / 180), spriteModel.rotate.z * ((float)Math.PI / 180));
            if (modelScript.scale != 1.0f) {
                modelDrawer.transform.scale(modelScript.scale);
            }
            if (spriteModel.scale != 1.0f) {
                modelDrawer.transform.scale(spriteModel.scale);
            }
        }
        modelDrawer.angle.set(0.0f);
        ModelInstanceRenderData.postMultiplyMeshTransform(modelDrawer.transform, model.mesh);
        modelDrawer.ambientR = colorInfo.r;
        modelDrawer.ambientG = colorInfo.g;
        modelDrawer.ambientB = colorInfo.b;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        float zoff = PZMath.max(renderYOffset / 96.0f, z - (float)PZMath.fastfloor(z));
        modelDrawer.alpha = !bApplySurfaceAlpha || square == null ? 1.0f : IsoWorldInventoryObject.getSurfaceAlpha(square, zoff, false);
        modelDrawer.alpha *= colorInfo.a;
        if (DebugOptions.instance.fboRenderChunk.forceAlphaAndTargetOne.getValue()) {
            modelDrawer.alpha = 1.0f;
        }
        float x1 = x;
        float y1 = y;
        float z1 = z;
        if (!modelScript.isStatic) {
            x1 += spriteModel.translate.x;
            y1 += spriteModel.translate.z;
            z1 += spriteModel.translate.y / 2.44949f;
        }
        PlayerCamera camera = IsoCamera.cameras[IsoCamera.frameState.playerIndex];
        if (PerformanceSettings.fboRenderChunk && !FBORenderChunkManager.instance.isCaching()) {
            x1 += camera.fixJigglyModelsSquareX;
            y1 += camera.fixJigglyModelsSquareY;
        }
        modelDrawer.renderToChunkTexture = !bOutline && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching();
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), x1, y1, z1);
        if (modelDrawer.renderToChunkTexture) {
            results.depthStart -= IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterX / 8.0f)), (int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterY / 8.0f)), (int)PZMath.fastfloor((float)(x / 8.0f)), (int)PZMath.fastfloor((float)(y / 8.0f)), (int)PZMath.fastfloor((float)z)).depthStart;
        }
        modelDrawer.squareDepth = results.depthStart;
        if (FBORenderObjectHighlight.getInstance().isRendering()) {
            modelDrawer.squareDepth -= 5.0E-5f;
        }
        modelDrawer.outline = bOutline;
        modelDrawer.outlineColor.set(colorInfo);
        modelDrawer.outlineBehindPlayer = x + y < IsoCamera.frameState.camCharacterX + IsoCamera.frameState.camCharacterY;
        SpriteRenderer.instance.drawGeneric(modelDrawer);
        return RenderStatus.Ready;
    }

    public static RenderStatus renderMain(SpriteModel spriteModel, float x, float y, float z, ColorInfo colorInfo, float renderYOffset, AnimationPlayer animationPlayer) {
        RenderStatus renderStatus;
        ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.modelScriptName);
        if (modelScript == null) {
            return RenderStatus.NoModel;
        }
        ImmutableColor tint = ImmutableColor.white;
        float hue = 1.0f;
        Model model = animationPlayer.getModel();
        if (model == null) {
            return RenderStatus.NoModel;
        }
        if (model.isFailure()) {
            return RenderStatus.Failed;
        }
        if (!model.isReady() || model.mesh == null || !model.mesh.isReady()) {
            return RenderStatus.Loading;
        }
        IsoObjectModelDrawer modelDrawer = s_modelDrawerPool.alloc();
        modelDrawer.model = model;
        modelDrawer.modelScript = modelScript;
        modelDrawer.texture = null;
        if (spriteModel.getTextureName() != null) {
            modelDrawer.texture = spriteModel.getTextureName().contains("media/") ? Texture.getSharedTexture(spriteModel.getTextureName()) : Texture.getSharedTexture("media/textures/" + spriteModel.getTextureName() + ".png");
            renderStatus = IsoObjectModelDrawer.checkTextureStatus(modelDrawer.texture);
            if (renderStatus != RenderStatus.Ready) {
                s_modelDrawerPool.release(modelDrawer);
                return renderStatus;
            }
        } else if (model.tex != null && (renderStatus = IsoObjectModelDrawer.checkTextureStatus(model.tex)) != RenderStatus.Ready) {
            s_modelDrawerPool.release(modelDrawer);
            return renderStatus;
        }
        modelDrawer.animationPlayer = animationPlayer;
        modelDrawer.initMatrixPalette();
        modelDrawer.x = x;
        modelDrawer.y = y;
        modelDrawer.z = z;
        modelDrawer.tintR = tint.r;
        modelDrawer.tintG = tint.g;
        modelDrawer.tintB = tint.b;
        modelDrawer.hue = 1.0f;
        modelDrawer.transform.identity();
        float dy = renderYOffset / 96.0f * 2.44949f;
        modelDrawer.transform.translate(-spriteModel.translate.x / 1.5f, (spriteModel.translate.y + dy) / 1.5f, spriteModel.translate.z / 1.5f);
        modelDrawer.transform.rotateXYZ(spriteModel.rotate.x * ((float)Math.PI / 180), -spriteModel.rotate.y * ((float)Math.PI / 180), spriteModel.rotate.z * ((float)Math.PI / 180));
        if (modelScript.scale != 1.0f) {
            modelDrawer.transform.scale(modelScript.scale);
        }
        if (spriteModel.scale != 1.0f) {
            modelDrawer.transform.scale(spriteModel.scale);
        }
        modelDrawer.angle.set(0.0f);
        ModelInstanceRenderData.postMultiplyMeshTransform(modelDrawer.transform, model.mesh);
        modelDrawer.ambientR = colorInfo.r;
        modelDrawer.ambientG = colorInfo.g;
        modelDrawer.ambientB = colorInfo.b;
        if (Core.debug) {
            // empty if block
        }
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        float zoff = PZMath.max(renderYOffset / 96.0f, z - (float)PZMath.fastfloor(z));
        modelDrawer.alpha = square == null ? 1.0f : IsoWorldInventoryObject.getSurfaceAlpha(square, zoff, false);
        modelDrawer.alpha *= colorInfo.a;
        if (DebugOptions.instance.fboRenderChunk.forceAlphaAndTargetOne.getValue()) {
            modelDrawer.alpha = 1.0f;
        }
        modelDrawer.renderToChunkTexture = PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching();
        float x1 = x;
        float y1 = y;
        float z1 = z;
        if (!modelScript.isStatic) {
            x1 += spriteModel.translate.x;
            y1 += spriteModel.translate.z;
            z1 += spriteModel.translate.y / 2.44949f;
        }
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), x1, y1, z1);
        if (modelDrawer.renderToChunkTexture) {
            results.depthStart -= IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterX / 8.0f)), (int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterY / 8.0f)), (int)PZMath.fastfloor((float)(x / 8.0f)), (int)PZMath.fastfloor((float)(y / 8.0f)), (int)PZMath.fastfloor((float)z)).depthStart;
        }
        modelDrawer.squareDepth = results.depthStart;
        modelDrawer.outline = false;
        SpriteRenderer.instance.drawGeneric(modelDrawer);
        return RenderStatus.Ready;
    }

    private static RenderStatus checkTextureStatus(Texture texture) {
        if (texture == null || texture.isFailure()) {
            return RenderStatus.Failed;
        }
        if (!texture.isReady()) {
            return RenderStatus.Loading;
        }
        return RenderStatus.Ready;
    }

    private void initMatrixPalette() {
        SkinningData skinningData = (SkinningData)this.model.tag;
        if (skinningData == null) {
            this.matrixPalette = null;
            return;
        }
        if (this.animationPlayer == null) {
            if (this.model.isStatic || this.spriteModel.getAnimationName() == null) {
                this.matrixPalette = null;
            } else {
                FloatBuffer matrixPalette = IsoObjectAnimations.getInstance().getMatrixPaletteForFrame(this.model, this.spriteModel.getAnimationName(), this.spriteModel.getAnimationTime());
                if (matrixPalette == null) {
                    return;
                }
                matrixPalette.position(0);
                if (this.matrixPalette == null || this.matrixPalette.capacity() < matrixPalette.capacity()) {
                    this.matrixPalette = BufferUtils.createFloatBuffer(matrixPalette.capacity());
                }
                this.matrixPalette.clear();
                this.matrixPalette.put(matrixPalette);
                this.matrixPalette.flip();
            }
            return;
        }
        Matrix4f[] skinTransforms = this.animationPlayer.getSkinTransforms(skinningData);
        int matrixFloats = 16;
        if (this.matrixPalette == null || this.matrixPalette.capacity() < skinTransforms.length * 16) {
            this.matrixPalette = BufferUtils.createFloatBuffer(skinTransforms.length * 16);
        }
        this.matrixPalette.clear();
        for (int i = 0; i < skinTransforms.length; ++i) {
            skinTransforms[i].store(this.matrixPalette);
        }
        this.matrixPalette.flip();
    }

    @Override
    public void render() {
        FBORenderChunk renderChunk = FBORenderChunkManager.instance.renderThreadCurrent;
        if (PerformanceSettings.fboRenderChunk && renderChunk != null) {
            this.angle.y += 180.0f;
            FBORenderItems.getInstance().setCamera(renderChunk, this.x, this.y, this.z, this.angle);
            this.angle.y -= 180.0f;
            this.renderToChunkTexture(FBORenderItems.getInstance().getCamera(), renderChunk.highRes);
            return;
        }
        this.renderToWorld();
    }

    @Override
    public void postRender() {
        s_modelDrawerPool.release(this);
    }

    private void renderToChunkTexture(IModelCamera camera, boolean bHighRes) {
        GL11.glPushAttrib(1048575);
        GL11.glPushClientAttrib(-1);
        camera.Begin();
        this.renderModel(bHighRes);
        camera.End();
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
        Texture.lastTextureID = -1;
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        SpriteRenderer.ringBuffer.restoreVbos = true;
    }

    private void renderToWorld() {
        GL11.glPushAttrib(1048575);
        GL11.glPushClientAttrib(-1);
        if (this.outline) {
            boolean clear = ModelOutlines.instance.beginRenderOutline(this.outlineColor, this.outlineBehindPlayer, false);
            GL11.glDepthMask(true);
            ModelOutlines.instance.fboA.startDrawing(clear, true);
            if (ModelSlotRenderData.solidColor == null) {
                ModelSlotRenderData.solidColor = new Shader("aim_outline_solid", false, false);
                ModelSlotRenderData.solidColorStatic = new Shader("aim_outline_solid", true, false);
            }
            ModelSlotRenderData.solidColor.Start();
            ModelSlotRenderData.solidColor.getShaderProgram().setVector4("u_color", this.outlineColor.r, this.outlineColor.g, this.outlineColor.b, 1.0f);
            ModelSlotRenderData.solidColor.End();
            ModelSlotRenderData.solidColorStatic.Start();
            ModelSlotRenderData.solidColorStatic.getShaderProgram().setVector4("u_color", this.outlineColor.r, this.outlineColor.g, this.outlineColor.b, 1.0f);
            ModelSlotRenderData.solidColorStatic.End();
        }
        IsoObjectModelDrawer.s_worldModelCamera.x = this.x;
        IsoObjectModelDrawer.s_worldModelCamera.y = this.y;
        IsoObjectModelDrawer.s_worldModelCamera.z = this.z;
        s_worldModelCamera.Begin();
        this.renderModel(false);
        s_worldModelCamera.End();
        if (this.outline) {
            ModelOutlines.instance.fboA.endDrawing();
        }
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
        GLStateRenderThread.restore();
        Texture.lastTextureID = -1;
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        SpriteRenderer.ringBuffer.restoreVbos = true;
    }

    private void renderModel(boolean bHighRes) {
        Model model = this.model;
        if (model.effect == null) {
            model.CreateShader("basicEffect");
        }
        Shader effect = model.effect;
        if (this.outline) {
            Shader shader = effect = model.isStatic ? ModelSlotRenderData.solidColorStatic : ModelSlotRenderData.solidColor;
        }
        if (effect == null || model.mesh == null || !model.mesh.isReady()) {
            return;
        }
        IndieGL.glDefaultBlendFuncA();
        GL11.glDepthFunc(513);
        GL11.glDepthMask(true);
        GL11.glDepthRange(0.0, 1.0);
        GL11.glEnable(2929);
        if (effect.getShaderProgram().getName().contains("door") && FBORenderChunkManager.instance.renderThreadCurrent == null) {
            GL11.glEnable(2884);
            GL11.glCullFace(1028);
        } else if (effect.getShaderProgram().getName().contains("door") && FBORenderChunkManager.instance.renderThreadCurrent != null) {
            GL11.glEnable(2884);
            GL11.glCullFace(1029);
        } else if (this.modelScript != null && this.modelScript.cullFace != -1) {
            if (this.modelScript.cullFace == 0) {
                GL11.glDisable(2884);
            } else {
                GL11.glEnable(2884);
                GL11.glCullFace(this.modelScript.cullFace);
            }
        }
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        boolean bPushTransform = false;
        if (!this.model.isStatic) {
            PZGLUtil.pushAndMultMatrix(5888, this.transform);
            bPushTransform = true;
            if (this.modelScript.meshName.contains("door1")) {
                // empty if block
            }
        }
        effect.Start();
        Texture tex = model.tex;
        if (this.texture != null) {
            tex = this.texture;
        }
        if (this.outline && model.effect != null && model.effect.getName().contains("door")) {
            effect.setTexture(Texture.getWhite(), "Texture", 0);
        } else if (tex != null) {
            if (!tex.getTextureId().hasMipMaps()) {
                GL11.glBlendFunc(770, 771);
            }
            effect.setTexture(tex, "Texture", 0);
            if (effect.getShaderProgram().getName().equalsIgnoreCase("door")) {
                int widthHW = tex.getWidthHW();
                int heightHW = tex.getHeightHW();
                float x1 = tex.xStart * (float)widthHW - tex.offsetX;
                float y1 = tex.yStart * (float)heightHW - tex.offsetY;
                float x2 = x1 + (float)tex.getWidthOrig();
                float y2 = y1 + (float)tex.getHeightOrig();
                effect.getShaderProgram().setValue("UVOffset", tempVector2_1.set(x1 / (float)widthHW, y1 / (float)heightHW));
                effect.getShaderProgram().setValue("UVScale", tempVector2_1.set((x2 - x1) / (float)widthHW, (y2 - y1) / (float)heightHW));
            }
        }
        effect.setDepthBias(0.0f);
        float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
        float targetDepth = this.squareDepth - (depthBufferValue + 1.0f) / 2.0f + 0.5f;
        if (!PerformanceSettings.fboRenderChunk) {
            targetDepth = 0.5f;
        }
        effect.setTargetDepth(targetDepth);
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            effect.setAmbient(1.0f, 1.0f, 1.0f);
        } else {
            effect.setAmbient(this.ambientR, this.ambientG, this.ambientB);
        }
        effect.setLightingAmount(1.0f);
        effect.setHueShift(this.hue);
        effect.setTint(this.tintR, this.tintG, this.tintB);
        effect.setAlpha(DebugOptions.instance.model.render.forceAlphaOne.getValue() ? 1.0f : this.alpha);
        for (int i = 0; i < 5; ++i) {
            effect.setLight(i, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, 0.0f, 0.0f, 0.0f, null);
        }
        if (!DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            Vector3f pos = tempVector3f;
            pos.x = 0.0f;
            pos.y = 5.0f;
            pos.z = -2.0f;
            pos.rotateY(this.angle.y * ((float)Math.PI / 180));
            float lightMul = 1.5f;
            effect.setLight(4, pos.x, pos.z, pos.y, this.ambientR / 4.0f * 1.5f, this.ambientG / 4.0f * 1.5f, this.ambientB / 4.0f * 1.5f, 5000.0f, Float.NaN, 0.0f, 0.0f, 0.0f, null);
        }
        if (model.isStatic) {
            effect.setTransformMatrix(this.transform, false);
        } else if (this.matrixPalette != null) {
            effect.setMatrixPalette(this.matrixPalette, true);
        }
        if (this.renderToChunkTexture && bHighRes) {
            effect.setHighResDepthMultiplier(0.5f);
        }
        model.mesh.Draw(effect);
        if (this.renderToChunkTexture && bHighRes) {
            effect.setHighResDepthMultiplier(0.0f);
        }
        effect.End();
        if (bPushTransform) {
            PZGLUtil.popMatrix(5888);
        }
        org.joml.Matrix4f m = BaseVehicle.allocMatrix4f();
        m.set(this.transform);
        m.mul(this.model.mesh.transform);
        if (DebugOptions.instance.model.render.attachments.getValue()) {
            for (int i = 0; i < this.modelScript.getAttachmentCount(); ++i) {
                ModelAttachment attachment = this.modelScript.getAttachment(i);
                org.joml.Matrix4f attachmentMatrix = BaseVehicle.allocMatrix4f();
                ModelInstanceRenderData.makeAttachmentTransform(attachment, attachmentMatrix);
                m.mul(attachmentMatrix, attachmentMatrix);
                PZGLUtil.pushAndMultMatrix(5888, attachmentMatrix);
                BaseVehicle.releaseMatrix4f(attachmentMatrix);
                Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 0.1f, 1.0f);
                PZGLUtil.popMatrix(5888);
                VBORenderer vboRenderer = VBORenderer.getInstance();
                vboRenderer.cmdPushAndMultMatrix(5888, m);
                vboRenderer.startRun(vboRenderer.formatPositionColor);
                vboRenderer.setMode(1);
                vboRenderer.setLineWidth(2.0f);
                vboRenderer.addLine(attachment.getOffset().x, attachment.getOffset().y, attachment.getOffset().z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                vboRenderer.endRun();
                vboRenderer.cmdPopMatrix(5888);
                vboRenderer.flush();
            }
        }
        if (Core.debug && DebugOptions.instance.model.render.axis.getValue() && this.modelScript.name.contains("Sheet")) {
            m.scale(100.0f);
            PZGLUtil.pushAndMultMatrix(5888, m);
            Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 0.5f, 1.0f);
            PZGLUtil.popMatrix(5888);
        }
        BaseVehicle.releaseMatrix4f(m);
    }

    public static enum RenderStatus {
        NoModel,
        Failed,
        Loading,
        Ready;

    }

    private static final class WorldModelCamera
    implements IModelCamera {
        float x;
        float y;
        float z;

        private WorldModelCamera() {
        }

        @Override
        public void Begin() {
            float cx = Core.getInstance().floatParamMap.get(0).floatValue();
            float cy = Core.getInstance().floatParamMap.get(1).floatValue();
            float cz = Core.getInstance().floatParamMap.get(2).floatValue();
            double x = cx;
            double y = cy;
            double z = cz;
            SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
            int playerIndex = renderState.playerIndex;
            PlayerCamera cam = renderState.playerCamera[playerIndex];
            float rcx = cam.rightClickX;
            float rcy = cam.rightClickY;
            float defx = cam.deferedX;
            float defy = cam.deferedY;
            float tx = (rcx + 2.0f * rcy) / (64.0f * (float)Core.tileScale);
            float ty = (rcx - 2.0f * rcy) / (-64.0f * (float)Core.tileScale);
            x += (double)tx;
            y += (double)ty;
            x += (double)defx;
            y += (double)defy;
            double screenWidth = (float)cam.offscreenWidth / 1920.0f;
            double screenHeight = (float)cam.offscreenHeight / 1920.0f;
            org.joml.Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
            projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, -((float)screenHeight) / 2.0f, (float)screenHeight / 2.0f, -10.0f, 10.0f);
            Core.getInstance().projectionMatrixStack.push(projection);
            org.joml.Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.scaling(Core.scale);
            modelView.scale((float)Core.tileScale / 2.0f);
            modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
            modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
            double difX = (double)this.x - x;
            double difY = (double)this.y - y;
            modelView.translate(-((float)difX), (float)((double)this.z - z) * 2.44949f, -((float)difY));
            modelView.scale(-1.5f, 1.5f, 1.5f);
            modelView.rotate((float)Math.PI, 0.0f, 1.0f, 0.0f);
            modelView.translate(0.0f, -0.48f, 0.0f);
            Core.getInstance().modelViewMatrixStack.push(modelView);
        }

        @Override
        public void End() {
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
        }
    }
}

