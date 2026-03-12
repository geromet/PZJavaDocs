/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorBoolAttribute
extends RandomGenerator<GeneratorBoolAttribute> {
    private final AttributeType.Bool attributeType;
    private final boolean value;

    public GeneratorBoolAttribute(AttributeType attributeType, boolean b) {
        this(attributeType, 1.0f, b);
    }

    public GeneratorBoolAttribute(AttributeType attributeType, float chance, boolean b) {
        if (chance < 0.0f) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        }
        if (!(attributeType instanceof AttributeType.Bool)) {
            throw new IllegalArgumentException("AttributeType valueType should be boolean.");
        }
        AttributeType.Bool bool = (AttributeType.Bool)attributeType;
        this.attributeType = bool;
        this.setChance(chance);
        this.value = b;
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.Boolean) {
            if (entity.getAttributes().contains(this.attributeType)) {
                entity.getAttributes().set(this.attributeType, this.value);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public GeneratorBoolAttribute copy() {
        return new GeneratorBoolAttribute(this.attributeType, this.getChance(), this.value);
    }
}

