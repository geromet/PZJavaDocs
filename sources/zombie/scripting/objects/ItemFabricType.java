/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum ItemFabricType {
    COTTON("Cotton"),
    DENIM("Denim"),
    LEATHER("Leather");

    private final String id;

    private ItemFabricType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

