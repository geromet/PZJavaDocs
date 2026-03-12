/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehicleMechanicArea {
    BACK("Back"),
    ENGINE("Engine"),
    INTERIOR("Interior"),
    LEFT("Left"),
    RIGHT("Right"),
    UNDER("Under");

    private final String id;

    private VehicleMechanicArea(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

