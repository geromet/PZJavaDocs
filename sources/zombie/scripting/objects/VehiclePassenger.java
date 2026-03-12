/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehiclePassenger {
    ALL("*"),
    FRONT_LEFT("FrontLeft"),
    FRONT_RIGHT("FrontRight"),
    MIDDLE_LEFT("MiddleLeft"),
    MIDDLE_RIGHT("MiddleRight"),
    REAR_LEFT("RearLeft"),
    REAR_RIGHT("RearRight");

    private final String id;

    private VehiclePassenger(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

