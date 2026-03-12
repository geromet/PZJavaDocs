/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.function.Consumer;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.util.list.PZArrayUtil;

public final class SkinningBone {
    public SkinningBone parent;
    public String name;
    public int index;
    public SkinningBone[] children;
    public SkeletonBone skeletonBone = SkeletonBone.None;

    public void forEachDescendant(Consumer<SkinningBone> consumer) {
        SkinningBone.forEachDescendant(this, consumer);
    }

    private static void forEachDescendant(SkinningBone bone, Consumer<SkinningBone> consumer) {
        if (bone.children == null || bone.children.length == 0) {
            return;
        }
        for (SkinningBone child : bone.children) {
            consumer.accept(child);
        }
        for (SkinningBone child : bone.children) {
            SkinningBone.forEachDescendant(child, consumer);
        }
    }

    public String toString() {
        String tab = " ";
        String endln = "";
        return this.getClass().getName() + "{ Name:\"" + this.name + "\", Index:" + this.index + ", SkeletonBone:" + String.valueOf((Object)this.skeletonBone) + ",}";
    }

    public int getParentBoneIndex() {
        return this.parent != null ? this.parent.index : -1;
    }

    public SkeletonBone getParentSkeletonBone() {
        return this.parent != null ? this.parent.skeletonBone : SkeletonBone.None;
    }

    public SkinningBone toRoot() {
        if (this.parent == null) {
            return this;
        }
        SkinningBone newRoot = new SkinningBone();
        newRoot.name = this.name;
        newRoot.index = this.index;
        newRoot.skeletonBone = this.skeletonBone;
        newRoot.children = PZArrayUtil.shallowClone(this.children);
        newRoot.parent = null;
        return newRoot;
    }

    public boolean isRoot() {
        return this.parent == null;
    }
}

