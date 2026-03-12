/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import zombie.iso.IsoObject;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;

public final class ObjectRenderInfo {
    public final IsoObject object;
    public ObjectRenderLayer layer = ObjectRenderLayer.None;
    public float targetAlpha = 1.0f;
    public boolean cutaway;
    public float renderX;
    public float renderY;
    public float renderWidth;
    public float renderHeight;
    public float renderScaleX;
    public float renderScaleY;
    public float renderAlpha;

    public ObjectRenderInfo(IsoObject object) {
        this.object = object;
    }
}

