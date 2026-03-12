/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import zombie.characters.animals.pathfind.Mesh;

public interface IPathRenderer {
    public void drawLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8);

    public void drawRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8);

    public void drawTriangleCentroid(Mesh var1, int var2, float var3, float var4, float var5, float var6);
}

