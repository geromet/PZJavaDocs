/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum AnimalFeedType {
    ANIMAL_FEED("AnimalFeed"),
    GRASS("Grass"),
    NUTS("Nuts"),
    SEEDS("Seeds");

    private final String id;

    private AnimalFeedType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

