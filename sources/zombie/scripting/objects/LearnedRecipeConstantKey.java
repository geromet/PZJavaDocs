/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public record LearnedRecipeConstantKey(String id) {
    public static final LearnedRecipeConstantKey ADVANCED_MECHANICS = new LearnedRecipeConstantKey("Advanced Mechanics");
    public static final LearnedRecipeConstantKey BASIC_MECHANICS = new LearnedRecipeConstantKey("Basic Mechanics");
    public static final LearnedRecipeConstantKey GENERATOR = new LearnedRecipeConstantKey("Generator");
    public static final LearnedRecipeConstantKey HERBALIST = new LearnedRecipeConstantKey("Herbalist");
    public static final LearnedRecipeConstantKey INTERMEDIATE_MECHANICS = new LearnedRecipeConstantKey("Intermediate Mechanics");

    @Override
    public String toString() {
        return this.id;
    }
}

