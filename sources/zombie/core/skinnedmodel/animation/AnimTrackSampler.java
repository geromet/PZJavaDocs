/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import org.lwjgl.util.vector.Matrix4f;

public interface AnimTrackSampler {
    public float getTotalTime();

    public boolean isLooped();

    public void moveToTime(float var1);

    public float getCurrentTime();

    public void getBoneMatrix(int var1, Matrix4f var2);

    public int getNumBones();
}

