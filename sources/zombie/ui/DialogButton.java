/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.Lua.LuaManager;
import zombie.core.Color;
import zombie.core.textures.Texture;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIEventHandler;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

public final class DialogButton
extends UIElement {
    public boolean clicked;
    public UIElement messageTarget;
    public boolean mouseOver;
    public String name;
    public String text;
    Texture downLeft;
    Texture downMid;
    Texture downRight;
    float origX;
    Texture upLeft;
    Texture upMid;
    Texture upRight;
    private UIEventHandler messageTarget2;

    public DialogButton(UIElement messages, float x, float y, String text, String name) {
        this.x = x;
        this.y = y;
        this.origX = x;
        this.messageTarget = messages;
        this.upLeft = Texture.getSharedTexture("ButtonL_Up");
        this.upMid = Texture.getSharedTexture("ButtonM_Up");
        this.upRight = Texture.getSharedTexture("ButtonR_Up");
        this.downLeft = Texture.getSharedTexture("ButtonL_Down");
        this.downMid = Texture.getSharedTexture("ButtonM_Down");
        this.downRight = Texture.getSharedTexture("ButtonR_Down");
        this.name = name;
        this.text = text;
        this.width = TextManager.instance.MeasureStringX(UIFont.Small, text);
        this.width += 8.0f;
        if (this.width < 40.0f) {
            this.width = 40.0f;
        }
        this.height = this.downMid.getHeight();
        this.x -= (double)(this.width / 2.0f);
    }

    public DialogButton(UIEventHandler messages, int x, int y, String text, String name) {
        this.x = x;
        this.y = y;
        this.origX = x;
        this.messageTarget2 = messages;
        this.upLeft = Texture.getSharedTexture("ButtonL_Up");
        this.upMid = Texture.getSharedTexture("ButtonM_Up");
        this.upRight = Texture.getSharedTexture("ButtonR_Up");
        this.downLeft = Texture.getSharedTexture("ButtonL_Down");
        this.downMid = Texture.getSharedTexture("ButtonM_Down");
        this.downRight = Texture.getSharedTexture("ButtonR_Down");
        this.name = name;
        this.text = text;
        this.width = TextManager.instance.MeasureStringX(UIFont.Small, text);
        this.width += 8.0f;
        if (this.width < 40.0f) {
            this.width = 40.0f;
        }
        this.height = this.downMid.getHeight();
        this.x -= (double)(this.width / 2.0f);
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible().booleanValue()) {
            return false;
        }
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseDown") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseDown"), this.table, x, y);
        }
        this.clicked = true;
        return Boolean.TRUE;
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        this.mouseOver = true;
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMove") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMove"), this.table, dx, dy);
        }
        return Boolean.TRUE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.clicked = false;
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMoveOutside") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMoveOutside"), this.table, dx, dy);
        }
        this.mouseOver = false;
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseUp") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseUp"), this.table, x, y);
        }
        if (this.clicked) {
            if (this.messageTarget2 != null) {
                this.messageTarget2.Selected(this.name, 0, 0);
            } else if (this.messageTarget != null) {
                this.messageTarget.ButtonClicked(this.name);
            }
        }
        this.clicked = false;
        return Boolean.TRUE;
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        boolean dy = false;
        if (this.clicked) {
            this.DrawTexture(this.downLeft, 0.0, 0.0, 1.0);
            this.DrawTextureScaledCol(this.downMid, this.downLeft.getWidth(), 0.0, (int)(this.getWidth() - (double)(this.downLeft.getWidth() * 2)), this.downLeft.getHeight(), new Color(255, 255, 255, 255));
            this.DrawTexture(this.downRight, (int)(this.getWidth() - (double)this.downRight.getWidth()), 0.0, 1.0);
            this.DrawTextCentre(this.text, this.getWidth() / 2.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        } else {
            this.DrawTexture(this.upLeft, 0.0, 0.0, 1.0);
            this.DrawTextureScaledCol(this.upMid, this.downLeft.getWidth(), 0.0, (int)(this.getWidth() - (double)(this.downLeft.getWidth() * 2)), this.downLeft.getHeight(), new Color(255, 255, 255, 255));
            this.DrawTexture(this.upRight, (int)(this.getWidth() - (double)this.downRight.getWidth()), 0.0, 1.0);
            this.DrawTextCentre(this.text, this.getWidth() / 2.0, 0.0, 1.0, 1.0, 1.0, 1.0);
        }
        super.render();
    }
}

