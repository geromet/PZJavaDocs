/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import java.util.Arrays;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.combat.CombatConfig;
import zombie.combat.CombatConfigKey;
import zombie.core.math.PZMath;
import zombie.debug.BaseDebugWindow;
import zombie.debug.LineDrawer;
import zombie.debug.debugWindows.Wrappers;
import zombie.inventory.types.HandWeapon;

public class AimPlotter
extends BaseDebugWindow {
    private final CombatManager combatManager = CombatManager.getInstance();
    private CombatConfig combatConfig;
    private final ImVec2[] vec2 = new ImVec2[]{new ImVec2(), new ImVec2()};
    private static final int MAX_RECORDS = 1000;
    private final Float[] aimPenalty = new Float[1000];
    private final Float[] movePenalty = new Float[1000];
    private final Float[] recoilDelay = new Float[1000];
    private final Float[] attackAnim = new Float[1000];
    private final Float[] racking = new Float[1000];
    private final Float[] time = new Float[1000];
    private boolean showAimDelay = true;
    private boolean showMovePenalty = true;
    private boolean showRecoilDelay = true;
    private boolean showAttack = true;
    private boolean showRack = true;
    private float rangeScaleMax = 100.0f;
    private float rangeScaleMin = -100.0f;
    private final boolean proneTarget = false;
    private boolean showCurrent;
    private boolean showMoodles = true;
    private boolean showWeather = true;
    private final float lightLevel = 1.0f;
    private final float scale = 10.0f;

    @Override
    public String getTitle() {
        return this.getClass().getSimpleName();
    }

    public AimPlotter() {
        for (int i = 999; i >= 0; --i) {
            this.time[i] = Float.valueOf(i);
        }
        Arrays.fill((Object[])this.aimPenalty, Float.valueOf(0.0f));
        Arrays.fill((Object[])this.movePenalty, Float.valueOf(0.0f));
        Arrays.fill((Object[])this.recoilDelay, Float.valueOf(0.0f));
        Arrays.fill((Object[])this.attackAnim, Float.valueOf(0.0f));
        Arrays.fill((Object[])this.racking, Float.valueOf(0.0f));
    }

    private void update() {
        this.shiftLeft(this.aimPenalty);
        this.shiftLeft(this.movePenalty);
        this.shiftLeft(this.recoilDelay);
        this.shiftLeft(this.attackAnim);
        this.shiftLeft(this.racking);
        IsoPlayer owner = IsoPlayer.getInstance();
        if (owner != null) {
            this.aimPenalty[0] = Float.valueOf(PZMath.max(0.0f, owner.getAimingDelay()));
            this.movePenalty[0] = Float.valueOf(PZMath.max(0.0f, owner.getBeenMovingFor() * 0.5f - ((float)owner.getPerkLevel(PerkFactory.Perks.Aiming) * 1.5f + (float)owner.getPerkLevel(PerkFactory.Perks.Nimble) * 1.0f)));
            this.recoilDelay[0] = Float.valueOf(owner.getRecoilDelay());
            this.attackAnim[0] = Float.valueOf(owner.isPerformingAttackAnimation() ? 50.0f : 0.0f);
            this.racking[0] = Float.valueOf(owner.getVariableBoolean("isracking") ? 50.0f : 0.0f);
        } else {
            this.aimPenalty[0] = Float.valueOf(0.0f);
            this.movePenalty[0] = Float.valueOf(0.0f);
            this.recoilDelay[0] = Float.valueOf(0.0f);
            this.attackAnim[0] = Float.valueOf(0.0f);
            this.racking[0] = Float.valueOf(0.0f);
        }
    }

    @Override
    protected void doWindowContents() {
        this.combatConfig = this.combatManager.getCombatConfig();
        if (!GameTime.isGamePaused()) {
            this.update();
        }
        if (ImGui.beginTabBar("tabSelector")) {
            if (ImGui.beginTabItem("State")) {
                this.showAimDelay = Wrappers.checkbox("Aiming Delay", this.showAimDelay);
                ImGui.sameLine();
                this.showRecoilDelay = Wrappers.checkbox("Recoil Delay", this.showRecoilDelay);
                ImGui.sameLine();
                this.showMovePenalty = Wrappers.checkbox("Movement Penalty", this.showMovePenalty);
                ImGui.sameLine();
                this.showAttack = Wrappers.checkbox("Attack Anim", this.showAttack);
                ImGui.sameLine();
                this.showRack = Wrappers.checkbox("Racking", this.showRack);
                ImPlot.setNextPlotLimits(0.0, 1000.0, 0.0, 100.0, 1);
                if (ImPlot.beginPlot("Aiming Variables", "ticks (1000)", "Value")) {
                    if (this.showAimDelay) {
                        ImPlot.plotLine((String)"Aiming Delay", (Number[])this.time, (Number[])this.aimPenalty);
                    }
                    if (this.showRecoilDelay) {
                        ImPlot.plotLine((String)"Recoil Delay", (Number[])this.time, (Number[])this.recoilDelay);
                    }
                    if (this.showMovePenalty) {
                        ImPlot.plotLine((String)"Movement Penalty", (Number[])this.time, (Number[])this.movePenalty);
                    }
                    if (this.showAttack) {
                        ImPlot.plotLine((String)"Attack Anim", (Number[])this.time, (Number[])this.attackAnim);
                    }
                    if (this.showRack) {
                        ImPlot.plotLine((String)"Racking", (Number[])this.time, (Number[])this.racking);
                    }
                    ImPlot.endPlot();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Range")) {
                if (IsoPlayer.getInstance() != null) {
                    this.doRangePlot(IsoPlayer.getInstance(), IsoPlayer.getInstance().getUseHandWeapon());
                }
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
    }

    private void doRangePlot(IsoPlayer player, HandWeapon weapon) {
        this.showCurrent = Wrappers.checkbox("Current Values", this.showCurrent);
        ImGui.sameLine();
        ImGui.beginDisabled(!this.showCurrent);
        this.showMoodles = Wrappers.checkbox("Moodles", this.showMoodles);
        ImGui.sameLine();
        this.showWeather = Wrappers.checkbox("Weather", this.showWeather);
        ImGui.endDisabled();
        if (this.showCurrent) {
            this.rangeScaleMax = 200.0f;
            this.rangeScaleMin = 0.0f;
        } else {
            this.rangeScaleMax = 100.0f;
            this.rangeScaleMin = -100.0f;
        }
        if (player == null || weapon == null) {
            return;
        }
        int entries = ((int)Math.floor(weapon.getMaxRange()) + 3) * 10;
        Number[] values2 = new Float[entries];
        Number[] distance = new Float[entries];
        Number[] crit = new Float[entries];
        ImPlot.setNextPlotLimits(0.0, weapon.getMaxRange(), this.rangeScaleMin, this.rangeScaleMax, 1);
        if (ImPlot.beginPlot("Range vs HitChance", "Distance", "Chance")) {
            float pointBlankDistance = this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE);
            float max = weapon.getMaxSightRange(player);
            float min = weapon.getMinSightRange(player);
            if (this.showCurrent) {
                this.vec2[0] = ImPlot.plotToPixels(0.0, 100.0, 0);
                this.vec2[1] = ImPlot.plotToPixels(weapon.getMaxRange(), 100.0, 0);
                ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(0, 200, 0));
            } else {
                this.vec2[0] = ImPlot.plotToPixels(0.0, 0.0, 0);
                this.vec2[1] = ImPlot.plotToPixels(weapon.getMaxRange(), 0.0, 0);
                ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(200, 0, 0));
            }
            this.vec2[0] = ImPlot.plotToPixels(pointBlankDistance, this.rangeScaleMin, 0);
            this.vec2[1] = ImPlot.plotToPixels(pointBlankDistance, this.rangeScaleMax, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(0, 150, 150));
            LineDrawer.DrawIsoCircle(player.getX(), player.getY(), player.getZ(), pointBlankDistance, 32, 0.0f, 0.75f, 0.75f, 0.3f);
            this.vec2[0] = ImPlot.plotToPixels(min + (max - min) * 0.5f, this.rangeScaleMin, 0);
            this.vec2[1] = ImPlot.plotToPixels(min + (max - min) * 0.5f, this.rangeScaleMax, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(0, 200, 0));
            LineDrawer.DrawIsoCircle(player.getX(), player.getY(), player.getZ(), min + (max - min) * 0.5f, 32, 0.0f, 1.0f, 0.0f, 0.3f);
            this.vec2[0] = ImPlot.plotToPixels(min, this.rangeScaleMin, 0);
            this.vec2[1] = ImPlot.plotToPixels(min, this.rangeScaleMax, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(2000, 200, 0));
            LineDrawer.DrawIsoCircle(player.getX(), player.getY(), player.getZ(), min, 32, 1.0f, 1.0f, 0.0f, 0.3f);
            this.vec2[0] = ImPlot.plotToPixels(max, this.rangeScaleMin, 0);
            this.vec2[1] = ImPlot.plotToPixels(max, this.rangeScaleMax, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(200, 200, 0));
            LineDrawer.DrawIsoCircle(player.getX(), player.getY(), player.getZ(), max, 32, 1.0f, 1.0f, 0.0f, 0.3f);
            LineDrawer.DrawIsoCircle(player.getX(), player.getY(), player.getZ(), weapon.getMaxRange(), 32, 1.0f, 0.0f, 0.0f, 0.3f);
            float base = 0.0f;
            float critbase = 0.0f;
            float move = 0.0f;
            if (this.showCurrent) {
                base = weapon.getHitChance();
                critbase = weapon.getCriticalChance();
                if (base > 95.0f) {
                    base = 95.0f;
                }
                base += weapon.getAimingPerkHitChanceModifier() * (float)player.getPerkLevel(PerkFactory.Perks.Aiming);
                critbase += (float)(weapon.getAimingPerkCritModifier() * player.getPerkLevel(PerkFactory.Perks.Aiming));
            }
            for (int i = 0; i < entries; ++i) {
                float dist = (float)i / 10.0f;
                float value = base;
                float critv = critbase;
                float combinedPenalty = 0.0f;
                combinedPenalty += 100.0f - 100.0f / player.getWornItemsVisionModifier();
                if (dist <= weapon.getMaxRange()) {
                    value += PZMath.max(this.combatManager.getDistanceModifierSightless(dist, false) - (this.showCurrent ? this.combatManager.getAimDelayPenaltySightless(PZMath.max(0.0f, player.getAimingDelay()), dist) : 0.0f), this.combatManager.getDistanceModifier(dist, min, max, false) - (this.showCurrent ? this.combatManager.getAimDelayPenalty(PZMath.max(0.0f, player.getAimingDelay()), dist, min, max) : 0.0f));
                    critv += PZMath.max(this.combatManager.getDistanceModifierSightless(dist, false) - (this.showCurrent ? this.combatManager.getAimDelayPenaltySightless(PZMath.max(0.0f, player.getAimingDelay()), dist) : 0.0f), this.combatManager.getDistanceModifier(dist, min, max, false) - (this.showCurrent ? this.combatManager.getAimDelayPenalty(PZMath.max(0.0f, player.getAimingDelay()), dist, min, max) : 0.0f));
                }
                if (this.showCurrent) {
                    combinedPenalty += CombatManager.getMovePenalty(player, dist);
                    if (this.showWeather) {
                        combinedPenalty += this.combatManager.getWeatherPenalty(player, weapon, player.getSquare(), dist);
                    }
                    if (this.showMoodles) {
                        combinedPenalty += this.combatManager.getMoodlesPenalty(player, dist);
                    }
                }
                if (dist < pointBlankDistance) {
                    combinedPenalty *= dist / pointBlankDistance;
                }
                crit[i] = Float.valueOf(critv -= combinedPenalty);
                distance[i] = Float.valueOf(dist);
                values2[i] = Float.valueOf(value -= combinedPenalty);
                if (!this.showCurrent || i <= 0 || !(value <= 0.0f && ((Float)values2[i - 1]).floatValue() > 0.0f) && (!(value >= 0.0f) || !(((Float)values2[i - 1]).floatValue() < 0.0f))) continue;
                this.vec2[0] = ImPlot.plotToPixels(dist, this.rangeScaleMin, 0);
                this.vec2[1] = ImPlot.plotToPixels(dist, this.rangeScaleMax, 0);
                ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(200, 100, 0));
                LineDrawer.DrawIsoCircle(player.getX(), player.getY(), player.getZ(), dist, 32, 1.0f, 0.5f, 0.0f, 0.3f);
            }
            ImPlot.plotLine((String)"HitChance", (Number[])distance, (Number[])values2);
            ImPlot.plotLine((String)"Critical", (Number[])distance, (Number[])crit);
            ImPlot.endPlot();
        }
    }

    private <T> void shiftLeft(T[] array) {
        for (int i = array.length - 1; i > 0; --i) {
            array[i] = array[i - 1];
        }
    }

    private <T> void shiftRight(T[] array) {
        for (int i = 0; i < array.length - 1; ++i) {
            array[i] = array[i + 1];
        }
    }
}

