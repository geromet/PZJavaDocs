/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehicleItemEquip {
    PRIMARY("primary"),
    SECONDARY("secondary");

    private final String id;

    private VehicleItemEquip(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

