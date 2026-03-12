/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import org.lwjgl.opengl.GL20;
import zombie.IndieGL;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

public class SDFShader
extends Shader {
    private int sdfThreshold;
    private int sdfShadow;
    private int sdfOutlineThick;
    private int sdfOutlineColor;
    private final float threshold = 0.0f;

    public SDFShader(String name) {
        super(name);
    }

    @Override
    protected void onCompileSuccess(ShaderProgram shaderProgram) {
        int shaderID = shaderProgram.getShaderID();
        this.sdfThreshold = GL20.glGetUniformLocation(shaderID, "sdfThreshold");
        this.sdfShadow = GL20.glGetUniformLocation(shaderID, "sdfShadow");
        this.sdfOutlineThick = GL20.glGetUniformLocation(shaderID, "sdfOutlineThick");
        this.sdfOutlineColor = GL20.glGetUniformLocation(shaderID, "sdfOutlineColor");
    }

    @Override
    public void startRenderThread(TextureDraw texd) {
    }

    public void updateThreshold(float threshold) {
        IndieGL.ShaderUpdate1f(this.getID(), this.sdfThreshold, threshold);
    }

    public void updateOutline(float thick, float r, float g, float b, float a) {
        IndieGL.ShaderUpdate1f(this.getID(), this.sdfOutlineThick, (1.0f - thick) / 2.0f);
        IndieGL.ShaderUpdate4f(this.getID(), this.sdfOutlineColor, r, g, b, a);
    }

    public void updateShadow(float shadow) {
        IndieGL.ShaderUpdate1f(this.getID(), this.sdfShadow, shadow);
    }
}

