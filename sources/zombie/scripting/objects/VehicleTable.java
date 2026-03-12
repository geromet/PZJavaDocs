/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehicleTable {
    HEADLIGHT("headlight"),
    INSTALL("install"),
    UNINSTALL("uninstall");

    private final String id;

    private VehicleTable(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

