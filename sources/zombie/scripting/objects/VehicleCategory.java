/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehicleCategory {
    BODYWORK("bodywork"),
    BRAKES("brakes"),
    DOOR("door"),
    ENGINE("engine"),
    GASTANK("gastank"),
    LIGHTS("lights"),
    NODISPLAY("nodisplay"),
    SEAT("seat"),
    SUSPENSION("suspension"),
    TIRE("tire");

    private final String id;

    private VehicleCategory(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

