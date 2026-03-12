/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import java.util.function.Consumer;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.fboRenderChunk.FBORenderLevels;

public class DiamondShaper
implements Consumer<TextureDraw> {
    public static final DiamondShaper instance = new DiamondShaper();

    @Override
    public void accept(TextureDraw ddraw) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.meshCutDown.getValue()) {
            return;
        }
        float dx = 0.5f;
        float dy = 0.5f;
        float du = 0.0f;
        float dv = 0.0f;
        if (PerformanceSettings.fboRenderChunk) {
            int textureScale = FBORenderLevels.getTextureScale(IsoCamera.frameState.zoom);
            dx = textureScale;
            dy = textureScale;
            du = dx;
            dv = dy;
        }
        float x0 = ddraw.x0 - dx;
        float y0 = ddraw.y0 - dy;
        float x1 = ddraw.x1 + dx;
        float y1 = ddraw.y1 - dy;
        float y2 = ddraw.y2 + dy;
        float y3 = ddraw.y3 + dy;
        float width = x1 - x0;
        float height = y2 - y1;
        float xHalf = x0 + width * 0.5f;
        float yHalf = y1 + height * 0.5f;
        float onePixelU = 1.0f / (float)ddraw.tex.getWidthHW();
        float onePixelV = 1.0f / (float)ddraw.tex.getHeightHW();
        float u0 = ddraw.u0 - onePixelU * du;
        float v0 = ddraw.v0 - onePixelV * dv;
        float u1 = ddraw.u1 + onePixelU * du;
        float v1 = ddraw.v1 - onePixelV * dv;
        float v2 = ddraw.v2 + onePixelV * dv;
        float v3 = ddraw.v3 + onePixelV * dv;
        float uWidth = u1 - u0;
        float vHeight = v2 - v0;
        float uHalf = u0 + uWidth * 0.5f;
        float vHalf = v1 + vHeight * 0.5f;
        ddraw.x0 = xHalf;
        ddraw.y0 = y0;
        ddraw.u0 = uHalf;
        ddraw.v0 = v0;
        ddraw.x1 = x1;
        ddraw.y1 = yHalf;
        ddraw.u1 = u1;
        ddraw.v1 = vHalf;
        ddraw.x2 = xHalf;
        ddraw.y2 = y3;
        ddraw.u2 = uHalf;
        ddraw.v2 = v3;
        ddraw.x3 = x0;
        ddraw.y3 = yHalf;
        ddraw.u3 = u0;
        ddraw.v3 = vHalf;
        if (ddraw.tex1 != null) {
            onePixelU = 1.0f / (float)ddraw.tex1.getWidthHW();
            onePixelV = 1.0f / (float)ddraw.tex1.getHeightHW();
            u0 = ddraw.tex1U0 - onePixelU * du;
            v0 = ddraw.tex1V0 - onePixelV * dv;
            u1 = ddraw.tex1U1 + onePixelU * du;
            v1 = ddraw.tex1V1 - onePixelV * dv;
            v2 = ddraw.tex1V2 + onePixelV * dv;
            v3 = ddraw.tex1V3 + onePixelV * dv;
            uWidth = u1 - u0;
            vHeight = v2 - v0;
            uHalf = u0 + uWidth * 0.5f;
            vHalf = v1 + vHeight * 0.5f;
            ddraw.tex1U0 = uHalf;
            ddraw.tex1V0 = v0;
            ddraw.tex1U1 = u1;
            ddraw.tex1V1 = vHalf;
            ddraw.tex1U2 = uHalf;
            ddraw.tex1V2 = v3;
            ddraw.tex1U3 = u0;
            ddraw.tex1V3 = vHalf;
        }
        if (ddraw.tex2 != null) {
            float u02 = ddraw.tex2U0;
            float v02 = ddraw.tex2V0;
            float u12 = ddraw.tex2U1;
            float v12 = ddraw.tex2V1;
            float v22 = ddraw.tex2V2;
            float v32 = ddraw.tex2V3;
            float uWidth2 = u12 - u02;
            float vHeight2 = v22 - v02;
            float uHalf2 = u02 + uWidth2 * 0.5f;
            float vHalf2 = v12 + vHeight2 * 0.5f;
            ddraw.tex2U0 = uHalf2;
            ddraw.tex2V0 = v02;
            ddraw.tex2U1 = u12;
            ddraw.tex2V1 = vHalf2;
            ddraw.tex2U2 = uHalf2;
            ddraw.tex2V2 = v32;
            ddraw.tex2U3 = u02;
            ddraw.tex2V3 = vHalf2;
        }
    }
}

