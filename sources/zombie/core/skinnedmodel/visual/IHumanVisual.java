/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.visual;

import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;

public interface IHumanVisual {
    public HumanVisual getHumanVisual();

    public void getItemVisuals(ItemVisuals var1);

    public boolean isFemale();

    public boolean isZombie();

    public boolean isSkeleton();
}

