/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.fonts;

import zombie.core.Color;

public interface Font {
    public void drawString(float var1, float var2, String var3);

    public void drawString(float var1, float var2, String var3, Color var4);

    public void drawString(float var1, float var2, String var3, Color var4, int var5, int var6);

    public int getHeight(String var1);

    public int getWidth(String var1);

    public int getWidth(String var1, boolean var2);

    public int getWidth(String var1, int var2, int var3);

    public int getWidth(String var1, int var2, int var3, boolean var4);

    public int getLineHeight();
}

