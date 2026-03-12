/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum WeaponReloadType {
    BOLT_ACTION("boltaction"),
    BOLT_ACTION_NO_MAG("boltactionnomag"),
    DOUBLE_BARREL_SHOTGUN("doublebarrelshotgun"),
    DOUBLE_BARREL_SHOTGUN_SAWN("doublebarrelshotgunsawn"),
    HANDGUN("handgun"),
    REVOLVER("revolver"),
    SHOTGUN("shotgun");

    private final String id;

    private WeaponReloadType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

