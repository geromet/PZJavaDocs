/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import gnu.trove.map.hash.TObjectLongHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.model.ModelOutlines;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;
import zombie.savefile.SavefileThumbnail;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class FBORenderObjectOutline {
    private static FBORenderObjectOutline instance;
    private boolean rendering;
    private final HashSet<IsoObject> objectSet = new HashSet();
    private final ArrayList<IsoObject> objectList = new ArrayList();
    private TextureFBO fbo;
    private Texture fboTexture;
    private final ObjectPool<Drawer> drawerPool = new ObjectPool<Drawer>(Drawer::new);
    private final ObjectPool<Drawer2> drawer2Pool = new ObjectPool<Drawer2>(Drawer2::new);
    private final PerPlayerData[] perPlayerData = new PerPlayerData[4];

    public static FBORenderObjectOutline getInstance() {
        if (instance == null) {
            instance = new FBORenderObjectOutline();
        }
        return instance;
    }

    private FBORenderObjectOutline() {
        for (int i = 0; i < 4; ++i) {
            this.perPlayerData[i] = new PerPlayerData();
        }
    }

    public boolean isRendering() {
        return this.rendering;
    }

    public void registerObject(IsoObject object) {
        if (object instanceof IsoGameCharacter) {
            return;
        }
        if (this.objectSet.contains(object)) {
            return;
        }
        this.objectSet.add(object);
    }

    public void unregisterObject(IsoObject object) {
        boolean bRemoved = this.objectSet.remove(object);
    }

    public void render(int playerIndex) {
        if (SavefileThumbnail.isCreatingThumbnail()) {
            return;
        }
        this.objectList.clear();
        if (!this.objectSet.isEmpty()) {
            this.objectList.addAll(this.objectSet);
        }
        this.rendering = true;
        this.render(playerIndex, true);
        this.render(playerIndex, false);
        this.rendering = false;
    }

    private void render(int playerIndex, boolean bBehindPlayer) {
        for (int i = 0; i < this.objectList.size(); ++i) {
            IsoObject object = this.objectList.get(i);
            if (object.getObjectIndex() == -1 && object.getStaticMovingObjectIndex() == -1) {
                this.objectSet.remove(object);
                continue;
            }
            if (!object.isOutlineHighlight()) {
                this.objectSet.remove(object);
                continue;
            }
            if (!object.isOutlineHighlight(playerIndex) || this.isBehindPlayer(object) != bBehindPlayer) continue;
            this.renderObject(playerIndex, object);
        }
    }

    private boolean isBehindPlayer(IsoObject object) {
        float objectX = (float)object.getSquare().getX() + 0.5f;
        float objectY = (float)object.getSquare().getY() + 0.5f;
        if (object instanceof IsoWorldInventoryObject) {
            IsoWorldInventoryObject worldInventoryObject = (IsoWorldInventoryObject)object;
            objectX = worldInventoryObject.getWorldPosX();
            objectY = worldInventoryObject.getWorldPosY();
        }
        return objectX + objectY < IsoCamera.frameState.camCharacterX + IsoCamera.frameState.camCharacterY;
    }

    private void renderObject(int playerIndex, IsoObject object) {
        Texture texture;
        IsoMannequin mannequin;
        IsoDeadBody corpse;
        if (object.getSpriteModel() != null) {
            return;
        }
        if (object.getSprite() == null) {
            return;
        }
        if (object instanceof IsoDeadBody ? (corpse = (IsoDeadBody)object).getAtlasTexture() == null || corpse.getAtlasTexture().getTexture() == null : (object instanceof IsoMannequin ? (mannequin = (IsoMannequin)object).getAtlasTexture() == null || mannequin.getAtlasTexture().getTexture() == null : (texture = object.getSprite().getTextureForCurrentFrame(object.getDir(), object)) == null && !this.hasWorldItemAtlasTexture(object))) {
            return;
        }
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        if (renderInfo.layer == ObjectRenderLayer.None || renderInfo.targetAlpha <= 0.0f) {
            return;
        }
        Drawer2 drawer = this.drawer2Pool.alloc();
        drawer.init(object);
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    private boolean hasWorldItemAtlasTexture(IsoObject object) {
        boolean bUseAtlas;
        IsoWorldInventoryObject worldInventoryObject = Type.tryCastTo(object, IsoWorldInventoryObject.class);
        if (worldInventoryObject == null || worldInventoryObject.getItem() == null) {
            return false;
        }
        boolean bl = bUseAtlas = Core.getInstance().isOption3DGroundItem() && DebugOptions.instance.worldItemAtlas.enable.getValue();
        if (!bUseAtlas) {
            return false;
        }
        InventoryItem item = worldInventoryObject.getItem();
        int playerIndex = IsoCamera.frameState.playerIndex;
        float zoom = Core.getInstance().getZoom(playerIndex);
        boolean bMaxZoomIsOne = !Core.getInstance().getOptionHighResPlacedItems() || zoom >= 0.75f;
        WorldItemAtlas.ItemTexture atlasTexture = item.atlasTexture;
        if (atlasTexture != null && !atlasTexture.isStillValid(item, bMaxZoomIsOne)) {
            atlasTexture = WorldItemAtlas.instance.getItemTexture(item, bMaxZoomIsOne);
        }
        return atlasTexture != null && atlasTexture.isStillValid(item, bMaxZoomIsOne);
    }

    public void setDuringUIRenderTime(int playerIndex, IsoObject object, long time) {
        PerPlayerData ppd = this.perPlayerData[playerIndex];
        if (time == 0L) {
            ppd.duringUiRenderTime.remove(object);
        } else {
            ppd.duringUiRenderTime.put(object, time);
        }
    }

    public long getDuringUIRenderTime(int playerIndex, IsoObject object) {
        PerPlayerData ppd = this.perPlayerData[playerIndex];
        return ppd.duringUiRenderTime.get(object);
    }

    public void setDuringUIUpdateTime(int playerIndex, IsoObject object, long time) {
        PerPlayerData ppd = this.perPlayerData[playerIndex];
        if (time == 0L) {
            ppd.duringUiUpdateTime.remove(object);
        } else {
            ppd.duringUiUpdateTime.put(object, time);
        }
    }

    public long getDuringUIUpdateTime(int playerIndex, IsoObject object) {
        PerPlayerData ppd = this.perPlayerData[playerIndex];
        return ppd.duringUiUpdateTime.get(object);
    }

    private static final class PerPlayerData {
        final TObjectLongHashMap<IsoObject> duringUiRenderTime = new TObjectLongHashMap();
        final TObjectLongHashMap<IsoObject> duringUiUpdateTime = new TObjectLongHashMap();

        PerPlayerData() {
            this.duringUiRenderTime.setAutoCompactionFactor(0.0f);
            this.duringUiUpdateTime.setAutoCompactionFactor(0.0f);
        }
    }

    private static final class Drawer2
    extends TextureDraw.GenericDrawer {
        final ArrayList<Texture> textures = new ArrayList();
        final ColorInfo outlineColor = new ColorInfo();
        boolean outlineBehindPlayer = true;
        float renderX;
        float renderY;
        float renderW;
        float renderH;
        static Shader outlineShader;

        private Drawer2() {
        }

        Drawer2 init(IsoObject object) {
            this.textures.clear();
            int playerIndex = IsoCamera.frameState.playerIndex;
            this.outlineColor.setABGR(object.getOutlineHighlightCol(playerIndex));
            if (object.isOutlineHlBlink(playerIndex)) {
                this.outlineColor.a *= Core.blinkAlpha;
            }
            this.outlineBehindPlayer = FBORenderObjectOutline.getInstance().isBehindPlayer(object);
            float jx = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareX;
            float jy = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareY;
            float sx = IsoUtils.XToScreen(object.getX() + jx, object.getY() + jy, object.getZ(), 0);
            float sy = IsoUtils.YToScreen(object.getX() + jx, object.getY() + jy, object.getZ(), 0);
            sx -= object.offsetX;
            sy -= object.offsetY + object.getRenderYOffset() * (float)Core.tileScale;
            if (IsoSprite.globalOffsetX == -1.0f) {
                IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
                IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
            }
            this.renderX = sx += IsoSprite.globalOffsetX;
            this.renderY = sy += IsoSprite.globalOffsetY;
            this.renderH = -1.0f;
            this.renderW = -1.0f;
            if (object instanceof IsoDeadBody) {
                IsoDeadBody corpse = (IsoDeadBody)object;
                DeadBodyAtlas.BodyTexture atlasTexture = corpse.getAtlasTexture();
                this.renderX = atlasTexture.getRenderX(sx);
                this.renderY = atlasTexture.getRenderY(sy);
                this.renderW = atlasTexture.getRenderWidth();
                this.renderH = atlasTexture.getRenderHeight();
                this.textures.add(atlasTexture.getTexture());
                return this;
            }
            if (object instanceof IsoMannequin) {
                IsoObject object1;
                IsoMannequin mannequin = (IsoMannequin)object;
                float surface = 0.0f;
                IsoObject[] objects = object.getSquare().getObjects().getElements();
                for (int i = 0; i < object.getSquare().getObjects().size() && (object1 = objects[i]) != object; ++i) {
                    if (!object1.isTableSurface()) continue;
                    surface += object1.getSurfaceOffset() * (float)Core.tileScale;
                }
                if (object.getRenderYOffset() + surface > 0.0f) {
                    sy -= 2.0f;
                }
                DeadBodyAtlas.BodyTexture atlasTexture = mannequin.getAtlasTexture();
                this.renderX = atlasTexture.getRenderX(sx);
                this.renderY = atlasTexture.getRenderY(sy);
                this.renderW = atlasTexture.getRenderWidth();
                this.renderH = atlasTexture.getRenderHeight();
                this.textures.add(atlasTexture.getTexture());
                return this;
            }
            if (this.initWorldInventoryObject(object)) {
                return this;
            }
            this.initTexture(object, object.getSprite());
            if (object.isOutlineHlAttached(playerIndex) && (object.hasOverlaySprite() || object.hasAttachedAnimSprites())) {
                this.initTexture(object, object.getOverlaySprite());
                if (object.hasAttachedAnimSprites() && !object.hasAnimatedAttachments()) {
                    for (int i = 0; i < object.getAttachedAnimSprite().size(); ++i) {
                        this.initTexture(object, object.getAttachedAnimSprite().get(i).getParentSprite());
                    }
                }
            }
            return this;
        }

        boolean initWorldInventoryObject(IsoObject object) {
            boolean bUseAtlas;
            if (!(object instanceof IsoWorldInventoryObject)) {
                return false;
            }
            IsoWorldInventoryObject worldInventoryObject = (IsoWorldInventoryObject)object;
            int playerIndex = IsoCamera.frameState.playerIndex;
            float jx = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareX;
            float jy = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareY;
            float sx = IsoUtils.XToScreen(object.getX() + worldInventoryObject.xoff + jx, object.getY() + worldInventoryObject.yoff + jy, object.getZ() + worldInventoryObject.zoff, 0);
            float sy = IsoUtils.YToScreen(object.getX() + worldInventoryObject.xoff + jx, object.getY() + worldInventoryObject.yoff + jy, object.getZ() + worldInventoryObject.zoff, 0);
            sx += IsoSprite.globalOffsetX;
            sy += IsoSprite.globalOffsetY;
            boolean bl = bUseAtlas = Core.getInstance().isOption3DGroundItem() && DebugOptions.instance.worldItemAtlas.enable.getValue();
            if (bUseAtlas) {
                InventoryItem item = worldInventoryObject.getItem();
                float zoom = Core.getInstance().getZoom(playerIndex);
                boolean bMaxZoomIsOne = !Core.getInstance().getOptionHighResPlacedItems() || zoom >= 0.75f;
                WorldItemAtlas.ItemTexture atlasTexture = item.atlasTexture;
                if (atlasTexture != null && !atlasTexture.isStillValid(item, bMaxZoomIsOne)) {
                    atlasTexture = WorldItemAtlas.instance.getItemTexture(item, bMaxZoomIsOne);
                }
                if (atlasTexture != null && atlasTexture.isStillValid(item, bMaxZoomIsOne) && atlasTexture.getTexture() != null) {
                    this.renderX = atlasTexture.getRenderX(sx);
                    this.renderY = atlasTexture.getRenderY(sy);
                    this.renderW = atlasTexture.getRenderWidth();
                    this.renderH = atlasTexture.getRenderHeight();
                    Texture texture = atlasTexture.getTexture();
                    this.textures.add(texture);
                    return true;
                }
            }
            if (object.sprite != null && object.sprite.getTextureForCurrentFrame(object.getDir(), object) != null) {
                Texture tex = object.sprite.getTextureForCurrentFrame(object.getDir());
                float dx = (float)tex.getWidthOrig() * object.sprite.def.getScaleX() / 2.0f;
                float dy = (float)tex.getHeightOrig() * object.sprite.def.getScaleY() * 3.0f / 4.0f;
                this.renderX = sx - dx;
                this.renderY = sy - dy;
            }
            return false;
        }

        void initTexture(IsoObject object, IsoSprite sprite) {
            if (sprite != null && sprite.getTextureForCurrentFrame(object.getDir(), object) != null) {
                Texture texture = sprite.getTextureForCurrentFrame(object.getDir(), object);
                this.textures.add(texture);
            }
        }

        @Override
        public void render() {
            boolean clear = ModelOutlines.instance.beginRenderOutline(this.outlineColor, this.outlineBehindPlayer, false);
            GL11.glDepthMask(true);
            ModelOutlines.instance.fboA.startDrawing(clear, true);
            if (outlineShader == null) {
                outlineShader = new Shader("vboRenderer_SpriteOutline", true, false);
            }
            VBORenderer vbor = VBORenderer.getInstance();
            for (int i = 0; i < this.textures.size(); ++i) {
                Texture texture = this.textures.get(i);
                float x1 = this.renderX + texture.getOffsetX();
                float y1 = this.renderY + texture.getOffsetY();
                vbor.startRun(VBORenderer.getInstance().formatPositionColorUv);
                vbor.setMode(7);
                vbor.setTextureID(texture.getTextureId());
                vbor.setShaderProgram(outlineShader.getShaderProgram());
                float texW = texture.getWidth();
                float texH = texture.getHeight();
                if (this.renderW > 0.0f) {
                    texW = this.renderW;
                    texH = this.renderH;
                }
                vbor.addQuad(x1, y1, texture.getXStart(), texture.getYStart(), x1 + texW, y1 + texH, texture.getXEnd(), texture.getYEnd(), 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                vbor.endRun();
            }
            vbor.flush();
            ModelOutlines.instance.fboA.endDrawing();
        }

        @Override
        public void postRender() {
            this.textures.clear();
            FBORenderObjectOutline.instance.drawer2Pool.release(this);
        }
    }

    private static final class Drawer
    extends TextureDraw.GenericDrawer {
        final ArrayList<Texture> textures = new ArrayList();
        float offsetY;

        private Drawer() {
        }

        Drawer init(IsoObject object) {
            this.textures.clear();
            this.offsetY = 0.0f;
            this.initTexture(object, object.getSprite());
            this.initTexture(object, object.getOverlaySprite());
            if (object.getAttachedAnimSprite() != null && !object.hasAnimatedAttachments()) {
                for (int i = 0; i < object.getAttachedAnimSprite().size(); ++i) {
                    this.initTexture(object, object.getAttachedAnimSprite().get(i).getParentSprite());
                }
            }
            return this;
        }

        void initTexture(IsoObject object, IsoSprite sprite) {
            if (sprite != null && sprite.getTextureForCurrentFrame(object.getDir(), object) != null) {
                Texture texture = sprite.getTextureForCurrentFrame(object.getDir(), object);
                this.textures.add(texture);
            }
        }

        @Override
        public void render() {
            if (FBORenderObjectOutline.instance.fbo == null) {
                FBORenderObjectOutline.instance.fboTexture = new Texture(64 * Core.tileScale, 128 * Core.tileScale, 16);
                FBORenderObjectOutline.instance.fbo = new TextureFBO(FBORenderObjectOutline.instance.fboTexture);
            }
            TextureFBO fbo = FBORenderObjectOutline.instance.fbo;
            GL11.glPushAttrib(2048);
            GL11.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
            Matrix4f projection = BaseVehicle.allocMatrix4f();
            projection.setOrtho2D(0.0f, fbo.getWidth(), 0.0f, fbo.getHeight());
            Matrix4f modelView = BaseVehicle.allocMatrix4f();
            modelView.identity();
            PZGLUtil.pushAndLoadMatrix(5889, projection);
            PZGLUtil.pushAndLoadMatrix(5888, modelView);
            BaseVehicle.releaseMatrix4f(projection);
            BaseVehicle.releaseMatrix4f(modelView);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(3089);
            fbo.startDrawing(true, true);
            VBORenderer vbor = VBORenderer.getInstance();
            for (int i = 0; i < this.textures.size(); ++i) {
                Texture texture = this.textures.get(i);
                float x1 = texture.getOffsetX();
                float y1 = texture.getOffsetY() + this.offsetY;
                vbor.startRun(VBORenderer.getInstance().formatPositionColorUv);
                vbor.setMode(7);
                vbor.setTextureID(texture.getTextureId());
                vbor.addQuad(x1, y1, texture.getXStart(), texture.getYStart(), x1 + (float)texture.getWidth(), y1 + (float)texture.getHeight(), texture.getXEnd(), texture.getYEnd(), 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                vbor.endRun();
            }
            vbor.flush();
            fbo.endDrawing();
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            GL11.glPopAttrib();
            GLStateRenderThread.restore();
            GL11.glEnable(3089);
        }

        @Override
        public void postRender() {
            this.textures.clear();
            FBORenderObjectOutline.instance.drawerPool.release(this);
        }
    }
}

