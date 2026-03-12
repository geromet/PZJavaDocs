/*
 * Decompiled with CFR 0.152.
 */
package zombie.interfaces;

import java.nio.ByteBuffer;
import zombie.core.textures.Mask;
import zombie.core.utils.WrappedBuffer;
import zombie.interfaces.IDestroyable;
import zombie.interfaces.IMaskerable;

public interface ITexture
extends IDestroyable,
IMaskerable {
    public void bind();

    public void bind(int var1);

    public WrappedBuffer getData();

    public int getHeight();

    public int getHeightHW();

    public int getID();

    public int getWidth();

    public int getWidthHW();

    public float getXEnd();

    public float getXStart();

    public float getYEnd();

    public float getYStart();

    public boolean isSolid();

    public void makeTransp(int var1, int var2, int var3);

    public void setAlphaForeach(int var1, int var2, int var3, int var4);

    public void setData(ByteBuffer var1);

    public void setMask(Mask var1);

    public void setRegion(int var1, int var2, int var3, int var4);
}

