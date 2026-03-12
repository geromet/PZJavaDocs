/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import com.google.common.collect.Lists;
import imgui.ImGui;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;

public class JavaInspector
extends BaseDebugWindow {
    Object obj;
    String selectedNode = "";
    int defaultflags = 192;
    int selectedflags = this.defaultflags | 1;

    public JavaInspector(Object obj) {
        this.obj = obj;
    }

    @Override
    public String getTitle() {
        if (this.obj == null) {
            return "Java Inspector";
        }
        return this.obj.toString();
    }

    boolean doTreeNode(String id, String label, boolean leaf) {
        int flags;
        boolean selected = id.equals(this.selectedNode);
        int n = flags = selected ? this.selectedflags : this.defaultflags;
        if (leaf) {
            flags |= 0x100;
        }
        boolean t = ImGui.treeNodeEx(id, flags, label);
        if (ImGui.isItemClicked()) {
            this.selectedNode = id;
        }
        return t;
    }

    public static Iterable<Field> getFieldsUpTo(Class<?> startClass, Class<?> exclusiveParent) {
        ArrayList<Field> currentClassFields = Lists.newArrayList(startClass.getDeclaredFields());
        Class<?> parentClass = startClass.getSuperclass();
        if (!(parentClass == null || exclusiveParent != null && parentClass.equals(exclusiveParent))) {
            List parentClassFields = (List)JavaInspector.getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(parentClassFields);
        }
        return currentClassFields;
    }

    @Override
    protected void doWindowContents() {
        Class<?> cls = this.obj.getClass();
        Iterable<Field> fields = JavaInspector.getFieldsUpTo(cls, Object.class);
        ArrayList<Field> fieldList = new ArrayList<Field>();
        for (Field field : fields) {
            if (field.getName().contains("BodyDamage")) {
                boolean bl = false;
            }
            fieldList.add(field);
        }
        fieldList.sort(Comparator.comparing(o -> o.getName().toLowerCase()));
        int flags = 3905;
        if (ImGui.beginTable("fields", 3, 3905)) {
            ImGui.tableSetupColumn("Name", 128);
            ImGui.tableSetupColumn("Type");
            ImGui.tableSetupColumn("Value", 16, 180.0f);
            ImGui.tableHeadersRow();
            for (int x = 0; x < fieldList.size(); ++x) {
                String fieldValueText;
                ImGui.tableNextRow();
                ImGui.tableNextColumn();
                Field field = (Field)fieldList.get(x);
                String fieldName = field.getName();
                Object fieldValue = null;
                boolean primitive = field.getType().isPrimitive();
                field.setAccessible(true);
                try {
                    fieldValue = field.get(this.obj);
                    fieldValueText = fieldValue == null ? "null" : fieldValue.toString();
                }
                catch (Exception e) {
                    fieldValueText = "<inaccessible>";
                }
                ImGui.textUnformatted(fieldName);
                ImGui.tableNextColumn();
                ImGui.textUnformatted(field.getType().getSimpleName());
                ImGui.tableNextColumn();
                if (fieldValue != null) {
                    ImGui.selectable(fieldValueText);
                    this.doPopupMenu(fieldValue, primitive);
                    continue;
                }
                ImGui.textUnformatted(fieldValueText);
            }
            ImGui.endTable();
        }
    }

    private void doPopupMenu(Object obj, boolean primitive) {
        if (obj == null) {
            return;
        }
        if (ImGui.beginPopupContextItem()) {
            if (!primitive && ImGui.selectable("inspect class")) {
                DebugContext.instance.inspectJava(obj);
            }
            ImGui.endPopup();
        }
    }
}

