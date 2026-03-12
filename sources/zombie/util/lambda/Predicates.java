/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.lambda;

import java.util.function.Predicate;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Predicates {

    public static final class Params3 {

        public static final class CallbackStackItem<E, T1, T2, T3>
        extends StackItem<T1, T2, T3>
        implements Predicate<E> {
            private ICallback<E, T1, T2, T3> predicate;
            private static final Pool<CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public boolean test(E e) {
                return this.predicate.test(e, this.val1, this.val2, this.val3);
            }

            public static <E, T1, T2, T3> CallbackStackItem<E, T1, T2, T3> alloc(T1 val1, T2 val2, T3 val3, ICallback<E, T1, T2, T3> predicate) {
                CallbackStackItem<Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.predicate = predicate;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.predicate = null;
            }
        }

        private static class StackItem<T1, T2, T3>
        extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;

            private StackItem() {
            }
        }

        public static interface ICallback<E, T1, T2, T3> {
            public boolean test(E var1, T1 var2, T2 var3, T3 var4);
        }
    }

    public static final class Params2 {

        public static final class CallbackStackItem<E, T1, T2>
        extends StackItem<T1, T2>
        implements Predicate<E> {
            private ICallback<E, T1, T2> predicate;
            private static final Pool<CallbackStackItem<Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public boolean test(E e) {
                return this.predicate.test(e, this.val1, this.val2);
            }

            public static <E, T1, T2> CallbackStackItem<E, T1, T2> alloc(T1 val1, T2 val2, ICallback<E, T1, T2> predicate) {
                CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.predicate = predicate;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.predicate = null;
            }
        }

        private static class StackItem<T1, T2>
        extends PooledObject {
            T1 val1;
            T2 val2;

            private StackItem() {
            }
        }

        public static interface ICallback<E, T1, T2> {
            public boolean test(E var1, T1 var2, T2 var3);
        }
    }

    public static final class Params1 {

        public static final class CallbackStackItem<E, T1>
        extends StackItem<T1>
        implements Predicate<E> {
            private ICallback<E, T1> predicate;
            private static final Pool<CallbackStackItem<Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public boolean test(E e) {
                return this.predicate.test(e, this.val1);
            }

            public static <E, T1> CallbackStackItem<E, T1> alloc(T1 val1, ICallback<E, T1> predicate) {
                CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.predicate = predicate;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.predicate = null;
            }
        }

        private static class StackItem<T1>
        extends PooledObject {
            T1 val1;

            private StackItem() {
            }
        }

        public static interface ICallback<E, T1> {
            public boolean test(E var1, T1 var2);
        }
    }
}

