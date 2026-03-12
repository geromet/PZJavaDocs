/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.popman.ObjectPool;

public class ExtendedPlacementDrawer
extends TextureDraw.GenericDrawer {
    IsoWorldInventoryObject worldItem;
    float x;
    float y;
    float z;
    float xoff;
    float yoff;
    float zoff;
    float minModelZ;
    float surfaceOffset1;
    float surfaceOffset2;
    float depth;
    static final ObjectPool<ExtendedPlacementDrawer> s_pool = new ObjectPool<ExtendedPlacementDrawer>(ExtendedPlacementDrawer::new);

    public void init(IsoWorldInventoryObject worldItem, float minModelZ) {
        this.worldItem = worldItem;
        this.x = (float)worldItem.getSquare().getX() + 0.5f;
        this.y = (float)worldItem.getSquare().getY() + 0.5f;
        this.z = worldItem.getSquare().getZ();
        this.xoff = worldItem.xoff;
        this.yoff = worldItem.yoff;
        this.zoff = worldItem.zoff;
        this.minModelZ = minModelZ;
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), this.x, this.y, this.z);
        this.depth = results.depthStart;
        this.surfaceOffset1 = 0.0f;
        this.surfaceOffset2 = 0.0f;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (square != null) {
            for (int i = square.getObjects().size() - 1; i >= 0; --i) {
                IsoObject object = square.getObjects().get(i);
                float surface = object.getSurfaceOffsetNoTable();
                if (!(surface > 0.0f)) continue;
                if (this.surfaceOffset2 == 0.0f) {
                    this.surfaceOffset2 = surface;
                    continue;
                }
                this.surfaceOffset1 = surface;
            }
        }
    }

    @Override
    public void render() {
        Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, 0.0f, false);
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.setDepthTestForAllRuns(Boolean.TRUE);
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.identity();
        modelView.scale(0.6666667f);
        Core.getInstance().modelViewMatrixStack.peek().mul(modelView, modelView);
        Core.getInstance().modelViewMatrixStack.push(modelView);
        float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
        float targetDepth = this.depth - (depthBufferValue + 1.0f) / 2.0f;
        vbor.setUserDepthForAllRuns(Float.valueOf(targetDepth));
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDepthFunc(515);
        GL11.glDisable(2884);
        GL11.glDepthMask(true);
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.5f;
        float surfaceOffset = this.surfaceOffset2;
        float deltaZ = this.zoff + this.minModelZ - surfaceOffset / 96.0f;
        float radius = deltaZ / 2.0f;
        if (deltaZ < 0.0f) {
            radius = 0.5f;
        }
        float aboveSurface = 0.015f;
        modelView.translate(-this.xoff + 0.5f, surfaceOffset / 96.0f * 2.44949f + 0.015f, this.yoff - 0.5f);
        modelView.rotateX(1.5707964f);
        vbor.addDisk_Fill(radius - 0.02f, radius, 32, 1, Texture.getErrorTexture().getTextureId(), r, g, 1.0f, a);
        vbor.flush();
        vbor.setOffset(0.0f, 0.0f, 0.0f);
        vbor.setDepthTestForAllRuns(null);
        vbor.setUserDepthForAllRuns(null);
        Core.getInstance().modelViewMatrixStack.pop();
        Core.getInstance().DoPopIsoStuff();
        GLStateRenderThread.restore();
    }

    @Override
    public void postRender() {
        s_pool.release(this);
    }

    private void onRotateGizmo(float x, float y, float z) {
        this.worldItem.getItem().setWorldXRotation(x);
        this.worldItem.getItem().setWorldYRotation(z);
        this.worldItem.getItem().setWorldZRotation(y);
    }

    private void onTranslateGizmo(float x, float y, float z) {
        this.worldItem.setOffX(PZMath.clamp(x - (float)this.worldItem.getSquare().getX(), 0.0f, 1.0f));
        this.worldItem.setOffY(PZMath.clamp(z - (float)this.worldItem.getSquare().getY(), 0.0f, 1.0f));
        this.worldItem.setOffZ(PZMath.clamp(y - (float)this.worldItem.getSquare().getZ(), 0.0f, 1.0f));
    }
}

