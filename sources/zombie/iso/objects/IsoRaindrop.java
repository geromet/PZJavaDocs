/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;

public class IsoRaindrop
extends IsoObject {
    public int animSpriteIndex;
    public float gravMod;
    public int life;
    public float splashY;
    public float offsetY;
    public float velY;

    @Override
    public boolean Serialize() {
        return false;
    }

    public IsoRaindrop(IsoCell cell, IsoGridSquare gridSquare, boolean canSee) {
        if (!canSee) {
            return;
        }
        if (gridSquare == null) {
            return;
        }
        if (gridSquare.getProperties().has(IsoFlagType.HasRaindrop)) {
            return;
        }
        this.life = 0;
        this.square = gridSquare;
        int texWidth = 1 * Core.tileScale;
        int texHeight = 64 * Core.tileScale;
        float dx = Rand.Next(0.1f, 0.9f);
        float dy = Rand.Next(0.1f, 0.9f);
        short soffX = (short)(IsoUtils.XToScreen(dx, dy, 0.0f, 0) - (float)(texWidth / 2));
        short soffY = (short)(IsoUtils.YToScreen(dx, dy, 0.0f, 0) - (float)texHeight);
        this.offsetX = 0.0f;
        this.offsetY = RainManager.raindropStartDistance;
        this.splashY = soffY;
        this.AttachAnim("Rain", "00", 1, 0.0f, -soffX, -soffY, true, 0, false, 0.7f, RainManager.raindropTintMod);
        this.animSpriteIndex = this.attachedAnimSprite != null ? this.attachedAnimSprite.size() - 1 : 0;
        ((IsoSpriteInstance)this.attachedAnimSprite.get(this.animSpriteIndex)).setScale(Core.tileScale, Core.tileScale);
        gridSquare.getProperties().set(IsoFlagType.HasRaindrop);
        this.velY = 0.0f;
        float modulus = 1000000.0f / (float)Rand.Next(1000000) + 1.0E-5f;
        this.gravMod = -(RainManager.gravModMin + (RainManager.gravModMax - RainManager.gravModMin) * modulus);
        RainManager.AddRaindrop(this);
    }

    @Override
    public String getObjectName() {
        return "RainDrops";
    }

    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare passedObjectSquare) {
        return this.square == passedObjectSquare;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        return IsoObject.VisionResult.NoEffect;
    }

    public void ChangeTintMod(ColorInfo newTintMod) {
    }

    @Override
    public void update() {
        this.sy = 0.0f;
        this.sx = 0.0f;
        ++this.life;
        for (int n = 0; n < this.attachedAnimSprite.size(); ++n) {
            IsoSpriteInstance s = (IsoSpriteInstance)this.attachedAnimSprite.get(n);
            s.update();
            s.frame += s.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0f);
            IsoSprite sp = s.parentSprite;
            if ((int)s.frame < sp.currentAnim.frames.size() || !sp.loop || !s.looped) continue;
            s.frame = 0.0f;
        }
        this.velY += this.gravMod * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0f);
        this.offsetY += this.velY;
        if (this.attachedAnimSprite != null && this.attachedAnimSprite.size() > this.animSpriteIndex && this.animSpriteIndex >= 0) {
            ((IsoSpriteInstance)this.attachedAnimSprite.get((int)this.animSpriteIndex)).parentSprite.soffY = (short)(this.splashY + (float)((int)this.offsetY));
        }
        if (this.offsetY < 0.0f) {
            this.offsetY = RainManager.raindropStartDistance;
            this.velY = 0.0f;
            float modulus = 1000000.0f / (float)Rand.Next(1000000) + 1.0E-5f;
            this.gravMod = -(RainManager.gravModMin + (RainManager.gravModMax - RainManager.gravModMin) * modulus);
        }
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                this.setAlphaAndTarget(playerIndex, 0.55f);
                continue;
            }
            this.setAlphaAndTarget(playerIndex, 1.0f);
        }
    }

    void Reset(IsoGridSquare gridSquare, boolean canSee) {
        if (!canSee) {
            return;
        }
        if (gridSquare == null) {
            return;
        }
        if (gridSquare.getProperties().has(IsoFlagType.HasRaindrop)) {
            return;
        }
        this.life = 0;
        this.square = gridSquare;
        this.offsetY = RainManager.raindropStartDistance;
        this.animSpriteIndex = this.attachedAnimSprite != null ? this.attachedAnimSprite.size() - 1 : 0;
        gridSquare.getProperties().set(IsoFlagType.HasRaindrop);
        this.velY = 0.0f;
        float modulus = 1000000.0f / (float)Rand.Next(1000000) + 1.0E-5f;
        this.gravMod = -(RainManager.gravModMin + (RainManager.gravModMax - RainManager.gravModMin) * modulus);
        RainManager.AddRaindrop(this);
    }
}

