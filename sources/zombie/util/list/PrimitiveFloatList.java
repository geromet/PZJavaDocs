/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import zombie.util.list.FloatConsumer;

public class PrimitiveFloatList
extends AbstractList<Float>
implements RandomAccess {
    private final float[] array;

    public PrimitiveFloatList(float[] array) {
        this.array = Objects.requireNonNull(array);
    }

    @Override
    public int size() {
        return this.array.length;
    }

    @Override
    public Object[] toArray() {
        return Arrays.asList(new float[][]{this.array}).toArray();
    }

    @Override
    public <T> T[] toArray(T[] result) {
        int count = this.size();
        for (int i = 0; i < count && i < result.length; ++i) {
            Float val = Float.valueOf(this.array[i]);
            result[i] = val;
        }
        if (result.length > count) {
            result[count] = null;
        }
        return result;
    }

    @Override
    public Float get(int index) {
        return Float.valueOf(this.array[index]);
    }

    @Override
    public Float set(int index, Float element) {
        return Float.valueOf(this.set(index, element.floatValue()));
    }

    @Override
    public float set(int index, float element) {
        float oldValue = this.array[index];
        this.array[index] = element;
        return oldValue;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            return -1;
        }
        if (o instanceof Number) {
            Number number = (Number)o;
            return this.indexOf(number.floatValue());
        }
        return -1;
    }

    public int indexOf(float val) {
        int indexOf = -1;
        int count = this.size();
        for (int i = 0; i < count; ++i) {
            if (this.array[i] != val) continue;
            indexOf = i;
            break;
        }
        return indexOf;
    }

    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) != -1;
    }

    public boolean contains(float val) {
        return this.indexOf(val) != -1;
    }

    @Override
    public void forEach(Consumer<? super Float> action) {
        Objects.requireNonNull(action);
        this.forEach(action::accept);
    }

    public void forEach(FloatConsumer action) {
        int count = this.size();
        for (int i = 0; i < count; ++i) {
            action.accept(this.array[i]);
        }
    }

    @Override
    public void replaceAll(UnaryOperator<Float> operator) {
        Objects.requireNonNull(operator);
        float[] a = this.array;
        for (int i = 0; i < a.length; ++i) {
            a[i] = ((Float)operator.apply(Float.valueOf(a[i]))).floatValue();
        }
    }

    @Override
    public void sort(Comparator<? super Float> unused) {
        this.sort();
    }

    public void sort() {
        Arrays.sort(this.array);
    }
}

