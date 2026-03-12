/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum DigType {
    HOE("Hoe"),
    PICK_AXE("PickAxe"),
    SHOVEL("Shovel"),
    TROWEL("Trowel");

    private final String id;

    private DigType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

