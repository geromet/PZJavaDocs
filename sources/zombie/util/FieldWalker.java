/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.lang.reflect.Field;

public class FieldWalker {
    public static void walkFields(Object obj, String parentName, TriConsumer<String, Object, Field> consumer) {
        FieldWalker.walkFields(obj, parentName, consumer, true);
    }

    public static void walkFields(Object obj, String parentName, TriConsumer<String, Object, Field> consumer, boolean recursive) {
        Field[] fields;
        if (obj == null) {
            return;
        }
        for (Field field : fields = obj.getClass().getFields()) {
            String fullName = parentName.isEmpty() ? field.getName() : parentName + "." + field.getName();
            try {
                Class<?> type = field.getType();
                if (type == Float.TYPE || type == Double.TYPE || type == Integer.TYPE) {
                    consumer.accept(fullName, obj, field);
                    continue;
                }
                if (!recursive) continue;
                Object child = field.get(obj);
                FieldWalker.walkFields(child, fullName, consumer, true);
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @FunctionalInterface
    public static interface TriConsumer<T, U, V> {
        public void accept(T var1, U var2, V var3);
    }
}

