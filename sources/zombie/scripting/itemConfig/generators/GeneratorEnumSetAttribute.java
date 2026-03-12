/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.generators;

import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumSetAttribute
extends RandomGenerator<GeneratorEnumSetAttribute> {
    private final AttributeType.EnumSet attributeType;
    private final String[] values;
    private final Mode mode;

    public GeneratorEnumSetAttribute(AttributeType attributeType, Mode mode, String[] s) {
        this(attributeType, mode, 1.0f, s);
    }

    public GeneratorEnumSetAttribute(AttributeType attributeType, Mode mode, float chance, String[] s) {
        if (chance < 0.0f) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        }
        if (!(attributeType instanceof AttributeType.EnumSet)) {
            throw new IllegalArgumentException("AttributeType valueType should be EnumSet.");
        }
        AttributeType.EnumSet enumSet = (AttributeType.EnumSet)attributeType;
        this.attributeType = enumSet;
        this.setChance(chance);
        this.values = s;
        this.mode = mode;
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.EnumSet) {
            if (entity.getAttributes().contains(this.attributeType)) {
                try {
                    AttributeInstance.EnumSet enumSet = (AttributeInstance.EnumSet)entity.getAttributes().getAttribute(this.attributeType);
                    if (this.mode == Mode.Set) {
                        enumSet.clear();
                    }
                    if (this.mode == Mode.Remove) {
                        for (String s : this.values) {
                            if (enumSet.removeValueFromString(s)) continue;
                            DebugLog.General.error("Unable to remove value '" + s + "'");
                        }
                    } else {
                        for (String s : this.values) {
                            enumSet.addValueFromString(s);
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public GeneratorEnumSetAttribute copy() {
        return new GeneratorEnumSetAttribute(this.attributeType, this.mode, this.getChance(), this.values);
    }

    public static enum Mode {
        Set,
        Add,
        Remove;

    }
}

