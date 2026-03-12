/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas;

import zombie.iso.areas.IsoBuilding;

public final class BuildingScore {
    public float weapons;
    public float food;
    public float wood;
    public float defense;
    public IsoBuilding building;
    public int size;
    public int safety;

    public BuildingScore(IsoBuilding b) {
        this.building = b;
    }
}

