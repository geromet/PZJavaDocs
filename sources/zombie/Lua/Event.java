/*
 * Decompiled with CFR 0.152.
 */
package zombie.Lua;

import java.util.ArrayList;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Platform;
import zombie.GameProfiler;
import zombie.Lua.LuaManager;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;

public final class Event {
    public static final int ADD = 0;
    public static final int NUM_FUNCTIONS = 1;
    private final Add add;
    private final Remove remove;
    public final ArrayList<LuaClosure> callbacks = new ArrayList();
    public String name;
    private final int index;

    public boolean trigger(KahluaTable env, LuaCaller caller, Object[] params) {
        if (this.callbacks.isEmpty()) {
            return false;
        }
        GameProfiler profiler = GameProfiler.getInstance();
        if (DebugOptions.instance.checks.slowLuaEvents.getValue()) {
            for (int n = 0; n < this.callbacks.size(); ++n) {
                LuaClosure closure = this.callbacks.get(n);
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Lua - " + this.name);){
                    long start = System.nanoTime();
                    caller.protectedCallVoid(LuaManager.thread, (Object)closure, params);
                    double delayMS = (double)(System.nanoTime() - start) / 1000000.0;
                    if (delayMS > 250.0) {
                        DebugLog.Lua.warn("SLOW Lua event callback %s %s %dms", closure.prototype.file, closure, (int)delayMS);
                    }
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
                if (this.callbacks.contains(closure)) continue;
                --n;
            }
            return true;
        }
        for (int n = 0; n < this.callbacks.size(); ++n) {
            LuaClosure closure = this.callbacks.get(n);
            try (GameProfiler.ProfileArea ex = profiler.profile("Lua - " + this.name);){
                caller.protectedCallVoid(LuaManager.thread, (Object)closure, params);
            }
            catch (Exception ex2) {
                ExceptionLogger.logException(ex2);
            }
            if (this.callbacks.contains(closure)) continue;
            --n;
        }
        return true;
    }

    public Event(String name, int index) {
        this.index = index;
        this.name = name;
        this.add = new Add(this);
        this.remove = new Remove(this);
    }

    public void register(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        table.rawset("Add", (Object)this.add);
        table.rawset("Remove", (Object)this.remove);
        environment.rawset(this.name, (Object)table);
    }

    public static final class Add
    implements JavaFunction {
        Event e;

        public Add(Event e) {
            this.e = e;
        }

        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (LuaCompiler.rewriteEvents) {
                return 0;
            }
            Object object = callFrame.get(0);
            if (object instanceof LuaClosure) {
                LuaClosure tab = (LuaClosure)object;
                this.e.callbacks.add(tab);
            }
            return 0;
        }
    }

    public static final class Remove
    implements JavaFunction {
        Event e;

        public Remove(Event e) {
            this.e = e;
        }

        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (LuaCompiler.rewriteEvents) {
                return 0;
            }
            Object param = callFrame.get(0);
            if (param instanceof LuaClosure) {
                LuaClosure tab = (LuaClosure)param;
                this.e.callbacks.remove(tab);
            }
            return 0;
        }
    }
}

