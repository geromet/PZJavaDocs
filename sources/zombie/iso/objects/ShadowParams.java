/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

public final class ShadowParams {
    public float w;
    public float fm;
    public float bm;

    public ShadowParams(float w, float fm, float bm) {
        this.w = w;
        this.fm = fm;
        this.bm = bm;
    }

    public ShadowParams set(float w, float fm, float bm) {
        this.w = w;
        this.fm = fm;
        this.bm = bm;
        return this;
    }
}

