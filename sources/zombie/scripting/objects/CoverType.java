/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum CoverType {
    HARDCOVER,
    SOFTCOVER,
    BOTH;


    public boolean matches(CoverType coverType) {
        return this == BOTH || coverType == BOTH || this == coverType;
    }
}

