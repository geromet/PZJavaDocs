/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.util.enums;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;
import zombie.core.utils.Bits;
import zombie.entity.util.enums.IOEnum;

public class EnumBitStore<E extends Enum<E>> {
    private static final String emptyToString = "[]";
    private int bits = 0;
    final transient Class<E> elementType;

    private EnumBitStore(Class<E> elementType) {
        this.elementType = elementType;
    }

    public static <E extends Enum<E>> EnumBitStore<E> noneOf(Class<E> elementType) {
        return new EnumBitStore<E>(elementType);
    }

    public static <E extends Enum<E>> EnumBitStore<E> allOf(Class<E> elementType) {
        EnumBitStore<E> result = EnumBitStore.noneOf(elementType);
        result.addAll();
        return result;
    }

    public static <E extends Enum<E>> EnumBitStore<E> copyOf(EnumBitStore<E> other) {
        EnumBitStore<E> result = EnumBitStore.noneOf(other.elementType);
        result.copyFrom(other);
        return result;
    }

    public static <E extends Enum<E>> EnumBitStore<E> of(E e) {
        EnumBitStore<E> result = EnumBitStore.noneOf(e.getDeclaringClass());
        result.add(e);
        return result;
    }

    public static <E extends Enum<E>> EnumBitStore<E> of(E e1, E e2) {
        EnumBitStore<E> result = EnumBitStore.noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        return result;
    }

    public static <E extends Enum<E>> EnumBitStore<E> of(E e1, E e2, E e3) {
        EnumBitStore<E> result = EnumBitStore.noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        return result;
    }

    public static <E extends Enum<E>> EnumBitStore<E> of(E e1, E e2, E e3, E e4) {
        EnumBitStore<E> result = EnumBitStore.noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        return result;
    }

    public static <E extends Enum<E>> EnumBitStore<E> of(E e1, E e2, E e3, E e4, E e5) {
        EnumBitStore<E> result = EnumBitStore.noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        result.add(e5);
        return result;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumBitStore<E> of(E first, E ... rest) {
        EnumBitStore<E> result = EnumBitStore.noneOf(first.getDeclaringClass());
        result.add(first);
        for (E e : rest) {
            result.add(e);
        }
        return result;
    }

    public void copyFrom(EnumBitStore<E> other) {
        this.bits = other.bits;
    }

    public void addAll(EnumBitStore<E> other) {
        this.bits = Bits.addFlags(this.bits, other.bits);
    }

    public void addAll() {
        for (Enum e : (Enum[])this.elementType.getEnumConstants()) {
            this.add(e);
        }
    }

    public void add(E e) {
        this.bits = Bits.addFlags(this.bits, ((IOEnum)e).getBits());
    }

    public void remove(E e) {
        this.bits = Bits.removeFlags(this.bits, ((IOEnum)e).getBits());
    }

    public boolean contains(E e) {
        return this.contains(((IOEnum)e).getBits());
    }

    public boolean contains(int bits) {
        return Bits.hasFlags(this.bits, bits);
    }

    public int size() {
        return Integer.bitCount(this.bits);
    }

    public boolean isEmpty() {
        return this.bits == 0;
    }

    public void clear() {
        this.bits = 0;
    }

    public int getBits() {
        return this.bits;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }

    public void save(ByteBuffer output) throws IOException {
        output.putInt(this.bits);
    }

    public void load(ByteBuffer input) throws IOException {
        this.bits = input.getInt();
    }

    public boolean equals(Object o) {
        if (!(o instanceof EnumBitStore)) {
            return false;
        }
        EnumBitStore es = (EnumBitStore)o;
        if (es.elementType == this.elementType) {
            return es.bits == this.bits;
        }
        return false;
    }

    public String toString() {
        if (this.size() <= 0) {
            return emptyToString;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        EnumBitStoreIterator iterator2 = new EnumBitStoreIterator(this);
        while (iterator2.hasNext()) {
            sb.append(iterator2.next());
            if (iterator2.returned >= iterator2.size) continue;
            sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public Iterator<E> iterator() {
        return new EnumBitStoreIterator(this);
    }

    private class EnumBitStoreIterator<E extends Enum<E>>
    implements Iterator<E> {
        int index;
        int returned;
        int size;
        final /* synthetic */ EnumBitStore this$0;

        EnumBitStoreIterator(EnumBitStore enumBitStore) {
            EnumBitStore enumBitStore2 = enumBitStore;
            Objects.requireNonNull(enumBitStore2);
            this.this$0 = enumBitStore2;
            this.index = 0;
            this.size = enumBitStore.size();
        }

        @Override
        public boolean hasNext() {
            return this.returned < this.size;
        }

        @Override
        public E next() {
            while (this.index < ((Enum[])this.this$0.elementType.getEnumConstants()).length) {
                Enum e = null;
                if (this.this$0.contains(((Enum[])this.this$0.elementType.getEnumConstants())[this.index])) {
                    e = ((Enum[])this.this$0.elementType.getEnumConstants())[this.index];
                }
                ++this.index;
                if (e == null) continue;
                ++this.returned;
                return (E)e;
            }
            throw new IllegalStateException();
        }
    }
}

