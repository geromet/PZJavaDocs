/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig;

import zombie.entity.GameEntity;

public abstract class RandomGenerator<T extends RandomGenerator<T>> {
    private float chance = 1.0f;

    protected void setChance(float f) {
        this.chance = f;
    }

    protected float getChance() {
        return this.chance;
    }

    public abstract boolean execute(GameEntity var1);

    public abstract T copy();
}

