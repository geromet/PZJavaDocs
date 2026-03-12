/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehiclePosition {
    OUTSIDE("outside"),
    INSIDE("inside"),
    OUTSIDE2("outside2");

    private final String id;

    private VehiclePosition(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

