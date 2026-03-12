/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import zombie.debug.debugWindows.PZDebugWindow;
import zombie.debug.debugWindows.PZImGui;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.scripting.objects.AmmoType;

public class TracerEffectsDebugWindow
extends PZDebugWindow {
    @Override
    public String getTitle() {
        return "Tracer Effects Editor";
    }

    @Override
    protected void doWindowContents() {
        ImGui.begin(this.getTitle());
        for (AmmoType ammoType : IsoBulletTracerEffects.getInstance().getIsoBulletTracerEffectsConfigOptionsHashMap().keySet()) {
            boolean isDirty = false;
            IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions isoBulletTracerEffectsConfigOption = IsoBulletTracerEffects.getInstance().getIsoBulletTracerEffectsConfigOptionsHashMap().get(ammoType);
            int optionCount = isoBulletTracerEffectsConfigOption.getOptionCount();
            if (PZImGui.collapsingHeader(ammoType.getTranslationName())) {
                ImGui.beginChild(ammoType.getTranslationName());
                for (int i = 0; i < optionCount; ++i) {
                    IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption configOption = (IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption)isoBulletTracerEffectsConfigOption.getOptionByIndex(i);
                    float value = PZImGui.sliderFloat(configOption.getName(), (float)configOption.getValue(), (float)configOption.getMin(), (float)configOption.getMax());
                    if ((double)value == configOption.getValue()) continue;
                    configOption.setValue(value);
                    isDirty = true;
                }
                if (PZImGui.button("Reset To Default")) {
                    IsoBulletTracerEffects.getInstance().reset(ammoType);
                    isDirty = true;
                }
                ImGui.endChild();
            }
            if (!isDirty) continue;
            IsoBulletTracerEffects.getInstance().save(ammoType);
        }
        ImGui.end();
    }
}

