/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.ArrayList;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.ui.UIElement;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;

public class UIPanel
extends BaseDebugWindow {
    private float selectedUiX;
    private float selectedUiY;
    private float selectedUiWidth;
    private float selectedUiHeight;
    private String selectedNode = "";

    @Override
    public String getTitle() {
        return "UI";
    }

    private void doUITree(UIElement element) {
        String name = element.getUIName();
        int flags = 192;
        if (element.controls.isEmpty()) {
            flags |= 0x100;
        }
        if (String.valueOf(element.hashCode()).equalsIgnoreCase(this.selectedNode)) {
            flags |= 1;
            if (element.isVisible().booleanValue()) {
                this.selectedUiX = element.getAbsoluteX().floatValue();
                this.selectedUiY = element.getAbsoluteY().floatValue();
                this.selectedUiWidth = element.getWidth().floatValue();
                this.selectedUiHeight = element.getHeight().floatValue();
            } else {
                this.selectedUiX = 0.0f;
                this.selectedUiY = 0.0f;
                this.selectedUiWidth = 0.0f;
                this.selectedUiHeight = 0.0f;
            }
            DebugContext.instance.viewport.highlight(this.selectedUiX, this.selectedUiY, this.selectedUiWidth, this.selectedUiHeight, ImGui.colorConvertFloat4ToU32(1.0f, 0.0f, 0.0f, 1.0f));
        }
        if (!element.isVisible().booleanValue()) {
            ImGui.pushStyleColor(0, ImGui.colorConvertFloat4ToU32(0.4f, 0.4f, 0.4f, 1.0f));
        }
        if (ImGui.treeNodeEx(String.valueOf(element.hashCode()), flags, name)) {
            if (ImGui.isItemClicked()) {
                this.selectedNode = String.valueOf(element.hashCode());
            }
            ArrayList<UIElement> controls = element.getControls();
            for (int i = 0; i < controls.size(); ++i) {
                UIElement control = controls.get(i);
                this.doUITree(control);
            }
            ImGui.treePop();
        } else if (ImGui.isItemClicked()) {
            this.selectedNode = String.valueOf(element.hashCode());
        }
        if (!element.isVisible().booleanValue()) {
            ImGui.popStyleColor();
        }
    }

    @Override
    protected void doWindowContents() {
        ArrayList<UIElementInterface> elements = UIManager.getUI();
        for (int i = 0; i < elements.size(); ++i) {
            UIElementInterface element = elements.get(i);
            if (!(element instanceof UIElement)) continue;
            UIElement uiElement = (UIElement)element;
            this.doUITree(uiElement);
        }
    }
}

