/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.util.Pool;
import zombie.util.list.PZArrayUtil;

public class TwistableBoneTransform
extends BoneTransform {
    public float blendWeight;
    public float twist;
    private static final Pool<TwistableBoneTransform> s_pool = new Pool<TwistableBoneTransform>(TwistableBoneTransform::new);

    protected TwistableBoneTransform() {
    }

    @Override
    public void reset() {
        super.reset();
        this.blendWeight = 0.0f;
        this.twist = 0.0f;
    }

    @Override
    public void set(BoneTransform rhs) {
        super.set(rhs);
        if (rhs instanceof TwistableBoneTransform) {
            TwistableBoneTransform transform = (TwistableBoneTransform)rhs;
            this.blendWeight = transform.blendWeight;
            this.twist = transform.twist;
        }
    }

    public static TwistableBoneTransform alloc() {
        return s_pool.alloc();
    }

    public static TwistableBoneTransform[] allocArray(int count) {
        TwistableBoneTransform[] newArray = new TwistableBoneTransform[count];
        PZArrayUtil.arrayPopulate(newArray, TwistableBoneTransform::alloc);
        return newArray;
    }
}

