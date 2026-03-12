/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.opengl.IModelCamera;
import zombie.core.rendering.RenderTarget;

public abstract class ModelCamera
implements IModelCamera {
    public static ModelCamera instance;
    public float useAngle;
    public boolean useWorldIso;
    public float x;
    public float y;
    public float z;
    public boolean inVehicle;
    public boolean depthMask = true;
    private static final Vector3f ImposterScale;

    public void BeginImposter(RenderTarget imposter) {
        int w = imposter.GetWidth();
        int h = imposter.GetHeight();
        float sizeV = 42.75f;
        Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
        float xScale = (float)w / (float)h;
        float hw = 0.5f;
        float hh = 0.5f;
        projection.setOrtho(-0.5f, 0.5f, -0.5f, 0.5f, -10.0f, 10.0f);
        Core.getInstance().projectionMatrixStack.push(projection);
        float scale = Core.scale * (float)Core.tileScale / 2.0f * 1.5f;
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.scaling(Core.scale * (float)Core.tileScale / 2.0f);
        modelView.rotateX(0.5235988f);
        modelView.rotateY(2.3561945f);
        modelView.scale(-1.5f, 1.5f, 1.5f);
        modelView.rotateY(this.useAngle + (float)Math.PI);
        modelView.translate(0.0f, -0.58f, 0.0f);
        modelView.getScale(ImposterScale);
        modelView.scaleLocal(1.0f / ModelCamera.ImposterScale.x, 1.0f / ModelCamera.ImposterScale.y, 1.0f);
        Core.getInstance().modelViewMatrixStack.push(modelView);
        GL11.glDepthRange(-10.0, 10.0);
        GL11.glDepthMask(this.depthMask);
    }

    public void EndImposter() {
        GL11.glDepthFunc(519);
        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
    }

    static {
        ImposterScale = new Vector3f();
    }
}

