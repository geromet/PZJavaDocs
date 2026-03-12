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

public class IsoRainSplash
extends IsoObject {
    public int age;

    @Override
    public boolean Serialize() {
        return false;
    }

    public IsoRainSplash(IsoCell cell, IsoGridSquare gridSquare) {
        if (gridSquare == null) {
            return;
        }
        if (gridSquare.getProperties().has(IsoFlagType.HasRainSplashes)) {
            return;
        }
        this.age = 0;
        this.square = gridSquare;
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
        int numRainSplashParticles = 1 + Rand.Next(2);
        int texWidth = 16;
        int texHeight = 8;
        for (int i = 0; i < numRainSplashParticles; ++i) {
            float dx = Rand.Next(0.1f, 0.9f);
            float dy = Rand.Next(0.1f, 0.9f);
            short soffX = (short)(IsoUtils.XToScreen(dx, dy, 0.0f, 0) - 8.0f);
            short soffY = (short)(IsoUtils.YToScreen(dx, dy, 0.0f, 0) - 4.0f);
            this.AttachAnim("RainSplash", "00", 4, RainManager.rainSplashAnimDelay, -soffX, -soffY, true, 0, false, 0.7f, RainManager.rainSplashTintMod);
            ((IsoSpriteInstance)this.attachedAnimSprite.get((int)i)).frame = (short)Rand.Next(4);
            ((IsoSpriteInstance)this.attachedAnimSprite.get(i)).setScale(Core.tileScale, Core.tileScale);
        }
        gridSquare.getProperties().set(IsoFlagType.HasRainSplashes);
        RainManager.AddRainSplash(this);
    }

    @Override
    public String getObjectName() {
        return "RainSplashes";
    }

    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare passedObjectSquare) {
        return this.square == passedObjectSquare;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        return IsoObject.VisionResult.NoEffect;
    }

    public void ChangeTintMod(ColorInfo newTintMod) {
        if (this.attachedAnimSprite != null) {
            for (int i = 0; i < this.attachedAnimSprite.size(); ++i) {
            }
        }
    }

    @Override
    public void update() {
        this.sy = 0.0f;
        this.sx = 0.0f;
        ++this.age;
        for (int n = 0; n < this.attachedAnimSprite.size(); ++n) {
            IsoSpriteInstance s = (IsoSpriteInstance)this.attachedAnimSprite.get(n);
            IsoSprite sp = s.parentSprite;
            s.update();
            s.frame += s.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0f);
            if ((int)s.frame < sp.currentAnim.frames.size() || !sp.loop || !s.looped) continue;
            s.frame = 0.0f;
        }
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                this.setAlphaAndTarget(playerIndex, 0.25f);
                continue;
            }
            this.setAlphaAndTarget(playerIndex, 0.6f);
        }
    }

    void Reset(IsoGridSquare gridSquare) {
        if (gridSquare == null) {
            return;
        }
        if (gridSquare.getProperties().has(IsoFlagType.HasRainSplashes)) {
            return;
        }
        this.age = 0;
        this.square = gridSquare;
        int numRainSplashParticles = 1 + Rand.Next(2);
        if (this.attachedAnimSprite != null) {
            for (int i = 0; i < this.attachedAnimSprite.size(); ++i) {
            }
        }
        gridSquare.getProperties().set(IsoFlagType.HasRainSplashes);
        RainManager.AddRainSplash(this);
    }
}

