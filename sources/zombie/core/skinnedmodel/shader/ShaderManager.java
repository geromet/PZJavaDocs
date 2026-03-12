/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.shader;

import java.util.ArrayList;
import zombie.core.skinnedmodel.shader.Shader;

public final class ShaderManager {
    public static final ShaderManager instance = new ShaderManager();
    private final ArrayList<Shader> shaders = new ArrayList();

    private Shader getShader(String name, boolean bStatic, boolean bInstanced) {
        for (int i = 0; i < this.shaders.size(); ++i) {
            Shader shader = this.shaders.get(i);
            if (!name.equals(shader.name) || bStatic != shader.isStatic || bInstanced != shader.instanced) continue;
            return shader;
        }
        return null;
    }

    public Shader getOrCreateShader(String name, boolean bStatic, boolean bInstanced) {
        Shader shader = this.getShader(name, bStatic, bInstanced);
        if (shader != null) {
            return shader;
        }
        for (int i = 0; i < this.shaders.size(); ++i) {
            Shader shader1 = this.shaders.get(i);
            if (!shader1.name.equalsIgnoreCase(name) || shader1.name.equals(name)) continue;
            throw new IllegalArgumentException("shader filenames are case-sensitive");
        }
        shader = new Shader(name, bStatic, bInstanced);
        this.shaders.add(shader);
        return shader;
    }

    public Shader getShaderByID(int shaderID) {
        for (int i = 0; i < this.shaders.size(); ++i) {
            Shader shader1 = this.shaders.get(i);
            if (shader1.getID() != shaderID) continue;
            return shader1;
        }
        return null;
    }
}

