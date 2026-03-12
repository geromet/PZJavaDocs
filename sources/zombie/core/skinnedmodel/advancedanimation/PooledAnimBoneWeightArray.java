/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.List;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.util.Pool;
import zombie.util.PooledArrayObject;
import zombie.util.list.PZArrayUtil;

public class PooledAnimBoneWeightArray
extends PooledArrayObject<AnimBoneWeight> {
    private static final PooledAnimBoneWeightArray s_empty = new PooledAnimBoneWeightArray();
    private static final Pool<PooledAnimBoneWeightArray> s_pool = new Pool<PooledAnimBoneWeightArray>(PooledAnimBoneWeightArray::new);

    public static PooledAnimBoneWeightArray alloc(int count) {
        if (count == 0) {
            return s_empty;
        }
        PooledAnimBoneWeightArray newObject = s_pool.alloc();
        newObject.initCapacity(count, x$0 -> new AnimBoneWeight[x$0.intValue()]);
        return newObject;
    }

    public static PooledAnimBoneWeightArray toArray(List<AnimBoneWeight> list) {
        if (list == null) {
            return null;
        }
        PooledAnimBoneWeightArray newObject = PooledAnimBoneWeightArray.alloc(list.size());
        PZArrayUtil.arrayCopy((AnimBoneWeight[])newObject.array(), list);
        return newObject;
    }

    public static PooledAnimBoneWeightArray toArray(PooledArrayObject<AnimBoneWeight> source2) {
        if (source2 == null) {
            return null;
        }
        PooledAnimBoneWeightArray newObject = PooledAnimBoneWeightArray.alloc(source2.length());
        PZArrayUtil.arrayCopy((AnimBoneWeight[])newObject.array(), source2.array());
        return newObject;
    }
}

