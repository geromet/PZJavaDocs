/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import org.jspecify.annotations.Nullable;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.reflect.ClassReflection;
import zombie.entity.util.reflect.Constructor;
import zombie.entity.util.reflect.ReflectionException;
import zombie.scripting.entity.ComponentScript;

public class ComponentScriptFactory {
    private final ObjectMap<Class<?>, ScriptConstructor> scriptConstructors = new ObjectMap();

    public <T extends ComponentScript> T create(Class<T> componentScriptClass) {
        ScriptConstructor<T> pool = this.scriptConstructors.get(componentScriptClass);
        if (pool == null) {
            pool = new ScriptConstructor<T>(componentScriptClass);
            this.scriptConstructors.put(componentScriptClass, pool);
        }
        return pool.obtain();
    }

    private static class ScriptConstructor<T extends ComponentScript> {
        private final Constructor constructor;

        public ScriptConstructor(Class<T> type) {
            this.constructor = this.findConstructor(type);
            if (this.constructor == null) {
                throw new RuntimeException("Class cannot be created (missing no-arg constructor): " + type.getName());
            }
        }

        private @Nullable Constructor findConstructor(Class<T> type) {
            try {
                return ClassReflection.getConstructor(type, null);
            }
            catch (Exception ex1) {
                try {
                    Constructor constructor = ClassReflection.getDeclaredConstructor(type, null);
                    constructor.setAccessible(true);
                    return constructor;
                }
                catch (ReflectionException ex2) {
                    return null;
                }
            }
        }

        protected T obtain() {
            try {
                return (T)((ComponentScript)this.constructor.newInstance(null));
            }
            catch (Exception ex) {
                throw new RuntimeException("Unable to create new instance: " + this.constructor.getDeclaringClass().getName(), ex);
            }
        }
    }
}

