/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.sprite;

public enum RenderStateSlot {
    Populating(0),
    Ready(1),
    Rendering(2);

    private final int index;

    private RenderStateSlot(int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }

    public int count() {
        return 3;
    }
}

