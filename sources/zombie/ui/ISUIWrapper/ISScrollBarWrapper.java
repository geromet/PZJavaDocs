/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.vm.KahluaTable;
import zombie.ui.ISUIWrapper.ISUIElementWrapper;
import zombie.ui.ISUIWrapper.LuaHelpers;

public class ISScrollBarWrapper
extends ISUIElementWrapper {
    public ISScrollBarWrapper(KahluaTable table) {
        super(table);
    }

    public void updatePos() {
        KahluaTable parentTable = (KahluaTable)this.table.rawget("parent");
        if (parentTable != null) {
            ISUIElementWrapper parent = new ISUIElementWrapper(parentTable);
            boolean vertical = LuaHelpers.castBoolean(this.table.rawget("vertical"));
            if (vertical) {
                double scrollAreaHeight;
                double sh = parent.getScrollHeight();
                if (sh > (scrollAreaHeight = parent.getScrollAreaHeight())) {
                    double yscroll = -parent.getYScroll();
                    double pos = yscroll / (sh - scrollAreaHeight);
                    if (pos < 0.0) {
                        pos = 0.0;
                    }
                    if (pos > 1.0) {
                        pos = 1.0;
                    }
                    this.table.rawset("pos", (Object)pos);
                }
            } else {
                double scrollAreaWidth;
                double sw = parent.getScrollWidth();
                if (sw > (scrollAreaWidth = parent.getScrollAreaWidth())) {
                    double xscroll = -parent.getXScroll();
                    double pos = xscroll / (sw - scrollAreaWidth);
                    if (pos < 0.0) {
                        pos = 0.0;
                    }
                    if (pos > 1.0) {
                        pos = 1.0;
                    }
                    this.table.rawset("pos", (Object)pos);
                }
            }
        }
    }
}

