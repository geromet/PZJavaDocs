/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumAttribute
extends RandomGenerator<GeneratorEnumAttribute> {
    private final AttributeType.Enum attributeType;
    private final String str;

    public GeneratorEnumAttribute(AttributeType attributeType, String s) {
        this(attributeType, 1.0f, s);
    }

    public GeneratorEnumAttribute(AttributeType attributeType, float chance, String s) {
        if (chance < 0.0f) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        }
        if (!(attributeType instanceof AttributeType.Enum)) {
            throw new IllegalArgumentException("AttributeType valueType should be Enum.");
        }
        AttributeType.Enum anEnum = (AttributeType.Enum)attributeType;
        this.attributeType = anEnum;
        this.setChance(chance);
        this.str = s;
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.Enum) {
            if (entity.getAttributes().contains(this.attributeType)) {
                entity.getAttributes().putFromScript(this.attributeType, this.str);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public GeneratorEnumAttribute copy() {
        return new GeneratorEnumAttribute(this.attributeType, this.getChance(), this.str);
    }
}

