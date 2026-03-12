/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ReturnValueContainerPrimitives {

    public static final class RVInt
    extends PooledObject {
        public int returnVal;
        private static final Pool<RVInt> s_pool = new Pool<RVInt>(RVInt::new);

        @Override
        public void onReleased() {
            this.returnVal = 0;
        }

        public static RVInt alloc() {
            return s_pool.alloc();
        }
    }

    public static final class RVFloat
    extends PooledObject {
        public float returnVal;
        private static final Pool<RVFloat> s_pool = new Pool<RVFloat>(RVFloat::new);

        @Override
        public void onReleased() {
            this.returnVal = 0.0f;
        }

        public static RVFloat alloc() {
            return s_pool.alloc();
        }
    }

    public static final class RVBoolean
    extends PooledObject {
        public boolean returnVal;
        private static final Pool<RVBoolean> s_pool = new Pool<RVBoolean>(RVBoolean::new);

        @Override
        public void onReleased() {
            this.returnVal = false;
        }

        public static RVBoolean alloc() {
            return s_pool.alloc();
        }
    }
}

