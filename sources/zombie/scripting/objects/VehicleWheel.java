/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehicleWheel {
    REAR_LEFT("RearLeft"),
    REAR_RIGHT("RearRight"),
    FRONT_RIGHT("FrontRight"),
    FRONT_LEFT("FrontLeft");

    private final String id;

    private VehicleWheel(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

