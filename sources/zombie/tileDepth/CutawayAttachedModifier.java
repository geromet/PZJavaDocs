/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.tileDepth.TileDepthMapManager;

public class CutawayAttachedModifier
implements Consumer<TextureDraw> {
    public static CutawayAttachedModifier instance = new CutawayAttachedModifier();
    private Texture depthTexture;
    private Texture cutawayTexture;
    private IsoSprite sprite;
    protected final int[] col = new int[4];
    protected int colTint;
    private int cutawayX;
    private int cutawayY;
    private int cutawayW;
    private int cutawayH;
    private SpriteRenderer.WallShaderTexRender wallShaderTexRender;

    @Override
    public void accept(TextureDraw textureDraw) {
        float y3;
        float y1;
        float x2;
        float x3;
        Texture tex0 = textureDraw.tex;
        Texture tex1 = this.depthTexture;
        Texture tex2 = this.cutawayTexture;
        float left = textureDraw.x0 - tex0.getOffsetX();
        float top = textureDraw.y0 - tex0.getOffsetY();
        int cutawayNWHeight = 226;
        int tileWidth = 128;
        int tileHeight = 226;
        int tex2OffsetX = this.cutawayX % 128;
        int tex2OffsetY = this.cutawayY % 226;
        int tex2Width = this.cutawayW;
        int tex2Height = this.cutawayH;
        float x0 = x3 = PZMath.max(tex0.offsetX, tex1.offsetX, (float)tex2OffsetX);
        float x1 = x2 = PZMath.min(tex0.offsetX + (float)tex0.getWidth(), tex1.offsetX + (float)tex1.getWidth(), (float)(tex2OffsetX + tex2Width));
        float y0 = y1 = PZMath.max(tex0.offsetY, tex1.offsetY, (float)tex2OffsetY);
        float y2 = y3 = PZMath.min(tex0.offsetY + (float)tex0.getHeight(), tex1.offsetY + (float)tex1.getHeight(), (float)(tex2OffsetY + tex2Height));
        if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.LeftOnly) {
            x1 = x2 = PZMath.min(x1, 63.0f);
        }
        if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.RightOnly) {
            x0 = x3 = PZMath.max(x0, 63.0f);
        }
        textureDraw.x0 = left + x0;
        textureDraw.x1 = left + x1;
        textureDraw.x2 = left + x2;
        textureDraw.x3 = left + x3;
        textureDraw.y0 = top + y0;
        textureDraw.y1 = top + y1;
        textureDraw.y2 = top + y2;
        textureDraw.y3 = top + y3;
        textureDraw.u0 = (tex0.getXStart() * (float)tex0.getWidthHW() + (x0 - tex0.offsetX)) / (float)tex0.getWidthHW();
        textureDraw.u1 = (tex0.getXStart() * (float)tex0.getWidthHW() + (x1 - tex0.offsetX)) / (float)tex0.getWidthHW();
        textureDraw.u2 = (tex0.getXStart() * (float)tex0.getWidthHW() + (x2 - tex0.offsetX)) / (float)tex0.getWidthHW();
        textureDraw.u3 = (tex0.getXStart() * (float)tex0.getWidthHW() + (x3 - tex0.offsetX)) / (float)tex0.getWidthHW();
        textureDraw.v0 = (tex0.getYStart() * (float)tex0.getHeightHW() + (y0 - tex0.offsetY)) / (float)tex0.getHeightHW();
        textureDraw.v1 = (tex0.getYStart() * (float)tex0.getHeightHW() + (y1 - tex0.offsetY)) / (float)tex0.getHeightHW();
        textureDraw.v2 = (tex0.getYStart() * (float)tex0.getHeightHW() + (y2 - tex0.offsetY)) / (float)tex0.getHeightHW();
        textureDraw.v3 = (tex0.getYStart() * (float)tex0.getHeightHW() + (y3 - tex0.offsetY)) / (float)tex0.getHeightHW();
        textureDraw.tex1 = this.depthTexture;
        textureDraw.tex1U0 = (tex1.getXStart() * (float)tex1.getWidthHW() + (x0 - tex1.offsetX)) / (float)tex1.getWidthHW();
        textureDraw.tex1U1 = (tex1.getXStart() * (float)tex1.getWidthHW() + (x1 - tex1.offsetX)) / (float)tex1.getWidthHW();
        textureDraw.tex1U2 = (tex1.getXStart() * (float)tex1.getWidthHW() + (x2 - tex1.offsetX)) / (float)tex1.getWidthHW();
        textureDraw.tex1U3 = (tex1.getXStart() * (float)tex1.getWidthHW() + (x3 - tex1.offsetX)) / (float)tex1.getWidthHW();
        textureDraw.tex1V0 = (tex1.getYStart() * (float)tex1.getHeightHW() + (y0 - tex1.offsetY)) / (float)tex1.getHeightHW();
        textureDraw.tex1V1 = (tex1.getYStart() * (float)tex1.getHeightHW() + (y1 - tex1.offsetY)) / (float)tex1.getHeightHW();
        textureDraw.tex1V2 = (tex1.getYStart() * (float)tex1.getHeightHW() + (y2 - tex1.offsetY)) / (float)tex1.getHeightHW();
        textureDraw.tex1V3 = (tex1.getYStart() * (float)tex1.getHeightHW() + (y3 - tex1.offsetY)) / (float)tex1.getHeightHW();
        textureDraw.tex2 = this.cutawayTexture;
        textureDraw.tex2U0 = ((float)this.cutawayX + (x0 - (float)tex2OffsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2U1 = ((float)this.cutawayX + (x1 - (float)tex2OffsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2U2 = ((float)this.cutawayX + (x2 - (float)tex2OffsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2U3 = ((float)this.cutawayX + (x3 - (float)tex2OffsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2V0 = ((float)this.cutawayY + (y0 - (float)tex2OffsetY)) / (float)tex2.getHeightHW();
        textureDraw.tex2V1 = ((float)this.cutawayY + (y1 - (float)tex2OffsetY)) / (float)tex2.getHeightHW();
        textureDraw.tex2V2 = ((float)this.cutawayY + (y2 - (float)tex2OffsetY)) / (float)tex2.getHeightHW();
        textureDraw.tex2V3 = ((float)this.cutawayY + (y3 - (float)tex2OffsetY)) / (float)tex2.getHeightHW();
        if (this.sprite == null || !this.sprite.getProperties().has(IsoFlagType.NoWallLighting)) {
            this.applyShading_Wall(textureDraw);
        }
    }

    public void setSprite(IsoSprite sprite) {
        this.sprite = sprite;
    }

    public void setVertColors(int col0, int col1, int col2, int col3) {
        this.col[0] = col0;
        this.col[1] = col1;
        this.col[2] = col2;
        this.col[3] = col3;
    }

    public void setAlpha4(float alpha) {
        int byteA = (int)(alpha * 255.0f) & 0xFF;
        this.col[0] = this.col[0] & 0xFFFFFF | byteA << 24;
        this.col[1] = this.col[1] & 0xFFFFFF | byteA << 24;
        this.col[2] = this.col[2] & 0xFFFFFF | byteA << 24;
        this.col[3] = this.col[3] & 0xFFFFFF | byteA << 24;
    }

    public void setTintColor(int tintABGR) {
        this.colTint = tintABGR;
    }

    public void setupWallDepth(IsoSprite sprite, IsoDirections dir, Texture cutawayTex, int cutawayX, int cutawayY, int cutawayW, int cutawayH, SpriteRenderer.WallShaderTexRender wallShaderTexRender) {
        TileDepthMapManager.TileDepthPreset presetID;
        switch (dir) {
            case N: {
                presetID = TileDepthMapManager.TileDepthPreset.NWall;
                if (!sprite.getProperties().has(IsoFlagType.DoorWallN) || sprite.getProperties().has(IsoFlagType.doorN)) break;
                presetID = TileDepthMapManager.TileDepthPreset.NDoorFrame;
                break;
            }
            case NW: {
                presetID = TileDepthMapManager.TileDepthPreset.NWWall;
                break;
            }
            case W: {
                presetID = TileDepthMapManager.TileDepthPreset.WWall;
                if (!sprite.getProperties().has(IsoFlagType.DoorWallW) || sprite.getProperties().has(IsoFlagType.doorW)) break;
                presetID = TileDepthMapManager.TileDepthPreset.WDoorFrame;
                break;
            }
            case SE: {
                presetID = TileDepthMapManager.TileDepthPreset.SEWall;
                break;
            }
            default: {
                presetID = TileDepthMapManager.TileDepthPreset.Floor;
            }
        }
        this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(presetID);
        if (sprite.depthTexture != null) {
            this.depthTexture = sprite.depthTexture.getTexture();
        }
        this.cutawayTexture = cutawayTex;
        this.cutawayX = cutawayX;
        this.cutawayY = cutawayY;
        this.cutawayW = cutawayW;
        this.cutawayH = cutawayH;
        this.wallShaderTexRender = wallShaderTexRender;
    }

    private void applyShading_Wall(TextureDraw texd) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lighting.getValue()) {
            texd.col0 = Color.blendBGR(texd.col0, this.col[0]);
            texd.col1 = Color.blendBGR(texd.col1, this.col[1]);
            texd.col2 = Color.blendBGR(texd.col2, this.col[2]);
            texd.col3 = Color.blendBGR(texd.col3, this.col[3]);
        }
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            texd.col0 = -1;
            texd.col1 = -1;
            texd.col2 = -1;
            texd.col3 = -1;
        }
        if (this.colTint != 0) {
            texd.col0 = Color.tintABGR(texd.col0, this.colTint);
            texd.col1 = Color.tintABGR(texd.col1, this.colTint);
            texd.col2 = Color.tintABGR(texd.col2, this.colTint);
            texd.col3 = Color.tintABGR(texd.col3, this.colTint);
        }
    }
}

