/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.enums;

public enum RootType {
    Attribute(true),
    FluidContainer(true),
    LuaFunc(false);

    private final boolean requiresId;

    private RootType(boolean requiresId) {
        this.requiresId = requiresId;
    }

    public boolean isRequiresId() {
        return this.requiresId;
    }
}

