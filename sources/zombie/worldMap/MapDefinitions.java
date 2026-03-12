/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.util.list.PZArrayUtil;

public final class MapDefinitions {
    private static MapDefinitions instance;
    private final ArrayList<String> definitions = new ArrayList();

    public static MapDefinitions getInstance() {
        if (instance == null) {
            instance = new MapDefinitions();
        }
        return instance;
    }

    public String pickRandom() {
        if (this.definitions.isEmpty()) {
            this.initDefinitionsFromLua();
        }
        if (this.definitions.isEmpty()) {
            return "Default";
        }
        return PZArrayUtil.pickRandom(this.definitions);
    }

    private void initDefinitionsFromLua() {
        Object object = LuaManager.env.rawget("LootMaps");
        if (!(object instanceof KahluaTable)) {
            return;
        }
        KahluaTable lootMaps = (KahluaTable)object;
        Object object2 = lootMaps.rawget("Init");
        if (!(object2 instanceof KahluaTable)) {
            return;
        }
        KahluaTable init = (KahluaTable)object2;
        KahluaTableIterator it = init.iterator();
        while (it.advance()) {
            Object object3 = it.getKey();
            if (!(object3 instanceof String)) continue;
            String mapID = (String)object3;
            this.definitions.add(mapID);
        }
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        MapDefinitions.instance.definitions.clear();
        instance = null;
    }
}

