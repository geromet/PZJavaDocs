/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum FaceType {
    N("N"),
    E("E"),
    S("S"),
    W("W"),
    SINGLE("SINGLE"),
    W_OPEN("W_OPEN"),
    N_OPEN("N_OPEN");

    private final String id;

    private FaceType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

