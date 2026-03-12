/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.debug.DebugLog;
import zombie.text.templating.ReplaceList;
import zombie.text.templating.ReplaceProvider;
import zombie.text.templating.ReplaceSingle;

public class ReplaceProviderLua
extends ReplaceProvider {
    private static final ConcurrentLinkedDeque<ReplaceSingle> pool_single = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<ReplaceList> pool_list = new ConcurrentLinkedDeque();
    private static final ConcurrentLinkedDeque<ReplaceProviderLua> pool = new ConcurrentLinkedDeque();

    private static ReplaceSingle alloc_single() {
        ReplaceSingle single = pool_single.poll();
        if (single == null) {
            single = new ReplaceSingle();
        }
        return single;
    }

    private static void release_single(ReplaceSingle single) {
        pool_single.offer(single);
    }

    private static ReplaceList alloc_list() {
        ReplaceList lua = pool_list.poll();
        if (lua == null) {
            lua = new ReplaceList();
        }
        return lua;
    }

    private static void release_list(ReplaceList list) {
        list.getReplacements().clear();
        pool_list.offer(list);
    }

    protected static ReplaceProviderLua Alloc() {
        ReplaceProviderLua provider = pool.poll();
        if (provider == null) {
            provider = new ReplaceProviderLua();
        }
        provider.reset();
        return provider;
    }

    private void reset() {
        for (Map.Entry entry : this.keys.entrySet()) {
            if (entry.getValue() instanceof ReplaceList) {
                ReplaceProviderLua.release_list((ReplaceList)entry.getValue());
                continue;
            }
            ReplaceProviderLua.release_single((ReplaceSingle)entry.getValue());
        }
        this.keys.clear();
    }

    public void release() {
        this.reset();
        pool.offer(this);
    }

    public void fromLuaTable(KahluaTableImpl table) {
        for (Map.Entry<Object, Object> entry : table.delegate.entrySet()) {
            if (!(entry.getKey() instanceof String)) continue;
            if (entry.getValue() instanceof String) {
                this.addKey((String)entry.getKey(), (String)entry.getValue());
                continue;
            }
            Object object = entry.getValue();
            if (!(object instanceof KahluaTableImpl)) continue;
            KahluaTableImpl subTable = (KahluaTableImpl)object;
            ReplaceList replaceList = ReplaceProviderLua.alloc_list();
            for (int i = 1; i < subTable.len() + 1; ++i) {
                replaceList.getReplacements().add((String)subTable.rawget(i));
            }
            if (!replaceList.getReplacements().isEmpty()) {
                this.addReplacer((String)entry.getKey(), replaceList);
                continue;
            }
            DebugLog.log("ReplaceProvider -> key '" + String.valueOf(entry.getKey()) + "' contains no entries, ignoring.");
            ReplaceProviderLua.release_list(replaceList);
        }
    }

    @Override
    public void addKey(String key, String value) {
        ReplaceSingle single = ReplaceProviderLua.alloc_single();
        single.setValue(value);
        this.addReplacer(key, single);
    }
}

