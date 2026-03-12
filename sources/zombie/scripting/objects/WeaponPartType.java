/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum WeaponPartType {
    CANON("Canon"),
    CLIP("Clip"),
    RECOIL_PAD("RecoilPad"),
    SCOPE("Scope"),
    SLING("Sling"),
    STOCK("Stock");

    private final String id;

    private WeaponPartType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

