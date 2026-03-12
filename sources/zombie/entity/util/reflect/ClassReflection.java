/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.util.reflect;

import zombie.entity.util.reflect.Constructor;
import zombie.entity.util.reflect.ReflectionException;

public final class ClassReflection {
    public static Constructor getConstructor(Class<?> c, Class<?> ... parameterTypes) throws ReflectionException {
        try {
            return new Constructor(c.getConstructor(parameterTypes));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation occurred while getting constructor for class: '" + c.getName() + "'.", e);
        }
        catch (NoSuchMethodException e) {
            throw new ReflectionException("Constructor not found for class: " + c.getName(), e);
        }
    }

    public static Constructor getDeclaredConstructor(Class<?> c, Class<?> ... parameterTypes) throws ReflectionException {
        try {
            return new Constructor(c.getDeclaredConstructor(parameterTypes));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation while getting constructor for class: " + c.getName(), e);
        }
        catch (NoSuchMethodException e) {
            throw new ReflectionException("Constructor not found for class: " + c.getName(), e);
        }
    }
}

