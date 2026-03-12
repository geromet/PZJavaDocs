/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.lambda;

import java.util.Objects;

@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {
    public void accept(T var1, U var2, V var3, W var4);

    default public QuadConsumer<T, U, V, W> andThen(QuadConsumer<? super T, ? super U, ? super V, ? super W> after) {
        Objects.requireNonNull(after);
        return (l, r, m, n) -> {
            this.accept(l, r, m, n);
            after.accept(l, r, m, n);
        };
    }
}

