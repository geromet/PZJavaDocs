/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel;

public final class Vector4 {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4() {
        this(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public Vector4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4(Vector4 vec) {
        this.set(vec);
    }

    public Vector4 set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4 set(Vector4 vec) {
        return this.set(vec.x, vec.y, vec.z, vec.w);
    }
}

