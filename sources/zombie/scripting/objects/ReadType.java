/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum ReadType {
    BOOK("book"),
    NEWSPAPER("newspaper"),
    PHOTO("photo");

    private final String id;

    private ReadType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

