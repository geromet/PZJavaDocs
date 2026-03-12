/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum PourType {
    BOWL("bowl"),
    BUCKET("Bucket"),
    KETTLE("Kettle"),
    MUG("Mug"),
    POT("Pot"),
    SAUCE_PAN("saucepan"),
    WATERING_CAN("wateringcan");

    private final String id;

    private PourType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

