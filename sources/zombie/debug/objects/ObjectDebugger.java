/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.objects;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.objects.DebugClass;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugField;
import zombie.debug.objects.DebugIgnoreField;
import zombie.debug.objects.DebugMethod;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.util.assoc.AssocArray;

public class ObjectDebugger {
    private static final ConcurrentLinkedDeque<ArrayList<String>> array_list_pool = new ConcurrentLinkedDeque();
    private static final String tab_str = "  ";
    private static final ThreadLocal<Parser> localParser = new ThreadLocal<Parser>(){

        @Override
        protected Parser initialValue() {
            return new Parser();
        }
    };
    private static final ConcurrentLinkedDeque<LogObject> pool_object = new ConcurrentLinkedDeque();
    private static final Comparator<LogField> fieldComparator = (object1, object2) -> {
        if (object1.value instanceof LogEntry && !(object2.value instanceof LogEntry)) {
            return 1;
        }
        if (!(object1.value instanceof LogEntry) && object2.value instanceof LogEntry) {
            return -1;
        }
        if (object1.isFunction && !object2.isFunction) {
            return 1;
        }
        if (!object1.isFunction && object2.isFunction) {
            return -1;
        }
        return object1.field.compareTo(object2.field);
    };
    private static final ConcurrentLinkedDeque<LogField> pool_field = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<LogList> pool_list = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<LogMap> pool_map = new ConcurrentLinkedDeque();

    public static ArrayList<String> AllocList() {
        ArrayList<String> list = array_list_pool.poll();
        if (list == null) {
            list = new ArrayList();
        }
        return list;
    }

    public static void ReleaseList(ArrayList<String> list) {
        list.clear();
        array_list_pool.offer(list);
    }

    public static void Log(Object o) {
        ObjectDebugger.Log(o, Integer.MAX_VALUE, true, true, Integer.MAX_VALUE);
    }

    public static void Log(Object o, boolean forceAccessFields) {
        ObjectDebugger.Log(o, Integer.MAX_VALUE, forceAccessFields, true, Integer.MAX_VALUE);
    }

    public static void Log(Object o, boolean forceAccessFields, boolean useClassAnnotations) {
        ObjectDebugger.Log(o, Integer.MAX_VALUE, forceAccessFields, useClassAnnotations, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth) {
        ObjectDebugger.Log(o, inheritanceDepth, true, true, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) {
        ObjectDebugger.Log(DebugLog.General, o, inheritanceDepth, forceAccessFields, useClassAnnotations, memberDepth);
    }

    public static void Log(DebugType logStream, Object o) {
        ObjectDebugger.Log(logStream, o, Integer.MAX_VALUE, true, true, Integer.MAX_VALUE);
    }

    public static void Log(DebugType logStream, Object o, boolean forceAccessFields) {
        ObjectDebugger.Log(logStream, o, Integer.MAX_VALUE, forceAccessFields, true, Integer.MAX_VALUE);
    }

    public static void Log(DebugType logStream, Object o, boolean forceAccessFields, boolean useClassAnnotations) {
        ObjectDebugger.Log(logStream, o, Integer.MAX_VALUE, forceAccessFields, useClassAnnotations, Integer.MAX_VALUE);
    }

    public static void Log(DebugType logStream, Object o, int inheritanceDepth) {
        ObjectDebugger.Log(logStream, o, inheritanceDepth, true, true, Integer.MAX_VALUE);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void Log(DebugType logStream, Object o, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) {
        if (logStream == null) {
            logStream = DebugLog.General;
        }
        if (o == null) {
            logStream.println("[null]");
            return;
        }
        if (!Core.debug) {
            logStream.println("ObjectDebugger can only run in debug mode.");
            return;
        }
        try {
            Parser parser = localParser.get();
            parser.parse(o, parser.lines, inheritanceDepth, forceAccessFields, useClassAnnotations, memberDepth);
            for (int i = 0; i < parser.lines.size(); ++i) {
                logStream.println(parser.lines.get(i));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            localParser.get().reset();
        }
    }

    public static void GetLines(Object o, ArrayList<String> list) {
        ObjectDebugger.GetLines(o, list, Integer.MAX_VALUE, true, false, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, boolean forceAccessFields) {
        ObjectDebugger.GetLines(o, list, Integer.MAX_VALUE, forceAccessFields, false, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, boolean forceAccessFields, boolean useClassAnnotations) {
        ObjectDebugger.GetLines(o, list, Integer.MAX_VALUE, forceAccessFields, useClassAnnotations, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth) {
        ObjectDebugger.GetLines(o, list, inheritanceDepth, true, false, Integer.MAX_VALUE);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) {
        if (o == null) {
            list.add("[null]");
            return;
        }
        if (!Core.debug) {
            list.add("ObjectDebugger can only run in debug mode.");
            return;
        }
        try {
            localParser.get().parse(o, list, inheritanceDepth, forceAccessFields, useClassAnnotations, memberDepth);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            localParser.get().reset();
        }
    }

    protected static LogObject alloc_log_object(Object o) {
        LogObject log = pool_object.poll();
        if (log == null) {
            log = new LogObject();
        }
        log.object = o;
        return log;
    }

    protected static LogField alloc_log_field(String field, Object value, boolean isFunction) {
        LogField log = pool_field.poll();
        if (log == null) {
            log = new LogField();
        }
        log.field = field;
        log.value = value;
        log.isFunction = isFunction;
        return log;
    }

    protected static LogList alloc_log_list() {
        LogList log = pool_list.poll();
        if (log == null) {
            log = new LogList();
        }
        return log;
    }

    protected static LogMap alloc_log_map() {
        LogMap log = pool_map.poll();
        if (log == null) {
            log = new LogMap();
        }
        return log;
    }

    private static class Parser {
        private final ArrayList<String> lines = new ArrayList();
        private final Set<Object> parsedObjects = new HashSet<Object>();
        private int originalInheritanceDepth = Integer.MAX_VALUE;
        private boolean useClassAnnotations;
        private boolean forceAccessFields;
        private LogObject root;

        private Parser() {
        }

        private void reset() {
            this.lines.clear();
            this.parsedObjects.clear();
            this.originalInheritanceDepth = Integer.MAX_VALUE;
            this.useClassAnnotations = false;
            this.forceAccessFields = false;
            if (this.root != null) {
                this.root.release();
            }
            this.root = null;
        }

        private boolean inheritsAnnotations(Class<?> c, int inheritanceDepth) {
            if (c.getAnnotation(DebugClass.class) != null || c.getAnnotation(DebugClassFields.class) != null) {
                return true;
            }
            return c.getSuperclass() != null && inheritanceDepth >= 0 && this.inheritsAnnotations(c.getSuperclass(), inheritanceDepth - 1);
        }

        private boolean validClass(Class<?> c, int inheritanceDept) {
            if (c == null) {
                return false;
            }
            if (this.useClassAnnotations) {
                return this.inheritsAnnotations(c, inheritanceDept);
            }
            if (String.class.isAssignableFrom(c)) {
                return false;
            }
            if (Boolean.class.isAssignableFrom(c)) {
                return false;
            }
            if (Byte.class.isAssignableFrom(c)) {
                return false;
            }
            if (Short.class.isAssignableFrom(c)) {
                return false;
            }
            if (Integer.class.isAssignableFrom(c)) {
                return false;
            }
            if (Long.class.isAssignableFrom(c)) {
                return false;
            }
            if (Float.class.isAssignableFrom(c)) {
                return false;
            }
            if (Double.class.isAssignableFrom(c)) {
                return false;
            }
            if (Enum.class.isAssignableFrom(c)) {
                return false;
            }
            if (Collection.class.isAssignableFrom(c)) {
                return false;
            }
            return !Iterable.class.isAssignableFrom(c);
        }

        private void parse(Object o, ArrayList<String> list, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) throws Exception {
            this.originalInheritanceDepth = inheritanceDepth;
            this.useClassAnnotations = useClassAnnotations;
            this.forceAccessFields = forceAccessFields;
            this.root = ObjectDebugger.alloc_log_object(o);
            this.parseInternal(this.root, inheritanceDepth, memberDepth);
            this.root.sort();
            this.root.build(list, 0);
        }

        private void parseInternal(LogObject log, int inheritanceDepth, int memberDepth) throws Exception {
            Class<?> c = log.object.getClass();
            if (!this.validClass(c, inheritanceDepth)) {
                return;
            }
            if (this.parsedObjects.contains(log.object)) {
                // empty if block
            }
            this.parsedObjects.add(log.object);
            this.parseClass(log, c, inheritanceDepth, memberDepth);
        }

        private void parseClass(LogObject log, Class<?> c, int inheritanceDepth, int memberDepth) throws Exception {
            this.parseClassMembers(log, c, memberDepth);
            if (c.getSuperclass() != null && inheritanceDepth >= 0) {
                this.parseClass(log, c.getSuperclass(), inheritanceDepth - 1, memberDepth);
            }
        }

        private void parseClassMembers(LogObject log, Class<?> c, int memberDepth) throws Exception {
            Method[] methods;
            Field[] fields;
            if (this.useClassAnnotations && c.getAnnotation(DebugClass.class) == null && c.getAnnotation(DebugClassFields.class) == null) {
                return;
            }
            boolean allFields = !this.useClassAnnotations || c.getAnnotation(DebugClassFields.class) != null;
            for (Field f : fields = c.getDeclaredFields()) {
                if (!allFields && f.getAnnotation(DebugField.class) == null || Modifier.isStatic(f.getModifiers()) || f.getAnnotation(DebugIgnoreField.class) != null) continue;
                boolean access = f.canAccess(log.object);
                if (!access) {
                    if (!this.forceAccessFields) continue;
                    if (!f.trySetAccessible()) {
                        DebugLog.log("Cannot debug field: failed accessibility. field = " + f.getName());
                        continue;
                    }
                }
                if (log == this.root) {
                    this.parsedObjects.clear();
                    this.parsedObjects.add(this.root.object);
                }
                this.parseMember(log, f.getName(), f.get(log.object), f.getAnnotation(DebugNonRecursive.class) != null ? 0 : memberDepth, false);
                if (access) continue;
                f.setAccessible(access);
            }
            for (Method m : methods = c.getDeclaredMethods()) {
                if (m.getAnnotation(DebugMethod.class) == null) continue;
                if (Modifier.isStatic(m.getModifiers())) {
                    DebugLog.log("Cannot debug method: is static. method = " + m.getName());
                    continue;
                }
                if (!Modifier.isPublic(m.getModifiers())) {
                    DebugLog.log("Cannot debug method: not public. method = " + m.getName());
                    continue;
                }
                if (m.getParameterCount() > 0) {
                    DebugLog.log("Cannot debug method: has parameters. method = " + m.getName());
                    continue;
                }
                if (log.containsMember(m.getName())) continue;
                if (log == this.root) {
                    this.parsedObjects.clear();
                    this.parsedObjects.add(this.root.object);
                }
                this.parseMember(log, m.getName(), m.invoke(log.object, new Object[0]), m.getAnnotation(DebugNonRecursive.class) != null ? 0 : memberDepth, true);
            }
        }

        private void parseMember(LogObject log, String memberName, Object value, int memberDepth, boolean isFunction) throws Exception {
            boolean handled = false;
            if (value != null) {
                Class<?> clazz = value.getClass();
                if (memberDepth > 0 && value instanceof List) {
                    List listObj = (List)value;
                    LogList logList = ObjectDebugger.alloc_log_list();
                    if (!listObj.isEmpty()) {
                        for (int i = 0; i < listObj.size(); ++i) {
                            Object element = listObj.get(i);
                            Class<?> elementClazz = element.getClass();
                            if (this.validClass(elementClazz, this.originalInheritanceDepth)) {
                                LogObject logObject = ObjectDebugger.alloc_log_object(element);
                                this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                                logList.addElement(logObject);
                                continue;
                            }
                            logList.addElement(element);
                        }
                    }
                    LogField logField = ObjectDebugger.alloc_log_field(memberName, logList, isFunction);
                    log.addMember(logField);
                    handled = true;
                } else if (memberDepth > 0 && value instanceof Map) {
                    Map mapObj = (Map)value;
                    LogMap logMap = ObjectDebugger.alloc_log_map();
                    for (Map.Entry entry : mapObj.entrySet()) {
                        Object key = entry.getKey();
                        Object val = entry.getValue();
                        Class<?> valClazz = val.getClass();
                        if (this.validClass(valClazz, this.originalInheritanceDepth)) {
                            LogObject logObject = ObjectDebugger.alloc_log_object(val);
                            this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                            logMap.putElement(key, logObject);
                            continue;
                        }
                        logMap.putElement(key, val);
                    }
                    LogField logField = ObjectDebugger.alloc_log_field(memberName, logMap, isFunction);
                    log.addMember(logField);
                    handled = true;
                } else if (memberDepth > 0 && value instanceof AssocArray) {
                    AssocArray mapObj = (AssocArray)value;
                    LogMap logMap = ObjectDebugger.alloc_log_map();
                    for (int i = 0; i < mapObj.size(); ++i) {
                        Object key = mapObj.getKey(i);
                        Object val = mapObj.getValue(i);
                        Class<?> valClazz = val.getClass();
                        if (this.validClass(valClazz, this.originalInheritanceDepth)) {
                            LogObject logObject = ObjectDebugger.alloc_log_object(val);
                            this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                            logMap.putElement(key, logObject);
                            continue;
                        }
                        logMap.putElement(key, val);
                    }
                    LogField logField = ObjectDebugger.alloc_log_field(memberName, logMap, isFunction);
                    log.addMember(logField);
                    handled = true;
                } else if (memberDepth > 0 && this.validClass(clazz, this.originalInheritanceDepth)) {
                    LogObject logObject = ObjectDebugger.alloc_log_object(value);
                    this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                    LogField logField = ObjectDebugger.alloc_log_field(memberName, logObject, isFunction);
                    log.addMember(logField);
                    handled = true;
                }
            }
            if (!handled) {
                LogField logField = ObjectDebugger.alloc_log_field(memberName, value, isFunction);
                log.addMember(logField);
            }
        }
    }

    private static class LogObject
    extends LogEntry {
        private Object object;
        protected final List<LogField> members = new ArrayList<LogField>();

        private LogObject() {
        }

        protected boolean containsMember(String fieldName) {
            for (int i = 0; i < this.members.size(); ++i) {
                if (!this.members.get((int)i).field.equals(fieldName)) continue;
                return true;
            }
            return false;
        }

        protected void addMember(LogField entry) {
            if (!this.members.contains(entry)) {
                this.members.add(entry);
            }
        }

        protected String getHeader() {
            return "[" + this.object.getClass().getCanonicalName() + "]";
        }

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            list.add(ObjectDebugger.tab_str.repeat(tabs) + this.getHeader());
            list.add(ObjectDebugger.tab_str.repeat(tabs) + "{");
            this.buildMembers(list, tabs + 1);
            list.add(ObjectDebugger.tab_str.repeat(tabs) + "}");
        }

        protected void buildMembers(ArrayList<String> list, int tabs) {
            for (int i = 0; i < this.members.size(); ++i) {
                LogField entry = this.members.get(i);
                entry.build(list, tabs);
            }
        }

        @Override
        protected void reset() {
            this.object = null;
            for (int i = 0; i < this.members.size(); ++i) {
                LogField entry = this.members.get(i);
                entry.release();
            }
            this.members.clear();
        }

        @Override
        protected void release() {
            this.reset();
            pool_object.offer(this);
        }

        @Override
        protected void sort() {
            for (int i = 0; i < this.members.size(); ++i) {
                LogField entry = this.members.get(i);
                entry.sort();
            }
            this.members.sort(fieldComparator);
        }
    }

    private static class LogField
    extends LogEntry {
        protected String field;
        protected Object value;
        protected boolean isFunction;

        private LogField() {
        }

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            Object object = this.value;
            if (object instanceof LogEntry) {
                LogCollection logCollection;
                LogEntry logEntry = (LogEntry)object;
                Object object2 = this.value;
                if (object2 instanceof LogCollection && (logCollection = (LogCollection)object2).size() == 0) {
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + this.getPrintName() + " = <EMPTY> " + this.getValueType());
                } else {
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + this.getPrintName() + " = " + this.getValueType());
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "{");
                    Object object3 = this.value;
                    if (object3 instanceof LogObject) {
                        LogObject logObject = (LogObject)object3;
                        logObject.buildMembers(list, tabs + 1);
                    } else {
                        logEntry.build(list, tabs + 1);
                    }
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "}");
                }
            } else {
                list.add(ObjectDebugger.tab_str.repeat(tabs) + this.getPrintName() + " = " + this.print(this.value));
            }
        }

        private String getPrintName() {
            if (this.isFunction) {
                return "func:" + this.field + "()";
            }
            return this.field;
        }

        private String getValueType() {
            Object object = this.value;
            if (object instanceof LogObject) {
                LogObject logObject = (LogObject)object;
                return logObject.getHeader();
            }
            if (this.value instanceof LogList) {
                return "(List<T>)";
            }
            if (this.value instanceof LogMap) {
                return "(Map<K,V>)";
            }
            return "";
        }

        @Override
        protected void reset() {
            this.field = null;
            Object object = this.value;
            if (object instanceof LogEntry) {
                LogEntry logEntry = (LogEntry)object;
                logEntry.release();
            }
            this.value = null;
            this.isFunction = false;
        }

        @Override
        protected void release() {
            this.reset();
            pool_field.offer(this);
        }

        @Override
        protected void sort() {
            Object object = this.value;
            if (object instanceof LogEntry) {
                LogEntry logEntry = (LogEntry)object;
                logEntry.sort();
            }
        }
    }

    private static class LogList
    extends LogCollection {
        private final List<Object> elements = new ArrayList<Object>();

        private LogList() {
        }

        protected void addElement(Object element) {
            this.elements.add(element);
        }

        @Override
        protected int size() {
            return this.elements.size();
        }

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            for (int i = 0; i < this.elements.size(); ++i) {
                Object o = this.elements.get(i);
                if (o instanceof LogObject) {
                    LogObject logObject = (LogObject)o;
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "[" + i + "] = " + logObject.getHeader());
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "{");
                    logObject.buildMembers(list, tabs + 1);
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "}");
                    continue;
                }
                list.add(ObjectDebugger.tab_str.repeat(tabs) + "[" + i + "] = " + this.print(o));
            }
        }

        @Override
        protected void reset() {
            for (int i = 0; i < this.elements.size(); ++i) {
                Object o = this.elements.get(i);
                if (!(o instanceof LogEntry)) continue;
                LogEntry logEntry = (LogEntry)o;
                logEntry.release();
            }
            this.elements.clear();
        }

        @Override
        protected void release() {
            this.reset();
            pool_list.offer(this);
        }

        @Override
        protected void sort() {
            for (int i = 0; i < this.elements.size(); ++i) {
                Object o = this.elements.get(i);
                if (!(o instanceof LogEntry)) continue;
                LogEntry logEntry = (LogEntry)o;
                logEntry.sort();
            }
        }
    }

    private static class LogMap
    extends LogCollection {
        private final Map<Object, Object> elements = new HashMap<Object, Object>();

        private LogMap() {
        }

        protected void putElement(Object key, Object value) {
            this.elements.put(key, value);
        }

        @Override
        protected int size() {
            return this.elements.size();
        }

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            for (Map.Entry<Object, Object> entry : this.elements.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof LogObject) {
                    LogObject logObject = (LogObject)val;
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "[" + String.valueOf(key) + "] = " + logObject.getHeader());
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "{");
                    logObject.buildMembers(list, tabs + 1);
                    list.add(ObjectDebugger.tab_str.repeat(tabs) + "}");
                    continue;
                }
                list.add(ObjectDebugger.tab_str.repeat(tabs) + "[" + String.valueOf(key) + "] = " + this.print(val));
            }
        }

        @Override
        protected void reset() {
            for (Map.Entry<Object, Object> entry : this.elements.entrySet()) {
                Object val = entry.getValue();
                if (!(val instanceof LogEntry)) continue;
                LogEntry logEntry = (LogEntry)val;
                logEntry.release();
            }
            this.elements.clear();
        }

        @Override
        protected void release() {
            this.reset();
            pool_map.offer(this);
        }

        @Override
        protected void sort() {
            for (Map.Entry<Object, Object> entry : this.elements.entrySet()) {
                Object val = entry.getValue();
                if (!(val instanceof LogEntry)) continue;
                LogEntry logEntry = (LogEntry)val;
                logEntry.sort();
            }
        }
    }

    private static abstract class LogEntry {
        private LogEntry() {
        }

        protected abstract void build(ArrayList<String> var1, int var2);

        protected abstract void reset();

        protected abstract void release();

        protected abstract void sort();

        protected String print(Object value) {
            if (value instanceof String) {
                return "\"" + String.valueOf(value) + "\"";
            }
            if (value instanceof Byte) {
                return "(byte) " + String.valueOf(value);
            }
            if (value instanceof Short) {
                return "(short) " + String.valueOf(value);
            }
            if (value instanceof Integer) {
                return "(int) " + String.valueOf(value);
            }
            if (value instanceof Float) {
                return "(float) " + String.valueOf(value);
            }
            if (value instanceof Double) {
                return "(double) " + String.valueOf(value);
            }
            if (value instanceof Long) {
                return "(long) " + String.valueOf(value);
            }
            if (value instanceof Enum) {
                return value.getClass().getSimpleName() + "." + String.valueOf(value);
            }
            return Objects.toString(value);
        }
    }

    private static abstract class LogCollection
    extends LogEntry {
        private LogCollection() {
        }

        protected abstract int size();
    }
}

