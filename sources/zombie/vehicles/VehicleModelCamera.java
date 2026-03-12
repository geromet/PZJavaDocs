/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelCamera;

public final class VehicleModelCamera
extends ModelCamera {
    public static final VehicleModelCamera instance = new VehicleModelCamera();

    @Override
    public void Begin() {
        if (this.useWorldIso) {
            Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, this.useAngle, true);
            GL11.glDepthMask(this.depthMask);
            return;
        }
        Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
        projection.setOrtho(-192.0f, 192.0f, -192.0f, 192.0f, -1000.0f, 1000.0f);
        float f = Math.sqrt(2048.0f);
        projection.scale(-f, f, f);
        Core.getInstance().projectionMatrixStack.push(projection);
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.identity();
        modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        modelView.rotate(0.7853982f, 0.0f, 1.0f, 0.0f);
        Core.getInstance().modelViewMatrixStack.push(modelView);
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

