/*
 * Decompiled with CFR 0.152.
 */
package zombie.Lua;

import se.krka.kahlua.converter.JavaToLuaConverter;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.converter.LuaToJavaConverter;
import zombie.core.BoxedStaticValues;

public final class KahluaNumberConverter {
    private KahluaNumberConverter() {
    }

    public static void install(KahluaConverterManager manager) {
        manager.addLuaConverter(new LuaToJavaConverter<Double, Long>(){

            @Override
            public Long fromLuaToJava(Double luaObject, Class<Long> javaClass) {
                return luaObject.longValue();
            }

            @Override
            public Class<Long> getJavaType() {
                return Long.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Integer>(){

            @Override
            public Integer fromLuaToJava(Double luaObject, Class<Integer> javaClass) {
                return luaObject.intValue();
            }

            @Override
            public Class<Integer> getJavaType() {
                return Integer.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Float>(){

            @Override
            public Float fromLuaToJava(Double luaObject, Class<Float> javaClass) {
                return Float.valueOf(luaObject.floatValue());
            }

            @Override
            public Class<Float> getJavaType() {
                return Float.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Byte>(){

            @Override
            public Byte fromLuaToJava(Double luaObject, Class<Byte> javaClass) {
                return luaObject.byteValue();
            }

            @Override
            public Class<Byte> getJavaType() {
                return Byte.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Character>(){

            @Override
            public Character fromLuaToJava(Double luaObject, Class<Character> javaClass) {
                return Character.valueOf((char)luaObject.intValue());
            }

            @Override
            public Class<Character> getJavaType() {
                return Character.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Short>(){

            @Override
            public Short fromLuaToJava(Double luaObject, Class<Short> javaClass) {
                return luaObject.shortValue();
            }

            @Override
            public Class<Short> getJavaType() {
                return Short.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addJavaConverter(new NumberToLuaConverter<Double>(Double.class));
        manager.addJavaConverter(new NumberToLuaConverter<Float>(Float.class));
        manager.addJavaConverter(new NumberToLuaConverter<Integer>(Integer.class));
        manager.addJavaConverter(new NumberToLuaConverter<Long>(Long.class));
        manager.addJavaConverter(new NumberToLuaConverter<Short>(Short.class));
        manager.addJavaConverter(new NumberToLuaConverter<Byte>(Byte.class));
        manager.addJavaConverter(new CharacterToLuaConverter(Character.class));
        manager.addJavaConverter(new NumberToLuaConverter<Double>(Double.TYPE));
        manager.addJavaConverter(new NumberToLuaConverter<Float>(Float.TYPE));
        manager.addJavaConverter(new NumberToLuaConverter<Integer>(Integer.TYPE));
        manager.addJavaConverter(new NumberToLuaConverter<Long>(Long.TYPE));
        manager.addJavaConverter(new NumberToLuaConverter<Short>(Short.TYPE));
        manager.addJavaConverter(new NumberToLuaConverter<Byte>(Byte.TYPE));
        manager.addJavaConverter(new CharacterToLuaConverter(Character.TYPE));
        manager.addJavaConverter(new JavaToLuaConverter<Boolean>(){

            @Override
            public Object fromJavaToLua(Boolean javaObject) {
                return (boolean)javaObject;
            }

            @Override
            public Class<Boolean> getJavaType() {
                return Boolean.class;
            }
        });
    }

    private record NumberToLuaConverter<T extends Number>(Class<T> clazz) implements JavaToLuaConverter<T>
    {
        @Override
        public Object fromJavaToLua(T javaObject) {
            if (javaObject instanceof Double) {
                return javaObject;
            }
            return BoxedStaticValues.toDouble(((Number)javaObject).doubleValue());
        }

        @Override
        public Class<T> getJavaType() {
            return this.clazz;
        }
    }

    private record CharacterToLuaConverter(Class<Character> clazz) implements JavaToLuaConverter<Character>
    {
        @Override
        public Object fromJavaToLua(Character javaObject) {
            return BoxedStaticValues.toDouble(javaObject.charValue());
        }

        @Override
        public Class<Character> getJavaType() {
            return this.clazz;
        }
    }
}

