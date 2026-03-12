/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum SoundCategory {
    ANIMALS("Animals"),
    AUTO("AUTO"),
    GENERAL("General"),
    ITEM("Item"),
    META("Meta"),
    MUSIC("Music"),
    OBJECT("Object"),
    PLAYER("Player"),
    UI("UI"),
    VEHICLE("Vehicle"),
    WORLD("World"),
    ZOMBIE("Zombie");

    private final String id;

    private SoundCategory(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

