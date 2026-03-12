/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.lambda;

import java.util.function.BooleanSupplier;
import zombie.debug.DebugType;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class Invokers {

    public static final class Params5 {

        public static final class CallbackStackItem<T1, T2, T3, T4, T5>
        extends StackItem<T1, T2, T3, T4, T5>
        implements Runnable {
            private ICallback<T1, T2, T3, T4, T5> invoker;
            private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void run() {
                this.invoker.accept(this.val1, this.val2, this.val3, this.val4, this.val5);
            }

            public static <T1, T2, T3, T4, T5> CallbackStackItem<T1, T2, T3, T4, T5> alloc(T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, ICallback<T1, T2, T3, T4, T5> consumer) {
                CallbackStackItem<Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.val5 = val5;
                item.invoker = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.val5 = null;
                this.invoker = null;
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

        public static interface ICallback<T1, T2, T3, T4, T5> {
            public void accept(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5);
        }
    }

    public static final class Params4 {

        public static final class CallbackStackItem<T1, T2, T3, T4>
        extends StackItem<T1, T2, T3, T4>
        implements Runnable {
            private ICallback<T1, T2, T3, T4> invoker;
            private static final Pool<CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void run() {
                this.invoker.accept(this.val1, this.val2, this.val3, this.val4);
            }

            public static <T1, T2, T3, T4> CallbackStackItem<T1, T2, T3, T4> alloc(T1 val1, T2 val2, T3 val3, T4 val4, ICallback<T1, T2, T3, T4> consumer) {
                CallbackStackItem<Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.invoker = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.invoker = null;
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

        public static interface ICallback<T1, T2, T3, T4> {
            public void accept(T1 var1, T2 var2, T3 var3, T4 var4);
        }
    }

    public static final class Params3 {

        public static final class CallbackStackItem<T1, T2, T3>
        extends StackItem<T1, T2, T3>
        implements Runnable {
            private ICallback<T1, T2, T3> invoker;
            private static final Pool<CallbackStackItem<Object, Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void run() {
                this.invoker.accept(this.val1, this.val2, this.val3);
            }

            public static <T1, T2, T3> CallbackStackItem<T1, T2, T3> alloc(T1 val1, T2 val2, T3 val3, ICallback<T1, T2, T3> consumer) {
                CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.invoker = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.invoker = null;
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

        public static interface ICallback<T1, T2, T3> {
            public void accept(T1 var1, T2 var2, T3 var3);
        }
    }

    public static final class Params2 {

        public static final class Boolean {

            public static interface IParam2<T>
            extends ICallback<T, T>,
            Params1.Boolean.ICallback<T> {
                @Override
                default public boolean accept(T val1, T val2) {
                    return this.accept(val2);
                }
            }

            public static interface IParam1<T>
            extends ICallback<T, T>,
            Params1.Boolean.ICallback<T> {
                @Override
                default public boolean accept(T val1, T val2) {
                    return this.accept(val1);
                }
            }

            public static interface ICallback<T1, T2> {
                public boolean accept(T1 var1, T2 var2);
            }
        }

        public static final class CallbackStackItem<T1, T2>
        extends StackItem<T1, T2>
        implements Runnable {
            private ICallback<T1, T2> invoker;
            private static final Pool<CallbackStackItem<Object, Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void run() {
                if (this.invoker == null) {
                    DebugType.General.warn("Cannot invoke null-invoker.");
                    return;
                }
                this.invoker.accept(this.val1, this.val2);
            }

            public static <T1, T2> CallbackStackItem<T1, T2> alloc(T1 val1, T2 val2, ICallback<T1, T2> consumer) {
                CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.invoker = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.invoker = null;
            }
        }

        private static class StackItem<T1, T2>
        extends PooledObject {
            T1 val1;
            T2 val2;

            private StackItem() {
            }
        }

        public static interface ICallback<T1, T2> {
            public void accept(T1 var1, T2 var2);
        }
    }

    public static final class Params1 {

        public static final class Boolean {

            public static interface ICallback<T1> {
                public boolean accept(T1 var1);
            }
        }

        public static final class CallbackStackItem<T1>
        extends StackItem<T1>
        implements Runnable {
            private ICallback<T1> invoker;
            private static final Pool<CallbackStackItem<Object>> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

            @Override
            public void run() {
                this.invoker.accept(this.val1);
            }

            public static <T1> CallbackStackItem<T1> alloc(T1 val1, ICallback<T1> consumer) {
                CallbackStackItem<Object> item = s_pool.alloc();
                item.val1 = val1;
                item.invoker = consumer;
                return item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.invoker = null;
            }
        }

        private static class StackItem<T1>
        extends PooledObject {
            T1 val1;

            private StackItem() {
            }
        }

        public static interface ICallback<T1> {
            public void accept(T1 var1);
        }
    }

    public static final class Params0 {

        public static final class Boolean {

            public static final class CallbackStackItem
            extends PooledObject
            implements Runnable,
            BooleanSupplier {
                private ICallback predicate;
                private boolean result;
                private static final Pool<CallbackStackItem> s_pool = new Pool<CallbackStackItem>(CallbackStackItem::new);

                @Override
                public void run() {
                    this.result = this.predicate.accept();
                }

                @Override
                public boolean getAsBoolean() {
                    return this.result;
                }

                public static CallbackStackItem alloc(ICallback predicate) {
                    CallbackStackItem item = s_pool.alloc();
                    item.predicate = predicate;
                    return item;
                }

                @Override
                public void onReleased() {
                    this.predicate = null;
                }
            }

            public static interface ICallback {
                public boolean accept();
            }
        }

        public static interface ICallback {
            public void accept();
        }
    }
}

