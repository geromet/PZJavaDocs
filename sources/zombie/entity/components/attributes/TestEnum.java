/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

import zombie.entity.util.enums.IOEnum;

public enum TestEnum implements IOEnum
{
    TestValueA(1),
    TestValueB(2),
    TestValueC(3),
    TestValueD(4),
    TestValueE(5),
    TestValueF(6);

    private final byte id;

    private TestEnum(byte id) {
        this.id = id;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    @Override
    public int getBits() {
        throw new UnsupportedOperationException("Not implemented");
    }
}

