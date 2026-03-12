/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public final class PZConvertIterable<T, S>
implements Iterable<T> {
    private final Iterable<S> srcIterable;
    private final Function<S, T> converter;

    public PZConvertIterable(Iterable<S> srcIterable, Function<S, T> converter) {
        this.srcIterable = srcIterable;
        this.converter = converter;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>(this){
            private final Iterator<S> srcIterator;
            final /* synthetic */ PZConvertIterable this$0;
            {
                PZConvertIterable pZConvertIterable = this$0;
                Objects.requireNonNull(pZConvertIterable);
                this.this$0 = pZConvertIterable;
                this.srcIterator = this.this$0.srcIterable.iterator();
            }

            @Override
            public boolean hasNext() {
                return this.srcIterator.hasNext();
            }

            @Override
            public T next() {
                return this.this$0.converter.apply(this.srcIterator.next());
            }
        };
    }
}

