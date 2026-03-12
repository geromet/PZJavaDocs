/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.textures.ImageData;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoUtils;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.popman.ObjectPool;

public final class IsoPuddlesCompute {
    private static final float Pi = 3.1415f;
    private static float puddlesDirNE;
    private static float puddlesDirNW;
    private static float puddlesDirAll;
    private static float puddlesDirNone;
    private static float puddlesSize;
    private static boolean hdQuality;
    private static final Vector2f add;
    private static final Vector3f add_xyy;
    private static final Vector3f add_xxy;
    private static final Vector3f add_xxx;
    private static final Vector3f add_xyx;
    private static final Vector3f add_yxy;
    private static final Vector3f add_yyx;
    private static final Vector3f add_yxx;
    private static final Vector3f HashVector31;
    private static final Vector3f HashVector32;
    private static final ObjectPool<Vector3f> pool_vector3f;
    private static final ArrayList<Vector3f> allocated_vector3f;
    private static final Vector2f temp_vector2f;
    private static final float puddleInteractionThreshold = 0.24f;

    private static Vector3f allocVector3f(float x, float y, float z) {
        Vector3f v = pool_vector3f.alloc().set(x, y, z);
        allocated_vector3f.add(v);
        return v;
    }

    private static Vector3f allocVector3f(Vector3f other) {
        return IsoPuddlesCompute.allocVector3f(other.x, other.y, other.z);
    }

    private static Vector3f floor(Vector3f a) {
        return IsoPuddlesCompute.allocVector3f(PZMath.fastfloor(a.x), PZMath.fastfloor(a.y), PZMath.fastfloor(a.z));
    }

    private static Vector3f fract(Vector3f a) {
        return IsoPuddlesCompute.allocVector3f(IsoPuddlesCompute.fract(a.x), IsoPuddlesCompute.fract(a.y), IsoPuddlesCompute.fract(a.z));
    }

    private static float fract(float a) {
        return a - (float)PZMath.fastfloor(a);
    }

    private static float mix(float x, float y, float a) {
        return x * (1.0f - a) + y * a;
    }

    private static float FuncHash(Vector3f p) {
        Vector3f p2 = IsoPuddlesCompute.allocVector3f(p.dot(HashVector31), p.dot(HashVector32), 0.0f);
        return IsoPuddlesCompute.fract((float)(Math.sin((double)p2.x * 2.1 + 1.1) + Math.sin((double)p2.y * 2.5 + 1.5)));
    }

    private static float FuncNoise(Vector3f p) {
        Vector3f j = IsoPuddlesCompute.floor(p);
        Vector3f f0 = IsoPuddlesCompute.fract(p);
        Vector3f f = IsoPuddlesCompute.allocVector3f(f0.x * f0.x * (4.5f - 3.5f * f0.x), f0.y * f0.y * (4.5f - 3.5f * f0.y), f0.z * f0.z * (4.5f - 3.5f * f0.z));
        float r1 = IsoPuddlesCompute.mix(IsoPuddlesCompute.FuncHash(j), IsoPuddlesCompute.FuncHash(IsoPuddlesCompute.allocVector3f(j).add(add_xyy)), f.x);
        float r2 = IsoPuddlesCompute.mix(IsoPuddlesCompute.FuncHash(IsoPuddlesCompute.allocVector3f(j).add(add_yxy)), IsoPuddlesCompute.FuncHash(IsoPuddlesCompute.allocVector3f(j).add(add_xxy)), f.x);
        float r3 = IsoPuddlesCompute.mix(IsoPuddlesCompute.FuncHash(IsoPuddlesCompute.allocVector3f(j).add(add_yyx)), IsoPuddlesCompute.FuncHash(IsoPuddlesCompute.allocVector3f(j).add(add_xyx)), f.x);
        float r4 = IsoPuddlesCompute.mix(IsoPuddlesCompute.FuncHash(IsoPuddlesCompute.allocVector3f(j).add(add_yxx)), IsoPuddlesCompute.FuncHash(IsoPuddlesCompute.allocVector3f(j).add(add_xxx)), f.x);
        float r12 = IsoPuddlesCompute.mix(r1, r2, f.y);
        float r34 = IsoPuddlesCompute.mix(r3, r4, f.y);
        return IsoPuddlesCompute.mix(r12, r34, f.z);
    }

    private static float PerlinNoise(Vector3f p) {
        if (hdQuality) {
            p.mul(0.5f);
            float f = 0.5f * IsoPuddlesCompute.FuncNoise(p);
            p.mul(3.0f);
            f = (float)((double)f + 0.25 * (double)IsoPuddlesCompute.FuncNoise(p));
            p.mul(3.0f);
            f = (float)((double)f + 0.125 * (double)IsoPuddlesCompute.FuncNoise(p));
            f = (float)((double)f * Math.min(1.0, 2.0 * (double)IsoPuddlesCompute.FuncNoise(IsoPuddlesCompute.allocVector3f(p).mul(0.02f)) * Math.min(1.0, 1.0 * (double)IsoPuddlesCompute.FuncNoise(IsoPuddlesCompute.allocVector3f(p).mul(0.1f)))));
            return f;
        }
        return IsoPuddlesCompute.FuncNoise(p) * 0.4f;
    }

    private static float getPuddles(Vector2f uv) {
        float dirNE = puddlesDirNE;
        float dirNW = puddlesDirNW;
        float dirA = puddlesDirAll;
        uv.mul(10.0f);
        float s = 1.02f * puddlesSize;
        s = (float)((double)s + (double)dirNE * Math.sin(((double)uv.x * 1.0 + (double)uv.y * 2.0) * (double)3.1415f * 1.0) * Math.cos(((double)uv.x * 1.0 + (double)uv.y * 2.0) * (double)3.1415f * 1.0) * 2.0);
        s = (float)((double)s + (double)dirNW * Math.sin(((double)uv.x * 1.0 - (double)uv.y * 2.0) * (double)3.1415f * 1.0) * Math.cos(((double)uv.x * 1.0 - (double)uv.y * 2.0) * (double)3.1415f * 1.0) * 2.0);
        s = (float)((double)s + (double)dirA * 0.3);
        float b = IsoPuddlesCompute.PerlinNoise(IsoPuddlesCompute.allocVector3f(uv.x * 1.0f, 0.0f, uv.y * 2.0f));
        float a = Math.min(0.7f, s * b);
        b = Math.min(0.7f, IsoPuddlesCompute.PerlinNoise(IsoPuddlesCompute.allocVector3f(uv.x * 0.7f, 1.0f, uv.y * 0.7f)));
        return a + b;
    }

    public static float computePuddle(IsoGridSquare square) {
        pool_vector3f.releaseAll((List<Vector3f>)allocated_vector3f);
        allocated_vector3f.clear();
        hdQuality = false;
        if (!Core.getInstance().getUseShaders()) {
            return -0.1f;
        }
        if (Core.getInstance().getPerfPuddlesOnLoad() == 3 || Core.getInstance().getPerfPuddles() == 3) {
            return -0.1f;
        }
        if (Core.getInstance().getPerfPuddles() > 0 && square.z > 0) {
            return -0.1f;
        }
        IsoPuddles isoPuddles = IsoPuddles.getInstance();
        puddlesSize = isoPuddles.getPuddlesSize();
        if (puddlesSize <= 0.0f) {
            return -0.1f;
        }
        if (PerformanceSettings.puddlesQuality == 2) {
            return IsoPuddlesCompute.GetPuddlesFromLQTexture(IsoPuddlesCompute.CalculatePuddlesUvTexture(square, isoPuddles));
        }
        return IsoPuddlesCompute.GetPuddlesFromPerlinNoise(IsoPuddlesCompute.CalculatePuddlesUvMain(square, isoPuddles));
    }

    private static Vector2f CalculatePuddlesUvMain(IsoGridSquare square, IsoPuddles isoPuddles) {
        Vector4f wOffset = isoPuddles.getShaderOffsetMain();
        wOffset.x -= 90000.0f;
        wOffset.y -= 640000.0f;
        int camOffX = (int)IsoCamera.frameState.offX;
        int camOffY = (int)IsoCamera.frameState.offY;
        float x = IsoUtils.XToScreen((float)square.x + 0.5f - (float)square.z * 3.0f, (float)square.y + 0.5f - (float)square.z * 3.0f, 0.0f, 0) - (float)camOffX;
        float y = IsoUtils.YToScreen((float)square.x + 0.5f - (float)square.z * 3.0f, (float)square.y + 0.5f - (float)square.z * 3.0f, 0.0f, 0) - (float)camOffY;
        x /= (float)IsoCamera.frameState.offscreenWidth;
        y /= (float)IsoCamera.frameState.offscreenHeight;
        if (Core.getInstance().getPerfPuddles() <= 1) {
            square.getPuddles().recalcIfNeeded();
            puddlesDirNE = (square.getPuddles().pdne[0] + square.getPuddles().pdne[2]) * 0.5f;
            puddlesDirNW = (square.getPuddles().pdnw[0] + square.getPuddles().pdnw[2]) * 0.5f;
            puddlesDirAll = (square.getPuddles().pda[0] + square.getPuddles().pda[2]) * 0.5f;
            puddlesDirNone = (square.getPuddles().pnon[0] + square.getPuddles().pnon[2]) * 0.5f;
        } else {
            puddlesDirNE = 0.0f;
            puddlesDirNW = 0.0f;
            puddlesDirAll = 1.0f;
            puddlesDirNone = 0.0f;
        }
        float fragCoordX = x;
        float fragCoordY = y;
        return temp_vector2f.set((fragCoordX * wOffset.z + wOffset.x) * 8.0E-4f + (float)square.z * 7.0f, (fragCoordY * wOffset.w + wOffset.y) * 8.0E-4f + (float)square.z * 7.0f);
    }

    private static Vector2f CalculatePuddlesUvTexture(IsoGridSquare square, IsoPuddles isoPuddles) {
        Vector4f wOffset = isoPuddles.getShaderOffsetMain();
        wOffset.y *= -1.0f;
        wOffset.x -= 90000.0f;
        wOffset.y -= 640000.0f;
        int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
        wOffset.x += (float)chunkFloorYSpan;
        wOffset.y += (float)chunkFloorYSpan;
        int camOffX = (int)IsoCamera.frameState.offX;
        int camOffY = (int)IsoCamera.frameState.offY;
        float x = IsoUtils.XToScreen((float)square.x + 0.5f - (float)square.z * 3.0f, (float)square.y + 0.5f - (float)square.z * 3.0f, 0.0f, 0) - (float)camOffX;
        float y = IsoUtils.YToScreen((float)square.x + 0.5f - (float)square.z * 3.0f, (float)square.y + 0.5f - (float)square.z * 3.0f, 0.0f, 0) - (float)camOffY;
        x /= (float)IsoCamera.frameState.offscreenWidth;
        y /= (float)IsoCamera.frameState.offscreenHeight;
        if (Core.getInstance().getPerfPuddles() <= 1) {
            square.getPuddles().recalcIfNeeded();
            puddlesDirNE = (square.getPuddles().pdne[0] + square.getPuddles().pdne[2]) * 0.5f;
            puddlesDirNW = (square.getPuddles().pdnw[0] + square.getPuddles().pdnw[2]) * 0.5f;
            puddlesDirAll = (square.getPuddles().pda[0] + square.getPuddles().pda[2]) * 0.5f;
            puddlesDirNone = (square.getPuddles().pnon[0] + square.getPuddles().pnon[2]) * 0.5f;
        } else {
            puddlesDirNE = 0.0f;
            puddlesDirNW = 0.0f;
            puddlesDirAll = 1.0f;
            puddlesDirNone = 0.0f;
        }
        float fragCoordX = x;
        float fragCoordY = -y;
        return temp_vector2f.set((fragCoordX * wOffset.z + wOffset.x) * 8.0E-4f + (float)square.z * 7.0f, (fragCoordY * wOffset.w + wOffset.y) * 8.0E-4f + (float)square.z * 7.0f);
    }

    private static float GetPuddlesFromPerlinNoise(Vector2f uv) {
        float level = (float)Math.pow(IsoPuddlesCompute.getPuddles(uv), 2.0);
        float levelf = (float)Math.min(Math.pow(level, 0.3), 1.0) + level;
        return levelf * puddlesSize - 0.24f;
    }

    private static float GetPuddlesFromLQTexture(Vector2f uv) {
        Texture texture = (Texture)IsoPuddles.getInstance().getHMTexture();
        ByteBuffer textureBuffer = IsoPuddles.getInstance().getHMTextureBuffer();
        float puddleSampleResult = -1.0f;
        if (texture != null && textureBuffer != null) {
            float puddleTextureScale = 0.5f;
            float imageUvX = uv.x * 0.5f % 1.0f;
            float imageUvY = uv.y * 0.5f % 1.0f;
            if (imageUvX < 0.0f) {
                imageUvX += 1.0f;
            }
            if (imageUvY < 0.0f) {
                imageUvY += 1.0f;
            }
            int[] result = new int[4];
            ImageData.getPixelDiscard(textureBuffer, texture.getWidthHW(), texture.getHeightHW(), (int)(imageUvX * (float)texture.getWidth()), (int)(imageUvY * (float)texture.getHeight()), result);
            float r = (float)result[0] / 255.0f;
            float g = (float)result[1] / 255.0f;
            float b = (float)result[2] / 255.0f;
            float level = IsoPuddlesCompute.mix(IsoPuddlesCompute.mix(b, r, puddlesDirNE), g, puddlesDirNW);
            float levelf = (float)Math.min(Math.pow(level, 0.3f), 1.0) + level;
            puddleSampleResult = levelf * puddlesSize - 0.24f;
        }
        return puddleSampleResult;
    }

    static {
        hdQuality = true;
        add = new Vector2f(1.0f, 0.0f);
        add_xyy = new Vector3f(1.0f, 0.0f, 0.0f);
        add_xxy = new Vector3f(1.0f, 1.0f, 0.0f);
        add_xxx = new Vector3f(1.0f, 1.0f, 1.0f);
        add_xyx = new Vector3f(1.0f, 0.0f, 1.0f);
        add_yxy = new Vector3f(0.0f, 1.0f, 0.0f);
        add_yyx = new Vector3f(0.0f, 0.0f, 1.0f);
        add_yxx = new Vector3f(0.0f, 1.0f, 1.0f);
        HashVector31 = new Vector3f(17.1f, 31.7f, 32.6f);
        HashVector32 = new Vector3f(29.5f, 13.3f, 42.6f);
        pool_vector3f = new ObjectPool<Vector3f>(Vector3f::new);
        allocated_vector3f = new ArrayList();
        temp_vector2f = new Vector2f();
    }
}

