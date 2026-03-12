/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.tileDepth.TileDepthMapManager;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileSeamManager;

public class TileSeamModifier
implements Consumer<TextureDraw> {
    public static TileSeamModifier instance = new TileSeamModifier();
    private Texture depthTexture;
    private Texture maskTexture;
    private IsoSprite sprite;
    private Type seamType;
    protected final int[] col = new int[4];
    protected int colTint;
    protected boolean isShore;
    protected final float[] waterDepth = new float[4];
    private float[] vertices;
    private final int[] colFloor = new int[4];

    @Override
    public void accept(TextureDraw textureDraw) {
        float y2;
        float y3;
        float y0;
        float y1;
        float x1;
        float x2;
        float x0;
        float x3;
        Texture tex0 = textureDraw.tex;
        Texture tex1 = this.depthTexture;
        Texture tex2 = this.maskTexture;
        float left = textureDraw.x0 - tex0.getOffsetX();
        float top = textureDraw.y0 - tex0.getOffsetY();
        if (this.vertices == null) {
            x0 = x3 = PZMath.max(tex0.offsetX, tex1.offsetX, tex2.offsetX);
            x1 = x2 = PZMath.min(tex0.offsetX + (float)tex0.getWidth(), tex1.offsetX + (float)tex1.getWidth(), tex2.offsetX + (float)tex2.getWidth());
            y0 = y1 = PZMath.max(tex0.offsetY, tex1.offsetY, tex2.offsetY);
            y2 = y3 = PZMath.min(tex0.offsetY + (float)tex0.getHeight(), tex1.offsetY + (float)tex1.getHeight(), tex2.offsetY + (float)tex2.getHeight());
        } else {
            x0 = this.vertices[0];
            y0 = this.vertices[1];
            x1 = this.vertices[2];
            y1 = this.vertices[3];
            x2 = this.vertices[4];
            y2 = this.vertices[5];
            x3 = this.vertices[6];
            y3 = this.vertices[7];
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
        textureDraw.tex2 = this.maskTexture;
        textureDraw.tex2U0 = (tex2.getXStart() * (float)tex2.getWidthHW() + (x0 - tex2.offsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2U1 = (tex2.getXStart() * (float)tex2.getWidthHW() + (x1 - tex2.offsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2U2 = (tex2.getXStart() * (float)tex2.getWidthHW() + (x2 - tex2.offsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2U3 = (tex2.getXStart() * (float)tex2.getWidthHW() + (x3 - tex2.offsetX)) / (float)tex2.getWidthHW();
        textureDraw.tex2V0 = (tex2.getYStart() * (float)tex2.getHeightHW() + (y0 - tex2.offsetY)) / (float)tex2.getHeightHW();
        textureDraw.tex2V1 = (tex2.getYStart() * (float)tex2.getHeightHW() + (y1 - tex2.offsetY)) / (float)tex2.getHeightHW();
        textureDraw.tex2V2 = (tex2.getYStart() * (float)tex2.getHeightHW() + (y2 - tex2.offsetY)) / (float)tex2.getHeightHW();
        textureDraw.tex2V3 = (tex2.getYStart() * (float)tex2.getHeightHW() + (y3 - tex2.offsetY)) / (float)tex2.getHeightHW();
        if (this.seamType == Type.Floor) {
            this.applyShading_Floor(textureDraw);
        }
        if (this.seamType == Type.Wall) {
            this.applyShading_Wall(textureDraw);
        }
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

    public void setShore(boolean isShore) {
        this.isShore = isShore;
    }

    public void setWaterDepth(float val0, float val1, float val2, float val3) {
        this.waterDepth[0] = val0;
        this.waterDepth[1] = val1;
        this.waterDepth[2] = val2;
        this.waterDepth[3] = val3;
    }

    public void setTintColor(int tintABGR) {
        this.colTint = tintABGR;
    }

    public void setupFloorDepth(IsoSprite sprite, TileSeamManager.Tiles tiles) {
        this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.Floor);
        this.maskTexture = TileSeamManager.instance.getTexture(tiles);
        this.sprite = sprite;
        this.seamType = Type.Floor;
        this.vertices = TileSeamManager.instance.getVertices(tiles);
        for (int i = 0; i < 4; ++i) {
            this.colFloor[i] = this.col[i];
        }
        if (tiles == TileSeamManager.Tiles.FloorSouth) {
            this.colFloor[0] = this.col[3];
            this.colFloor[1] = this.col[2];
        }
        if (tiles == TileSeamManager.Tiles.FloorEast) {
            this.colFloor[0] = this.colFloor[3] = this.col[2];
            this.colFloor[2] = this.col[1];
        }
    }

    public void setupFloorDepth(IsoSprite sprite, TileSeamManager.Tiles tiles, TileDepthTexture depthTexture) {
        this.depthTexture = depthTexture.getTexture();
        this.maskTexture = TileSeamManager.instance.getTexture(tiles);
        this.sprite = sprite;
        this.seamType = Type.Floor;
        this.vertices = TileSeamManager.instance.getVertices(tiles);
        for (int i = 0; i < 4; ++i) {
            this.colFloor[i] = this.col[i];
        }
        if (tiles == TileSeamManager.Tiles.FloorSouthOneThird || tiles == TileSeamManager.Tiles.FloorSouthTwoThirds) {
            this.colFloor[0] = this.col[3];
            this.colFloor[1] = this.col[2];
        }
        if (tiles == TileSeamManager.Tiles.FloorEastOneThird || tiles == TileSeamManager.Tiles.FloorEastTwoThirds) {
            this.colFloor[0] = this.colFloor[3] = this.col[2];
            this.colFloor[2] = this.col[1];
        }
    }

    public void setupWallDepth(IsoSprite sprite, IsoDirections dir) {
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
        this.maskTexture = TileSeamManager.instance.getTexture(IsoSprite.seamFix2);
        this.sprite = sprite;
        this.seamType = Type.Wall;
        this.vertices = null;
    }

    private void applyShading_Floor(TextureDraw ddraw) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lighting.getValue()) {
            ddraw.col0 = Color.blendBGR(ddraw.col0, this.colFloor[0]);
            ddraw.col1 = Color.blendBGR(ddraw.col1, this.colFloor[1]);
            ddraw.col2 = Color.blendBGR(ddraw.col2, this.colFloor[2]);
            ddraw.col3 = Color.blendBGR(ddraw.col3, this.colFloor[3]);
        }
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            ddraw.col0 = -1;
            ddraw.col1 = -1;
            ddraw.col2 = -1;
            ddraw.col3 = -1;
        }
        if (this.isShore && DebugOptions.instance.terrain.renderTiles.isoGridSquare.shoreFade.getValue()) {
            ddraw.col0 = Color.setAlphaChannelToABGR(ddraw.col0, 1.0f - this.waterDepth[0]);
            ddraw.col1 = Color.setAlphaChannelToABGR(ddraw.col1, 1.0f - this.waterDepth[1]);
            ddraw.col2 = Color.setAlphaChannelToABGR(ddraw.col2, 1.0f - this.waterDepth[2]);
            ddraw.col3 = Color.setAlphaChannelToABGR(ddraw.col3, 1.0f - this.waterDepth[3]);
        }
        if (this.colTint != 0) {
            ddraw.col0 = Color.tintABGR(ddraw.col0, this.colTint);
            ddraw.col1 = Color.tintABGR(ddraw.col1, this.colTint);
            ddraw.col2 = Color.tintABGR(ddraw.col2, this.colTint);
            ddraw.col3 = Color.tintABGR(ddraw.col3, this.colTint);
        }
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

    static enum Type {
        Floor,
        Wall;

    }
}

