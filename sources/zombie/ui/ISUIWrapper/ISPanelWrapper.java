/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.ui.ISUIWrapper.ISUIElementWrapper;

public class ISPanelWrapper
extends ISUIElementWrapper {
    public ISPanelWrapper(KahluaTable table) {
        super(table);
    }

    public ISPanelWrapper(double x, double y, double width, double height) {
        super(x, y, width, height);
        KahluaTable self = (KahluaTable)LuaManager.env.rawget("ISPanel");
        this.table.setMetatable(self);
        self.rawset("__index", (Object)self);
        this.table.rawset("x", (Object)x);
        this.table.rawset("y", (Object)y);
        this.table.rawset("background", (Object)true);
        this.table.rawset("backgroundColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 0.0, 0.0, 0.0, 0.5));
        this.table.rawset("borderColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 0.4, 0.4, 0.4, 1.0));
        this.table.rawset("width", (Object)width);
        this.table.rawset("height", (Object)height);
        this.table.rawset("anchorLeft", (Object)true);
        this.table.rawset("anchorRight", (Object)false);
        this.table.rawset("anchorTop", (Object)true);
        this.table.rawset("anchorBottom", (Object)false);
        this.table.rawset("moveWithMouse", (Object)false);
    }

    public void noBackground() {
        this.table.rawset("background", (Object)false);
    }
}

