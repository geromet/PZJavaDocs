/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum CustomContextMenu {
    CHEW("Chew"),
    DRINK("Drink"),
    EAT("Eat"),
    SMOKE("Smoke"),
    SNIFF("Sniff"),
    TAKE("Take");

    private final String id;

    private CustomContextMenu(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

