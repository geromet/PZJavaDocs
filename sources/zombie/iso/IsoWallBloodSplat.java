/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthModifier;

public final class IsoWallBloodSplat {
    private static final ColorInfo info = new ColorInfo();
    public float worldAge;
    public IsoSprite sprite;

    public IsoWallBloodSplat() {
    }

    public IsoWallBloodSplat(float worldAge, IsoSprite sprite) {
        this.worldAge = worldAge;
        this.sprite = sprite;
    }

    public void render(float x, float y, float z, ColorInfo objectColor, Consumer<TextureDraw> texdModifier) {
        if (this.sprite == null) {
            return;
        }
        if (this.sprite.hasNoTextures()) {
            return;
        }
        int scale = Core.tileScale;
        int offsetX = 32 * scale;
        int offsetY = 96 * scale;
        if (IsoSprite.globalOffsetX == -1.0f) {
            IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
            IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
        }
        float goX = IsoSprite.globalOffsetX;
        float goY = IsoSprite.globalOffsetY;
        if (FBORenderChunkManager.instance.isCaching()) {
            goX = FBORenderChunkManager.instance.getXOffset();
            goY = FBORenderChunkManager.instance.getYOffset();
            x -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wx * 8);
            y -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wy * 8);
        }
        float sx = IsoUtils.XToScreen(x, y, z, 0);
        float sy = IsoUtils.YToScreen(x, y, z, 0);
        sx -= (float)offsetX;
        sy -= (float)offsetY;
        sx += goX;
        sy += goY;
        if (!PerformanceSettings.fboRenderChunk) {
            if (sx >= (float)IsoCamera.frameState.offscreenWidth || sx + (float)(64 * scale) <= 0.0f) {
                return;
            }
            if (sy >= (float)IsoCamera.frameState.offscreenHeight || sy + (float)(128 * scale) <= 0.0f) {
                return;
            }
        }
        IsoWallBloodSplat.info.r = 0.7f * objectColor.r;
        IsoWallBloodSplat.info.g = 0.9f * objectColor.g;
        IsoWallBloodSplat.info.b = 0.9f * objectColor.b;
        IsoWallBloodSplat.info.a = 0.4f;
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        float deltaAge = worldAge - this.worldAge;
        if (deltaAge >= 0.0f && deltaAge < 72.0f) {
            float f = 1.0f - deltaAge / 72.0f;
            IsoWallBloodSplat.info.r *= 0.2f + f * 0.8f;
            IsoWallBloodSplat.info.g *= 0.2f + f * 0.8f;
            IsoWallBloodSplat.info.b *= 0.2f + f * 0.8f;
            IsoWallBloodSplat.info.a *= 0.25f + f * 0.75f;
        } else {
            IsoWallBloodSplat.info.r *= 0.2f;
            IsoWallBloodSplat.info.g *= 0.2f;
            IsoWallBloodSplat.info.b *= 0.2f;
            IsoWallBloodSplat.info.a *= 0.25f;
        }
        IsoWallBloodSplat.info.a = Math.max(IsoWallBloodSplat.info.a, 0.15f);
        if (texdModifier == CutawayAttachedModifier.instance) {
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
            }
            this.sprite.render(null, x, y, z, IsoDirections.N, offsetX, (float)offsetY, info, false, texdModifier);
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
            }
            return;
        }
        if (PerformanceSettings.fboRenderChunk) {
            SpriteRenderer.instance.StartShader(SceneShaderStore.defaultShaderId, IsoCamera.frameState.playerIndex);
            IndieGL.enableDepthTest();
            IndieGL.glDepthMask(false);
            IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
        }
        this.sprite.render(null, x, y, z, IsoDirections.N, offsetX, (float)offsetY, info, false, TileDepthModifier.instance);
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
        }
    }

    public void save(ByteBuffer output) {
        output.putFloat(this.worldAge);
        output.putInt(this.sprite.id);
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.worldAge = input.getFloat();
        int spriteID = input.getInt();
        this.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, spriteID);
    }
}

