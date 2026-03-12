/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;

public class AttributeFactory {
    private static final boolean POOL_ENABLED = true;
    private static final ConcurrentLinkedDeque<AttributeInstance.Bool> pool_bool = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.String> pool_string = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.Float> pool_float = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.Double> pool_double = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.Byte> pool_byte = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.Short> pool_short = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.Int> pool_int = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.Long> pool_long = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.Enum<?>> pool_enum = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.EnumSet<?>> pool_enumSet = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<AttributeInstance.EnumStringSet<?>> pool_enumStringSet = new ConcurrentLinkedDeque();

    public static void Reset() {
    }

    public static <T extends AttributeInstance> T CreateTyped(AttributeType type) {
        AttributeInstance attribute = AttributeFactory.AllocAttribute(type);
        attribute.setType(type);
        return (T)attribute;
    }

    public static AttributeInstance Create(AttributeType type) {
        AttributeInstance attribute = AttributeFactory.AllocAttribute(type);
        attribute.setType(type);
        return attribute;
    }

    private static AttributeInstance AllocAttribute(AttributeType type) {
        switch (type.getValueType()) {
            case String: {
                return AttributeFactory.AllocAttributeString();
            }
            case Boolean: {
                return AttributeFactory.AllocAttributeBool();
            }
            case Float: {
                return AttributeFactory.AllocAttributeFloat();
            }
            case Double: {
                return AttributeFactory.AllocAttributeDouble();
            }
            case Byte: {
                return AttributeFactory.AllocAttributeByte();
            }
            case Short: {
                return AttributeFactory.AllocAttributeShort();
            }
            case Int: {
                return AttributeFactory.AllocAttributeInt();
            }
            case Long: {
                return AttributeFactory.AllocAttributeLong();
            }
            case Enum: {
                return AttributeFactory.AllocAttributeEnum();
            }
            case EnumSet: {
                return AttributeFactory.AllocAttributeEnumSet();
            }
            case EnumStringSet: {
                return AttributeFactory.AllocAttributeEnumStringSet();
            }
        }
        throw new RuntimeException("Could not allocate Attribute. [" + String.valueOf(type) + ", valueType = " + String.valueOf((Object)type.getValueType()) + "]");
    }

    protected static AttributeInstance.Enum AllocAttributeEnum() {
        AttributeInstance.Enum<Object> attribute = pool_enum.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Enum();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Enum attribute) {
        attribute.reset();
        pool_enum.offer(attribute);
    }

    protected static AttributeInstance.EnumSet AllocAttributeEnumSet() {
        AttributeInstance.EnumSet<Object> attribute = pool_enumSet.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.EnumSet();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.EnumSet attribute) {
        attribute.reset();
        pool_enumSet.offer(attribute);
    }

    protected static AttributeInstance.EnumStringSet AllocAttributeEnumStringSet() {
        AttributeInstance.EnumStringSet<Object> attribute = pool_enumStringSet.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.EnumStringSet();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.EnumStringSet attribute) {
        attribute.reset();
        pool_enumStringSet.offer(attribute);
    }

    protected static AttributeInstance.String AllocAttributeString() {
        AttributeInstance.String attribute = pool_string.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.String();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.String attribute) {
        attribute.reset();
        pool_string.offer(attribute);
    }

    protected static AttributeInstance.Bool AllocAttributeBool() {
        AttributeInstance.Bool attribute = pool_bool.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Bool();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Bool attribute) {
        attribute.reset();
        pool_bool.offer(attribute);
    }

    protected static AttributeInstance.Float AllocAttributeFloat() {
        AttributeInstance.Float attribute = pool_float.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Float();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Float attribute) {
        attribute.reset();
        pool_float.offer(attribute);
    }

    protected static AttributeInstance.Double AllocAttributeDouble() {
        AttributeInstance.Double attribute = pool_double.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Double();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Double attribute) {
        attribute.reset();
        pool_double.offer(attribute);
    }

    protected static AttributeInstance.Byte AllocAttributeByte() {
        AttributeInstance.Byte attribute = pool_byte.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Byte();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Byte attribute) {
        attribute.reset();
        pool_byte.offer(attribute);
    }

    protected static AttributeInstance.Short AllocAttributeShort() {
        AttributeInstance.Short attribute = pool_short.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Short();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Short attribute) {
        attribute.reset();
        pool_short.offer(attribute);
    }

    protected static AttributeInstance.Int AllocAttributeInt() {
        AttributeInstance.Int attribute = pool_int.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Int();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Int attribute) {
        attribute.reset();
        pool_int.offer(attribute);
    }

    protected static AttributeInstance.Long AllocAttributeLong() {
        AttributeInstance.Long attribute = pool_long.poll();
        if (attribute == null) {
            attribute = new AttributeInstance.Long();
        }
        return attribute;
    }

    protected static void Release(AttributeInstance.Long attribute) {
        attribute.reset();
        pool_long.offer(attribute);
    }
}

