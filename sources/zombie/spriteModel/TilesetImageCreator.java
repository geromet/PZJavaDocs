/*
 * Decompiled with CFR 0.152.
 */
package zombie.spriteModel;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.iso.SpriteModel;
import zombie.iso.SpriteModelsFile;
import zombie.iso.Vector2;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;
import zombie.spriteModel.SpriteModelManager;

public final class TilesetImageCreator {
    private static final int TILE_WIDTH = 128;
    private static final int TILE_HEIGHT = 256;
    TextureFBO fbo;
    final Matrix4f projection = new Matrix4f();
    final Matrix4f modelview = new Matrix4f();

    public void createImage(String modID, String tilesetName, String filePath) {
        SpriteModelsFile.Tileset tileset = SpriteModelManager.getInstance().findTileset(modID, tilesetName);
        if (tileset == null) {
            return;
        }
        int cols = 8;
        int rows = this.getTilesetRows(tilesetName);
        Model[] models = new Model[8 * rows];
        Texture[] textures = new Texture[8 * rows];
        FloatBuffer[] matrixPalettes = new FloatBuffer[8 * rows];
        this.loadModelsEtc(8, rows, tileset, models, textures, matrixPalettes);
        this.fbo = this.createFBO(1024, rows * 256);
        RenderThread.invokeOnRenderContext(() -> {
            this.renderTilesetToFBO(tileset, 8, rows, models, textures, matrixPalettes);
            ((Texture)this.fbo.getTexture()).saveOnRenderThread(filePath);
        });
        this.fbo.destroy();
    }

    int getTilesetRows(String tilesetName) {
        int cols = 8;
        for (int row = 63; row >= 0; --row) {
            for (int col = 0; col < 8; ++col) {
                int index = col + row * 8;
                Texture texture = Texture.getSharedTexture(tilesetName + "_" + index);
                if (texture == null) continue;
                return row + 1;
            }
        }
        return 0;
    }

    void loadModelsEtc(int cols, int rows, SpriteModelsFile.Tileset tileset, Model[] models, Texture[] textures, FloatBuffer[] matrixPalettes) {
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                SpriteModelsFile.Tile tile = tileset.getTile(col, row);
                if (tile == null) {
                    textures[col + row * cols] = Texture.getSharedTexture(String.format("%s_%d", tileset.getName(), col + row * cols));
                    continue;
                }
                this.loadModelsEtc(col, row, cols, tile.spriteModel, models, textures, matrixPalettes);
                if (models[col + row * cols] != null) continue;
                textures[col + row * cols] = Texture.getSharedTexture(String.format("%s_%d", tileset.getName(), col + row * cols));
            }
        }
        while (GameWindow.fileSystem.hasWork()) {
            GameWindow.fileSystem.updateAsyncTransactions();
        }
    }

    void loadModelsEtc(int col, int row, int columns, SpriteModel spriteModel, Model[] models, Texture[] textures, FloatBuffer[] matrixPalettes) {
        AnimationsMesh animationsMesh;
        String shaderName;
        boolean bStatic;
        String texName;
        ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.modelScriptName);
        if (modelScript == null) {
            return;
        }
        String meshName = modelScript.getMeshName();
        Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName = modelScript.getTextureName(), bStatic = modelScript.isStatic, shaderName = modelScript.getShaderName(), true);
        if (model == null && !bStatic && modelScript.animationsMesh != null && (animationsMesh = ScriptManager.instance.getAnimationsMesh(modelScript.animationsMesh)) != null && animationsMesh.modelMesh != null) {
            model = ModelManager.instance.loadModel(meshName, texName, animationsMesh.modelMesh, shaderName);
        }
        if (model == null) {
            ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
            model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
        }
        if (model == null) {
            return;
        }
        if (model.isFailure()) {
            return;
        }
        Texture texture = null;
        if (spriteModel.getTextureName() != null) {
            texture = spriteModel.getTextureName().contains("media/") ? Texture.getSharedTexture(spriteModel.getTextureName()) : Texture.getSharedTexture("media/textures/" + spriteModel.getTextureName() + ".png");
        } else if (model.tex != null) {
            texture = model.tex;
        }
        if (texture == null || texture.isFailure()) {
            return;
        }
        int index = col + row * columns;
        models[index] = model;
        textures[index] = texture;
        matrixPalettes[index] = null;
        if (!bStatic && spriteModel.getAnimationName() != null) {
            matrixPalettes[index] = IsoObjectAnimations.getInstance().getMatrixPaletteForFrame(model, spriteModel.getAnimationName(), spriteModel.getAnimationTime());
        }
    }

    TextureFBO createFBO(int width, int height) {
        Texture texture = new Texture(width, height, 16);
        return new TextureFBO(texture, false);
    }

    void renderTilesetToFBO(SpriteModelsFile.Tileset tileset, int cols, int rows, Model[] models, Texture[] textures, FloatBuffer[] matrixPalettes) {
        GL11.glPushAttrib(1048575);
        GL11.glPushClientAttrib(-1);
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
        GL11.glDisable(3089);
        this.fbo.startDrawing(true, true);
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                int index = col + row * cols;
                SpriteModelsFile.Tile tile = tileset.getTile(col, row);
                if (tile == null) {
                    this.renderTileToFBO(col, row, textures[index]);
                    continue;
                }
                this.renderTileToFBO(col, row, tile.spriteModel, models[index], textures[index], matrixPalettes[index]);
            }
        }
        this.fbo.endDrawing();
        Texture.lastTextureID = -1;
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        SpriteRenderer.ringBuffer.restoreVbos = true;
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
    }

    void renderTileToFBO(int col, int row, Texture texture) {
        if (texture == null) {
            return;
        }
        GL11.glViewport(0, 0, this.fbo.getWidth(), this.fbo.getHeight());
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(2929);
        GL11.glDisable(2884);
        this.projection.setOrtho2D(0.0f, this.fbo.getWidth(), 0.0f, this.fbo.getHeight());
        this.modelview.identity();
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.cmdPushAndLoadMatrix(5889, this.projection);
        vbor.cmdPushAndLoadMatrix(5888, this.modelview);
        vbor.startRun(vbor.formatPositionColorUv);
        vbor.setMode(7);
        vbor.setTextureID(texture.getTextureId());
        int x = col * 128;
        int y = row * 256;
        vbor.addQuad((float)x + texture.offsetX, (float)y + texture.offsetY, texture.xStart, texture.yStart, (float)x + texture.offsetX + (float)texture.getWidth(), (float)y + texture.offsetY + (float)texture.getHeight(), texture.xEnd, texture.yEnd, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        vbor.endRun();
        vbor.cmdPopMatrix(5889);
        vbor.cmdPopMatrix(5888);
        vbor.flush();
    }

    void renderTileToFBO(int col, int row, SpriteModel spriteModel, Model model, Texture texture, FloatBuffer matrixPalette) {
        if (!model.isStatic && matrixPalette == null) {
            return;
        }
        if (!model.isReady()) {
            return;
        }
        if (!texture.isReady()) {
            return;
        }
        Shader effect = model.effect;
        if (effect == null || model.mesh == null || !model.mesh.isReady()) {
            return;
        }
        int x = col * 128 - (this.fbo.getWidth() - 128) / 2;
        int y = row * 256 - (this.fbo.getHeight() - 256) / 2;
        GL11.glViewport(x, y += 96, this.fbo.getWidth(), this.fbo.getHeight());
        this.calcMatrices(this.projection, this.modelview, spriteModel, model);
        PZGLUtil.pushAndLoadMatrix(5889, this.projection);
        PZGLUtil.pushAndLoadMatrix(5888, this.modelview);
        IndieGL.glDefaultBlendFuncA();
        GL11.glDepthFunc(513);
        GL11.glDepthMask(true);
        GL11.glDepthRange(0.0, 1.0);
        GL11.glEnable(2929);
        if (effect.getShaderProgram().getName().contains("door")) {
            GL11.glEnable(2884);
            GL11.glCullFace(1029);
        } else {
            GL11.glDisable(2884);
        }
        effect.Start();
        if (texture == null) {
            effect.setTexture(Texture.getErrorTexture(), "Texture", 0);
        } else {
            if (!texture.getTextureId().hasMipMaps()) {
                GL11.glBlendFunc(770, 771);
            }
            effect.setTexture(texture, "Texture", 0);
            if (effect.getShaderProgram().getName().equalsIgnoreCase("door")) {
                int widthHW = texture.getWidthHW();
                int heightHW = texture.getHeightHW();
                float x1 = texture.xStart * (float)widthHW - texture.offsetX;
                float y1 = texture.yStart * (float)heightHW - texture.offsetY;
                float x2 = x1 + (float)texture.getWidthOrig();
                float y2 = y1 + (float)texture.getHeightOrig();
                Vector2 tempVector21 = new Vector2();
                effect.getShaderProgram().setValue("UVOffset", tempVector21.set(x1 / (float)widthHW, y1 / (float)heightHW));
                effect.getShaderProgram().setValue("UVScale", tempVector21.set((x2 - x1) / (float)widthHW, (y2 - y1) / (float)heightHW));
            }
        }
        effect.setDepthBias(0.0f);
        effect.setTargetDepth(0.5f);
        effect.setAmbient(1.0f, 1.0f, 1.0f);
        effect.setLightingAmount(1.0f);
        effect.setHueShift(1.0f);
        effect.setTint(1.0f, 1.0f, 1.0f);
        effect.setAlpha(1.0f);
        for (int i = 0; i < 5; ++i) {
            effect.setLight(i, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, 0.0f, 0.0f, 0.0f, null);
        }
        if (model.isStatic) {
            effect.setTransformMatrix(new Matrix4f(), false);
        } else {
            matrixPalette.position(0);
            effect.setMatrixPalette(matrixPalette, true);
        }
        model.mesh.Draw(effect);
        effect.End();
        PZGLUtil.popMatrix(5889);
        PZGLUtil.popMatrix(5888);
    }

    void calcMatrices(Matrix4f projection, Matrix4f modelView, SpriteModel spriteModel, Model model) {
        double screenWidth = (float)this.fbo.getWidth() / 1920.0f;
        double screenHeight = (float)this.fbo.getHeight() / 1920.0f;
        projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, (float)screenHeight / 2.0f, (float)(-screenHeight) / 2.0f, -1.0f, 1.0f);
        modelView.identity();
        modelView.scale(Core.scale * (float)Core.tileScale / 2.0f);
        modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
        modelView.rotateY((float)Math.PI);
        modelView.scale(-1.5f, 1.5f, 1.5f);
        modelView.translate(-spriteModel.translate.x / 1.5f, spriteModel.translate.y / 1.5f, spriteModel.translate.z / 1.5f);
        modelView.rotateXYZ(spriteModel.rotate.x * ((float)Math.PI / 180), -spriteModel.rotate.y * ((float)Math.PI / 180), spriteModel.rotate.z * ((float)Math.PI / 180));
        modelView.scale(spriteModel.scale);
        ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.modelScriptName);
        modelView.scale(modelScript.scale);
        ModelInstanceRenderData.postMultiplyMeshTransform(modelView, model.mesh);
    }
}

