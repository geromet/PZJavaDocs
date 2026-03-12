/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.visual;

import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;

public interface IAnimalVisual
extends IHumanVisual {
    public AnimalVisual getAnimalVisual();

    public String getAnimalType();

    public float getAnimalSize();
}

