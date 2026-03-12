/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

public class Type {
    public static <R, I> R tryCastTo(I val, Class<R> clazz) {
        if (clazz.isInstance(val)) {
            return clazz.cast(val);
        }
        return null;
    }

    public static boolean asBoolean(Object val) {
        return Type.asBoolean(val, false);
    }

    public static boolean asBoolean(Object val, boolean defaultVal) {
        if (val == null) {
            return defaultVal;
        }
        if (!(val instanceof Boolean)) {
            return defaultVal;
        }
        Boolean valSt = (Boolean)val;
        return valSt;
    }
}

