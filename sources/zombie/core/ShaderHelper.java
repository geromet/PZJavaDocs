/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import zombie.core.DefaultShader;
import zombie.core.SceneShaderStore;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.ShaderPrograms;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.debug.DebugOptions;

public class ShaderHelper {
    private static int currentlyBound = -1;

    public static void glUseProgramObjectARB(int shaderID) {
        if (shaderID == 0) {
            if (currentlyBound == SceneShaderStore.defaultShaderId) {
                return;
            }
            GL20.glUseProgram(SceneShaderStore.defaultShaderId);
            DefaultShader.isActive = true;
            currentlyBound = SceneShaderStore.defaultShaderId;
        } else {
            if (currentlyBound == shaderID) {
                if (DebugOptions.instance.checks.boundShader.getValue() && currentlyBound != GL11.glGetInteger(35725)) {
                    boolean bl = true;
                }
                return;
            }
            GL20.glUseProgram(shaderID);
            DefaultShader.isActive = SceneShaderStore.defaultShaderId == shaderID;
            currentlyBound = shaderID;
        }
    }

    public static void forgetCurrentlyBound() {
        currentlyBound = -1;
        DefaultShader.isActive = false;
    }

    public static void setModelViewProjection() {
        ShaderProgram shaderProgram;
        if (currentlyBound <= 0) {
            return;
        }
        if (DebugOptions.instance.checks.boundShader.getValue() && currentlyBound != GL11.glGetInteger(35725)) {
            currentlyBound = GL11.glGetInteger(35725);
        }
        if ((shaderProgram = ShaderPrograms.getInstance().getProgramByID(currentlyBound)) == null || !shaderProgram.isCompiled()) {
            return;
        }
        VertexBufferObject.setModelViewProjection(shaderProgram);
    }
}

