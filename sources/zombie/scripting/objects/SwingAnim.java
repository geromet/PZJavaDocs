/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum SwingAnim {
    BAT("Bat"),
    HANDGUN("Handgun"),
    HEAVY("Heavy"),
    RIFLE("Rifle"),
    SHOVE("Shove"),
    SPEAR("Spear"),
    STAB("Stab"),
    STONE("Stone"),
    THROW("Throw");

    private final String id;

    private SwingAnim(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

