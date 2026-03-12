/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.zombie;

import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.iso.worldgen.zombie.ZombieVoronoiEntry;

public class ZombieVoronoiReader {
    public List<ZombieVoronoiEntry> getEntries(KahluaTable mainTable) {
        ArrayList<ZombieVoronoiEntry> zombieVoronoiEntries = new ArrayList<ZombieVoronoiEntry>();
        KahluaTableIterator iterZombieVoronoi = mainTable.iterator();
        while (iterZombieVoronoi.advance()) {
            KahluaTable table = (KahluaTable)iterZombieVoronoi.getValue();
            int points = this.loadInteger(table.rawget("points"), 1);
            String closest = this.loadString(table.rawget("closest"), "SECOND_MINUS_FIRST");
            double scale = this.loadDouble(table.rawget("scale"), 16.0);
            double cutoff = this.loadDouble(table.rawget("cutoff"), 0.2);
            zombieVoronoiEntries.add(new ZombieVoronoiEntry(points, closest, scale, cutoff));
        }
        return zombieVoronoiEntries;
    }

    private int loadInteger(Object object, int defaultValue) {
        return object == null ? defaultValue : ((Double)object).intValue();
    }

    private Double loadDouble(Object object, Double defaultValue) {
        return object == null ? defaultValue : (Double)object;
    }

    private String loadString(Object object, String defaultValue) {
        return object == null ? defaultValue : (String)object;
    }
}

