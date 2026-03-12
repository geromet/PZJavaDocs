/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum WeaponFireMode {
    AUTO("Auto"),
    SINGLE("Single");

    private final String id;

    private WeaponFireMode(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

