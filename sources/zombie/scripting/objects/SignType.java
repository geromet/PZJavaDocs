/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum SignType {
    SIGN_SKULL(36),
    SIGN_RIGHTARROW(32),
    SIGN_LEFTARROW(33),
    SIGN_DOWNARROW(34),
    SIGN_UPARROW(35);

    private final Integer index;

    private SignType(int index) {
        this.index = index;
    }

    public Integer getSpriteIndex() {
        return this.index;
    }

    public String getSpriteName() {
        return "constructedobjects_signs_01_" + this.index;
    }

    public String toString() {
        return this.index.toString();
    }

    public static SignType typeOf(int index) {
        for (SignType type : SignType.values()) {
            if (type.getSpriteIndex() != index) continue;
            return type;
        }
        throw new IllegalArgumentException("No enum constant SignType with value " + index);
    }
}

