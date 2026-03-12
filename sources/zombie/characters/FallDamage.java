/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.FallSeverity;
import zombie.characters.FallingConstants;
import zombie.characters.IsoGameCharacter;

public class FallDamage {
    private float impactIsoSpeed;
    private boolean isDamagingFall;
    private FallSeverity impactFallSeverity = FallSeverity.None;

    public void registerVariableCallbacks(IsoGameCharacter owner) {
        owner.setVariable("bLandLight", this::isDamagingFall, iAnimationVariableSource -> "Character has landed lightly.");
        owner.setVariable("bLandLightMask", () -> this.isLightFall() && this.isDamagingFall(), iAnimationVariableSource -> "Character has landed lightly, with some damage incurred.");
        owner.setVariable("bHardFall", this::isHardFall, iAnimationVariableSource -> "Character has landed hard, with damage incurred.");
        owner.setVariable("bHardFall2", this::isMoreThanHardFall, iAnimationVariableSource -> "Character has had a severe or fatal fall, with severe or fatal damage likely incurred.");
        owner.setVariable("fallImpactSeverity", FallSeverity.class, this::getFallImpactSeverity, iAnimationVariableSource -> "Character has impacted the ground, with the specified FallSeverity.");
    }

    public void registerDebugGameVariables(IsoGameCharacter owner) {
        owner.setVariable("dbg.fallImpactIsoSpeed", this::getImpactIsoSpeed, iAnimationVariableSource -> "Character has impacted the ground at this IsoSpeed.");
    }

    public void reset() {
        this.impactIsoSpeed = 0.0f;
        this.isDamagingFall = false;
        this.impactFallSeverity = FallSeverity.None;
    }

    public void setLandingImpact(float impactIsoSpeed) {
        this.impactIsoSpeed = impactIsoSpeed;
        this.isDamagingFall = FallingConstants.isDamagingFall(impactIsoSpeed);
        this.impactFallSeverity = FallingConstants.getFallSeverity(impactIsoSpeed);
    }

    public boolean isFall() {
        return this.impactFallSeverity != FallSeverity.None;
    }

    public boolean isDamagingFall() {
        return this.isDamagingFall;
    }

    public boolean isLightFall() {
        return this.impactFallSeverity == FallSeverity.Light;
    }

    public boolean isMoreThanLightFall() {
        return this.impactFallSeverity.ordinal() > FallSeverity.Light.ordinal();
    }

    public boolean isHardFall() {
        return this.impactFallSeverity == FallSeverity.Hard;
    }

    public boolean isMoreThanHardFall() {
        return this.impactFallSeverity.ordinal() > FallSeverity.Hard.ordinal();
    }

    public boolean isSevereFall() {
        return this.impactFallSeverity == FallSeverity.Severe;
    }

    public boolean isLethalFall() {
        return this.impactFallSeverity == FallSeverity.Lethal;
    }

    public float getImpactIsoSpeed() {
        return this.impactIsoSpeed;
    }

    public FallSeverity getFallImpactSeverity() {
        return this.impactFallSeverity;
    }
}

