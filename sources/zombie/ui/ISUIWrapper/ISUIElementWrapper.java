/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.ui.ISUIWrapper.ISScrollBarWrapper;
import zombie.ui.ISUIWrapper.LuaHelpers;
import zombie.ui.UIElement;
import zombie.ui.UIManager;

public class ISUIElementWrapper {
    protected KahluaTable table;

    public ISUIElementWrapper(KahluaTable table) {
        this.table = table;
    }

    protected UIElement getJavaObject() {
        return (UIElement)this.table.rawget("javaObject");
    }

    public ISUIElementWrapper(double x, double y, double width, double height) {
        KahluaTable self = (KahluaTable)LuaManager.env.rawget("ISUIElement");
        this.table = LuaManager.platform.newTable();
        this.table.setMetatable(self);
        self.rawset("__index", (Object)self);
        this.table.rawset("x", (Object)x);
        this.table.rawset("y", (Object)y);
        this.table.rawset("width", (Object)width);
        this.table.rawset("height", (Object)height);
        this.table.rawset("anchorLeft", (Object)true);
        this.table.rawset("anchorRight", (Object)false);
        this.table.rawset("anchorTop", (Object)true);
        this.table.rawset("anchorBottom", (Object)false);
        this.table.rawset("dock", (Object)"none");
        this.table.rawset("minimumWidth", (Object)0.0);
        this.table.rawset("minimumHeight", (Object)0.0);
        this.table.rawset("scrollwidth", (Object)0.0);
        this.table.rawset("removed", (Object)false);
    }

    protected void instantiate() {
        UIElement javaObject = new UIElement(this.table);
        this.table.rawset("javaObject", (Object)javaObject);
        javaObject.setX(LuaHelpers.castDouble(this.table.rawget("x")));
        javaObject.setY(LuaHelpers.castDouble(this.table.rawget("y")));
        javaObject.setHeight(LuaHelpers.castDouble(this.table.rawget("height")));
        javaObject.setWidth(LuaHelpers.castDouble(this.table.rawget("width")));
        javaObject.setAnchorLeft(LuaHelpers.castBoolean(this.table.rawget("anchorLeft")));
        javaObject.setAnchorRight(LuaHelpers.castBoolean(this.table.rawget("anchorRight")));
        javaObject.setAnchorTop(LuaHelpers.castBoolean(this.table.rawget("anchorTop")));
        javaObject.setAnchorBottom(LuaHelpers.castBoolean(this.table.rawget("anchorBottom")));
        javaObject.setWantKeyEvents(LuaHelpers.castBoolean(this.table.rawget("wantKeyEvents")));
        javaObject.setWantExtraMouseEvents(LuaHelpers.castBoolean(this.table.rawget("wantExtraMouseEvents")));
        javaObject.setForceCursorVisible(LuaHelpers.castBoolean(this.table.rawget("forceCursorVisible")));
        LuaHelpers.callLuaClass(this.table.getMetatable().getString("Type"), "createChildren", this.table, new Object[0]);
    }

    public void initialise() {
        this.table.rawset("children", (Object)LuaManager.platform.newTable());
        KahluaTable uiElement = (KahluaTable)LuaManager.env.rawget("ISUIElement");
        double id = LuaHelpers.castDouble(uiElement.rawget("IDMax"));
        this.table.rawset("ID", (Object)id);
        uiElement.rawset("IDMax", (Object)(id + 1.0));
    }

    protected void addToUIManager() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
            javaObject = this.getJavaObject();
        }
        UIManager.AddUI(javaObject);
    }

    public KahluaTable getTable() {
        return this.table;
    }

    public void setVisible(boolean bVisible) {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
            javaObject = this.getJavaObject();
        }
        javaObject.setVisible(bVisible);
        Object visibleFunction = this.table.rawget("visibleFunction");
        Object visibleTarget = this.table.rawget("visibleTarget");
        if (visibleTarget != null && visibleFunction != null) {
            LuaManager.caller.protectedCall(LuaManager.thread, visibleFunction, visibleTarget, this.table);
        }
    }

    protected void bringToTop() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            return;
        }
        javaObject.bringToTop();
    }

    protected void setForceCursorVisible(boolean force) {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.table.rawset("forceCursorVisible", (Object)force);
        } else {
            this.table.rawset("forceCursorVisible", null);
            javaObject.setForceCursorVisible(force);
        }
    }

    public void setScrollHeight(double h) {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            return;
        }
        javaObject.setScrollHeight(h);
        this.updateScrollbars();
    }

    public void updateScrollbars() {
        UIElement javaObject;
        double y;
        KahluaTable vscrollTable = (KahluaTable)this.table.rawget("vscroll");
        if (vscrollTable != null) {
            ISScrollBarWrapper vscroll = new ISScrollBarWrapper(vscrollTable);
            vscroll.updatePos();
        }
        if (-(y = (javaObject = this.getJavaObject()).getYScroll().doubleValue()) > this.getScrollHeight() - this.getScrollAreaHeight()) {
            y = -(this.getScrollHeight() - this.getScrollAreaHeight());
        }
        if (-y < 0.0) {
            y = 0.0;
        }
        javaObject.setYScroll(y);
        KahluaTable hscrollTable = (KahluaTable)this.table.rawget("hscroll");
        if (hscrollTable != null) {
            ISScrollBarWrapper hscroll = new ISScrollBarWrapper(hscrollTable);
            hscroll.updatePos();
        }
    }

    public double getScrollHeight() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            return 0.0;
        }
        return javaObject.getScrollHeight();
    }

    public double getScrollAreaHeight() {
        KahluaTable hscrollTable = (KahluaTable)this.table.rawget("hscroll");
        if (hscrollTable != null) {
            ISScrollBarWrapper hscroll = new ISScrollBarWrapper(hscrollTable);
            return this.getHeight() - hscroll.getHeight();
        }
        return this.getHeight();
    }

    public double getHeight() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
        }
        return javaObject.getHeight();
    }

    public double getWidth() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
            javaObject = this.getJavaObject();
        }
        return javaObject.getWidth();
    }

    public void setHeight(double h) {
        UIElement javaObject;
        this.table.rawset("height", (Object)h);
        double y = LuaHelpers.castDouble(this.table.rawget("y"));
        if (this.getKeepOnScreen()) {
            double maxY = Core.getInstance().getScreenHeight();
            double height = LuaHelpers.castDouble(this.table.rawget("height"));
            y = Math.max(0.0, Math.min(y, maxY - height));
            this.table.rawset("y", (Object)y);
        }
        if ((javaObject = this.getJavaObject()) != null) {
            javaObject.setHeight(h);
            javaObject.setY(y);
        }
    }

    public void setWidth(double w) {
        UIElement javaObject;
        this.table.rawset("width", (Object)w);
        double x = LuaHelpers.castDouble(this.table.rawget("x"));
        if (this.getKeepOnScreen()) {
            double maxX = Core.getInstance().getScreenWidth();
            double width = LuaHelpers.castDouble(this.table.rawget("width"));
            x = Math.max(0.0, Math.min(x, maxX - width));
            this.table.rawset("x", (Object)x);
        }
        if ((javaObject = this.getJavaObject()) != null) {
            javaObject.setWidth(w);
            javaObject.setX(x);
        }
    }

    public void setX(double x) {
        double xs = x;
        if (this.getKeepOnScreen()) {
            double maxX = Core.getInstance().getScreenWidth();
            double width = LuaHelpers.castDouble(this.table.rawget("width"));
            xs = Math.max(0.0, Math.min(x, maxX - width));
        }
        this.table.rawset("x", (Object)xs);
        UIElement javaObject = this.getJavaObject();
        if (javaObject != null) {
            javaObject.setX(xs);
        }
    }

    public void setY(double y) {
        double ys = y;
        if (this.getKeepOnScreen()) {
            double maxY = Core.getInstance().getScreenHeight();
            double height = LuaHelpers.castDouble(this.table.rawget("height"));
            ys = Math.max(0.0, Math.min(y, maxY - height));
        }
        this.table.rawset("y", (Object)ys);
        UIElement javaObject = this.getJavaObject();
        if (javaObject != null) {
            javaObject.setY(ys);
        }
    }

    public double getX() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
            javaObject = this.getJavaObject();
        }
        return javaObject.getX();
    }

    public double getY() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
            javaObject = this.getJavaObject();
        }
        return javaObject.getY();
    }

    public double getYScroll() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
            javaObject = this.getJavaObject();
        }
        return javaObject.getYScroll();
    }

    public double getXScroll() {
        UIElement javaObject = this.getJavaObject();
        if (javaObject == null) {
            this.instantiate();
            javaObject = this.getJavaObject();
        }
        return javaObject.getXScroll();
    }

    public double getScrollWidth() {
        return LuaHelpers.castDouble(this.table.rawget("scrollwidth"));
    }

    public double getScrollAreaWidth() {
        if (this.isVScrollBarVisible()) {
            KahluaTable vscrollTable = (KahluaTable)this.table.rawget("vscroll");
            ISScrollBarWrapper vscroll = new ISScrollBarWrapper(vscrollTable);
            return this.getWidth() - vscroll.getWidth();
        }
        return this.getWidth();
    }

    public boolean isVScrollBarVisible() {
        KahluaTable vscrollTable = (KahluaTable)this.table.rawget("vscroll");
        if (vscrollTable != null) {
            ISScrollBarWrapper vscroll = new ISScrollBarWrapper(vscrollTable);
            return vscroll.getHeight() < this.getScrollHeight();
        }
        return false;
    }

    private boolean getKeepOnScreen() {
        Object keepOnScreen = this.table.rawget("keepOnScreen");
        if (keepOnScreen != null) {
            return LuaHelpers.castBoolean(keepOnScreen);
        }
        return this.table.rawget("parent") == null;
    }

    protected KahluaTable setRGBA(KahluaTable rgba, double r, double g, double b, double a) {
        rgba.rawset("r", (Object)r);
        rgba.rawset("g", (Object)g);
        rgba.rawset("b", (Object)b);
        rgba.rawset("a", (Object)a);
        return rgba;
    }
}

