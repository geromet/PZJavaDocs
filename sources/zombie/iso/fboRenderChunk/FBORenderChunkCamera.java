/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderLevels;

public class FBORenderChunkCamera
implements IModelCamera {
    FBORenderChunk renderChunk;
    float originX;
    float originY;
    float originZ;
    float x;
    float y;
    float z;
    float useAngle;
    int width;
    int height;
    int bottom;

    public void set(FBORenderChunk renderChunk, float x, float y, float z, float angle) {
        this.renderChunk = renderChunk;
        this.originX = (float)(renderChunk.chunk.wx * 8) + 4.0f;
        this.originY = (float)(renderChunk.chunk.wy * 8) + 4.0f;
        this.originZ = renderChunk.getMinLevel();
        int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
        int numLevels = renderChunk.getTopLevel() - renderChunk.getMinLevel() + 1;
        this.bottom = chunkFloorYSpan + numLevels * FBORenderChunk.PIXELS_PER_LEVEL;
        this.bottom += FBORenderLevels.extraHeightForJumboTrees(renderChunk.getMinLevel(), renderChunk.getTopLevel());
        this.x = x;
        this.y = y;
        this.z = z;
        this.useAngle = angle;
        this.width = renderChunk.w;
        this.height = renderChunk.h;
    }

    public void pushProjectionMatrix() {
        double screenWidth = (float)this.width / 1920.0f;
        Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
        int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
        float bottom = (float)this.bottom - (float)chunkFloorYSpan / 2.0f;
        if (this.renderChunk.highRes) {
            bottom *= 2.0f;
        }
        float top = (float)this.height - bottom;
        projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, bottom / 1920.0f, -top / 1920.0f, -10.0f, 10.0f);
        Core.getInstance().projectionMatrixStack.push(projection);
    }

    public void pushModelViewMatrix(float ox, float oy, float oz, float useangle, boolean vehicle) {
        float cx = this.originX;
        float cy = this.originY;
        float cz = this.originZ;
        double x = cx;
        double y = cy;
        double z = cz;
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.scaling(Core.scale);
        modelView.scale((float)Core.tileScale / 2.0f);
        if (this.renderChunk.highRes) {
            modelView.scale(2.0f);
        }
        modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
        double difX = (double)ox - x;
        double difY = (double)oy - y;
        modelView.translate(-((float)difX), (float)((double)oz - z) * 2.44949f, -((float)difY));
        if (vehicle) {
            modelView.scale(-1.0f, 1.0f, 1.0f);
        } else {
            modelView.scale(-1.5f, 1.5f, 1.5f);
        }
        modelView.rotate(useangle + (float)Math.PI, 0.0f, 1.0f, 0.0f);
        if (!vehicle) {
            // empty if block
        }
        Core.getInstance().modelViewMatrixStack.push(modelView);
    }

    public void DoPushIsoStuff(float ox, float oy, float oz, float useangle, boolean vehicle) {
        this.pushProjectionMatrix();
        this.pushModelViewMatrix(ox, oy, oz, useangle, vehicle);
        GL11.glDepthRange(0.0, 1.0);
    }

    public void DoPopIsoStuff() {
        GL11.glDepthRange(0.0, 1.0);
        GL11.glEnable(3008);
        GL11.glDepthFunc(519);
        GL11.glDepthMask(false);
        GLStateRenderThread.AlphaTest.restore();
        GLStateRenderThread.DepthFunc.restore();
        GLStateRenderThread.DepthMask.restore();
        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
    }

    @Override
    public void Begin() {
        this.DoPushIsoStuff(this.x, this.y, this.z, this.useAngle, false);
    }

    @Override
    public void End() {
        this.DoPopIsoStuff();
    }
}

