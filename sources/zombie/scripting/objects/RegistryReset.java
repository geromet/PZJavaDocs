/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.scripting.objects.Registries;
import zombie.scripting.objects.Registry;
import zombie.scripting.objects.ResourceLocation;

public class RegistryReset {
    public static void resetAll() {
        for (Registry<?> registry : Registries.REGISTRY) {
            registry.reset();
        }
        Registries.REGISTRY.reset();
    }

    public static ResourceLocation createLocation(String id, boolean allowsBaseNamespace) {
        ResourceLocation rl = ResourceLocation.of(id);
        if (!allowsBaseNamespace && "base".equals(rl.getNamespace())) {
            throw new IllegalArgumentException(String.format("Default namespace '%s' is not allowed!", rl));
        }
        return rl;
    }
}

