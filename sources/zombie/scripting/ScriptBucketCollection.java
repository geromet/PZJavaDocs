/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptBucket;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

public abstract class ScriptBucketCollection<E extends BaseScriptObject> {
    private final ScriptManager scriptManager;
    private final ScriptType scriptType;
    private final HashMap<ScriptModule, ScriptBucket<E>> map = new HashMap();
    private final ArrayList<ScriptModule> scriptModules = new ArrayList();
    private final ArrayList<ScriptBucket<E>> scriptBuckets = new ArrayList();
    private final ArrayList<E> allScripts = new ArrayList();
    private final HashMap<String, E> fullTypeToScriptMap = new HashMap();
    protected final ArrayList<String> loadFiles = new ArrayList();

    public ScriptBucketCollection(ScriptManager scriptManager, ScriptType scriptType) {
        this.scriptManager = scriptManager;
        this.scriptType = scriptType;
    }

    public ScriptType getScriptType() {
        return this.scriptType;
    }

    public boolean isTemplate() {
        return this.scriptType.isTemplate();
    }

    public void reset() {
        this.map.clear();
        this.scriptModules.clear();
        this.scriptBuckets.clear();
        this.allScripts.clear();
        this.fullTypeToScriptMap.clear();
        this.loadFiles.clear();
    }

    public boolean hasFullType(String type) {
        return this.fullTypeToScriptMap.containsKey(type);
    }

    public E getFullType(String type) {
        return (E)((BaseScriptObject)this.fullTypeToScriptMap.get(type));
    }

    public HashMap<String, E> getFullTypeToScriptMap() {
        return this.fullTypeToScriptMap;
    }

    public void setReloadBuckets(boolean bReload) {
        for (ScriptBucket<E> bucket : this.scriptBuckets) {
            bucket.setReload(bReload);
        }
    }

    public void registerModule(ScriptModule module) {
        this.scriptModules.add(module);
        ScriptBucket<E> bucket = this.getBucketFromModule(module);
        if (this.scriptType != bucket.scriptType) {
            throw new RuntimeException("ScriptType does not match bucket ScriptType");
        }
        this.scriptBuckets.add(bucket);
        this.map.put(module, bucket);
    }

    public abstract ScriptBucket<E> getBucketFromModule(ScriptModule var1);

    public E getScript(String name) {
        if (this.scriptType.hasFlag(ScriptType.Flags.CacheFullType) && name.contains(".") && this.fullTypeToScriptMap.containsKey(name)) {
            return (E)((BaseScriptObject)this.fullTypeToScriptMap.get(name));
        }
        ScriptModule module = !name.contains(".") ? this.scriptManager.getModule("Base") : this.scriptManager.getModule(name);
        if (module == null) {
            return null;
        }
        ScriptBucket<E> bucket = this.map.get(module);
        return bucket.get(ScriptManager.getItemName(name));
    }

    public ArrayList<E> getAllScripts() {
        if (!this.scriptType.hasFlag(ScriptType.Flags.Clear) && !this.allScripts.isEmpty()) {
            return this.allScripts;
        }
        this.allScripts.clear();
        for (int i = 0; i < this.scriptBuckets.size(); ++i) {
            ScriptBucket<E> bucket = this.scriptBuckets.get(i);
            if (bucket.module.disabled) continue;
            if (this.scriptType.hasFlag(ScriptType.Flags.FromList)) {
                this.allScripts.addAll(bucket.scriptList);
                continue;
            }
            this.allScripts.addAll(bucket.scriptMap.values());
        }
        this.onSortAllScripts(this.allScripts);
        return this.allScripts;
    }

    public void onSortAllScripts(ArrayList<E> scripts) {
    }

    public void LoadScripts(ScriptLoadMode loadMode) {
        DebugLog.Script.trace("Load Scripts: " + this.scriptType.toString() + ", loadMode = " + String.valueOf((Object)loadMode));
        for (ScriptBucket<E> scriptBucket : this.scriptBuckets) {
            scriptBucket.LoadScripts(loadMode);
            for (String loadFile : scriptBucket.loadFiles) {
                if (this.loadFiles.contains(loadFile)) continue;
                this.loadFiles.add(loadFile);
            }
            for (BaseScriptObject script : scriptBucket.getScriptList()) {
                this.fullTypeToScriptMap.put(script.getScriptObjectFullType(), script);
            }
        }
    }

    public void PreReloadScripts() throws Exception {
        ScriptManager.println(this.scriptType, "<- PreReloadScripts ->");
        for (BaseScriptObject script : this.getAllScripts()) {
            script.PreReload();
        }
        this.allScripts.clear();
    }

    public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
    }

    public boolean hasLoadErrors() {
        return this.hasLoadErrors(false);
    }

    public boolean hasLoadErrors(boolean onlyCritical) {
        for (ScriptBucket<E> bucket : this.scriptBuckets) {
            if (!bucket.isHasLoadErrors() || onlyCritical && !this.scriptType.isCritical()) continue;
            return true;
        }
        return false;
    }

    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        ScriptManager.println(this.scriptType, "<- OnScriptsLoaded ->");
        for (BaseScriptObject script : this.getAllScripts()) {
            script.OnScriptsLoaded(loadMode);
        }
    }

    public void OnLoadedAfterLua() throws Exception {
        ScriptManager.println(this.scriptType, "<- OnLoadedAfterLua ->");
        for (BaseScriptObject script : this.getAllScripts()) {
            script.OnLoadedAfterLua();
        }
    }

    public void OnPostTileDefinitions() throws Exception {
        ScriptManager.println(this.scriptType, "<- OnPostTileDefinitions ->");
    }

    public void OnPostWorldDictionaryInit() throws Exception {
        ScriptManager.println(this.scriptType, "<- OnPostWorldDictionaryInit ->");
        for (BaseScriptObject script : this.getAllScripts()) {
            script.OnPostWorldDictionaryInit();
        }
    }
}

