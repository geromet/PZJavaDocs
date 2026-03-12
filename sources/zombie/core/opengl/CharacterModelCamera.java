/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelCamera;

public final class CharacterModelCamera
extends ModelCamera {
    public static final CharacterModelCamera instance = new CharacterModelCamera();

    @Override
    public void Begin() {
        if (this.useWorldIso) {
            Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, this.useAngle, this.inVehicle);
            GL11.glDepthMask(this.depthMask);
            return;
        }
        int w = 1024;
        int h = 1024;
        float sizeV = 42.75f;
        float offsetX = 0.0f;
        float offsetY = -0.45f;
        float offsetZ = 0.0f;
        Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
        float xScale = 1.0f;
        boolean flipY = false;
        projection.setOrtho(-42.75f, 42.75f, -42.75f, 42.75f, -100.0f, 100.0f);
        float f = Math.sqrt(2048.0f);
        projection.scale(-f, f, f);
        Core.getInstance().projectionMatrixStack.push(projection);
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.translation(0.0f, -0.45f, 0.0f);
        modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        modelView.rotate(this.useAngle + 0.7853982f, 0.0f, 1.0f, 0.0f);
        Core.getInstance().modelViewMatrixStack.push(modelView);
        GL11.glDepthRange(0.0, 1.0);
        GL11.glDepthMask(this.depthMask);
    }

    @Override
    public void End() {
        if (this.useWorldIso) {
            Core.getInstance().DoPopIsoStuff();
            return;
        }
        GL11.glDepthFunc(519);
        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
    }
}

