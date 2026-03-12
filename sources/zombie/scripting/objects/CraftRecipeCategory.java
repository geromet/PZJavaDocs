/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum CraftRecipeCategory {
    ARMOR("Armor"),
    ASSEMBLY("Assembly"),
    BLACKSMITHING("Blacksmithing"),
    BLADE("Blade"),
    CARPENTRY("Carpentry"),
    CARVING("Carving"),
    COOKING("Cooking"),
    COOKWARE("Cookware"),
    ELECTRICAL("Electrical"),
    FARMING("Farming"),
    FISHING("Fishing"),
    GLASSMAKING("Glassmaking"),
    KNAPPING("Knapping"),
    MASONRY("Masonry"),
    MEDICAL("Medical"),
    METALWORKING("Metalworking"),
    MISCELLANEOUS("Miscellaneous"),
    PACKING("Packing"),
    POTTERY("Pottery"),
    REPAIR("Repair"),
    TAILORING("Tailoring"),
    TOOLS("Tools"),
    WEAPONRY("Weaponry");

    private final String id;

    private CraftRecipeCategory(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

