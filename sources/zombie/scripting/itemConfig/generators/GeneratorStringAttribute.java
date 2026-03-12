/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorStringAttribute
extends RandomGenerator<GeneratorStringAttribute> {
    private final AttributeType.String attributeType;
    private final String str;

    public GeneratorStringAttribute(AttributeType attributeType, String s) {
        this(attributeType, 1.0f, s);
    }

    public GeneratorStringAttribute(AttributeType attributeType, float chance, String s) {
        if (chance < 0.0f) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        }
        if (!(attributeType instanceof AttributeType.String)) {
            throw new IllegalArgumentException("AttributeType valueType should be string.");
        }
        AttributeType.String string = (AttributeType.String)attributeType;
        this.attributeType = string;
        this.setChance(chance);
        this.str = s;
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.String) {
            if (entity.getAttributes().contains(this.attributeType)) {
                entity.getAttributes().set(this.attributeType, this.str);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public GeneratorStringAttribute copy() {
        return new GeneratorStringAttribute(this.attributeType, this.getChance(), this.str);
    }
}

