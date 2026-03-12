/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum VehicleAttachment {
    TRAILER("trailer"),
    TRAILERFRONT("trailerfront");

    private final String id;

    private VehicleAttachment(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

