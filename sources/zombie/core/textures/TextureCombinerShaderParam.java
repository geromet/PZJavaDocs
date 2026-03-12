/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

public final class TextureCombinerShaderParam {
    public String name;
    public float min;
    public float max;

    public TextureCombinerShaderParam(String name, float min, float max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    public TextureCombinerShaderParam(String name, float val) {
        this.name = name;
        this.min = val;
        this.max = val;
    }
}

