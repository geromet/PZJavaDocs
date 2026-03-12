/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum EntityCategory {
    BARRICADES("Barricades"),
    BLACKSMITHING("Blacksmithing"),
    CARPENTRY("Carpentry"),
    DEBUG("Debug"),
    FARMING("Farming"),
    FURNITURE("Furniture"),
    MASONRY("Masonry"),
    MISCELLANEOUS("Miscellaneous"),
    OUTDOORS("Outdoors"),
    POTTERY("Pottery"),
    WELDING("Welding"),
    WALL_COVERING("Wall Coverings");

    private final String id;

    private EntityCategory(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

