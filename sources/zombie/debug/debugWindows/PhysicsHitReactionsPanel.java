/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImFloat;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.physics.RagdollBodyPart;
import zombie.debug.debugWindows.PZDebugWindow;
import zombie.debug.debugWindows.PZImGui;
import zombie.inventory.types.HandWeapon;
import zombie.scripting.objects.AmmoType;
import zombie.scripting.objects.PhysicsHitReaction;
import zombie.scripting.objects.PhysicsHitReactionScript;
import zombie.util.StringUtils;

public class PhysicsHitReactionsPanel
extends PZDebugWindow {
    private HandWeapon previousHandWeapon;
    private final ImFloat maxForwardImpulse = new ImFloat(200.0f);
    private final ImFloat maxUpwardImpulse = new ImFloat(200.0f);

    @Override
    public String getTitle() {
        return "Physics Hit Reactions";
    }

    @Override
    protected void doWindowContents() {
        HandWeapon currentHandWeapon;
        boolean weaponChanged = false;
        IsoPlayer player = IsoPlayer.players[0];
        if (player != null && this.previousHandWeapon != (currentHandWeapon = player.getUseHandWeapon())) {
            this.previousHandWeapon = currentHandWeapon;
            if (this.previousHandWeapon != null) {
                weaponChanged = true;
            }
        }
        ImGui.beginChild("Begin");
        if (PZImGui.button("Write All To File")) {
            PhysicsHitReactionScript.writeToFile();
        }
        if (ImGui.inputFloat("Max Forward Impulse", this.maxForwardImpulse, 0.1f, 1.0f, "%0.2f") && this.maxForwardImpulse.floatValue() <= 0.0f) {
            this.maxForwardImpulse.set(1.0f);
        }
        if (ImGui.inputFloat("Max Upward Impulse", this.maxUpwardImpulse, 0.1f, 1.0f, "%0.2f") && this.maxUpwardImpulse.floatValue() <= 0.0f) {
            this.maxUpwardImpulse.set(1.0f);
        }
        if (ImGui.beginTabBar("tabSelector")) {
            for (PhysicsHitReaction physicsHitReaction : PhysicsHitReactionScript.physicsHitReactionList) {
                String translationName;
                AmmoType ammoType;
                boolean tabSelected = false;
                AmmoType ammoType2 = ammoType = this.previousHandWeapon != null ? this.previousHandWeapon.getAmmoType() : null;
                if (weaponChanged && physicsHitReaction.ammoType == this.previousHandWeapon.getAmmoType()) {
                    tabSelected = true;
                }
                if (!ImGui.beginTabItem(translationName = physicsHitReaction.ammoType != null ? physicsHitReaction.ammoType.getTranslationName() : StringUtils.stripModule(physicsHitReaction.physicsObject), tabSelected ? 2 : 0)) continue;
                this.physicsHitReactionTab(physicsHitReaction);
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
        ImGui.endChild();
    }

    private void physicsHitReactionTab(PhysicsHitReaction physicsHitReaction) {
        String translationName = physicsHitReaction.ammoType != null ? physicsHitReaction.ammoType.getTranslationName() : StringUtils.stripModule(physicsHitReaction.physicsObject);
        ImGui.beginChild(translationName);
        physicsHitReaction.useImpulseOverride = PZImGui.checkbox("Use Override", physicsHitReaction.useImpulseOverride);
        if (physicsHitReaction.useImpulseOverride) {
            physicsHitReaction.overrideForwardImpulse = PZImGui.sliderFloat("Forward Impulse Override", physicsHitReaction.overrideForwardImpulse, 0.0f, this.maxForwardImpulse.floatValue());
            physicsHitReaction.overrideUpwardImpulse = PZImGui.sliderFloat("Upward Impulse Override", physicsHitReaction.overrideUpwardImpulse, 0.0f, this.maxUpwardImpulse.floatValue());
        }
        for (int i = 0; i < physicsHitReaction.impulse.length; ++i) {
            if (!physicsHitReaction.useImpulseOverride) {
                physicsHitReaction.impulse[i] = PZImGui.sliderFloat(Translator.getText(RagdollBodyPart.values()[i].name()) + " Forward Impulse", physicsHitReaction.impulse[i], 0.0f, this.maxForwardImpulse.floatValue());
                physicsHitReaction.upwardImpulse[i] = PZImGui.sliderFloat(Translator.getText(RagdollBodyPart.values()[i].name()) + " Upward Impulse", physicsHitReaction.upwardImpulse[i], 0.0f, this.maxUpwardImpulse.floatValue());
                continue;
            }
            physicsHitReaction.impulse[i] = physicsHitReaction.overrideUpwardImpulse;
            physicsHitReaction.upwardImpulse[i] = physicsHitReaction.overrideForwardImpulse;
        }
        ImGui.endChild();
    }
}

