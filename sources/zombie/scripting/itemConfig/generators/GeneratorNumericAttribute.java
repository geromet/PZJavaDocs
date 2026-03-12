/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.generators;

import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorNumericAttribute
extends RandomGenerator<GeneratorNumericAttribute> {
    private final AttributeType.Numeric<?, ?> attributeType;
    private final float min;
    private final float max;

    public GeneratorNumericAttribute(AttributeType attributeType, float max) {
        this(attributeType, 1.0f, 0.0f, max);
    }

    public GeneratorNumericAttribute(AttributeType attributeType, float min, float max) {
        this(attributeType, 1.0f, min, max);
    }

    public GeneratorNumericAttribute(AttributeType attributeType, float chance, float min, float max) {
        if (min > max) {
            float omin = min;
            max = min;
            min = omin;
        }
        if (chance < 0.0f) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        }
        if (!(attributeType instanceof AttributeType.Numeric)) {
            throw new IllegalArgumentException("AttributeType valueType should be numeric.");
        }
        AttributeType.Numeric numeric = (AttributeType.Numeric)attributeType;
        this.attributeType = numeric;
        this.setChance(chance);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() != null && AttributeValueType.IsNumeric(this.attributeType.getValueType())) {
            if (entity.getAttributes().contains(this.attributeType)) {
                if (this.min == this.max) {
                    entity.getAttributes().setFloatValue(this.attributeType, this.min);
                } else {
                    entity.getAttributes().setFloatValue(this.attributeType, Rand.Next(this.min, this.max));
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public GeneratorNumericAttribute copy() {
        return new GeneratorNumericAttribute(this.attributeType, this.getChance(), this.min, this.max);
    }
}

