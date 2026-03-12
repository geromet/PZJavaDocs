/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.ui.ISUIWrapper.ISPanelWrapper;
import zombie.ui.ISUIWrapper.LuaHelpers;

public class ISToolTipWrapper
extends ISPanelWrapper {
    public ISToolTipWrapper(KahluaTable table) {
        super(table);
    }

    public ISToolTipWrapper() {
        super(0.0, 0.0, 0.0, 0.0);
        KahluaTable self = (KahluaTable)LuaManager.env.rawget("ISToolTip");
        this.table.setMetatable(self);
        self.rawset("__index", (Object)self);
        this.noBackground();
        this.table.rawset("name", null);
        this.table.rawset("description", (Object)"");
        this.table.rawset("borderColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 0.4, 0.4, 0.4, 1.0));
        this.table.rawset("backgroundColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 0.0, 0.0, 0.0, 0.0));
        this.table.rawset("width", (Object)0.0);
        this.table.rawset("height", (Object)0.0);
        this.table.rawset("anchorLeft", (Object)true);
        this.table.rawset("anchorRight", (Object)false);
        this.table.rawset("anchorTop", (Object)true);
        this.table.rawset("anchorBottom", (Object)false);
        KahluaTable descriptionPanel = (KahluaTable)LuaHelpers.callLuaClass("ISRichTextPanel", "new", null, 0.0, 0.0, 0.0, 0.0);
        ISPanelWrapper descriptionPanelWrapper = new ISPanelWrapper(descriptionPanel);
        this.table.rawset("descriptionPanel", (Object)descriptionPanel);
        descriptionPanel.rawset("marginLeft", (Object)0.0);
        descriptionPanel.rawset("marginRight", (Object)0.0);
        descriptionPanelWrapper.initialise();
        descriptionPanelWrapper.instantiate();
        descriptionPanelWrapper.noBackground();
        descriptionPanel.rawset("backgroundColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 0.0, 0.0, 0.0, 0.3));
        descriptionPanel.rawset("borderColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 1.0, 1.0, 1.0, 0.1));
        this.table.rawset("owner", null);
        this.table.rawset("followMouse", (Object)true);
        this.table.rawset("nameMarginX", (Object)50.0);
        this.table.rawset("defaultMyWidth", (Object)220.0);
    }

    public void setName(String name) {
        this.table.rawset("name", (Object)name);
    }

    public void reset() {
        this.setVisible(false);
        this.noBackground();
        this.table.rawset("name", null);
        this.table.rawset("description", (Object)"");
        this.table.rawset("texture", null);
        this.table.rawset("footNote", null);
        this.setRGBA((KahluaTable)this.table.rawget("borderColor"), 0.4, 0.4, 0.4, 1.0);
        this.setRGBA((KahluaTable)this.table.rawget("backgroundColor"), 0.0, 0.0, 0.0, 0.0);
        this.table.rawset("width", (Object)0.0);
        this.table.rawset("height", (Object)0.0);
        this.table.rawset("maxLineWidth", null);
        this.table.rawset("desiredX", null);
        this.table.rawset("desiredY", null);
        this.table.rawset("anchorLeft", (Object)true);
        this.table.rawset("anchorRight", (Object)false);
        this.table.rawset("anchorTop", (Object)true);
        this.table.rawset("anchorBottom", (Object)false);
        KahluaTable descriptionPanel = (KahluaTable)this.table.rawget("descriptionPanel");
        descriptionPanel.rawset("marginLeft", (Object)0.0);
        descriptionPanel.rawset("marginRight", (Object)0.0);
        this.setRGBA((KahluaTable)descriptionPanel.rawget("backgroundColor"), 0.0, 0.0, 0.0, 0.3);
        this.setRGBA((KahluaTable)descriptionPanel.rawget("borderColor"), 1.0, 1.0, 1.0, 0.1);
        this.table.rawset("owner", null);
        this.table.rawset("contextMenu", null);
        this.table.rawset("followMouse", (Object)true);
    }
}

