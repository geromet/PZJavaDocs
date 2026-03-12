/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.generators;

import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.scripting.itemConfig.RandomGenerator;
import zombie.util.StringUtils;

public class GeneratorFluidContainer
extends RandomGenerator<GeneratorFluidContainer> {
    private final String containerId;
    private final Fluid[] fluids;
    private final float[] ratios;
    private final float min;
    private final float max;

    public GeneratorFluidContainer(String containerId, Fluid[] fluids, float[] ratios, float max) {
        this(containerId, fluids, ratios, 1.0f, 0.0f, max);
    }

    public GeneratorFluidContainer(String containerId, Fluid[] fluids, float[] ratios, float min, float max) {
        this(containerId, fluids, ratios, 1.0f, min, max);
    }

    public GeneratorFluidContainer(String containerId, Fluid[] fluids, float[] ratios, float chance, float min, float max) {
        if (min > max) {
            float omin = min;
            max = min;
            min = omin;
        }
        if (chance <= 0.0f) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        }
        if (StringUtils.isNullOrWhitespace(containerId)) {
            throw new IllegalArgumentException("ContainerID cannot be null or whitespace.");
        }
        if (fluids != null && ratios == null) {
            throw new IllegalArgumentException("Ratios can not be null if fluids are added.");
        }
        if (fluids != null && fluids.length != ratios.length) {
            throw new IllegalArgumentException("Fluids and ratios size must be equal.");
        }
        if (fluids != null) {
            for (int i = 0; i < fluids.length; ++i) {
                if (fluids[i] == null) {
                    throw new IllegalArgumentException("Fluid can not be null.");
                }
                if (!(ratios[i] <= 0.0f)) continue;
                throw new IllegalArgumentException("Ratio can not be <= 0.");
            }
            this.fluids = fluids;
            this.ratios = PZMath.normalize(ratios);
        } else {
            this.fluids = null;
            this.ratios = null;
        }
        this.containerId = containerId;
        this.setChance(chance);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean execute(GameEntity entity) {
        FluidContainer container = null;
        if (entity.getFluidContainer() != null && entity.getFluidContainer().getContainerName().equalsIgnoreCase(this.containerId)) {
            container = entity.getFluidContainer();
        }
        if (container != null) {
            float ratio = this.min == this.max ? this.min : Rand.Next(this.min, this.max);
            float amount = container.getCapacity() * PZMath.clamp_01(ratio);
            if (this.fluids != null) {
                container.Empty();
                for (int i = 0; i < this.fluids.length; ++i) {
                    container.addFluid(this.fluids[i], amount * this.ratios[i]);
                }
            } else {
                if (container.isEmpty()) {
                    return true;
                }
                container.adjustAmount(amount);
            }
            return true;
        }
        return false;
    }

    @Override
    public GeneratorFluidContainer copy() {
        return new GeneratorFluidContainer(this.containerId, this.fluids, this.ratios, this.getChance(), this.min, this.max);
    }
}

