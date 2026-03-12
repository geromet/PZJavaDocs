/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.HashMap;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public final class ModelTxt {
    boolean isStatic;
    boolean reverse;
    VertexBufferObject.VertexArray vertices;
    int[] elements;
    HashMap<String, Integer> boneIndices = new HashMap();
    ArrayList<Integer> skeletonHierarchy = new ArrayList();
    ArrayList<Matrix4f> bindPose = new ArrayList();
    ArrayList<Matrix4f> skinOffsetMatrices = new ArrayList();
    ArrayList<Matrix4f> invBindPose = new ArrayList();
    HashMap<String, AnimationClip> clips = new HashMap();
}

