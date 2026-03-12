/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

public abstract class ScriptBucket<E extends BaseScriptObject> {
    private static String currentScriptObject;
    private final HashMap<String, LoadData<E>> loadData = new HashMap();
    protected final ArrayList<String> loadFiles = new ArrayList();
    protected final HashSet<String> dotInName = new HashSet();
    protected final ArrayList<E> scriptList = new ArrayList();
    protected final Map<String, E> scriptMap;
    protected final ScriptModule module;
    protected final ScriptType scriptType;
    private boolean reload;
    protected boolean hasLoadErrors;
    private boolean verbose;

    public static final String getCurrentScriptObject() {
        return currentScriptObject;
    }

    public ScriptBucket(ScriptModule module, ScriptType scriptType) {
        this(module, scriptType, new HashMap());
    }

    public ScriptBucket(ScriptModule module, ScriptType scriptType, Map<String, E> customMap) {
        this.module = module;
        this.scriptType = scriptType;
        if (!(this instanceof Template) && scriptType.isTemplate()) {
            throw new RuntimeException("ScriptType '" + String.valueOf((Object)scriptType) + "' should not be template!");
        }
        this.scriptMap = customMap != null ? customMap : new HashMap<String, E>();
    }

    public ScriptType getScriptType() {
        return this.scriptType;
    }

    protected void setReload(boolean reload) {
        this.reload = reload;
    }

    public boolean isVerbose() {
        return this.verbose || this.scriptType.isVerbose();
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public boolean isHasLoadErrors() {
        return this.hasLoadErrors;
    }

    protected void setLoadError() {
        this.hasLoadErrors = true;
    }

    public ArrayList<E> getScriptList() {
        return this.scriptList;
    }

    public Map<String, E> getScriptMap() {
        return this.scriptMap;
    }

    public void reset() {
        this.loadData.clear();
        this.loadFiles.clear();
        this.dotInName.clear();
        this.scriptList.clear();
        this.scriptMap.clear();
    }

    public abstract E createInstance(ScriptModule var1, String var2, String var3);

    public boolean CreateFromTokenPP(ScriptLoadMode loadMode, String type, String token) {
        try {
            if (this.scriptType.getScriptTag().equals(type)) {
                String[] waypoint = token.split("[{}]");
                String name = waypoint[0];
                name = name.replace(this.scriptType.getScriptTag(), "");
                name = name.trim();
                if (loadMode == ScriptLoadMode.Init && !this.loadFiles.contains(ScriptManager.instance.currentFileName)) {
                    this.loadFiles.add(ScriptManager.instance.currentFileName);
                }
                if (this.loadData.containsKey(name)) {
                    LoadData<E> data = this.loadData.get(name);
                    if (loadMode == ScriptLoadMode.Init) {
                        data.reloaded = false;
                        ((BaseScriptObject)data.script).InitLoadPP(name);
                        data.scriptBodies.add(token);
                        ((BaseScriptObject)data.script).addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                        ScriptManager.println(this.scriptType, ": Add ScriptBody: '" + data.name + "' " + ((BaseScriptObject)data.script).debugString());
                    } else if (loadMode == ScriptLoadMode.Reload && this.reload) {
                        if (this.scriptType.hasFlag(ScriptType.Flags.NewInstanceOnReload)) {
                            E script = this.createInstance(this.module, name, token);
                            ((BaseScriptObject)script).setModule(this.module);
                            ((BaseScriptObject)script).InitLoadPP(name);
                            data = new LoadData<E>(script);
                            data.scriptBodies.add(token);
                            ((BaseScriptObject)data.script).addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                            data.reloaded = true;
                            data.addedOnReload = false;
                            this.loadData.put(data.name, data);
                            return true;
                        }
                        if (!data.reloaded && !data.scriptBodies.isEmpty()) {
                            data.scriptBodies.clear();
                            ((BaseScriptObject)data.script).resetLoadedScriptBodies();
                        }
                        data.reloaded = true;
                        data.scriptBodies.add(token);
                        ((BaseScriptObject)data.script).addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                        ScriptManager.println(this.scriptType, ": Reload ScriptBody: '" + data.name + "' " + ((BaseScriptObject)data.script).debugString());
                    }
                } else if (loadMode == ScriptLoadMode.Init || this.scriptType.hasFlag(ScriptType.Flags.AllowNewScriptDiscoveryOnReload)) {
                    E script = this.createInstance(this.module, name, token);
                    ((BaseScriptObject)script).setModule(this.module);
                    ((BaseScriptObject)script).InitLoadPP(name);
                    LoadData<E> data = new LoadData<E>(script);
                    data.scriptBodies.add(token);
                    ((BaseScriptObject)data.script).addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                    data.reloaded = loadMode == ScriptLoadMode.Reload;
                    data.addedOnReload = loadMode == ScriptLoadMode.Reload;
                    this.loadData.put(data.name, data);
                    ScriptManager.println(this.scriptType, ": New ScriptBody: '" + data.name + "' " + ((BaseScriptObject)data.script).debugString());
                } else {
                    DebugLog.General.warn("Found new script but was unable to load, possibly due to not being allowed during reload...");
                    DebugLog.log(">>> : Load ScriptBody: '" + name + "', File: " + ScriptManager.instance.currentFileName);
                    if (!this.scriptType.hasFlag(ScriptType.Flags.AllowNewScriptDiscoveryOnReload)) {
                        DebugLog.log(">>> : Discovery of new scripts during reload not allowed for scripts of type: " + String.valueOf((Object)this.scriptType));
                        if (Core.debug) {
                            throw new Exception("Not allowed");
                        }
                    }
                }
                return true;
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            this.hasLoadErrors = true;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void LoadScripts(ScriptLoadMode loadMode) {
        for (LoadData<E> data : this.loadData.values()) {
            try {
                String string = currentScriptObject = data.script != null ? ((BaseScriptObject)data.script).getScriptObjectFullType() : null;
                if (loadMode == ScriptLoadMode.Reload && !data.reloaded) continue;
                data.reloaded = false;
                Object script = data.script;
                if (this.isVerbose()) {
                    DebugLog.General.debugln("[" + this.scriptType.getScriptTag() + "] load script = " + ((BaseScriptObject)script).getScriptObjectName());
                }
                if (loadMode == ScriptLoadMode.Reload && this.scriptType.hasFlag(ScriptType.Flags.ResetOnceOnReload)) {
                    ((BaseScriptObject)script).reset();
                }
                int resetStartIndex = loadMode == ScriptLoadMode.Reload ? 0 : 1;
                for (int index = 0; index < data.scriptBodies.size(); ++index) {
                    String body = data.scriptBodies.get(index);
                    try {
                        if (this.scriptType.hasFlag(ScriptType.Flags.ResetExisting) && index >= resetStartIndex) {
                            ((BaseScriptObject)script).reset();
                        }
                        ((BaseScriptObject)script).Load(data.name, body);
                        ScriptManager.println(this.scriptType, " - Load: '" + data.name + "' " + String.valueOf(data.script));
                        continue;
                    }
                    catch (Exception ex) {
                        if (this.scriptType.hasFlag(ScriptType.Flags.RemoveLoadError)) {
                            DebugLog.log("[" + this.scriptType.getScriptTag() + "] removing script due to load error = " + ((BaseScriptObject)script).getScriptObjectName());
                            script = null;
                        }
                        this.hasLoadErrors = true;
                        ExceptionLogger.logException(ex);
                        break;
                    }
                }
                if (script != null) {
                    if (((BaseScriptObject)script).getObsolete()) {
                        DebugLog.Script.debugln("[" + this.scriptType.getScriptTag() + "] ignoring script, obsolete = " + ((BaseScriptObject)script).getScriptObjectName());
                        continue;
                    }
                    if (!((BaseScriptObject)script).isEnabled()) {
                        DebugLog.Script.debugln("[" + this.scriptType.getScriptTag() + "] ignoring script, disabled = " + ((BaseScriptObject)script).getScriptObjectName());
                        continue;
                    }
                    if (((BaseScriptObject)script).isDebugOnly() && !Core.debug) {
                        DebugLog.Script.debugln("[" + this.scriptType.getScriptTag() + "] ignoring script, is debug only = " + ((BaseScriptObject)script).getScriptObjectName());
                        continue;
                    }
                    ((BaseScriptObject)script).calculateScriptVersion();
                    if (loadMode == ScriptLoadMode.Init || data.addedOnReload) {
                        this.onScriptLoad(loadMode, script);
                        this.scriptMap.put(((BaseScriptObject)script).getScriptObjectName(), script);
                        this.scriptList.add(script);
                        if (((BaseScriptObject)script).getScriptObjectName().contains(".")) {
                            this.dotInName.add(((BaseScriptObject)script).getScriptObjectName());
                        }
                    } else if (loadMode == ScriptLoadMode.Reload) {
                        this.onScriptLoad(loadMode, script);
                    }
                }
                data.addedOnReload = false;
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                this.hasLoadErrors = true;
            }
            finally {
                currentScriptObject = null;
            }
        }
    }

    protected void onScriptLoad(ScriptLoadMode loadMode, E script) {
    }

    protected abstract E getFromManager(String var1);

    protected abstract E getFromModule(String var1, ScriptModule var2);

    public E get(String name) {
        if (name.contains(".") && !this.dotInName.contains(name)) {
            return this.getFromManager(name);
        }
        BaseScriptObject script = (BaseScriptObject)this.scriptMap.get(name);
        if (script != null) {
            return (E)script;
        }
        if (this.scriptType.hasFlag(ScriptType.Flags.SeekImports)) {
            for (int n = 0; n < this.module.imports.size(); ++n) {
                String moduleName = this.module.imports.get(n);
                ScriptModule module = ScriptManager.instance.getModule(moduleName);
                script = this.getFromModule(name, module);
                if (script == null) continue;
                return (E)script;
            }
        }
        return null;
    }

    public static abstract class Template<E extends BaseScriptObject>
    extends ScriptBucket<E> {
        public Template(ScriptModule module, ScriptType scriptType) {
            super(module, scriptType);
            if (!scriptType.isTemplate()) {
                throw new RuntimeException("ScriptType '" + String.valueOf((Object)scriptType) + "' should be template!");
            }
        }

        @Override
        public boolean CreateFromTokenPP(ScriptLoadMode loadMode, String type, String token) {
            try {
                if ("template".equals(type)) {
                    String[] waypoint = token.split("[{}]");
                    String typeAndName = waypoint[0];
                    String[] split = (typeAndName = typeAndName.replace("template", "")).trim().split("\\s+");
                    if (split.length == 2) {
                        String type1 = split[0].trim();
                        String name = split[1].trim();
                        if (this.scriptType.getScriptTag().equals(type1)) {
                            Object script = this.createInstance(this.module, name, token);
                            ((BaseScriptObject)script).InitLoadPP(name);
                            this.scriptMap.put(((BaseScriptObject)script).getScriptObjectName(), script);
                            if (((BaseScriptObject)script).getScriptObjectName().contains(".")) {
                                this.dotInName.add(((BaseScriptObject)script).getScriptObjectName());
                            }
                            ScriptManager.println(this.scriptType, "Loaded template: " + ((BaseScriptObject)script).getScriptObjectName());
                            return true;
                        }
                    }
                }
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                this.hasLoadErrors = true;
            }
            return false;
        }

        @Override
        public void LoadScripts(ScriptLoadMode loadMode) {
            this.scriptList.clear();
            this.scriptList.addAll(this.scriptMap.values());
        }
    }

    private static class LoadData<E extends BaseScriptObject> {
        private final String name;
        private final ArrayList<String> scriptBodies = new ArrayList();
        private final E script;
        private boolean reloaded;
        private boolean addedOnReload;

        private LoadData(E script) {
            this.name = ((BaseScriptObject)script).getScriptObjectName();
            this.script = script;
        }
    }
}

