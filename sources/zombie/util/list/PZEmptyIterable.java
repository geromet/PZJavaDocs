/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import java.util.Iterator;
import java.util.Objects;

public final class PZEmptyIterable<T>
implements Iterable<T> {
    private static final PZEmptyIterable<Object> instance = new PZEmptyIterable();
    private final Iterator<T> it = new Iterator<T>(this){
        {
            Objects.requireNonNull(this$0);
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new ArrayIndexOutOfBoundsException("Empty Iterator. Has no data.");
        }
    };

    private PZEmptyIterable() {
    }

    public static <E> PZEmptyIterable<E> getInstance() {
        return instance;
    }

    @Override
    public Iterator<T> iterator() {
        return this.it;
    }
}

