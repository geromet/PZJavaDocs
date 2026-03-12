/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig;

import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.itemConfig.RandomGenerator;

public class Randomizer {
    private final RandomGenerator[] generators;

    public Randomizer(RandomGenerator[] rngConfigs) throws ItemConfig.ItemConfigException {
        this.generators = rngConfigs;
        if (this.generators.length == 0) {
            throw new ItemConfig.ItemConfigException("Attempting to construct a Randomizer with no entries.");
        }
        PZMath.normalize(this.generators, RandomGenerator::getChance, RandomGenerator::setChance);
    }

    public Randomizer(Randomizer other) {
        this.generators = new RandomGenerator[other.generators.length];
        for (int i = 0; i < this.generators.length; ++i) {
            this.generators[i] = other.generators[i].copy();
        }
    }

    public boolean execute(GameEntity entity) {
        RandomGenerator generator;
        if (this.generators.length > 1) {
            float roll = Rand.Next(0.0f, 1.0f);
            float chance = 1.0f;
            for (int i = this.generators.length - 1; i >= 1; --i) {
                generator = this.generators[i];
                if (roll > chance - generator.getChance() && roll <= chance) {
                    return generator.execute(entity);
                }
                chance -= generator.getChance();
            }
        }
        generator = this.generators[0];
        return generator.execute(entity);
    }

    public Randomizer copy() {
        return new Randomizer(this);
    }
}

