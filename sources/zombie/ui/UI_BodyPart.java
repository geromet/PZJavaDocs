/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.textures.Texture;
import zombie.network.GameClient;
import zombie.ui.UIElement;

public final class UI_BodyPart
extends UIElement {
    public float alpha = 1.0f;
    public final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    public BodyPartType bodyPartType;
    public boolean isFlipped;
    public float maxOscilatorRate = 0.58f;
    public float minOscilatorRate = 0.025f;
    public float oscilator;
    public float oscilatorRate = 0.02f;
    public float oscilatorStep;
    IsoGameCharacter chr;
    boolean mouseOver;
    Texture scratchTex;
    Texture bandageTex;
    Texture dirtyBandageTex;
    Texture infectionTex;
    Texture deepWoundTex;
    Texture stitchTex;
    Texture biteTex;
    Texture glassTex;
    Texture boneTex;
    Texture splintTex;
    Texture burnTex;
    Texture bulletTex;

    public UI_BodyPart(BodyPartType type, int x, int y, String part, IsoGameCharacter character, boolean renderFlipped) {
        String sex = "male";
        if (character.isFemale()) {
            sex = "female";
        }
        this.chr = character;
        this.bodyPartType = type;
        this.scratchTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_scratch_" + part);
        this.bandageTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bandage_" + part);
        this.dirtyBandageTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bandagedirty_" + part);
        this.infectionTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_infection_" + part);
        this.biteTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bite_" + part);
        this.deepWoundTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_deepwound_" + part);
        this.stitchTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_stitches_" + part);
        this.glassTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_glass_" + part);
        this.boneTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bones_" + part);
        this.splintTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_splint_" + part);
        this.burnTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_burn_" + part);
        this.bulletTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bullet_" + part);
        this.x = x;
        this.y = y;
        this.width = this.scratchTex.getWidth();
        this.height = this.scratchTex.getHeight();
        this.isFlipped = renderFlipped;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.mouseOver = false;
    }

    @Override
    public void render() {
        IsoPlayer isoPlayer;
        IsoGameCharacter isoGameCharacter;
        BodyDamage bodyDamage = this.chr.getBodyDamage();
        if (GameClient.client && (isoGameCharacter = this.chr) instanceof IsoPlayer && !(isoPlayer = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
            bodyDamage = this.chr.getBodyDamageRemote();
        }
        if (this.infectionTex != null && !bodyDamage.IsBandaged(this.bodyPartType) && bodyDamage.getBodyPart(this.bodyPartType).getWoundInfectionLevel() > 0.0f) {
            this.DrawTexture(this.infectionTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getWoundInfectionLevel() / 10.0f);
        }
        if (this.bandageTex != null && bodyDamage.IsBandaged(this.bodyPartType) && bodyDamage.getBodyPart(this.bodyPartType).getBandageLife() > 0.0f) {
            this.DrawTexture(this.bandageTex, 0.0, 0.0, 1.0);
        } else if (this.dirtyBandageTex != null && bodyDamage.IsBandaged(this.bodyPartType) && bodyDamage.getBodyPart(this.bodyPartType).getBandageLife() <= 0.0f) {
            this.DrawTexture(this.dirtyBandageTex, 0.0, 0.0, 1.0);
        } else if (this.scratchTex != null && bodyDamage.IsScratched(this.bodyPartType)) {
            this.DrawTexture(this.scratchTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getScratchTime() / 20.0f);
        } else if (this.scratchTex != null && bodyDamage.IsCut(this.bodyPartType)) {
            this.DrawTexture(this.scratchTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getCutTime() / 20.0f);
        } else if (this.biteTex != null && !bodyDamage.IsBandaged(this.bodyPartType) && bodyDamage.IsBitten(this.bodyPartType) && bodyDamage.getBodyPart(this.bodyPartType).getBiteTime() >= 0.0f) {
            this.DrawTexture(this.biteTex, 0.0, 0.0, 1.0);
        } else if (this.deepWoundTex != null && bodyDamage.IsDeepWounded(this.bodyPartType)) {
            this.DrawTexture(this.deepWoundTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getDeepWoundTime() / 15.0f);
        } else if (this.stitchTex != null && bodyDamage.IsStitched(this.bodyPartType)) {
            this.DrawTexture(this.stitchTex, 0.0, 0.0, 1.0);
        }
        if (this.boneTex != null && bodyDamage.getBodyPart(this.bodyPartType).getFractureTime() > 0.0f && bodyDamage.getBodyPart(this.bodyPartType).getSplintFactor() == 0.0f) {
            this.DrawTexture(this.boneTex, 0.0, 0.0, 1.0);
        } else if (this.splintTex != null && bodyDamage.getBodyPart(this.bodyPartType).getSplintFactor() > 0.0f) {
            this.DrawTexture(this.splintTex, 0.0, 0.0, 1.0);
        }
        if (this.glassTex != null && bodyDamage.getBodyPart(this.bodyPartType).haveGlass() && !bodyDamage.getBodyPart(this.bodyPartType).bandaged()) {
            this.DrawTexture(this.glassTex, 0.0, 0.0, 1.0);
        }
        if (this.bulletTex != null && bodyDamage.getBodyPart(this.bodyPartType).haveBullet() && !bodyDamage.getBodyPart(this.bodyPartType).bandaged()) {
            this.DrawTexture(this.bulletTex, 0.0, 0.0, 1.0);
        }
        if (this.burnTex != null && bodyDamage.getBodyPart(this.bodyPartType).getBurnTime() > 0.0f && !bodyDamage.getBodyPart(this.bodyPartType).bandaged()) {
            this.DrawTexture(this.burnTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getBurnTime() / 100.0f);
        }
        super.render();
    }
}

