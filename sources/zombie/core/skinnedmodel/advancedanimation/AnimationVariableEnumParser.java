/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.Map;
import java.util.TreeMap;
import zombie.AttackType;
import zombie.util.StringUtils;

public class AnimationVariableEnumParser {
    private static final AnimationVariableEnumParser s_instance = new AnimationVariableEnumParser();
    private final Map<String, Slot<? extends Enum<?>>> registeredEnumClasses = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    private Slot<? extends Enum<?>> findEnumClass(String className) {
        if (StringUtils.isNullOrWhitespace(className)) {
            return null;
        }
        return this.registeredEnumClasses.get(className.trim().toLowerCase());
    }

    private <E extends Enum<E>> void registerClassInternal(Class<E> clazz, E defaultValue) {
        String className = clazz.getSimpleName();
        String classNameKey = className.toLowerCase();
        this.registeredEnumClasses.put(classNameKey, new Slot<E>(clazz, defaultValue));
    }

    public static <E extends Enum<E>> void registerEnumClass(Class<E> clazz, E defaultValue) {
        AnimationVariableEnumParser.getInstance().registerClassInternal(clazz, defaultValue);
    }

    public static <E extends Enum<?>> E tryParse(String enumClassName, String enumStr) {
        Slot<Enum<?>> slot = AnimationVariableEnumParser.getInstance().findEnumClass(enumClassName);
        if (slot == null) {
            return null;
        }
        return (E)slot.tryParse(enumStr);
    }

    public static AnimationVariableEnumParser getInstance() {
        return s_instance;
    }

    static {
        AnimationVariableEnumParser.registerEnumClass(AttackType.class, AttackType.NONE);
    }

    private static class Slot<EnumType extends Enum<EnumType>> {
        public final Class<EnumType> enumClass;
        public final EnumType defaultValue;

        public Slot(Class<EnumType> enumClass, EnumType defaultValue) {
            this.enumClass = enumClass;
            this.defaultValue = defaultValue;
        }

        EnumType tryParse(String enumStr) {
            return StringUtils.tryParseEnum(this.enumClass, enumStr, this.defaultValue);
        }
    }
}

