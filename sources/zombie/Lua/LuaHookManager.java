/*
 * Decompiled with CFR 0.152.
 */
package zombie.Lua;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Platform;
import zombie.Lua.Event;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;

public final class LuaHookManager
implements JavaFunction {
    public static final ArrayList<LuaClosure> OnTickCallbacks = new ArrayList();
    static Object[] a = new Object[1];
    static Object[] b = new Object[2];
    static Object[] c = new Object[3];
    static Object[] d = new Object[4];
    static Object[] f = new Object[5];
    static Object[] g = new Object[6];
    private static final ArrayList<Event> EventList = new ArrayList();
    private static final HashMap<String, Event> EventMap = new HashMap();

    public static boolean TriggerHook(String event) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            LuaHookManager.a[0] = null;
            return e.trigger(LuaManager.env, LuaManager.caller, a);
        }
        return false;
    }

    public static boolean TriggerHook(String event, Object param1) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            LuaHookManager.a[0] = param1;
            return e.trigger(LuaManager.env, LuaManager.caller, a);
        }
        return false;
    }

    public static boolean TriggerHook(String event, Object param1, Object param2) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            LuaHookManager.b[0] = param1;
            LuaHookManager.b[1] = param2;
            return e.trigger(LuaManager.env, LuaManager.caller, b);
        }
        return false;
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            LuaHookManager.c[0] = param1;
            LuaHookManager.c[1] = param2;
            LuaHookManager.c[2] = param3;
            return e.trigger(LuaManager.env, LuaManager.caller, c);
        }
        return false;
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3, Object param4) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            LuaHookManager.d[0] = param1;
            LuaHookManager.d[1] = param2;
            LuaHookManager.d[2] = param3;
            LuaHookManager.d[3] = param4;
            return e.trigger(LuaManager.env, LuaManager.caller, d);
        }
        return false;
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3, Object param4, Object param5) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            LuaHookManager.f[0] = param1;
            LuaHookManager.f[1] = param2;
            LuaHookManager.f[2] = param3;
            LuaHookManager.f[3] = param4;
            LuaHookManager.f[4] = param5;
            return e.trigger(LuaManager.env, LuaManager.caller, f);
        }
        return false;
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            LuaHookManager.g[0] = param1;
            LuaHookManager.g[1] = param2;
            LuaHookManager.g[2] = param3;
            LuaHookManager.g[3] = param4;
            LuaHookManager.g[4] = param5;
            LuaHookManager.g[5] = param6;
            return e.trigger(LuaManager.env, LuaManager.caller, g);
        }
        return false;
    }

    public static void AddEvent(String name) {
        if (EventMap.containsKey(name)) {
            return;
        }
        Event event = new Event(name, EventList.size());
        EventList.add(event);
        EventMap.put(name, event);
        Object o = LuaManager.env.rawget("Hook");
        if (o instanceof KahluaTable) {
            KahluaTable table = (KahluaTable)o;
            event.register(LuaManager.platform, table);
        } else {
            DebugLog.log("ERROR: 'Hook' table not found or not a table");
        }
    }

    private static void AddEvents() {
        LuaHookManager.AddEvent("AutoDrink");
        LuaHookManager.AddEvent("UseItem");
        LuaHookManager.AddEvent("Attack");
        LuaHookManager.AddEvent("CalculateStats");
        LuaHookManager.AddEvent("ContextualAction");
        LuaHookManager.AddEvent("WeaponHitCharacter");
        LuaHookManager.AddEvent("WeaponSwing");
        LuaHookManager.AddEvent("WeaponSwingHitPoint");
    }

    public static void clear() {
        LuaHookManager.a[0] = null;
        LuaHookManager.b[0] = null;
        LuaHookManager.b[1] = null;
        LuaHookManager.c[0] = null;
        LuaHookManager.c[1] = null;
        LuaHookManager.c[2] = null;
        LuaHookManager.d[0] = null;
        LuaHookManager.d[1] = null;
        LuaHookManager.d[2] = null;
        LuaHookManager.d[3] = null;
        LuaHookManager.f[0] = null;
        LuaHookManager.f[1] = null;
        LuaHookManager.f[2] = null;
        LuaHookManager.f[3] = null;
        LuaHookManager.f[4] = null;
        LuaHookManager.g[0] = null;
        LuaHookManager.g[1] = null;
        LuaHookManager.g[2] = null;
        LuaHookManager.g[3] = null;
        LuaHookManager.g[4] = null;
        LuaHookManager.g[5] = null;
    }

    public static void register(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        environment.rawset("Hook", (Object)table);
        LuaHookManager.AddEvents();
    }

    public static void Reset() {
        for (Event e : EventList) {
            e.callbacks.clear();
        }
        EventList.clear();
        EventMap.clear();
    }

    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return 0;
    }

    private int OnTick(LuaCallFrame callFrame, int nArguments) {
        return 0;
    }
}

