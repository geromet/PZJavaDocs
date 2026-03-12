/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

public interface UIElementInterface {
    public Boolean isIgnoreLossControl();

    public Boolean isFollowGameWorld();

    public Boolean isDefaultDraw();

    public void render();

    public Boolean isVisible();

    public Boolean isCapture();

    public boolean isModalVisible();

    public Double getMaxDrawHeight();

    public Double getX();

    public Double getY();

    public Double getWidth();

    public Double getHeight();

    public boolean isOverElement(double var1, double var3);

    public UIElementInterface getParent();

    public boolean onConsumeMouseButtonDown(int var1, double var2, double var4);

    public boolean onConsumeMouseButtonUp(int var1, double var2, double var4);

    public void onMouseButtonDownOutside(int var1, double var2, double var4);

    public void onMouseButtonUpOutside(int var1, double var2, double var4);

    public Boolean onConsumeMouseWheel(double var1, double var3, double var5);

    public Boolean isPointOver(double var1, double var3);

    public Boolean onConsumeMouseMove(double var1, double var3, double var5, double var7);

    public void onExtendMouseMoveOutside(double var1, double var3, double var5, double var7);

    public void update();

    public Boolean isMouseOver();

    public boolean isWantKeyEvents();

    public boolean onConsumeKeyPress(int var1);

    public boolean onConsumeKeyRepeat(int var1);

    public boolean onConsumeKeyRelease(int var1);

    public boolean isForceCursorVisible();

    public int getRenderThisPlayerOnly();

    public boolean isAlwaysOnTop();

    public boolean isBackMost();
}

