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

public class GeneratorEnumStringSetAttribute
extends RandomGenerator<GeneratorEnumStringSetAttribute> {
    private final AttributeType.EnumStringSet attributeType;
    private final String[] enumsValues;
    private final String[] stringValues;
    private final Mode mode;

    public GeneratorEnumStringSetAttribute(AttributeType attributeType, Mode mode, String[] enums, String[] strings) {
        this(attributeType, mode, 1.0f, enums, strings);
    }

    public GeneratorEnumStringSetAttribute(AttributeType attributeType, Mode mode, float chance, String[] enums, String[] strings) {
        if (chance < 0.0f) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        }
        if (!(attributeType instanceof AttributeType.EnumStringSet)) {
            throw new IllegalArgumentException("AttributeType valueType should be EnumStringSet.");
        }
        AttributeType.EnumStringSet enumStringSet = (AttributeType.EnumStringSet)attributeType;
        this.attributeType = enumStringSet;
        this.setChance(chance);
        this.enumsValues = enums;
        this.stringValues = strings;
        this.mode = mode;
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.EnumSet) {
            if (entity.getAttributes().contains(this.attributeType)) {
                try {
                    AttributeInstance.EnumStringSet enumStringSet = (AttributeInstance.EnumStringSet)entity.getAttributes().getAttribute(this.attributeType);
                    if (this.mode == Mode.Set) {
                        enumStringSet.clear();
                    }
                    if (this.mode == Mode.Remove) {
                        if (this.enumsValues != null) {
                            for (String s : this.enumsValues) {
                                if (enumStringSet.removeEnumValueFromString(s)) continue;
                                DebugLog.General.error("Unable to remove value '" + s + "'");
                            }
                        }
                        if (this.stringValues != null) {
                            for (String s : this.stringValues) {
                                if (enumStringSet.removeStringValue(s)) continue;
                                DebugLog.General.error("Unable to remove value '" + s + "'");
                            }
                        }
                    } else {
                        if (this.enumsValues != null) {
                            for (String s : this.enumsValues) {
                                enumStringSet.addEnumValueFromString(s);
                            }
                        }
                        if (this.stringValues != null) {
                            for (String s : this.stringValues) {
                                enumStringSet.addStringValue(s);
                            }
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
    public GeneratorEnumStringSetAttribute copy() {
        return new GeneratorEnumStringSetAttribute(this.attributeType, this.mode, this.getChance(), this.enumsValues, this.stringValues);
    }

    public static enum Mode {
        Set,
        Add,
        Remove;

    }
}

