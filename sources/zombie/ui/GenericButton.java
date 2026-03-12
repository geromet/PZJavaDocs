/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.Lua.LuaManager;
import zombie.core.textures.Texture;
import zombie.ui.UIElement;
import zombie.ui.UIEventHandler;
import zombie.ui.UIManager;

public final class GenericButton
extends UIElement {
    public boolean clicked;
    public UIElement messageTarget;
    public boolean mouseOver;
    public String name;
    public String text;
    Texture upTexture;
    Texture downTexture;
    private UIEventHandler messageTarget2;

    public GenericButton(UIElement messages, float x, float y, float width, float height, String name, String text, Texture upTex, Texture downTex) {
        this.x = x;
        this.y = y;
        this.messageTarget = messages;
        this.name = name;
        this.text = text;
        this.width = width;
        this.height = height;
        this.upTexture = upTex;
        this.downTexture = downTex;
    }

    public GenericButton(UIEventHandler messages, float x, float y, float width, float height, String name, String text, Texture upTex, Texture downTex) {
        this.x = x;
        this.y = y;
        this.messageTarget2 = messages;
        this.name = name;
        this.text = text;
        this.width = width;
        this.height = height;
        this.upTexture = upTex;
        this.downTexture = downTex;
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible().booleanValue()) {
            return Boolean.FALSE;
        }
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseDown") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseDown"), this.table, x, y);
        }
        this.clicked = true;
        return Boolean.TRUE;
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMove") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMove"), this.table, dx, dy);
        }
        this.mouseOver = true;
        return Boolean.TRUE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMoveOutside") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMoveOutside"), this.table, dx, dy);
        }
        this.clicked = false;
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
            } else {
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
            this.DrawTextureScaled(this.downTexture, 0.0, 0.0, this.getWidth(), this.getHeight(), 1.0);
            this.DrawTextCentre(this.text, this.getWidth() / 2.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        } else {
            this.DrawTextureScaled(this.upTexture, 0.0, 0.0, this.getWidth(), this.getHeight(), 1.0);
            this.DrawTextCentre(this.text, this.getWidth() / 2.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        }
        super.render();
    }
}

