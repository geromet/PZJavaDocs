/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehicleEngineRpmType {
    FIREBIRD("firebird"),
    VAN("van");

    private final String id;

    private VehicleEngineRpmType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

