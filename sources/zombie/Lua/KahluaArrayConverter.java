/*
 * Decompiled with CFR 0.152.
 */
package zombie.Lua;

import java.lang.reflect.Array;
import java.util.Objects;
import se.krka.kahlua.converter.JavaToLuaConverter;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.converter.LuaToJavaConverter;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;

public class KahluaArrayConverter {
    private final J2SEPlatform platform;
    private final KahluaConverterManager manager;

    public KahluaArrayConverter(J2SEPlatform platform, KahluaConverterManager manager) {
        this.platform = platform;
        this.manager = manager;
    }

    public void install() {
        this.manager.addJavaConverter(new JavaToLuaConverter<Object>(this){
            final /* synthetic */ KahluaArrayConverter this$0;
            {
                KahluaArrayConverter kahluaArrayConverter = this$0;
                Objects.requireNonNull(kahluaArrayConverter);
                this.this$0 = kahluaArrayConverter;
            }

            @Override
            public Object fromJavaToLua(Object javaObject) {
                if (javaObject.getClass().isArray()) {
                    KahluaTable t = this.this$0.platform.newTable();
                    int n = Array.getLength(javaObject);
                    for (int i = 0; i < n; ++i) {
                        Object value = Array.get(javaObject, i);
                        t.rawset(i + 1, this.this$0.manager.fromJavaToLua(value));
                    }
                    return t;
                }
                return null;
            }

            @Override
            public Class<Object> getJavaType() {
                return Object.class;
            }
        });
        this.manager.addLuaConverter(new LuaToJavaConverter<KahluaTable, Object>(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public Object fromLuaToJava(KahluaTable luaObject, Class<Object> javaClass) throws IllegalArgumentException {
                if (!javaClass.isArray()) {
                    return null;
                }
                Class<?> arrayElementType = javaClass.getComponentType();
                int numElements = luaObject.len();
                boolean canCast = true;
                for (int i = 0; i < numElements; ++i) {
                    Class<?> elementType;
                    Object element = luaObject.rawget(i + 1);
                    if (element == null || arrayElementType.isAssignableFrom(elementType = element.getClass())) continue;
                    canCast = false;
                    break;
                }
                if (!canCast) {
                    return null;
                }
                Object elementArray = Array.newInstance(arrayElementType, numElements);
                for (int i = 0; i < numElements; ++i) {
                    Object element = luaObject.rawget(i + 1);
                    Array.set(elementArray, i, element);
                }
                return elementArray;
            }

            @Override
            public Class<Object> getJavaType() {
                return Object.class;
            }

            @Override
            public Class<KahluaTable> getLuaType() {
                return KahluaTable.class;
            }
        });
    }
}

