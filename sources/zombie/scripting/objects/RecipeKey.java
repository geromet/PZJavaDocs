/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.scripting.objects.Registries;
import zombie.scripting.objects.Registry;
import zombie.scripting.objects.ResourceLocation;

public interface RecipeKey {
    public String getTranslationName();

    public ResourceLocation getRegistryId();

    public static RecipeKey fromId(ResourceLocation id) {
        for (Registry<? extends RecipeKey> registry : Registries.getAllRecipeRegistries()) {
            if (!registry.contains(id)) continue;
            return registry.get(id);
        }
        throw new IllegalArgumentException("Unknown recipe ID: " + String.valueOf(id));
    }
}

