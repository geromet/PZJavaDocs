/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import zombie.GameTime;
import zombie.core.opengl.Shader;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.sprite.IsoSpriteInstance;

public class IsoZombieHead
extends IsoMovingObject {
    public float tintb = 1.0f;
    public float tintg = 1.0f;
    public float tintr = 1.0f;
    public float time;

    public IsoZombieHead(IsoCell cell) {
    }

    @Override
    public boolean Serialize() {
        return false;
    }

    @Override
    public String getObjectName() {
        return "ZombieHead";
    }

    @Override
    public void update() {
        super.update();
        this.time += GameTime.instance.getMultipliedSecondsSinceLastUpdate();
        this.sy = 0.0f;
        this.sx = 0.0f;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        this.setTargetAlpha(1.0f);
        super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
    }

    public IsoZombieHead(GibletType type, IsoCell cell, float x, float y, float z) {
        this.solid = false;
        this.shootable = false;
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.setAlpha(0.5f);
        this.def = IsoSpriteInstance.get(this.sprite);
        this.def.alpha = 1.0f;
        this.sprite.def.alpha = 1.0f;
        this.offsetX = -26.0f;
        this.offsetY = -242.0f;
        switch (type.ordinal()) {
            case 0: {
                this.sprite.LoadFramesNoDirPageDirect("media/gibs/Giblet", "00", 3);
                break;
            }
            case 1: {
                this.sprite.LoadFramesNoDirPageDirect("media/gibs/Giblet", "01", 3);
            }
        }
    }

    public static enum GibletType {
        A,
        B,
        Eye;

    }
}

