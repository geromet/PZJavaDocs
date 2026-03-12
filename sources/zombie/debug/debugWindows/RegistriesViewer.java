/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.List;
import java.util.function.Function;
import zombie.debug.debugWindows.PZDebugWindow;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.Registry;
import zombie.scripting.objects.ResourceLocation;

public class RegistriesViewer
extends PZDebugWindow {
    @Override
    public String getTitle() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void doWindowContents() {
        ImGui.beginChild("Begin");
        if (ImGui.beginTabBar("tabSelector")) {
            for (ResourceLocation key : Registries.REGISTRY.keys()) {
                this.renderGenericRegistryTab(key, Registries.REGISTRY.get(key));
            }
            ImGui.endTabBar();
        }
        ImGui.endChild();
    }

    private <T> void renderGenericRegistryTab(ResourceLocation resourceLocation, Registry<T> registry) {
        String tabName = resourceLocation.toString();
        if (!ImGui.beginTabItem(tabName)) {
            return;
        }
        List columns = List.of(new TableColumn<Object>("ID", item -> {
            ResourceLocation loc = registry.getLocation(item);
            return loc != null ? loc.getNamespace() + ":" + loc.getPath() : "";
        }), new TableColumn<Object>("Namespace", item -> {
            ResourceLocation loc = registry.getLocation(item);
            return loc != null ? loc.getNamespace() : "";
        }), new TableColumn<Object>("Path", item -> {
            ResourceLocation loc = registry.getLocation(item);
            return loc != null ? loc.getPath() : "";
        }));
        ImGuiTableRenderer.renderTable(tabName, columns, registry);
        ImGui.endTabItem();
    }

    public static class TableColumn<T> {
        private final String header;
        private final Function<T, String> valueExtractor;

        public TableColumn(String header, Function<T, String> valueExtractor) {
            this.header = header;
            this.valueExtractor = valueExtractor;
        }
    }

    public static class ImGuiTableRenderer {
        public static <T> void renderTable(String tableId, List<TableColumn<T>> columns, Iterable<T> registry) {
            if (!ImGui.beginTable(tableId, columns.size(), 1984)) {
                return;
            }
            for (TableColumn<T> column : columns) {
                ImGui.tableSetupColumn(column.header);
            }
            ImGui.tableHeadersRow();
            for (TableColumn<T> item : registry) {
                ImGui.tableNextRow();
                for (int col = 0; col < columns.size(); ++col) {
                    ImGui.tableSetColumnIndex(col);
                    ImGui.text(columns.get((int)col).valueExtractor.apply(item));
                }
            }
            ImGui.endTable();
        }
    }
}

