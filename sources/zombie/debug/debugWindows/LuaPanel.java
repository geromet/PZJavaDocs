/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImString;
import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.debug.BaseDebugWindow;

public class LuaPanel
extends BaseDebugWindow {
    ImString searchString = new ImString(128);
    int selectedIndex;

    @Override
    public String getTitle() {
        return "Lua";
    }

    @Override
    protected void doWindowContents() {
        ArrayList<String> items = new ArrayList<String>();
        if (!this.searchString.toString().isEmpty()) {
            KahluaTableImpl lua = (KahluaTableImpl)LuaManager.env;
            KahluaTableIterator it = lua.iterator();
            while (it.advance()) {
                if (!it.getKey().toString().toLowerCase().contains(this.searchString.toString().toLowerCase())) continue;
                items.add(it.getKey().toString());
            }
        }
        if (ImGui.inputText("search string", this.searchString, 0)) {
            boolean lua = false;
        }
        if (ImGui.beginListBox("##empty", -1.17549435E-38f, 0.0f)) {
            for (int i = 0; i < items.size(); ++i) {
                if (!ImGui.selectable((String)items.get(i), this.selectedIndex == i)) continue;
                this.selectedIndex = i;
            }
            ImGui.endListBox();
        }
    }
}

