/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.util.reflect;

import java.lang.reflect.InvocationTargetException;
import zombie.entity.util.reflect.ReflectionException;

public final class Constructor {
    private final java.lang.reflect.Constructor<?> constructor;

    Constructor(java.lang.reflect.Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Class<?>[] getParameterTypes() {
        return this.constructor.getParameterTypes();
    }

    public Class<?> getDeclaringClass() {
        return this.constructor.getDeclaringClass();
    }

    public boolean isAccessible() {
        return this.constructor.isAccessible();
    }

    public void setAccessible(boolean accessible) {
        this.constructor.setAccessible(accessible);
    }

    public Object newInstance(Object ... args2) throws ReflectionException {
        try {
            return this.constructor.newInstance(args2);
        }
        catch (IllegalArgumentException e) {
            throw new ReflectionException("Illegal argument(s) supplied to constructor for class: " + this.getDeclaringClass().getName(), e);
        }
        catch (IllegalAccessException | InstantiationException e) {
            throw new ReflectionException("Could not instantiate instance of class: " + this.getDeclaringClass().getName(), e);
        }
        catch (InvocationTargetException e) {
            throw new ReflectionException("Exception occurred in constructor for class: " + this.getDeclaringClass().getName(), e);
        }
    }
}

