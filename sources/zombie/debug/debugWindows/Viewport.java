/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import zombie.core.Core;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.input.Mouse;

public class Viewport
extends BaseDebugWindow {
    private float viewWidth;
    private float viewHeight;
    private float highlightX1;
    private float highlightY1;
    private float highlightX2;
    private float highlightY2;
    private int highlightCol;

    @Override
    public boolean doFrameStartTick() {
        return true;
    }

    @Override
    public String getTitle() {
        return "Viewport";
    }

    public float transformXToGame(float x) {
        x -= this.contentMin.x;
        x -= this.x;
        x /= this.viewWidth;
        return x *= (float)Core.width;
    }

    public float transformXToWindow(float x) {
        x /= (float)Core.width;
        x *= this.viewWidth;
        x += this.x;
        return x += this.contentMin.x;
    }

    public float transformYToGame(float y) {
        y -= this.contentMin.y;
        y -= this.y;
        y /= this.viewHeight;
        return y *= (float)Core.height;
    }

    public float transformYToWindow(float y) {
        y /= (float)Core.height;
        y *= this.viewHeight;
        y += this.y;
        return y += this.contentMin.y;
    }

    @Override
    protected void doWindowContents() {
        float xRatio = (float)DebugContext.instance.debugViewportTexture.getWidth() / (float)DebugContext.instance.debugViewportTexture.getTexture().getWidthHW();
        float yRatio = (float)DebugContext.instance.debugViewportTexture.getHeight() / (float)DebugContext.instance.debugViewportTexture.getTexture().getHeightHW();
        float aspectRatio = (float)DebugContext.instance.debugViewportTexture.getHeight() / (float)DebugContext.instance.debugViewportTexture.getWidth();
        float width = (ImGui.getWindowHeight() - this.contentMin.y) / aspectRatio;
        float height = ImGui.getWindowHeight() - this.contentMin.y;
        width -= 10.0f;
        height -= 10.0f;
        if (width > ImGui.getWindowWidth()) {
            width = ImGui.getWindowWidth() - this.contentMin.x;
            height = (width -= 10.0f) * aspectRatio;
        }
        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        ImGui.getCurrentDrawList().addRectFilled(x, y, x + ImGui.getWindowWidth(), y + ImGui.getWindowWidth(), ImGui.colorConvertFloat4ToU32(0.0f, 0.0f, 0.0f, 1.0f));
        ImGui.image(DebugContext.instance.debugViewportTexture.getTexture().getID(), width, height, 0.0f, yRatio, xRatio, 0.0f);
        DebugContext.instance.focusedGameViewport = ImGui.isWindowFocused();
        this.viewWidth = width;
        this.viewHeight = height;
        if (DebugContext.instance.focusedGameViewport) {
            float viewportMouseX = ImGui.getMousePosX();
            float viewportMouseY = ImGui.getMousePosY();
            viewportMouseX -= this.contentMin.x;
            viewportMouseY -= this.contentMin.y;
            viewportMouseX -= ImGui.getWindowPosX();
            viewportMouseY -= ImGui.getWindowPosY();
            viewportMouseX /= width;
            viewportMouseY /= height;
            DebugContext.instance.setViewportX(viewportMouseX *= (float)Core.width);
            DebugContext.instance.setViewportY(viewportMouseY *= (float)Core.height);
        } else {
            DebugContext.instance.setViewportX(-1.0f);
            DebugContext.instance.setViewportY(-1.0f);
        }
        if (DebugContext.instance.focusedGameViewport) {
            if (Mouse.isButtonDown(1)) {
                ImGui.setMouseCursor(-1);
            } else {
                ImGui.setMouseCursor(0);
            }
        }
        ImGui.getCurrentDrawList().addRect(this.highlightX1, this.highlightY1, this.highlightX2, this.highlightY2, this.highlightCol, 0.0f, 0, 3.0f);
    }

    public void highlight(float x, float y, float w, float h, int col) {
        float x1 = this.transformXToWindow(x);
        float x2 = this.transformXToWindow(x + w);
        float y1 = this.transformYToWindow(y);
        float y2 = this.transformYToWindow(y + h);
        this.highlightX1 = x1;
        this.highlightX2 = x2;
        this.highlightY1 = y1;
        this.highlightY2 = y2;
        this.highlightCol = col;
    }
}

