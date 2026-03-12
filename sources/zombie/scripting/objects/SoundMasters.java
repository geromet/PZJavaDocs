/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum SoundMasters {
    AMBIENT("Ambient"),
    MUSIC("Music"),
    VEHICLE_ENGINE("VehicleEngine");

    private final String id;

    private SoundMasters(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

