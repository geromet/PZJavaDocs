/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.lambda;

import java.util.Comparator;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Comparators {

    public static final class Params2 {

        public static final class CallbackStackItem<E, T1, T2>
        extends StackItem<T1, T2>
        implements Comparator<E> {
            private ICallback<E, T1, T2> comparator;
            private static final Pool<CallbackStackItem<Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public int compare(E e1, E e2) {
                return this.comparator.compare(e1, e2, this.val1, this.val2);
            }

            public static <E, T1, T2> CallbackStackItem<E, T1, T2> alloc(T1 val1, T2 val2, ICallback<E, T1, T2> comparator) {
                CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.comparator = comparator;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.comparator = null;
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
            public int compare(E var1, E var2, T1 var3, T2 var4);
        }
    }

    public static final class Params1 {

        public static final class CallbackStackItem<E, T1>
        extends StackItem<T1>
        implements Comparator<E> {
            private ICallback<E, T1> comparator;
            private static final Pool<CallbackStackItem<Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public int compare(E e1, E e2) {
                return this.comparator.compare(e1, e2, this.val1);
            }

            public static <E, T1> CallbackStackItem<E, T1> alloc(T1 val1, ICallback<E, T1> comparator) {
                CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.comparator = comparator;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.comparator = null;
            }
        }

        private static class StackItem<T1>
        extends PooledObject {
            T1 val1;

            private StackItem() {
            }
        }

        public static interface ICallback<E, T1> {
            public int compare(E var1, E var2, T1 var3);
        }
    }
}

