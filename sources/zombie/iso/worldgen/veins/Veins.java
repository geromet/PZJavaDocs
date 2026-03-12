/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.veins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import zombie.debug.DebugLog;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.veins.OreVein;
import zombie.iso.worldgen.veins.OreVeinConfig;

public class Veins {
    private final Map<String, List<OreVein>> cache = new HashMap<String, List<OreVein>>();
    private final Map<String, OreVeinConfig> config;

    public Veins(Map<String, OreVeinConfig> config) {
        this.config = config;
    }

    public List<OreVein> get(int cellX, int cellY) {
        if (this.cache.containsKey(cellX + "_" + cellY)) {
            return this.cache.get(cellX + "_" + cellY);
        }
        Random rnd = WorldGenParams.INSTANCE.getRandom(cellX, cellY);
        ArrayList<OreVein> ret = new ArrayList<OreVein>();
        for (OreVeinConfig subConfig : this.config.values()) {
            if (rnd.nextFloat() > subConfig.getProbability()) continue;
            ret.add(new OreVein(cellX, cellY, subConfig, rnd));
        }
        this.cache.put(cellX + "_" + cellY, ret);
        if (!ret.isEmpty()) {
            DebugLog.log(((Object)ret).toString());
        }
        return ret;
    }
}

