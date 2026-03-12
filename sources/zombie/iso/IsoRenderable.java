/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.core.opengl.Shader;
import zombie.core.textures.ColorInfo;

public interface IsoRenderable {
    public void setDoRender(boolean var1);

    public boolean getDoRender();

    public void setSceneCulled(boolean var1);

    public boolean isSceneCulled();

    public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7);
}

