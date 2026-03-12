/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.enums;

public enum SelectorType {
    Default(false),
    Zone(true),
    Room(true),
    Container(true),
    Tile(true),
    WorldAge(false),
    Situated(false),
    Vehicle(true),
    OnCreate(false),
    None(false);

    private final boolean allowChaining;

    private SelectorType(boolean allowChaining) {
        this.allowChaining = allowChaining;
    }

    public boolean isAllowChaining() {
        return this.allowChaining;
    }
}

