/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum CullFace {
    BACK("Back"),
    FRONT("Front"),
    NONE("None");

    private final String id;

    private CullFace(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

