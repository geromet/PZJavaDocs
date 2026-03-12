/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.zones;

import se.krka.kahlua.vm.KahluaTable;
import zombie.iso.zones.Zone;

public class WorldGenZone
extends Zone {
    private boolean rocks = true;

    public WorldGenZone(String name, String type, int x, int y, int z, int w, int h, KahluaTable properties) {
        super(name, type, x, y, z, w, h);
        Object o;
        if (properties != null && (o = properties.rawget("Rocks")) instanceof Boolean) {
            Boolean b = (Boolean)o;
            this.rocks = b;
        }
    }

    public boolean getRocks() {
        return this.rocks;
    }
}

