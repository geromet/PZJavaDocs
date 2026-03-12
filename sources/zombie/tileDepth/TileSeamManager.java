/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import zombie.core.Core;
import zombie.core.textures.Texture;

public final class TileSeamManager {
    public static final TileSeamManager instance = new TileSeamManager();
    private final Texture[] textures = new Texture[Tiles.values().length];
    private final float[][] vertices = new float[Tiles.values().length][];

    public void init() {
        Tiles[] tiles = Tiles.values();
        Texture texture = Texture.getSharedTexture("media/depthmaps/SEAMS_01.png", 0);
        for (int i = 0; i < tiles.length; ++i) {
            int col = i % 8;
            int row = i / 8;
            this.textures[i] = new Texture(texture.getTextureId(), "SEAMS_01_" + i, col * 64 * Core.tileScale, row * 128 * Core.tileScale, 64 * Core.tileScale, 128 * Core.tileScale);
        }
        this.vertices[Tiles.FloorSouth.ordinal()] = new float[]{5.0f, 221.0f, 69.0f, 253.0f, 63.0f, 256.0f, -1.0f, 224.0f};
        this.vertices[Tiles.FloorEast.ordinal()] = new float[]{57.0f, 253.0f, 121.0f, 221.0f, 127.0f, 224.0f, 63.0f, 256.0f};
        this.vertices[Tiles.FloorSouthOneThird.ordinal()] = new float[]{5.0f, 157.0f, 69.0f, 189.0f, 63.0f, 192.0f, -1.0f, 160.0f};
        this.vertices[Tiles.FloorEastOneThird.ordinal()] = new float[]{57.0f, 189.0f, 121.0f, 157.0f, 127.0f, 160.0f, 63.0f, 192.0f};
        this.vertices[Tiles.FloorSouthTwoThirds.ordinal()] = new float[]{5.0f, 93.0f, 69.0f, 125.0f, 63.0f, 128.0f, -1.0f, 96.0f};
        this.vertices[Tiles.FloorEastTwoThirds.ordinal()] = new float[]{57.0f, 125.0f, 121.0f, 93.0f, 127.0f, 96.0f, 63.0f, 128.0f};
    }

    public Texture getTexture(Tiles tiles) {
        return this.textures[tiles.ordinal()];
    }

    public float[] getVertices(Tiles tiles) {
        return this.vertices[tiles.ordinal()];
    }

    public static enum Tiles {
        FloorSouth,
        FloorEast,
        WallSouth,
        WallEast,
        FloorSouthOneThird,
        FloorEastOneThird,
        FloorSouthTwoThirds,
        FloorEastTwoThirds;

    }
}

