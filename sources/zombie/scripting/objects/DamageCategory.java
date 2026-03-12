/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum DamageCategory {
    SLASH("Slash");

    private final String id;

    private DamageCategory(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

