/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.lambda;

import java.util.function.Consumer;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Consumers {

    public static final class Params5 {

        public static final class CallbackStackItem<E, T1, T2, T3, T4, T5>
        extends StackItem<T1, T2, T3, T4, T5>
        implements Consumer<E> {
            private ICallback<E, T1, T2, T3, T4, T5> consumer;
            private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2, this.val3, this.val4, this.val5);
            }

            public static <E, T1, T2, T3, T4, T5> CallbackStackItem<E, T1, T2, T3, T4, T5> alloc(T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, ICallback<E, T1, T2, T3, T4, T5> consumer) {
                CallbackStackItem<Object, Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.val5 = val5;
                item.consumer = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.val5 = null;
                this.consumer = null;
            }
        }

        private static class StackItem<T1, T2, T3, T4, T5>
        extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
            T5 val5;

            private StackItem() {
            }
        }

        public static interface ICallback<E, T1, T2, T3, T4, T5> {
            public void accept(E var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6);
        }
    }

    public static final class Params4 {

        public static final class CallbackStackItem<E, T1, T2, T3, T4>
        extends StackItem<T1, T2, T3, T4>
        implements Consumer<E> {
            private ICallback<E, T1, T2, T3, T4> consumer;
            private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2, this.val3, this.val4);
            }

            public static <E, T1, T2, T3, T4> CallbackStackItem<E, T1, T2, T3, T4> alloc(T1 val1, T2 val2, T3 val3, T4 val4, ICallback<E, T1, T2, T3, T4> consumer) {
                CallbackStackItem<Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.consumer = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.consumer = null;
            }
        }

        private static class StackItem<T1, T2, T3, T4>
        extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;

            private StackItem() {
            }
        }

        public static interface ICallback<E, T1, T2, T3, T4> {
            public void accept(E var1, T1 var2, T2 var3, T3 var4, T4 var5);
        }
    }

    public static final class Params3 {

        public static final class CallbackStackItem<E, T1, T2, T3>
        extends StackItem<T1, T2, T3>
        implements Consumer<E> {
            private ICallback<E, T1, T2, T3> consumer;
            private static final Pool<CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2, this.val3);
            }

            public static <E, T1, T2, T3> CallbackStackItem<E, T1, T2, T3> alloc(T1 val1, T2 val2, T3 val3, ICallback<E, T1, T2, T3> consumer) {
                CallbackStackItem<Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.consumer = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.consumer = null;
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
            public void accept(E var1, T1 var2, T2 var3, T3 var4);
        }
    }

    public static class Params2 {

        public static final class CallbackStackItem<E, T1, T2>
        extends StackItem<T1, T2>
        implements Consumer<E> {
            private ICallback<E, T1, T2> consumer;
            private static final Pool<CallbackStackItem<Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2);
            }

            public static <E, T1, T2> CallbackStackItem<E, T1, T2> alloc(T1 val1, T2 val2, ICallback<E, T1, T2> consumer) {
                CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.consumer = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.consumer = null;
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
            public void accept(E var1, T1 var2, T2 var3);
        }
    }

    public static final class Params1 {

        public static final class CallbackStackItem<E, T1>
        extends StackItem<T1>
        implements Consumer<E> {
            private ICallback<E, T1> consumer;
            private static final Pool<CallbackStackItem<Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1);
            }

            public static <E, T1> CallbackStackItem<E, T1> alloc(T1 val1, ICallback<E, T1> consumer) {
                CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.consumer = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.consumer = null;
            }
        }

        private static class StackItem<T1>
        extends PooledObject {
            T1 val1;

            private StackItem() {
            }
        }

        public static interface ICallback<E, T1> {
            public void accept(E var1, T1 var2);
        }
    }
}

