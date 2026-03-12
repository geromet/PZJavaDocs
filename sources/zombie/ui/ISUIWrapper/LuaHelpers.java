/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui.ISUIWrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;

public class LuaHelpers {
    public static Object callLuaClass(String type, String function, KahluaTable context, Object ... args2) {
        Object[] result = LuaHelpers.callLuaClassReturnMultiple(type, function, context, args2);
        if (result == null) {
            return null;
        }
        return result[0];
    }

    public static Object[] callLuaClassReturnMultiple(String type, String function, KahluaTable context, Object ... args2) {
        Object[] arguments;
        Object classObject = LuaManager.get(type);
        Object functionObject = LuaManager.getFunctionObject(type + "." + function);
        if (function.equals("new") && context == null) {
            context = (KahluaTable)classObject;
        }
        if (context == null) {
            arguments = args2;
        } else {
            arguments = new Object[args2.length + 1];
            arguments[0] = context;
            for (int i = 0; i < args2.length; ++i) {
                arguments[i + 1] = args2[i];
            }
        }
        LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, arguments);
        if (!result.isSuccess() || result.isEmpty()) {
            return null;
        }
        Object[] output = new Object[result.size()];
        for (int i = 0; i < result.size(); ++i) {
            output[i] = result.get(i);
        }
        return output;
    }

    public static KahluaTable getJoypadState(double playerNum) {
        KahluaTable joypadState = (KahluaTable)LuaManager.env.rawget("JoypadState");
        KahluaTable joypadStatePlayers = (KahluaTable)joypadState.rawget("players");
        return (KahluaTable)joypadStatePlayers.rawget(playerNum + 1.0);
    }

    public static boolean castBoolean(Object luaObject) {
        Boolean result = (Boolean)luaObject;
        return result == null ? false : result;
    }

    public static Double castDouble(Object luaObject) {
        Double result = (Double)luaObject;
        return result == null ? 0.0 : result;
    }

    public static String castString(Object luaObject) {
        String result = (String)luaObject;
        return result == null ? "" : result;
    }

    public static boolean tableContainsKey(KahluaTable table, Object value) {
        if (table == null) {
            return false;
        }
        KahluaTableIterator iterator2 = table.iterator();
        while (iterator2.advance()) {
            if (iterator2.getKey() != value) continue;
            return true;
        }
        return false;
    }

    public static boolean tableContainsValue(KahluaTable table, Object value) {
        if (table == null) {
            return false;
        }
        KahluaTableIterator iterator2 = table.iterator();
        while (iterator2.advance()) {
            if (iterator2.getValue() != value) continue;
            return true;
        }
        return false;
    }

    public static void tableSort(KahluaTable table, Comparator<Map.Entry<Object, Object>> comparator) {
        ArrayList<Map.Entry<Object, Object>> tempList = new ArrayList<Map.Entry<Object, Object>>();
        KahluaTableIterator iterator2 = table.iterator();
        while (iterator2.advance()) {
            if (iterator2.getKey() == null) continue;
            tempList.add(Map.entry(iterator2.getKey(), iterator2.getValue()));
        }
        tempList.sort(comparator);
        table.wipe();
        for (Map.Entry entry : tempList) {
            table.rawset(entry.getKey(), entry.getValue());
        }
    }

    public static KahluaTable getPlayerContextMenu(double id) {
        KahluaTable data = LuaHelpers.getPlayerData(id);
        if (data != null) {
            return (KahluaTable)data.rawget("contextMenu");
        }
        return null;
    }

    public static KahluaTable getPlayerData(double id) {
        KahluaTable playerData = (KahluaTable)LuaManager.env.rawget("ISPlayerData");
        return (KahluaTable)playerData.rawget(id + 1.0);
    }
}

