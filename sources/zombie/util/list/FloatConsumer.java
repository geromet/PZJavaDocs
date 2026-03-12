/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import java.util.Objects;

public interface FloatConsumer {
    public void accept(float var1);

    default public FloatConsumer andThen(FloatConsumer after) {
        Objects.requireNonNull(after);
        return t -> {
            this.accept(t);
            after.accept(t);
        };
    }
}

