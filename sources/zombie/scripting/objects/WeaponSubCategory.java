/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum WeaponSubCategory {
    FIREARM("Firearm"),
    SPEAR("Spear"),
    STAB("Stab"),
    SWINGING("Swinging");

    private final String id;

    private WeaponSubCategory(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

