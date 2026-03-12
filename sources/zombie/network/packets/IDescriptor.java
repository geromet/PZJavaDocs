/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public interface IDescriptor {
    default public void getClassDescription(StringBuilder s, Class<?> cls, HashSet<Object> excludedObjects) {
        boolean isFirst = false;
        for (Field field : cls.getDeclaredFields()) {
            Annotation[] annotations = field.getAnnotationsByType(JSONField.class);
            if (annotations.length <= 0) continue;
            String name = field.getName();
            try {
                field.setAccessible(true);
                Object obj = field.get(this);
                if (isFirst) {
                    s.append(", ");
                } else {
                    isFirst = true;
                }
                s.append("\"").append(name).append("\" : ").append(this.toJSON(obj, excludedObjects));
            }
            catch (IllegalAccessException e) {
                DebugLog.Multiplayer.printException(e, "INetworkPacketField.getDescription: can't get the value of the " + name + " field", LogSeverity.Error);
            }
        }
    }

    default public String getDescription(HashSet<Object> excludedObjects) {
        StringBuilder s = new StringBuilder("{ ");
        for (Class<?> cls = this.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            if (cls != this.getClass()) {
                s.append(" , ");
            }
            s.append("\"").append(cls.getSimpleName()).append("\": { ");
            this.getClassDescription(s, cls, excludedObjects);
            s.append(" } ");
        }
        s.append(" }");
        return s.toString();
    }

    default public String getDescription() {
        return this.getDescription(new HashSet<Object>());
    }

    private String toJSON(Object obj, HashSet<Object> excludedObjects) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof INetworkPacketField) {
            INetworkPacketField iNetworkPacketField = (INetworkPacketField)obj;
            if (excludedObjects.contains(obj)) {
                return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
            }
            excludedObjects.add(obj);
            return iNetworkPacketField.getDescription(excludedObjects);
        }
        if (obj instanceof String) {
            return "\"" + String.valueOf(obj) + "\"";
        }
        if (obj instanceof Boolean || obj instanceof Byte || obj instanceof Short || obj instanceof Integer || obj instanceof Long || obj instanceof Float || obj instanceof Double) {
            return obj.toString();
        }
        if (obj instanceof Iterable) {
            Iterable iterable = (Iterable)obj;
            if (excludedObjects.contains(obj)) {
                return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
            }
            excludedObjects.add(obj);
            StringBuilder s = new StringBuilder("[");
            boolean isFirst = true;
            for (Object o : iterable) {
                if (!isFirst) {
                    s.append(", ");
                }
                s.append(this.toJSON(o, excludedObjects));
                isFirst = false;
            }
            return String.valueOf(s) + "]";
        }
        if (obj instanceof KahluaTable) {
            KahluaTable kahluaTable = (KahluaTable)obj;
            if (excludedObjects.contains(obj)) {
                return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
            }
            excludedObjects.add(obj);
            KahluaTableIterator it = kahluaTable.iterator();
            StringBuilder s = new StringBuilder("{");
            boolean isFirst = true;
            while (it.advance()) {
                Object key;
                if (!isFirst) {
                    s.append(", ");
                }
                if ("netAction".equals(key = it.getKey())) continue;
                s.append(this.toJSON(it.getKey(), excludedObjects));
                s.append(": ");
                s.append(this.toJSON(it.getValue(), excludedObjects));
                isFirst = false;
            }
            s.append("}");
            return s.toString();
        }
        if (excludedObjects.contains(obj)) {
            return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
        }
        excludedObjects.add(obj);
        return "\"" + String.valueOf(obj) + "\"";
    }
}

