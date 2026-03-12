/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import zombie.core.textures.Texture;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;

public class TileDepthMapManager {
    public static TileDepthMapManager instance = new TileDepthMapManager();
    Texture[] presets;

    public Texture getTextureForPreset(TileDepthPreset preset) {
        return this.presets[preset.index];
    }

    public void init() {
        this.presets = new Texture[8];
        for (int x = 0; x < TileDepthPreset.Max.index; ++x) {
            TileDepthTexture depthTexture = TileDepthTextureManager.getInstance().getPresetDepthTexture(x, 0);
            this.presets[x] = depthTexture == null ? null : depthTexture.getTexture();
        }
    }

    public static enum TileDepthPreset {
        Floor(0),
        WDoorFrame(2),
        NDoorFrame(3),
        WWall(4),
        NWall(5),
        NWWall(6),
        SEWall(7),
        Max(8);

        private static final TileDepthPreset[] VALUES;
        private final int index;

        private TileDepthPreset(int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }

        public static TileDepthPreset fromIndex(int index) {
            return VALUES[index];
        }

        static {
            VALUES = TileDepthPreset.values();
        }
    }
}

