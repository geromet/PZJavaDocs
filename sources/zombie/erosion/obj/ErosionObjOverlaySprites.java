/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion.obj;

import zombie.erosion.ErosionMain;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;

public final class ErosionObjOverlaySprites {
    public String name;
    public int stages;
    private final Stage[] sprites;

    public ErosionObjOverlaySprites(int stages, String name) {
        this.name = name;
        this.stages = stages;
        this.sprites = new Stage[stages];
        for (int i = 0; i < this.stages; ++i) {
            this.sprites[i] = new Stage();
        }
    }

    public IsoSprite getSprite(int stage, int season) {
        return this.sprites[stage].seasons[season].getSprite();
    }

    public IsoSpriteInstance getSpriteInstance(int stage, int season) {
        return this.sprites[stage].seasons[season].getInstance();
    }

    public void setSprite(int stage, String sprite, int season) {
        this.sprites[stage].seasons[season] = new Sprite(sprite);
    }

    private static class Stage {
        public Sprite[] seasons = new Sprite[6];

        private Stage() {
        }
    }

    private static final class Sprite {
        private final String sprite;

        public Sprite(String sprite) {
            this.sprite = sprite;
        }

        public IsoSprite getSprite() {
            if (this.sprite != null) {
                return ErosionMain.getInstance().getSpriteManager().getSprite(this.sprite);
            }
            return null;
        }

        public IsoSpriteInstance getInstance() {
            if (this.sprite != null) {
                return ErosionMain.getInstance().getSpriteManager().getSprite(this.sprite).newInstance();
            }
            return null;
        }
    }
}

