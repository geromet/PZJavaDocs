/*
 * Decompiled with CFR 0.152.
 */
package zombie.combat;

public enum ShotDirection {
    NORTH("North"),
    SOUTH("South"),
    LEFT("Left"),
    RIGHT("Right");

    private final String value;

    private ShotDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}

